package jobs.generation;

class JobReport {
    class CronTriggerInfo {
        String cronString
        String[] branches
        boolean alwaysRuns
    }
    class PRTriggerInfo {
        String context
        String triggerPhrase
        String[] branches
        boolean isDefault
    }
    class PushTriggerInfo {
        String[] branches
    }

    def cronTriggeredJobs = [:]
    def prTriggeredJobs = [:]
    def pushTriggeredJobs = [:]
    def prJobs = []
    def overallJobs = []
    def referencedJobs = []
    def targetBranchMap = [String:String[]]

    // Sets a job as being referenced.  The ref might have folder names in it.
    // This makes things a bit tricky, since if they generated folders then
    // we could have jobs in two separate folders with the same name.  However, we also don't know
    // what the base folder.  So for now just strip out everything including/before the last '/' and
    // then add a ref for that job
    def addReference(def jobName) {
        int lastSlash = jobName.lastIndexOf('/')
        if (lastSlash != -1 && lastSlash != jobName.length()-1) {
            referencedJobs += jobName.substring(lastSlash + 1)
        }
        else {
            referencedJobs += jobName
        }
    }

    def addJob(def jobName, def isPR) {
        if (overallJobs.find { it == jobName }) {
            // The job has already been added.  We may have been using getFullJobName for other reasons
            // (e.g. jobs names for dependencies)
            return
        }
        overallJobs += jobName

        if (isPR) {
            prJobs += jobName
        }
    }

    def addTargetBranchesForJob(String jobName, String[] targetBranches) {
        if (targetBranchMap.containsKey(jobName)) {
            targetBranchMap[jobName] += targetBranches
        }
        else {
            targetBranchMap.put(jobName, targetBranches)
        }
    }

    def addTargetBranchForJob(String jobName, String targetBranch) {
        addTargetBranchesForJob(jobName, (String[])[targetBranch])
    }

    def String[] getTargetBranchesForJob(String jobName) {
        if (targetBranchMap.containsKey(jobName)) {
            return targetBranchMap[jobName]
        }
        else {
            return ["Unknown Branch"]
        }
    }

    def addPushTriggeredJob(String jobName) {
        def triggerInfo = new PushTriggerInfo()
        triggerInfo.branches = getTargetBranchesForJob(jobName)

        pushTriggeredJobs += ["${jobName}":triggerInfo]
        addReference(jobName)
    }

    def addCronTriggeredJob(String jobName, String cronString, boolean alwaysRuns) {
        def triggerInfo = new CronTriggerInfo()
        triggerInfo.cronString = cronString
        triggerInfo.branches = getTargetBranchesForJob(jobName)
        triggerInfo.alwaysRuns = alwaysRuns

        cronTriggeredJobs += ["${jobName}":triggerInfo]
        addReference(jobName)
    }

    def addManuallyTriggeredJob(String jobName) {
        addReference(jobName)
    }

    def addPRTriggeredJob(String jobName, String[] targetBranches, String context, String triggerPhrase, boolean isDefault) {

        def simplifiedTriggerPhraseString = triggerPhrase
        // Replace case insensitivity string
        simplifiedTriggerPhraseString = simplifiedTriggerPhraseString.replace("(?i)", "")
        // Replace escaped .
        simplifiedTriggerPhraseString = simplifiedTriggerPhraseString.replace("\\.", ".")
        // Replace .* regex
        simplifiedTriggerPhraseString = simplifiedTriggerPhraseString.replace(".*", "")
        // Replace W+ regex with a single space
        simplifiedTriggerPhraseString = simplifiedTriggerPhraseString.replace("\\W+", " ")

        def triggerInfo = new PRTriggerInfo()
        triggerInfo.context = context
        triggerInfo.branches = targetBranches
        triggerInfo.triggerPhrase = simplifiedTriggerPhraseString
        triggerInfo.isDefault = isDefault

        prTriggeredJobs += ["${jobName}":triggerInfo]
        addReference(jobName)
    }

    // Generate the job report.  A call to this function from netci.groovy should be done with
    // JobReport.Report.generateJobReport(out)
    def generateJobReport(def outStream) {
        // Determine the unreferenced jobs
        def unreferencedJobs = []
        overallJobs.sort().each { jobName ->
            // Check to see whether it was referenced
            if (!referencedJobs.find { it == jobName }) {
                unreferencedJobs += jobName
            }
        }

        // Print additional statistics.  What jobs didn't get a trigger or weren't referenced, total job count, etc.
        outStream.println()
        outStream.println("Job Report")
        outStream.println("===================")
        outStream.println("Statistics:")
        outStream.println("    Total jobs generated: ${overallJobs.size()}")
        outStream.println("    PR jobs generated: ${prJobs.size()}")
        outStream.println("    Non-PR jobs generated: ${overallJobs.size() - prJobs.size()}")
        outStream.println("    Triggered PR jobs generated: ${prTriggeredJobs.size()}")
        outStream.println("    Triggered Commit jobs generated: ${pushTriggeredJobs.size()}")
        outStream.println("    Triggered cron (timed) jobs generated: ${cronTriggeredJobs.size()}")
        outStream.println("Jobs withoutStream triggers/references: ${unreferencedJobs.size()}")
        unreferencedJobs.each { jobName ->
            outStream.println("    ${jobName}")
        }

        outStream.println()
        outStream.println("PR Job CSV")
        outStream.println("================================================================")

        // Job DSL technically runs on the server.  Becuase of this, there are
        // security retrictions in writing files (you can't).  Instead, we just write
        // the output to the console.  The Local-Job-Gen tool could take this output and write it to separate files

        def prTriggerFormatString = "%s,%s,%s,%s,%s"
        outStream.println(String.format(prTriggerFormatString, "Job Name", "Context", "Trigger", "Branches", "Runs by Default?"))

        prTriggeredJobs.sort().each { jobName, triggerInfo ->
            String defaultString = triggerInfo.isDefault ? 'Yes' : 'No'
            String branchString = triggerInfo.branches == null ? 'All' : triggerInfo.branches.join('; ')
            outStream.println(String.format(prTriggerFormatString, jobName, triggerInfo.context, triggerInfo.triggerPhrase, branchString, defaultString))
        }
        outStream.println("================================================================")

        outStream.println()
        outStream.println("Push Job CSV")
        outStream.println("================================================================")

        // Push job CSV report
        def pushTriggerFormatString = "%s,%s"
        outStream.println(String.format(pushTriggerFormatString, "Job Name", "Branches"))
        pushTriggeredJobs.sort().each { jobName, triggerInfo ->
            String branchString = triggerInfo.branches == null ? 'All' : triggerInfo.branches.join('; ')
            outStream.println(String.format(pushTriggerFormatString, jobName, branchString))
        }
        outStream.println("================================================================")

        // Cron trigger CSV report

        outStream.println()
        outStream.println("Cron Job CSV")
        outStream.println("================================================================")

        def cronTriggerFormatString = "%s,%s,%s,%s"
        outStream.println(String.format(cronTriggerFormatString, "Job Name", "Cron", "Branches", "Always Runs?"))
        cronTriggeredJobs.sort().each { jobName, triggerInfo ->
            String branchString = triggerInfo.branches == null ? 'All' : triggerInfo.branches.join('; ')
            String alwaysRunsString = triggerInfo.alwaysRuns ? 'Yes' : 'No'
            outStream.println(String.format(cronTriggerFormatString, jobName, triggerInfo.cronString, branchString, alwaysRunsString))
        }
        outStream.println("================================================================")
    }

    public def static JobReport Report = new JobReport()
}
