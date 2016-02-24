package jobs.generation;

class Utilities {

    // Get the folder name for a job.
    //
    // Parameters:
    //  project: Project name (e.g. dotnet/coreclr)
    //
    // Returns:
    //  Folder name for the project.  Typically project name with / turned to _
    def static getFolderName(String project) {
        return project.replace('/', '_')
    }
  
    // Get the standard job name of a job given the base job name and whether the job is a PR
    // or not.
    //
    // Parameters:
    //  baseJobName: Base name of the job (e.g. ubuntu_debug).
    //  isPR: True if PR job, false otherwise
    //
    // Returns:
    //  Full job name, essentially the baseJobName + _prtest if isPR is true
    def static getFullJobName(String baseJobName, boolean isPR) {
        def jobSuffix = ''
        if (isPR) { 
            jobSuffix = '_prtest'
        }

        return "${baseJobName}_prtest"
    }
    
    // Get the standard job name of a job given the base job name, project, whether the
    // job is a PR or not, and an optional folder
    //
    // Parameters:
    //  project: Project name (e.g. dotnet/coreclr)
    //  jobName: Base name of the job
    //  isPR: True if PR job, false otherwise
    //  folder (optional): If folder is specified, project is not used as the folder name
    //
    // Returns:
    //  Full job name.  If folder prefix is specified, 
    def static getFullJobName(String project, String jobName, boolean isPR, String folder = '') {
        def jobSuffix = ''
        if (isPR) { 
            jobSuffix = '_prtest'
        }

        def folderPrefix = ''
        if (folder != '') {
            folderPrefix = "${folder}/"
        }

        if (jobName == '') {
            return "${folderPrefix}innerloop${jobSuffix}"
        }
        else {
            return "${folderPrefix}${jobName}${jobSuffix}"
        }
    }
  
    // Given the github full project name (e.g. dotnet/coreclr), get the
    // project name (coreclr)
    //
    // Parameters:
    //  project: Qualified project name
    //
    // Returns: Project name
    def static getProjectName(String project) {
        return project.split('/')[1];
    }
  
    // Given the github full project name (e.g. dotnet/coreclr), get the
    // org name (dotnet)
    //
    // Parameters:
    //  project: Qualified project name
    //
    // Returns: Org name
    def static getOrgName(String project) {
        return project.split('/')[0];
    }
  
    // Given the name of an OS, set the nodes that this job runs on.
    //
    // Parameters:
    //  job: Job to set affinity for
    //  osName: Name of OS to to run on.
    //  version: Optional version of the image.  This version can be the date potentially followed
    //           by .1, .2, etc. or it could be a static image version (like a perf label).
    def static setMachineAffinity(def job, String osName, String version = '') {
        def machineMap    = [
                            'Ubuntu' :
                                [
                                // Generic version label
                                '':'ubuntu',
                                // Specific auto-image label
                                '201626':'auto-ubuntu1404-201626',
                                // Contains an updated version of mono
                                '20160211':'auto-ubuntu1404-20160211',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-ubuntu1404-20160211'
                                ],
                            'Ubuntu14.04' :
                                [
                                // Generic version label
                                '':'ubuntu',
                                // Specific auto-image label
                                '201626':'auto-ubuntu1404-201626',
                                '201626test':'auto-ubuntu1404-201626test',
                                // Contains an updated version of mono
                                '20160211':'auto-ubuntu1404-20160211',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-ubuntu1404-20160211'
                                ],
                            'Ubuntu15.10' :
                                [
                                // Generic version label
                                '' : 'auto-ubuntu1510-20160131',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-ubuntu1510-20160131'
                                ],
                            'OSX' :
                                [
                                // Generic version label
                                '' : 'mac',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'mac'
                                ],
                            // This is Windows Server 2012 R2
                            'Windows_NT' :
                                [
                                // Generic version label
                                '' : 'windows',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'windows'
                                ],
                            'Windows_2016' : 
                                [
                                // Generic version label
                                '' : 'auto-win2016-20160223',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-win2016-20160223'
                                ],
                            'Windows 10' : 
                                [
                                // Generic version label
                                '' : 'windows10',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'windows10'
                                ],
                            'Windows 7' : 
                                [
                                // Generic version label
                                '' : 'windows7',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'windows7'
                                ],
                            'FreeBSD' :
                                [
                                '' : 'freebsd',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'freebsd'
                                ],
                            'RHEL7.2' : 
                                [
                                '' : 'auto-rhel72-20160211',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-rhel72-20160211'
                                ],
                            'CentOS7.1' : 
                                [
                                '' : 'centos-71',
                                // First functioning auto image.  Based directly off of the
                                // existing centos image
                                '20160211.1':'auto-centos71-20160211.1',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-centos71-20160211.1'
                                ],
                            'OpenSUSE13.2' :
                                [
                                '' : 'openSuSE-132',
                                // First functioning auto image.  Based directly off of the
                                // existing suse image
                                '20160211':'auto-suse132-20160211',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-suse132-20160211'
                                ],
                            'Debian8.2' : 
                                [
                                '' : 'debian-82',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'debian-82'
                                ]
                            ]
        def versionLabelMap = machineMap.get(osName, null)
        assert versionLabelMap != null : "Could not find os ${osName}"
        def machineLabel = versionLabelMap.get(version, null)
        assert machineLabel != null : "Could not find version ${version} of ${osName}"
        job.with {
            label(machineLabel)
        }
    }
  
    // Does simple setup for a job.  Used for easy setup when building a
    // bunch of jobs in a loop
    // 
    // Parameters:
    //  job - Job to modify
    //  project - Project the job belongs to
    //  isPR - True if the job is a PR job, false otherwise
    //  prContext - If the job is a PR job, it will get a PR trigger.  This context will
    //              show up in github for the PR.  If left blank, will use the job name.
    def static simpleInnerLoopJobSetup(def job, String project, boolean isPR, String prContext = '') {
        
        Utilities.standardJobSetup(job, project, isPR)
        
        if (isPR) {
            if (prContext == '') {
                prContext = job.name
            }
            Utilities.addGithubPRTrigger(job, prContext)
        }
        else {
            Utilities.addGithubPushTrigger(job)
        }
    }
    
    // Performs standard job setup for a newly created job.
    // Includes: Basic parameters, Git SCM, and standard options
    // 
    // Parameters:
    //  job: Job to set up.
    //  project: Name of project
    //  isPR: True if job is PR job, false otherwise.
    //  defaultBranch: If not a PR job, the branch that we should be building.
    def static standardJobSetup(def job, String project, boolean isPR, String defaultBranch = '*/master') {
        Utilities.addStandardParameters(job, project, isPR, defaultBranch)
        Utilities.addScm(job, project, isPR)
        Utilities.addStandardOptions(job, isPR)
    }
  
    // Set the job timeout to the specified value.
    // job - Input job to modify
    // jobTimeout - Set the job timeout.
    def static setJobTimeout(def job, int jobTimeout) {
        job.with {
            wrappers {
                timeout {
                    absolute(jobTimeout)
                }
            }
        }
    }

    // Adds a retention policy for artifacts
    //  job - Job to modify
    //  isPR - True if the job is a pull request job, false otherwise.  If isPR is true,
    //         a more restrictive retention policy is use.
    def static addRetentionPolicy(def job, boolean isPR = false) {
        job.with {
            // Enable the log rotator
            logRotator {
                if (isPR) {
                    artifactDaysToKeep(7)
                    daysToKeep(14)
                    artifactNumToKeep(25)
                    numToKeep(100)
                }
                else {
                    artifactDaysToKeep(14)
                    daysToKeep(30)
                    artifactNumToKeep(-1)
                    numToKeep(100)
                }
            }
        }
    }
    
    
    // Add standard options to a job.
    // job - Input job
    // isPR - True if the job is a pull request job, false otherwise.
    def static addStandardOptions(def job, def isPR = false) {
        job.with {
            // Enable concurrent builds 
            concurrentBuild()
            
            wrappers {
                timestamps()
                // Add a pre-build wipe-out
                preBuildCleanup()
            }
            
            // Add a post-build cleanup.  Order that this post-build step doesn't matter.
            // It runs after everything.  Avoid cleaning when not succesful
            publishers {
                wsCleanup {
                    cleanWhenFailure(false)
                    cleanWhenAborted(false)
                    cleanWhenUnstable(false)
                }
            }
        }

        // Add netci.groovy as default.  Only add if it's a PR.
        if (isPR) {
            Utilities.addIgnoredPaths(job, ['netci.groovy']);
        }
        Utilities.setJobTimeout(job, 120)
        Utilities.addRetentionPolicy(job, isPR)
    }
    
    // Sets up the job to fast exit if only certain paths were edited.
    //
    // Parameters:
    //  job - Input job to modify
    //  ignoredPaths - Array of strings containing paths that should be ignored
    // Description:
    //  If only files in the paths were changed (these paths are evaluated as globs)
    //  then the build exits early.  Multiple calls to this function will replace the original
    //  ignored paths.
    def static addIgnoredPaths(def job, Iterable<String> ignoredPaths) {
        // Doing this instead of String.join because for whatever reason it doesn't resolve
        // in CI.
        def ignoredPathsString = ''
        def foundNetCi = false
        ignoredPaths.each { path ->
            if (ignoredPathsString == '') {
                ignoredPathsString = path
            }
            else {
                ignoredPathsString += ',' + path
            }
        }
        // Put in the raw configure object
        job.with {
            // Add option to ignore changes to netci.groovy when building
            configure {
                it / 'buildWrappers' / 'ruby-proxy-object' {
                        'ruby-object' ('ruby-class': 'Jenkins::Plugin::Proxies::BuildWrapper', pluginid: 'pathignore') {
                            pluginid(pluginid: 'pathignore', 'ruby-class': 'String', 'pathignore' )
                            object('ruby-class': 'PathignoreWrapper', pluginid: 'pathignore') {
                            ignored__paths(pluginid: 'pathignore', 'ruby-class': 'String', ignoredPathsString)
                            invert__ignore(pluginid: 'pathignore', 'ruby-class': 'FalseClass')
                        }
                    }
                }
            }
        }
    }

    def static addGithubPushTrigger(def job) {
        job.with {
            triggers {
                githubPush()
            } 
        }
    }
    
    // Adds a github PR trigger for a job
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //    permitAllSubmittters - If true all PR submitters may run the job
    //    permittedOrgs - If permitAllSubmittters is false, at least permittedOrgs or permittedUsers should be non-empty.
    //    permittedUsers - If permitAllSubmittters is false, at least permittedOrgs or permittedUsers should be non-empty.
    //    branchName - If null, all branches are tested.  If not null, then is the target branch of this trigger
    //
    def private static addGithubPRTriggerImpl(def job, String branchName, String contextString, String triggerPhraseString, boolean triggerOnPhraseOnly, boolean permitAllSubmittters, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        job.with {
            triggers {
                pullRequest {
                    useGitHubHooks()
                    if (permitAllSubmittters) {
                        admin('Microsoft')
                    }
                    admin('mmitche')
                    if (permitAllSubmittters) {
                        permitAll()
                    }
                    else {
                        assert permittedOrgs != null || permittedUsers != null
                        permitAll(false)
                        orgWhitelist(permittedOrgs)
                        userWhitelist(permittedUsers)
                        allowMembersOfWhitelistedOrgsAsAdmin()
                    }
                    extensions {
                        commitStatus {
                            context(contextString)
                        }
                    }
                  
                    onlyTriggerPhrase(triggerOnPhraseOnly)
                    triggerPhrase(triggerPhraseString)
                }
            }
            
            if (branchName != null) {
                // We should only have a flat branch name, no wildcards
                assert branchName.indexOf('*') == -1
                
                // Add option to ignore changes to netci.groovy when building
               	// Add option to ignore changes to netci.groovy when building
                configure { project ->
                    def currentTrigger = project / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / 'whiteListTargetBranches' / 'org.jenkinsci.plugins.ghprb.GhprbBranch' { 
                        'branch'(branchName)
                    }
                }
            }
        }
    }
    
    // Adds a github PR trigger only triggerable by member of certain organizations
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //    permittedOrgs - If permitAllSubmittters is false, permittedOrgs should be non-empty list of organizations
    //
    def static addPrivateGithubPRTrigger(def job, String contextString, String triggerPhraseString, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''
        assert triggerPhraseString != ''
        
        Utilities.addGithubPRTriggerImpl(job, null, contextString, triggerPhraseString, true, false, permittedOrgs, permittedUsers)
    }
    
    // Adds a github PR trigger only triggerable by member of certain organizations
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //    permittedOrgs - If permitAllSubmittters is false, permittedOrgs should be non-empty list of organizations
    //    branchName - Branch that this trigger is specific to.  If a PR comes in from another branch, this trigger is ignored.
    //
    def static addPrivateGithubPRTriggerForBranch(def job, def branchName, String contextString, String triggerPhraseString, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''
        assert triggerPhraseString != ''
        
        Utilities.addGithubPRTriggerImpl(job, branchName, contextString, triggerPhraseString, true, false, permittedOrgs, permittedUsers)
    }

    // Adds a github PR trigger for a job that is specific to a particular branch
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //    targetBranch - If the target branch for the PR message matches this target branch, then the trigger is run.
    //
    def static addGithubPRTriggerForBranch(def job, String branchName, String contextString,
        String triggerPhraseString = '', boolean triggerOnPhraseOnly = true) {
        
        assert contextString != ''
        
        if (triggerPhraseString == '') {
            triggerOnPhraseOnly = false
            triggerPhraseString = "(?i).*test\\W+${contextString}.*"
        }
        
        Utilities.addGithubPRTriggerImpl(job, branchName, contextString, triggerPhraseString, triggerOnPhraseOnly, true, null, null)
    }
    
    // Adds a github PR trigger for a job
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //
    def static addGithubPRTrigger(def job, String contextString, String triggerPhraseString = '', boolean triggerOnPhraseOnly = true) {
        assert contextString != ''
        
        if (triggerPhraseString == '') {
            triggerOnPhraseOnly = false
            triggerPhraseString = "(?i).*test\\W+${contextString}.*"
        }
        
        Utilities.addGithubPRTriggerImpl(job, null, contextString, triggerPhraseString, triggerOnPhraseOnly, true, null, null)
    }

    def static calculateGitURL(def project, String protocol = 'https') {
        // Example: git://github.com/dotnet/corefx.git
        return "${protocol}://github.com/${project}.git"
    }

    // Adds the standard parameters for PR and non-PR jobs.
    //
    // Parameters:
    //  job: Job to change
    //  project: Github project
    //  isPR: True if this is a PR job, false otherwise.
    //  defaultBranch: Branch to build by default if this is NOT a PR job.  Defaults to */master.
    def static addStandardParameters(def job, String project, boolean isPR, String defaultBranch = '*/master') {
        if (isPR) {
            addStandardPRParameters(job, project)
        }
        else {
            addStandardNonPRParameters(job, defaultBranch)
        }
    }
    
    // Adds parameters to a non-PR job.  Right now this is only the git branch or commit.
    // This is added so that downstream jobs get the same hash as the root job
    def static addStandardNonPRParameters(def job, String defaultBranch = '*/master') {
        job.with {
            parameters {
                stringParam('GitBranchOrCommit', defaultBranch, 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
            }
        }
    }

    // Adds the private job/PR parameters to a job.  Note that currently this shouldn't used on a non-pr job because
    // push triggering may not work.
  
    def static addStandardPRParameters(def job, String project) {
        job.with {
            parameters {
                stringParam('GitBranchOrCommit', '${sha1}', 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                stringParam('GitRepoUrl', calculateGitURL(project), 'Git repo to clone.')
                stringParam('GitRefSpec', '+refs/pull/*:refs/remotes/origin/pr/*', 'RefSpec.  WHEN SUBMITTING PRIVATE JOB FROM YOUR OWN REPO, CLEAR THIS FIELD (or it won\'t find your code)')
            }
        }
    }
  
    def static addScm(def job, String project, boolean isPR, String buildBranch = '${GitBranchOrCommit}') {
        if (isPR) {
            addPRTestSCM(job, project)
        }
        else {
            addScm(job, project, buildBranch)
        }
    }
  
    def static addScm(def job, String project, String buildBranch = '${GitBranchOrCommit}') {
        job.with {
            scm {
                git {
                    remote {
                        github(project)
                    }

                    branch(buildBranch)
                }
            }
        }
    }
  
    // Adds private job/PR test SCM.  This is slightly different than normal
    // SCM since we use the parameterized fields for the refspec, repo, and branch
    def static addPRTestSCM(def job, String project) {
        job.with {
            scm {
                git {
                    remote {
                        github(project)

                        // Set the refspec
                        refspec('${GitRefSpec}')

                        // Reset the url to the parameterized version
                        url('${GitRepoUrl}')
                    }

                    branch('${GitBranchOrCommit}')
                }
            }
        }
    }
  
    def static addArchival(def job, String filesToArchive, String filesToExclude = '',
        def doNotFailIfNothingArchived = false, def archiveOnlyIfSuccessful = true) {

        job.with {
            publishers {
                archiveArtifacts {
                    pattern(filesToArchive)
                    exclude(filesToExclude)
                    onlyIfSuccessful(archiveOnlyIfSuccessful)
                    allowEmpty(doNotFailIfNothingArchived)
                }
            }
        }
    }

    def static addHtmlPublisher(def job, String reportDir, String name, String reportHtml, boolean keepAllReports = true) {
        job.with {
            publishers {
                publishHtml {
                    report(reportDir) {
                        reportName(name)
                        keepAll(keepAllReports)
                        reportFiles(reportHtml)
                    }
                }
            }
        }
    }

    def static addPeriodicTrigger(def job, String cronString) {
        job.with {
            triggers {
                cron(cronString)
            }
        }
    }
    
    // Adds xunit.NET v2 test results.
    // Parameters:
    //
    //  job - Job to modify
    //  resultFilePattern - Ant style pattern of test results.  Defaults to **/testResults.xml
    //  skipIfNoTestFiles - Do not fail the build if there were no test files found.
    def static addXUnitDotNETResults(def job, String resultFilePattern = '**/testResults.xml', boolean skipIfNoTestFiles = false) {
        job.with {
            publishers {
                archiveXUnit {
                    xUnitDotNET {
                        pattern(resultFilePattern)
                        skipNoTestFiles(skipIfNoTestFiles)
                        failIfNotNew(true)
                        deleteOutputFiles(true)
                        stopProcessingIfError(true)
                    }
                    
                    failedThresholds {
                        unstable(0)
                        unstableNew(0)
                        failure(0)
                        failureNew(0)
                    }
                    skippedThresholds {
                        unstable(100)
                        unstableNew(100)
                        failure(100)
                        failureNew(100)
                    }
                    thresholdMode(ThresholdMode.PERCENT)
                    timeMargin(60000)
                }
            }
        }
    }
    
    // Adds MSTest test results.
    // Parameters:
    //
    //  job - Job to modify
    //  resultFilePattern - Ant style pattern of test results.  Defaults to **/testResults.xml
    //  skipIfNoTestFiles - Do not fail the build if there were no test files found.
    def static addMSTestResults(def job, String resultFilePattern = '**/testResults.xml', boolean skipIfNoTestFiles = false) {
        job.with {
            publishers {
                archiveXUnit {
                    msTest {
                        pattern(resultFilePattern)
                        skipNoTestFiles(skipIfNoTestFiles)
                        failIfNotNew(true)
                        deleteOutputFiles(true)
                        stopProcessingIfError(true)
                    }
                    
                    failedThresholds {
                        unstable(0)
                        unstableNew(0)
                        failure(0)
                        failureNew(0)
                    }
                    skippedThresholds {
                        unstable(100)
                        unstableNew(100)
                        failure(100)
                        failureNew(100)
                    }
                    thresholdMode(ThresholdMode.PERCENT)
                    timeMargin(3000)
                }
            }
        }
    }
}
