import groovy.json.JsonSlurperClassic
import hudson.Util
// Given a set of launched Helix runs,
// wait for them to finish.  If running a PR, also reports the
// the state of the runs on the PR.
// Expects a json blob with the form:
// [
//  {
//      CorrelationId
//      QueueId
//      QueueTimeUtc
//  },
//  {
//      CorrelationId
//      QueueId
//      QueueTimeUtc
//  }
// ]

def call (def helixRunsBlob, String prStatusPrefix) {
    // Parallel stages that wait for the runs.
    def helixRunTasks = [:]
    def correlationIdUrlMap = [:]
    def helixRunKeys = [:]
    def failedRunMap = [:]
    def passed = true

    // Wrap in the default timeout of 15 mins
    timeout(15) {
        for (int i = 0; i < helixRunsBlob.size(); i++) {
            waitUntil(minRecurrencePeriod: 60, maxRecurrencePeriod: 180, unit: 'SECONDS') {
                try {
                    def currentRun = helixRunsBlob[i];
                    def queueId = currentRun['QueueId']
                    def correlationId = currentRun['CorrelationId']
                    def statusUrl = "https://helix.dot.net/api/2017-04-14/jobs/${correlationId}/details"
                    def statusResponse = httpRequest consoleLogResponseBody: true, url: statusUrl
                    assert statusResponse != null
                    assert statusResponse.content != null
                    def statusContent = (new JsonSlurperClassic()).parseText(statusResponse.content)
                    def mcResultsUrl = "https://mc.dot.net/#/user/${getEncodedUrl(statusContent.Creator)}/${getEncodedUrl(statusContent.Source)}/${getEncodedUrl(statusContent.Type)}/${getEncodedUrl(statusContent.Build)}"

                    // Attempt to use the property bag (arch and config), if available, to make the helix keys to the task map more descriptive.
                    helixRunKeys[correlationId] = queueId
                    correlationIdUrlMap[correlationId] = mcResultsUrl

                    if (statusContent.Properties != null) {
                        if (statusContent.Properties.architecture != null) {
                            helixRunKeys[correlationId] += " - ${statusContent.Properties.architecture}"
                        }
                        if (statusContent.Properties.configuration != null) {
                            helixRunKeys[correlationId] += " - ${statusContent.Properties.configuration}"
                        }
                    }
                }
                catch (Exception ex) {
                    println(ex.toString());
                    println(ex.getMessage().toString());
                    println(ex.getStackTrace().toString());
                    return false
                }
                return true
            }
        }
    }
    // Dedupe the values in the run keys map, then map those keys into the results map
    def tempRunKeys = [:]
    helixRunKeys.each { k, v ->
        def allEntries = helixRunKeys.findAll { it.value == v }
        if (allEntries.size() > 1) {
            tempRunKeys.putAll(allEntries.collectEntries { a,b ->
                [a, "${b} - (${a})"]
            })
        } else {
            tempRunKeys.putAll(allEntries)
        }
    }
    helixRunKeys = tempRunKeys
    mcUrlMap[helixRunKeys[correlationId]] = mcResultsUrl
    addSummaryLink('Test Run Results', mcUrlMap)

    for (int i = 0; i < helixRunsBlob.size(); i++) {
        def currentRun = helixRunsBlob[i];
        def queueId = currentRun['QueueId']
        def correlationId = currentRun['CorrelationId']

        helixRunTasks[helixRunKeys[correlationId]] = {
            // State to minimize status updates.
            // 0 = Not yet updated/started
            // 1 = Pending updated
            // 2 = Started updated
            int state = 0;
            timestamps {
                // Wait until the Helix runs complete. Wait up to 5 mins between checks
                waitUntil(minRecurrencePeriod: 120, maxRecurrencePeriod: 300, unit: 'SECONDS') {
                    try {
                        // Check the state against the Helix API
                        def statusUrl = "https://helix.dot.net/api/2017-04-14/jobs/${correlationId}/details"
                        def statusResponse = httpRequest consoleLogResponseBody: true, url: statusUrl
                        assert statusResponse != null
                        assert statusResponse.content != null
                        def statusContent = (new JsonSlurperClassic()).parseText(statusResponse.content)

                        // If the job info hasn't been propagated to the helix api, then we need to wait around.
                        boolean isNotStarted = statusContent.JobList == null
                        boolean isPending = !isNotStarted && statusContent.WorkItems.Running == 0 && statusContent.WorkItems.Finished == 0
                        boolean isFinished = !isNotStarted && statusContent.WorkItems.Unscheduled == 0 && statusContent.WorkItems.Waiting == 0 && statusContent.WorkItems.Running == 0
                        boolean someFinished = statusContent.WorkItems.Finished > 0
                        boolean isRunning = !isNotStarted && !isPending && !isFinished
                        // Construct the link to the results page.
                        def mcResultsUrl = "https://mc.dot.net/#/user/${getEncodedUrl(statusContent.Creator)}/${getEncodedUrl(statusContent.Source)}/${getEncodedUrl(statusContent.Type)}/${getEncodedUrl(statusContent.Build)}"
                        statusContent = null

                        def resultValue
                        def subMessage
                        // If it's running, grab the current state of results too
                        if (isRunning || isFinished) {
                            // Check the results
                            // We check the results by going to the API aggregating by correlation id
                            def resultsUrl = "https://helix.dot.net/api/2017-04-14/aggregate/jobs?groupBy=job.name&maxResultSets=1&filter.name=${correlationId}"

                            def resultsResponse = httpRequest consoleLogResponseBody: true, url: resultsUrl
                            def resultsContent = (new JsonSlurperClassic()).parseText(resultsResponse.content)

                            // Example content
                            // If the data isn't complete, then Analysis will be empty.
                            /*[
                                {
                                    "Key": {
                                        "job.name": "0715528c-a31f-46ac-963e-8679c5880dc8"
                                    },
                                    "Data": {
                                        "Analysis": [
                                            {
                                                "Name": "xunit",
                                                "Status": {
                                                    "pass": 374595,
                                                    "fail": 4,
                                                    "skip": 234
                                                }
                                            }
                                        ],
                                        "WorkItemStatus": {
                                            "run": 1,
                                            "pass": 204,
                                            "fail":1
                                        }
                                    }
                                }
                            ]*/
                            assert resultsContent.size() == 1 : "No results found for helix results API"
                            assert resultsContent[0].Data != null : "No data found in first result for helix results API"

                            def resultData = resultsContent[0].Data
                            def workItemStatus = resultData.WorkItemStatus
                            def analyses = resultData.Analysis

                            if (workItemStatus.fail) {
                                resultValue = "FAILURE"
                                passed = false
                                failedRunMap[helixRunKeys[correlationId]] = mcResultsUrl
                                subMessage = "Catastrophic Failure: ${workItemStatus.fail} work items failed"
                            } else if (workItemStatus.none) {
                                resultValue = "PROCESSING XUNIT"
                                isFinished = false
                                isRunning = true
                                subMessage = "Processing XUnit Results: ${workItemStatus.none} remaining"
                            } else if (analyses.size() > 0) {
                                assert analyses.size() == 1 : "More than one set of analysis results"
                                def analysis = analyses[0]
                                assert analysis.Name == "xunit" : "Data in results api not xunit format"
                                assert analysis.Status != null : "No status found in Analysis section"
                                def status = analysis.Status
                                assert status.pass != null ||
                                    status.fail != null ||
                                    status.skip != null : "Expected at least one of pass/fail/skip"

                                def passedTests = status.pass ?: 0
                                def failedTests = status.fail ?: 0
                                def skippedTests  = status.skip ?: 0
                                def totalTests = passedTests + failedTests + skippedTests

                                if (failedTests > 0) {
                                    resultValue = "FAILURE"
                                    passed = false
                                    failedRunMap[helixRunKeys[correlationId]] = mcResultsUrl
                                    subMessage = "Failed ${failedTests}/${totalTests} (${skippedTests} skipped)"
                                }
                                else {
                                    resultValue = "SUCCESS"
                                    subMessage = "Passed ${passedTests} (${skippedTests} skipped)"
                                }
                            } else {
                                subMessage = "No results yet"
                            }

                            resultsContent = null

                            echo "Info: ${subMessage}"
                        }

                        // We can also grab the info necessary to construct the link for Mission Control from this API.

                        if (isPending && state == 0) {
                            state = 1
                        }
                        else if (isRunning) {
                            state = 2
                        }
                        else if (isFinished) {
                            state = 3
                            return true
                        }
                    }
                    catch (Exception ex) {
                        println(ex.toString());
                        println(ex.getMessage());
                        println(ex.getStackTrace());
                        println('Allowing retry to occur...')
                    }
                    return false
                }
            }
        }
    }
    stage('Execute Tests') {
        // Set timeout to 240 minutes to avoid the accidental job getting stuck
        timeout(720) {
            parallel helixRunTasks
        }
        if (!passed) {
            addSummaryLink('Failed Test Runs', failedRunMap, true)
            error "Test leg failure. Please check status page"
        }
    }
}
