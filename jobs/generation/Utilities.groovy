package jobs.generation;
    
class Utilities {

    private static String DefaultBranchOrCommitPR = '${sha1}'
    private static String DefaultBranchOrCommitPush = '*/master'
    private static String DefaultRefSpec = '+refs/pull/*:refs/remotes/origin/pr/*'

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

        def fullJobName = ''
        if (jobName == '') {
            fullJobName = "${folderPrefix}innerloop${jobSuffix}"
        }
        else {
            fullJobName = "${folderPrefix}${jobName}${jobSuffix}"
        }
        
        // Add the job to the overall jobs
        JobReport.Report.addJob(fullJobName, isPR)
        return fullJobName
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
        if (osName == 'Ubuntu') {
            osName = 'Ubuntu14.04'
        }
        
        def machineMap    = [
                            'Ubuntu14.04' :
                                [
                                // Generic version label
                                '':'ubuntu',
                                // Specific auto-image label
                                '201626':'auto-ubuntu1404-201626',
                                // Contains an updated version of mono
                                '20160211':'auto-ubuntu1404-20160211',
                                // Contains the rootfs setup for arm/arm64 builds.  Move this label forward
                                // till we have the working build/test, then apply to everything.
                                'arm-cross-latest':'auto-ubuntu1404-20160524',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-ubuntu1404-20160211',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-ubuntu1404-201626outer'
                                ],
                            'Ubuntu15.10' :
                                [
                                // Generic version label
                                '' : 'auto-ubuntu1510-20160131',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-ubuntu1510-20160131',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-ubuntu1510-20160131outer'
                                ],
                            'Ubuntu16.04' :
                                [
                                // Generic version label
                                '' : 'auto-ubuntu1604-20160426',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-ubuntu1604-20160426',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-ubuntu1604-20160426outer'
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
                                '' : 'auto-win2012-20160325',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-win2012-20160325',
                                // Dev15 image
                                'latest-dev15':'auto-win2012-20160506',
                                // For internal runs
                                'latest-or-auto-internal':'windows-internal || auto-win2012-20160325-internal',
                                // For elevated runs
                                'latest-or-auto-elevated':'windows-elevated || auto-win2012-20160325-elevated'
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
                            'Windows Nano 2016' :
                                [
                                // Generic version label
                                '' : 'windowsnano16'
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
                                '' : 'freebsd || auto-freebsd-20160415',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'freebsd || auto-freebsd-20160415'
                                ],
                            'RHEL7.2' : 
                                [
                                '' : 'auto-rhel72-20160211',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-rhel72-20160211',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-rhel72-20160211outer'
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
                                'latest-or-auto':'auto-centos71-20160211.1',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-centos71-20160211.1outer'
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
                                'latest-or-auto':'auto-suse132-20160211',
                                // For outerloop runs
                                'outer-latest-or-auto':'auto-suse132-20160211outer'
                                ],
                            'Debian8.2' : 
                                [
                                '' : 'auto-deb82-20160323',
                                '20160323':'auto-deb82-20160323',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-deb82-20160323'
                                ],
                            'Debian8.4' :
                                [
                                '' : 'auto-deb84-20160513',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-deb84-20160513',
                                // For outerloop runs
                                'outer-latest-or-auto':'auto-deb84-20160513outer'
                                ],
                           'Fedora23' :
                                [
                                '' : 'auto-fedora23-20160514',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'auto-fedora23-20160514',
                                // For outerloop runs
                                'outer-latest-or-auto':'auto-fedora23-20160514outer'
                                ],
                                // Some nodes don't have git, which is what is required for the
                                // generators.
                            'Generators' :
                                [
                                '' : '!windowsnano16',
                                'latest-or-auto':'!windowsnano16'
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
    
    // Performs standard job setup for a newly created job.
    // Includes: Basic parameters, Git SCM, and standard options
    // 
    // Parameters:
    //  job: Job to set up.
    //  project: Name of project
    //  isPR: True if job is PR job, false otherwise.
    //  defaultBranch: If not a PR job, the branch that we should be building.
    def static standardJobSetup(def job, String project, boolean isPR, String defaultBranch = '*/master') {
        String defaultRefSpec = getDefaultRefSpec(null)
        if (isPR) {
            defaultBranch = getDefaultBranchOrCommitPR(null)
        }
        standardJobSetupEx(job, project, isPR, defaultBranch, defaultRefSpec)
    }

    // Performs standard job setup for a newly created push job.
    // Includes: Basic parameters, Git SCM, and standard options
    // 
    // Parameters:
    //  job: Job to set up.
    //  project: Name of project
    //  defaultBranch: Branch to build on push. 
    def static standardJobSetupPush(def job, String project, String defaultBranch = null) {
        defaultBranch = getDefaultBranchOrCommitPush(defaultBranch);
        standardJobSetupEx(job, project, false, defaultBranch, null);
    }

    // Performs standard job setup for a newly created push job.
    // Includes: Basic parameters, Git SCM, and standard options
    // 
    // Parameters:
    //  job: Job to set up.
    //  project: Name of project
    //  defaultBranchOrCommit: Commit / branch to build. 
    //  defaultRefSpec: the refs that Jenkins must sync on a PR job
    def static standardJobSetupPR(def job, String project, String defaultBranchOrCommit = null, String defaultRefSpec = null) {
        defaultBranchOrCommit = getDefaultBranchOrCommitPR(defaultBranchOrCommit)
        defaultRefSpec = getDefaultRefSpec(defaultRefSpec)
        standardJobSetupEx(job, project, true, defaultBranchOrCommit, defaultRefSpec)
    }

    // Performs standard job setup for a newly created job.
    // Includes: Basic parameters, Git SCM, and standard options
    // 
    // Parameters:
    //  job: Job to set up.
    //  project: Name of project
    //  isPR: True if job is PR job, false otherwise.
    //  defaultBranchOrCommit: Commit / branch to build.  
    //  defaultRefSpec: the refs that Jenkins must sync on a PR job
    def private static standardJobSetupEx(def job, String project, boolean isPR, String defaultBranchOrCommit, String defaultRefSpec) {
        Utilities.addStandardParametersEx(job, project, isPR, defaultBranchOrCommit, defaultRefSpec)
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
                    daysToKeep(10)
                    artifactNumToKeep(25)
                    numToKeep(100)
                }
                else {
                    artifactDaysToKeep(7)
                    daysToKeep(14)
                    artifactNumToKeep(50)
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
            
            // 5 second quiet period before the job can be scheduled
            quietPeriod(5)
            
            wrappers {
                timestamps()
                // Add a pre-build wipe-out
                preBuildCleanup()
            }
            
            // Add a post-build cleanup.  Order that this post-build step doesn't matter.
            // It runs after everything.
            publishers {
                wsCleanup {
                    cleanWhenFailure(true)
                    cleanWhenAborted(true)
                    cleanWhenUnstable(true)
                }
            }

            if (job instanceof javaposse.jobdsl.dsl.jobs.BuildFlowJob) {
                // Needs a workspace to avoid building other branches when not needed.
                configure {
                    def buildNeedsWorkspace = it / 'buildNeedsWorkspace'
                    buildNeedsWorkspace.setValue('true')
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
    
    def private static String joinStrings(Iterable<String> strings, String combineDelim) {
        // Doing this instead of String.join because for whatever reason it doesn't resolve
        // in CI.
        def combinedString = ''
        strings.each { str ->
            if (combinedString == '') {
                combinedString = str
            }
            else {
                combinedString += combineDelim + str
            }
        }
        
        return combinedString
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
        /*
        def ignoredPathsString = Utilities.joinStrings(ignoredPaths, ',')
        def foundNetCi = false
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
        */
    }

    def static addGithubPushTrigger(def job) {
        job.with {
            triggers {
                githubPush()
            }
        }
        
        // Record the push trigger.  We look up in the side table to see what branches this
        // job was set up to build
        JobReport.Report.addPushTriggeredJob(job.name)
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
                githubPullRequest {
                    useGitHubHooks()
                    // Add default individual admins here
                    admin('mmitche')
                    if (permitAllSubmittters) {
                        permitAll()
                    }
                    else {
                        assert permittedOrgs != null || permittedUsers != null
                        permitAll(false)
                        if (permittedUsers != null) {
                            permittedUsers.each { permittedUser ->
                                admin(permittedUser)
                            }
                        }
                        if (permittedOrgs != null) {
                            String orgListString = Utilities.joinStrings(permittedOrgs, ',')
                            orgWhitelist(orgListString)
                            allowMembersOfWhitelistedOrgsAsAdmin(true)
                        }
                    }
                    extensions {
                        commitStatus {
                            context(contextString)
                        }
                    }
                    
                    if (triggerOnPhraseOnly) {
                        onlyTriggerPhrase(triggerOnPhraseOnly)
                    }
                    triggerPhrase(triggerPhraseString)
                    
                    if (branchName != null) {
                        // We should only have a flat branch name, no wildcards
                        assert branchName.indexOf('*') == -1
                        whiteListTargetBranches([branchName])
                    }
                }
            }
            
            JobReport.Report.addPRTriggeredJob(job.name, (String[])[branchName], contextString, triggerPhraseString, !triggerOnPhraseOnly)
        }
    }

    // Adds a github PR trigger only triggerable by member of certain organizations. Either permittedOrgs or
    // permittedUsers must be non-null.
    //
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //    permittedOrgs - orgs permitted to trigger the job
    //    permittedUsers - users permitted to trigger the job
    //
    def static addPrivateGithubPRTrigger(def job, String contextString, String triggerPhraseString, boolean triggerPhraseOnly, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''
        assert triggerPhraseString != ''
        
        Utilities.addGithubPRTriggerImpl(job, null, contextString, triggerPhraseString, triggerPhraseOnly, false, permittedOrgs, permittedUsers)
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

    // Adds the standard parameters for PR and Push jobs.
    //
    // Parameters:
    //  job: Job to change
    //  project: Github project
    //  isPR: True if this is a PR job, false otherwise.
    //  defaultBranch: Branch to build by default if this is NOT a PR job.  Defaults to */master.
    def static addStandardParameters(def job, String project, boolean isPR, String defaultBranch = '*/master') {
        String defaultRefSpec = getDefaultRefSpec(null)
        if (isPR) {
            defaultBranch = getDefaultBranchOrCommitPR(null)
        }

        addStandardParametersEx(job, project, isPR, defaultBranch, defaultRefSpec)
    }

    // Adds the standard parameters for PR and Push jobs.
    //
    // Parameters:
    //  job: Job to set up.
    //  project: Name of project
    //  isPR: True if job is PR job, false otherwise.
    //  defaultBranchOrCommit: Commit / branch to build.  
    //  defaultRefSpec: the refs that Jenkins must sync on a PR job
    def private static addStandardParametersEx(def job, String project, boolean isPR, String defaultBranchOrCommit, String defaultRefSpec) {
        if (isPR) {
            addStandardPRParameters(job, project, defaultBranchOrCommit, defaultRefSpec)
        }
        else {
            addStandardNonPRParameters(job, defaultBranchOrCommit)
            // Add the size map info for the reporting
            JobReport.Report.addTargetBranchForJob(job.name, defaultBranchOrCommit)
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
    def static addStandardPRParameters(def job, String project, String defaultBranchOrCommit = null, String defaultRefSpec = null) {
        defaultBranchOrCommit = getDefaultBranchOrCommitPR(defaultBranchOrCommit)
        defaultRefSpec = getDefaultRefSpec(defaultRefSpec)

        job.with {
            parameters {
                stringParam('GitBranchOrCommit', defaultBranchOrCommit, 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                stringParam('GitRepoUrl', calculateGitURL(project), 'Git repo to clone.')
                stringParam('GitRefSpec', defaultRefSpec, 'RefSpec.  WHEN SUBMITTING PRIVATE JOB FROM YOUR OWN REPO, CLEAR THIS FIELD (or it won\'t find your code)')
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

    // Adds private permissions to a job, making it visible only to certain users
    // Parameters:
    //
    //  job - Job to modify
    //  permitted - Array of groups and users that are permitted to view the job.
    def static addPrivatePermissions(def job, def permitted = ['mmitche', 'Microsoft']) {
        job.with {
            authorization {
                blocksInheritance()
                permitted.each { user ->
                    permissionAll(user)
                }
                permission('hudson.model.Item.Discover', 'anonymous')
                permission('hudson.model.Item.ViewStatus', 'anonymous')
            }
        }
    }
     
    // Archives data for a job when specific job result conditions are met.
    // Parameters:
    //
    //  job - Job to modify
    //  settings - Archival settings
    def static addArchival(def job, ArchivalSettings settings) {
        job.with {
            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition {
                            status(settings.getArchiveStatusRange()[0],settings.getArchiveStatusRange()[1])
                        }
                        
                        publishers {
                            archiveArtifacts {
                                allowEmpty(!settings.failIfNothingArchived)
                                pattern(joinStrings(Arrays.asList(settings.filesToArchive), ','))
                                if (settings.filesToExclude != null) {
                                    exclude(joinStrings(Arrays.asList(settings.filesToExclude), ','))
                                } else {
                                    exclude('')
                                }
                                // Always archive so that the flexible publishing
                                // handles pass/fail
                                onlyIfSuccessful(false)
                            }
                        }
                    }
                }
            }
        }
    }
  
    // Archives data for a job
    // Parameters:
    //
    //  job - Job to modify
    //  filesToArchive - Files to archive.  Comma separated glob syntax
    //  filesToExclude - Files to exclude from archival.  Defaults to no files excluded.  Comma separated glob syntax
    //  doNotFailIfNothingArchived - If true, and nothing is archived, will not fail the build.
    //  archiveOnlyIfSuccessful - If true, will only archive if the build passes.  If false, then will archive always 
    @Deprecated
    def static addArchival(def job, String filesToArchive, String filesToExclude = '',
        def doNotFailIfNothingArchived = false, def archiveOnlyIfSuccessful = true) {

        ArchivalSettings settings = new ArchivalSettings();
        settings.addFiles(filesToArchive);
        if (filesToExclude != '') {
            settings.excludeFiles(filesToExclude);
        }
        if (doNotFailIfNothingArchived) {
            settings.setDoNotFailIfNothingArchived()
        } else {
            settings.setFailIfNothingArchived()
        }
        if (archiveOnlyIfSuccessful) {
            settings.setArchiveOnSuccess()
        } else {
            settings.setAlwaysArchive()
        }
        
        addArchival(job, settings)
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
        
        JobReport.Report.addCronTriggeredJob(job.name, cronString)
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

    def static String getDefaultBranchOrCommitPush(String defaultBranch) {
        return getDefaultBranchOrCommit(false, defaultBranch);
    }

    def static String getDefaultBranchOrCommitPR(String defaultBranchOrCommit) {
        return getDefaultBranchOrCommit(true, defaultBranchOrCommit);
    }

    def static String getDefaultBranchOrCommit(boolean isPR, String defaultBranchOrCommit) {
        if (defaultBranchOrCommit != null) {
            return defaultBranchOrCommit;
        }

        if (isPR) { 
            return DefaultBranchOrCommitPR;
        }
        else { 
            return DefaultBranchOrCommitPush;
        }
    }

    def static String getDefaultRefSpec(String refSpec) {
        if (refSpec != null) {
            return refSpec;
        }

        return DefaultRefSpec;
    }
}
