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
    //  jobName: Base name of the job
    //  isPR: True if PR job, false otherwise
    //  folder (optional): If folder is specified, project is not used as the folder name
    //
    // Returns:
    //  Full job name.  If folder prefix is specified,
    def static getFullJobName(String jobName, boolean isPR, String folder = '') {
        return getFullJobName('', jobName, isPR, folder);
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

    // Define a set of OS names which can resore and use managed build tools with a non-default RID.
    // This controls the __PUBLISH_RID environment variable which affects init-tools behavior.
    // Entries placed in this list are temporary, and should be removed when NuGet packages are published
    // for the new OS.
    //
    // Parameters:
    //  os: The name of the operating system. Ex: Windows_NT, OSX, openSUSE42.1.
    //
    // Returns: The name of an alternate RID to use while bootstrapping. If no RID mapping exists, returns null.
    def static getBoostrapPublishRid(def os) {
        def bootstrapRidMap = [
            'OpenSUSE42.1': 'opensuse.13.2-x64',
            'Ubuntu16.10': 'ubuntu.16.04-x64',
            'Fedora24': 'fedora.23-x64'
        ]
        return bootstrapRidMap.get(os, null)
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
        // Special case OSX.  We did not use to have
        // an OS version.  Current OSX job run against 10.11
        if (osName == 'OSX') {
            osName = 'OSX10.11'
        }

        def machineMap    = [
                            'Ubuntu14.04' :
                                [
                                // Generic version label
                                '':'auto-ubuntu1404-20160211',
                                // Specific auto-image label
                                '201626':'auto-ubuntu1404-201626',
                                // Contains an updated version of mono
                                '20160211':'auto-ubuntu1404-20160211.1',
                                // Contains npm, njs, nvm
                                '20161020':'ubuntu1404-20161020',
                                // Contains 20160211-1 + gdb + mono 4.6.2.16
                                '20170109':'ubuntu1404-20170109',
                                // Contains 20160211-1 + clang 3.9
                                '20170118':'ubuntu1404-20170118',
                                // Contains the rootfs setup for arm/arm64 builds.  Move this label forward
                                // till we have the working build/test, then apply to everything.
                                'arm-cross-latest':'auto-ubuntu1404-20170120',
                                // Latest auto image.
                                'latest-or-auto':'auto-ubuntu1404-20160211.1',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-ubuntu1404-201626outer',
                                // For internal Ubuntu runs
                                'latest-or-auto-internal':'auto-ubuntu1404-20160211.1-internal'
                                ],
                            'Ubuntu15.10' :
                                [
                                // Generic version label
                                '' : 'auto-ubuntu1510-20160307',
                                // Latest auto image.
                                'latest-or-auto':'auto-ubuntu1510-20160307',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-ubuntu1510-20160307outer'
                                ],
                            'Ubuntu16.04' :
                                [
                                // Contains auto-ubuntu1604-20160803 + gdb + mono 4.6.2.16
                                '20170109':'ubuntu1604-20170109',
                                // Latest auto image.
                                'latest-or-auto':'ubuntu1604-20170216',
                                // auto-ubuntu1604-20160510 + docker.
                                // Move this to latest-or-auto after validation
                                'latest-or-auto-docker':'ubuntu1604-20170216',
                                // For outerloop runs.
                                'outer-latest-or-auto':'ubuntu1604-20170216-outer',
                                // For outerloop runs, using Linux kernel version 4.6.2
                                'outer-linux462': 'auto-auto-ubuntu1604-20160510-20160715outer'
                                ],
                            'Ubuntu16.10' :
                                [
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'ubuntu1610-20170216',
                                // For outerloop runs.
                                'outer-latest-or-auto':'ubuntu1610-20170216-outer',
                                ],
                            'OSX10.11' :
                                [
                                // Generic version label
                                '' : 'mac',
                                // Latest auto image.
                                'latest-or-auto':'mac',
                                // For elevated runs
                                'latest-or-auto-elevated':'mac-elevated'
                                ],
                            // El Capitan
                            'OSX10.11' :
                                [
                                // Generic version label
                                '' : 'osx-10.11',
                                // Latest auto image.
                                'latest-or-auto':'osx-10.11',
                                // For elevated runs
                                'latest-or-auto-elevated':'osx-10.11-elevated'
                                ],
                            // Sierra
                            'OSX10.12' :
                                [
                                // Generic version label
                                '' : 'osx-10.12',
                                // Latest auto image.
                                'latest-or-auto':'osx-10.12',
                                // For elevated runs
                                'latest-or-auto-elevated':'osx-10.12-elevated'
                                ],
                            // This is Windows Server 2012 R2
                            'Windows_NT' :
                                [
                                // Older images.  VS update 1
                                '20160325' : 'auto-win2012-20160325',
                                // Older images.  VS update 1
                                '20160325-elevated' : 'auto-win2012-20160325-elevated',
                                // Older images.  VS update 3
                                '20160627' : 'auto-win2012-20160627',
                                // Older images.  VS update 3
                                '20160627-elevated' : 'auto-win2012-20160627-elevated',
                                // auto-win2012-20160824 + .NET 4.6.2
                                '20161027' : 'win2012-20161027',
                                // Latest auto image.
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'win2012-20170303',
                                // Win2012.R2 + VS2013.5 + VS2015.3 + VS15.P3
                                'latest-or-auto-dev15':'auto-win2012-20160707',
                                // Win2012.R2 + VS2013.5 + VS2015.3 + VS15.P4
                                'latest-or-auto-dev15-preview4':'auto-win2012-20160912',
                                // Win2016 + VS15.P5
                                'latest-or-auto-dev15-preview5':'win2016-20161013-1',
                                // Win2016 + VS15.RC2
                                'latest-or-auto-dev15-rc2':'win2016-20170105',
                                // Win2016 + VS15.RC4
                                'latest-or-auto-dev15-rc':'win2016-20170214',
                                // Win2016 + VS15.0
                                'latest-or-auto-dev15-0':'win2016-20170307',
                                // Dev15 image
                                'latest-dev15':'auto-win2012-20160506',
                                // For internal runs
                                'latest-or-auto-internal':'auto-win2012-20160707-internal',
                                // For internal runs - Win2016 + VS15.RC2
                                'latest-or-auto-dev15-rc2-internal':'win2016-20170105-internal',
                                // For internal runs - Win2016 + VS15.RC4
                                'latest-or-auto-dev15-internal':'win2016-20170214-internal',
                                // For internal runs - Win2016 + VS15.0
                                'latest-or-auto-dev15-0-internal':'win2016-20170307-internal',
                                // For internal runs which don't need/want the static 'windows-internal' pool
                                'latest-dev15-internal':'auto-win2012-20160707-internal',
                                // For elevated runs
                                'latest-or-auto-elevated':'win2012-20170303-elevated',
								// For arm64 builds
                                'latest-arm64':'win2012-20170328',
                                // For perf runs
                                'latest-or-auto-perf':'windows-perf-internal',
                                // Win2016
                                'win2016-base': 'win2016-base',
                                // Win2016
                                'win2016-base-internal': 'win2016-base-internal'
                                ],
                            'Windows_2016' :
                                [
                                // First working containers image
                                'win2016-20161018-1':'win2016-20161018-1',
                                // Latest auto image w/docker (move to latest-or-auto when possible)
                                'latest-containers':'win2016-20161018-1',
                                // Latest auto image.
                                'latest-or-auto':'auto-win2016-20160223'
                                ],
                            'Windows Nano 2016' :
                                [
                                // Generic version label
                                '' : 'windowsnano16'
                                ],
                            'Windows 10' :
                                [
                                // Latest auto image.
                                'latest-or-auto':'win2016-20170303'
                                ],
                            'Windows 7' :
                                [
                                '20161104':'win2008-20170303',
                                // Latest auto image.
                                'latest-or-auto':'win2008-20170303'
                                ],
                            'FreeBSD' :
                                [
                                // Latest auto image.
                                'latest-or-auto':'freebsd-20161026'
                                ],
                            'RHEL7.2' :
                                [
                                '' : 'auto-rhel72-20160211',
                                // Latest auto image.
                                'latest-or-auto':'auto-rhel72-20160211',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-rhel72-20160412.1outer'
                                ],
                            'CentOS7.1' :
                                [
                                // Latest auto image.
                                'latest-or-auto':'centos71-20170216',
                                // For outerloop runs.
                                'outer-latest-or-auto':'centos71-20170216-outer',
                                // For outerloop runs, using Linux kernel version 4.6.4
                                'outer-linux464': 'auto-auto-centos71-20160609.1-20160715outer'
                                ],
                            'OpenSUSE13.2' :
                                [
                                '' : 'auto-suse132-20160315',
                                // Latest auto image.
                                'latest-or-auto':'auto-suse132-20160315',
                                // For outerloop runs
                                'outer-latest-or-auto':'auto-suse132-20160315outer'
                                ],
                            'OpenSUSE42.1' :
                                [
                                // Latest auto image.
                                'latest-or-auto':'suse421-20170216',
                                // For outerloop runs
                                'outer-latest-or-auto':'suse421-20170216-outer'
                                ],
                            'Debian8.2' :
                                [
                                '' : 'auto-deb82-20160323',
                                '20160323':'auto-deb82-20160323',
                                // Latest auto image.
                                'latest-or-auto':'auto-deb82-20160323'
                                ],
                            'Debian8.4' :
                                [
                                // Latest auto image.
                                'latest-or-auto':'deb84-20170214',
                                // For outerloop runs
                                'outer-latest-or-auto':'deb84-20170214-outer'
                                ],
                           'Fedora23' :
                                [
                                '' : 'auto-fedora23-20160622',
                                // Latest auto image.
                                'latest-or-auto':'auto-fedora23-20160622',
                                // For outerloop runs
                                'outer-latest-or-auto':'auto-fedora23-20160622outer'
                                ],
                            'Fedora24' :
                                [
                                // Latest auto image.
                                'latest-or-auto':'fedora24-20161024',
                                // For outerloop runs
                                'outer-latest-or-auto':'fedora24-20161024-outer'
                                ],
                            'Tizen' :
                                [
                                // Use ubuntu14.04 images
                                // Contains the rootfs setup for arm/arm64 builds.  Move this label forward
                                // till we have the working build/test, then apply to everything.
                                'arm-cross-latest':'auto-ubuntu1404-20170120',
                                // Latest auto image.
                                'latest-or-auto':'auto-ubuntu1404-20170120',
                                ],
                                // Some nodes don't have git, which is what is required for the
                                // generators.
                            'Generators' :
                                [
                                '' : '!windowsnano16',
                                'latest-or-auto':'!windowsnano16 && !performance'
                                ]
                            ]
        def versionLabelMap = machineMap.get(osName, null)
        assert versionLabelMap != null : "Could not find os ${osName}"
        def machineLabel = versionLabelMap.get(version, null)
        assert machineLabel != null : "Could not find version ${version} of ${osName}"
        job.with {
            label(machineLabel)
        }
        
        // Temporary, nano isn't working on TP5 any longer.  Getting random restarts.
        if (osName.equals('Windows Nano 2016')) {
            job.with {
                disabled(true)
            }
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
                    artifactNumToKeep(50)
                    numToKeep(150)
                }
                else {
                    artifactDaysToKeep(10)
                    daysToKeep(21)
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
        
        // Check Generate Disabled setting (for pr tests)
        if (GenerationSettings.isTestGeneration()) {
            job.with {
                disabled(true)
            }
        }

        Utilities.setJobTimeout(job, 120)
        Utilities.addRetentionPolicy(job, isPR)
        // Add a webhook to gather job events for Jenkins monitoring.
        // The event hook is the id of the event hook URL in the Jenkins store
        Utilities.setBuildEventWebHooks(job, ['helix-int-notification-url', 'helix-prod-notification-url', 'legacy-notification-url'])
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
    
    // Adds an auto-retry to a job
    def private static addJobRetry(def job) {
        List<String> expressionsToRetry = [
            'channel is already closed',
            'Connection aborted',
            'Cannot delete workspace',
            'failed to mkdirs',
            'ERROR: Timeout after 10 minutes',
            'Slave went offline during the build',
            '\'type_traits\' file not found', // This is here for certain flavors of clang on Ubuntu, which can exhibit odd errors.
            '\'typeinfo\' file not found', // This is here for certain flavors of clang on Ubuntu, which can exhibit odd errors.
            'Only AMD64 and I386 are supported', // Appears to be a flaky CMAKE failure
            'java.util.concurrent.ExecutionException: Invalid object ID'
            ]
        def regex = '(?i).*('
        regex += Utilities.joinStrings(expressionsToRetry, '|')
        regex += ').*'
        
        def naginatorNode = new NodeBuilder().'com.chikli.hudson.plugin.naginator.NaginatorPublisher' {
            regexpForRerun(regex)
            rerunIfUnstable(false)
            rerunMatrixPart(false)
            checkRegexp(true)
            maxSchedule(3)
        }
        
        def delayNode = new NodeBuilder().delay(class: 'com.chikli.hudson.plugin.naginator.FixedDelay') {
            delegate.delay(15)
        }
        
        naginatorNode.append(delayNode)
        
        job.with {
            configure { proj ->
                def currentPublishers = proj / publishers
                currentPublishers << naginatorNode
            }
        }
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
        addJobRetry(job)
    }

    // Adds a github PR trigger for a job
    // Parameters:
    //    job - Job to add the PR trigger for
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
    //    permitAllSubmitters - If true all PR submitters may run the job
    //    permittedOrgs - If permitAllSubmitters is false, at least permittedOrgs or permittedUsers should be non-empty.
    //    permittedUsers - If permitAllSubmitters is false, at least permittedOrgs or permittedUsers should be non-empty.
    //    branchName - If null, all branches are tested.  If not null, then is the target branch of this trigger
    //
    def private static addGithubPRTriggerImpl(def job, String branchName, String contextString, String triggerPhraseString, boolean triggerOnPhraseOnly, boolean permitAllSubmitters, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        job.with {
            triggers {
                githubPullRequest {
                    useGitHubHooks()
                    // Add default individual admins here
                    admin('mmitche')
                    if (permitAllSubmitters) {
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
                            updateQueuePosition(true)
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
        
        addJobRetry(job)
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
    //    branchName - If the target branch for the PR message matches this target branch, then the trigger is run.
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    permittedOrgs - If permitAllSubmitters is false, permittedOrgs should be non-empty list of organizations
    //    branchName - Branch that this trigger is specific to.  If a PR comes in from another branch, this trigger is ignored.
    //
    def static addPrivateGithubPRTriggerForBranch(def job, def branchName, String contextString, String triggerPhraseString, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''
        assert triggerPhraseString != ''

        Utilities.addGithubPRTriggerImpl(job, branchName, contextString, triggerPhraseString, true, false, permittedOrgs, permittedUsers)
    }

    // Adds a github PR trigger only triggerable by member of certain organizations
    // Parameters:
    //    job - Job to add the PR trigger for
    //    branchName - If the target branch for the PR message matches this target branch, then the trigger is run.
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    permittedOrgs - If permitAllSubmitters is false, permittedOrgs should be non-empty list of organizations
    //    branchName - Branch that this trigger is specific to.  If a PR comes in from another branch, this trigger is ignored.
    //
    def static addDefaultPrivateGithubPRTriggerForBranch(def job, def branchName, String contextString, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''

        String triggerPhraseString = "(?i).*test\\W+${contextString}.*"

        Utilities.addGithubPRTriggerImpl(job, branchName, contextString, triggerPhraseString, false, false, permittedOrgs, permittedUsers)
    }

    // Adds a github PR trigger for a job that is specific to a particular branch
    // Parameters:
    //    job - Job to add the PR trigger for
    //    branchName - If the target branch for the PR message matches this target branch, then the trigger is run.
    //    contextString - String to use as the context (appears in github as the name of the test being run).
    //                    If empty, the job name is used.
    //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
    //    triggerOnPhraseOnly - If true and trigger phrase string is non-empty, triggers only using the specified trigger
    //                          phrase.
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
            addStandardNonPRParameters(job, project, defaultBranchOrCommit)
            // Add the size map info for the reporting
            JobReport.Report.addTargetBranchForJob(job.name, defaultBranchOrCommit)
        }
    }

    // Adds parameters to a non-PR job.  Right now this is only the git branch or commit.
    // This is added so that downstream jobs get the same hash as the root job
    def private static addStandardNonPRParameters(def job, String project, String defaultBranch = '*/master') {
        job.with {
            parameters {
                stringParam('GitBranchOrCommit', defaultBranch, 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                // Telemetry
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')
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
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')
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
                    
                    // Raise up the timeout
                    extensions {
                        cloneOptions {
                            timeout(30)
                        }
                    }
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
                    
                    // Raise up the timeout
                    extensions {
                        cloneOptions {
                            timeout(30)
                        }
                    }
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

    // Archives data in Azure for a job when specific job result conditions are met.
    // Parameters:
    //
    // job - Job to modify
    // storageAccount - Storage account to use
    // settings - Archival settings
    def static addAzureArchival(def job, String storageAccount, ArchivalSettings settings) {
        job.with {
            publishers {
                flexiblePublish {
                    conditionalAction {
                        condition {
                            status(settings.getArchiveStatusRange()[0],settings.getArchiveStatusRange()[1])
                        }

                        publishers {
                            azureStorageUpload {
                                doNotFailIfArchivingReturnsNothing(!settings.failIfNothingArchived)
                                filesToUpload(joinStrings(Arrays.asList(settings.filesToArchive), ','))
                                if (settings.filesToExclude != null) {
                                    excludeFilesPattern(joinStrings(Arrays.asList(settings.filesToExclude), ','))
                                }
                                storageAccountName(storageAccount)
                                allowAnonymousAccess(true)
                                uploadArtifactsOnlyIfSuccessful(false)
                                manageArtifacts(true)
                                uploadZips(false)
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

    // Adds a trigger that causes the job to run on a schedule.
    // If alwaysRuns is true, then this is a true cron trigger.  If false,
    // then only runs if source control has changed
    // Parameters:
    //
    //  job - Job to modify
    //  cronString - Cron job string indicating the frequency
    //  alwaysRuns - If true, the job will be a true cron job, if false, will run only when source has changed
    def static addPeriodicTrigger(def job, String cronString, boolean alwaysRuns = false) {
        job.with {
            triggers {
                if (alwaysRuns) {
                    cron(cronString)
                }
                else {
                    scm(cronString)
                }
            }
        }

        JobReport.Report.addCronTriggeredJob(job.name, cronString, alwaysRuns)
        addJobRetry(job)
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

    // Calls a web hook on Jenkins build events.  Allows our build monitoring jobs to be push notified
    // vs. polling.
    //
    // Each call to this overwrites the previous set of notifications
    //
    // Parameters:
    //  job - Job to add hook to
    //  endPoints - List of credential IDs of the endpoints to run
    private static void setBuildEventWebHooks(def job, def endPoints) {
        job.with {
            notifications {
                endPoints.each { endPoint -> 
                    endpoint(endPoint) {
                        event('all')
                    }
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
    
    // Adds cross integration to this repo
    def static addCROSSCheck(def dslFactory, String project, String branch, boolean runByDefault = true) {
        def crossJob = dslFactory.job(Utilities.getFullJobName(project, 'CROSS_check', true)) {
            steps {
                // Download the tool
                batchFile("powershell -Command \"wget '%CROSS_SOURCE%' -OutFile cross-integration.zip\"")
                // Unzip it
                batchFile("powershell -Command \"Add-Type -Assembly 'System.IO.Compression.FileSystem'; [System.IO.Compression.ZipFile]::ExtractToDirectory('cross-integration.zip', 'cross-tool')\"")
                def orgName = Utilities.getOrgName(project)
                def repoName = Utilities.getProjectName(project)
                batchFile("cd cross-tool && cross --organization ${orgName} --repository ${repoName} --token %UPDATE_TOKEN% --writecomments %ghprbPullId%")
            }
            
            // Ensure credentials are bound
            wrappers {
                // SAAS URL
                credentialsBinding {
                    string('CROSS_SOURCE', 'cross-sas-url')
                    string('UPDATE_TOKEN', 'cross-update-token')
                }
            }
        }
        
        
        // Cross tool currently works on Windows only
        Utilities.setMachineAffinity(crossJob, 'Windows_NT', 'latest-or-auto')
        Utilities.addPrivatePermissions(crossJob, ['Microsoft'])
        Utilities.standardJobSetup(crossJob, project, true, "*/${branch}")
        Utilities.addGithubPRTriggerForBranch(crossJob, branch, 'CROSS Check', '(?i).*test\\W+cross\\W+please.*', !runByDefault)
    }

    // Emits a helper job.  This job prints a help message to the GitHub comments section
    // Should only be emitted if the system has "ignore comments from bot" enabled.  The job is triggered
    // by commenting '@dotnet-bot help please'
    //
    // This function should be called at the *end* of the groovy file (it needs a list of the 
    // generated jobs obtained through the reporting framework.
    //
    // Parameters:
    //  dslFactory - Factory object that can be used to create new jobs.  In your primary groovy
    //               definitions, this parameter is "this".
    //  project - Current project (required)
    //  branchName - Branch this help message is defined for.
    //  customHeader - Custom header message prepended to the help message.
    //  customFooter - Footer message appended. Could be used to provide repo specific info (bug reports, known issues, etc.)
    //
    // Example:
    //  At the end of the netci.groovy:
    //  Utilities.createHelperJob(this, 'Welcome to CoreFX CI')
    //
    def static createHelperJob(def dslFactory, String project, String branchName, String customHeader, String customFooter = '') {
        String defaultLegList = ""
        String nonDefaultLegList = ""
        // Construct a formatted leg list
        JobReport.Report.prTriggeredJobs.sort().each { jobName, triggerInfo ->
            // Since we're in a table, ensure that the | char is replaced by \|
            def processedTriggerPhrase = triggerInfo.triggerPhrase.replaceAll("\\|", "\\\\|")
            def processedContext = triggerInfo.context.replaceAll("\\|", "\\\\|")
            if (triggerInfo.isDefault) {
                defaultLegList += "@dotnet-bot ${processedTriggerPhrase} | ${processedContext}\n"
            }
            else {
                nonDefaultLegList += "@dotnet-bot ${processedTriggerPhrase} | Queues ${processedContext}\n"
            }
        }

        // Construct the help message.
        String helpMessage = """${customHeader}

The following is a list of valid commands on this PR.  To invoke a command, comment the indicated phrase on the PR

**The following commands are valid for all PRs and repositories.**

<details>
  <summary>Click to expand</summary>

Comment Phrase | Action
-------------- | ------
@dotnet-bot test this please | Re-run all legs.  Use sparingly
@dotnet-bot test ci please | Generates (but does not run) jobs based on changes to the groovy job definitions in this branch
@dotnet-bot help | Print this help message
</details>
"""

        if (defaultLegList != "") {
            helpMessage += """
**The following jobs are launched by default for each PR against ${project}:${branchName}.**

<details>
  <summary>Click to expand</summary>

Comment Phrase | Job Launched
-------------- | ------------
${defaultLegList}
</details>
"""
        }

        if (nonDefaultLegList != "") {
            helpMessage += """
**The following optional jobs are available in PRs against ${project}:${branchName}.**

<details>
  <summary>Click to expand</summary>

Comment Phrase | Job Launched
-------------- | ------------
${nonDefaultLegList}
</details>
"""
        }

        helpMessage += """
${customFooter}"""

        def newJob = dslFactory.job('help_message') {
            // This job does nothing except update a build status message
            triggers {
                githubPullRequest {
                    useGitHubHooks()
                    // Add default individual admins here
                    admin('mmitche')
                    permitAll()
                    onlyTriggerPhrase(true)
                    triggerPhrase('(?i).*@dotnet-bot\\W+help.*')
                    extensions {
                        commitStatus {
                            context('Help Message')
                        }
                        buildStatus {
                            completedStatus('SUCCESS', helpMessage)
                        }
                    }

                    // We should only have a flat branch name, no wildcards
                    assert branchName.indexOf('*') == -1
                    whiteListTargetBranches([branchName])
                }
            }
            
            // Directly set the Github project property so that we don't have to actually clone source
            configure { node ->
                node / 'properties' / 'com.coravy.hudson.plugins.github.GithubProjectProperty' {
                    'projectUrl'("https://github.com/${project}")
                }
            }
            
            quietPeriod(0)
        }
    }
}
