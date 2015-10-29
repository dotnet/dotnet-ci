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
    def static setMachineAffinity(def job, String osName) {
        def machineLabelMap = ['Ubuntu':'ubuntu',
                               'OSX':'mac',
                               'Windows_NT':'windows',
                               'FreeBSD': 'freebsd',
                               'CentOS7.1': 'centos-71',
                               'OpenSUSE13.2': 'openSuSE-132']
        def machineLabel = machineLabelMap.get(osName, null) 
        assert machineLabel != null : "Could not find machine label for ${osName}"
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
        Utilities.addStandardOptions(job)
    }
  
    // Add standard options to a job.
    // job - Input job
    def static addStandardOptions(def job) {
        job.with {
            // Enable concurrent builds 
            concurrentBuild()

            // Enable the log rotator

            logRotator {    
                artifactDaysToKeep(4)
                daysToKeep(21)
                artifactNumToKeep(25)
            }

            wrappers {
                timeout {
                    absolute(120)
                }
                timestamps()
            }
      
            // Add option to ignore changes to netci.groovy when building
            configure {
                it / 'buildWrappers' / 'ruby-proxy-object' {
                        'ruby-object' ('ruby-class': 'Jenkins::Plugin::Proxies::BuildWrapper', pluginid: 'pathignore') {
                            pluginid(pluginid: 'pathignore', 'ruby-class': 'String', 'pathignore' )
                            object('ruby-class': 'PathignoreWrapper', pluginid: 'pathignore') {
                            ignored__paths(pluginid: 'pathignore', 'ruby-class': 'String', 'netci.groovy')
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

    // Adds a github PR 3 for a job
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //
    def static addGithubPRTrigger(def job, String contextString = '', String triggerPhraseString = '', boolean triggerOnPhraseOnly = true) {
        def commitContext = contextString
        if (commitContext == '') {
            commitContext = job.name
        }
    
        job.with {
            triggers {
                pullRequest {
                    useGitHubHooks()
                    admin('Microsoft')
                    admin('mmitche')
                    permitAll()            
                    extensions {
                        commitStatus {
                            context(commitContext)
                        }
                    }
                  
                    if (triggerPhraseString != '') {
                        onlyTriggerPhrase(triggerOnPhraseOnly)
                        regexTriggerPhrase(triggerPhraseString)
                    }
                    else {
                        // If the triggerPhrase is empty, set it to the commitContext
                        // and set onlyTriggerPhrase to false so that the job can be rerun by name.    
                        onlyTriggerPhrase(false)
                        regexTriggerPhrase("(?i).*test\\W+${commitContext}.*")
                    }
                }
            }
        }
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
                    wipeOutWorkspace()

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
                    wipeOutWorkspace()

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
    // TODO: Once native support for this type appears in the plugin (should be next version),
    // this code can be simplified.
    // Parameters:
    //
    //  job - Job to modify
    //  resultFilePattern - Ant style pattern of test results.  Defaults to **/testResults.xml
    //  skipIfNoTestFiles - Do not fail the build if there were no test files found.
    def static addXUnitDotNETResults(def job, String resultFilePattern = '**/testResults.xml', String skipIfNoTestFiles = false) {
        job.with {
            configure { node ->
                node / 'publishers' << {
                    'xunit'('plugin': 'xunit@1.97') {
                        'types' {
                            'XUnitDotNetTestType' {
                                'pattern'(resultFilePattern)
                                'skipNoTestFiles'(skipIfNoTestFiles)
                                'failIfNotNew'(true)
                                'deleteOutputFiles'(true)
                                'stopProcessingIfError'(true)
                            }
                        }
                        'thresholds' {
                            'org.jenkinsci.plugins.xunit.threshold.FailedThreshold' {
                                'unstableThreshold'('')
                                'unstableNewThreshold'('')
                                'failureThreshold'('')
                                'failureNewThreshold'('')
                            }
                            'org.jenkinsci.plugins.xunit.threshold.SkippedThreshold' {
                                'unstableThreshold'('')
                                'unstableNewThreshold'('')
                                'failureThreshold'('')
                                'failureNewThreshold'('')
                            }
                        }
                        'thresholdMode'('1')
                        'extraConfiguration' {
                            testTimeMargin('3000')
                        }
                    }
                }
            }
        }
    }
}
