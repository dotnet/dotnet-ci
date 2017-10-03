package jobs.generation;

class Utilities {

    private static String DefaultBranchOrCommitPR = '${sha1}'
    private static String DefaultBranchOrCommitPush = '*/master'
    private static String DefaultRefSpec = '+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*'

    /**
     * Get the folder name for a job.
     *
     * @param project Project name (e.g. dotnet/coreclr)
     * @return Folder name for the project. Typically project name with / turned to _
     */
    def static getFolderName(String project) {
        return project.replace('/', '_')
    }

    /**
     * Get the standard job name of a job given the base job name, project, whether the
     * job is a PR or not, and an optional folder
     *
     * @param jobName Base name of the job
     * @param isPR True if PR job, false otherwise
     * @param folder (optional) If folder is specified, project is not used as the folder name
     * @return Full job name.  If folder prefix is specified,
     */
    def static getFullJobName(String jobName, boolean isPR, String folder = '') {
        return getFullJobName('', jobName, isPR, folder);
    }

    /**
     * Get the standard job name of a job given the base job name, project, whether the
     * job is a PR or not, and an optional folder
     *
     * @param project Project name (e.g. dotnet/coreclr)
     * @param jobName Base name of the job
     * @param isPR True if PR job, false otherwise
     * @param folder (optional) If folder is specified, project is not used as the folder name
     *
     * @return Full job name.  If folder prefix is specified,
     */
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

    /**
     * Given the github full project name (e.g. dotnet/coreclr), get the
     * project name (coreclr)
     *
     * @param project Qualified project name
     *
     * @return Project name
     */
    def static getProjectName(String project) {
        return project.split('/')[1];
    }

    /**
     * Given the github full project name (e.g. dotnet/coreclr), get the
     * org name (dotnet)
     *
     * @param project Qualified project name
     *
     * @return Org name
     */
    def static getOrgName(String project) {
        return project.split('/')[0];
    }

    /**
     * Retrieve the branch name without the
     *
     * @param rawBranchName Branch name, potentially with star slash
     *
     * @return Branch namer without star slash
     */
    def private static getBranchName(String rawBranchName) {
        // Remove */ for branch name
        String branchName = rawBranchName
        if (rawBranchName.indexOf("*/") == 0) {
            branchName = rawBranchName.substring(2)
        }
        return branchName
    }

    /**
     * Define a set of OS names which can resore and use managed build tools with a non-default RID.
     * This controls the __PUBLISH_RID environment variable which affects init-tools behavior.
     * Entries placed in this list are temporary, and should be removed when NuGet packages are published
     * for the new OS.
     *
     * @param os The name of the operating system. Ex: Windows_NT, OSX.
     *
     * @return The name of an alternate RID to use while bootstrapping. If no RID mapping exists, returns null.
     */
    def static getBoostrapPublishRid(def os) {
        def bootstrapRidMap = [
            'Ubuntu16.10': 'ubuntu.16.04-x64',
            'Fedora24': 'fedora.23-x64'
        ]
        return bootstrapRidMap.get(os, null)
    }

    /**
     * Given the name of an OS, set the nodes that this job runs on.
     *
     * @param job Job to set affinity for
     * @param osName Name of OS to to run on.
     * @param version Optional version of the image.  This version can be the date potentially followed
     *                by .1, .2, etc. or it could be a static image version (like a perf label).
     */
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
                                // Contains the rootfs setup for arm builds.
                                '20170821':'ubuntu1404-20170821',
                                // 20170821 + clang 3.9
                                '20170925':'ubuntu1404-20170925',
                                // Contains Mono 5.0.1
                                'arm-cross-latest':'auto-ubuntu1404-20170120',
                                // Contains the rootfs setup for arm64 builds.
                                'arm64-cross-latest':'ubuntu1604-20170526',
                                // Pool of arm64 lab machines 4k page size
                                'arm64-small-page-size':'arm64_ubuntu',
                                // Pool of arm64 lab machines 64k page size
                                'arm64-huge-page-size':'arm64_ubuntu_huge_pages',
                                // Image installing the latest mono-devel
                                'latest-mono-devel':'ubuntu1404-20160211-1-latest-mono',
                                // Latest auto image.
                                'latest-or-auto':'ubuntu1404-20170925',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-ubuntu1404-201626outer',
                                // For internal Ubuntu runs
                                'latest-or-auto-internal':'auto-ubuntu1404-20160211.1-internal'
                                ],
                            'Ubuntu16.04' :
                                [
                                // 20170526 + clang 3.9
                                '20170925':'ubuntu1604-20170925',
                                // Contains the rootfs setup for arm64 builds.
                                'arm64-cross-latest':'ubuntu1604-20170925',
                                // Contains auto-ubuntu1604-20160803 + gdb + mono 4.6.2.16
                                '20170109':'ubuntu1604-20170109',
                                //Contains Mono 5.0.1
                                '20170731':'ubuntu1604-20170731',
                                // 20170731 + clang 3.9
                                '20170925-1':'ubuntu1604-20170925-1',
                                // Latest auto image.
                                'latest-or-auto':'ubuntu1604-20170925-1',
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
                                // 20170216 + clang 3.9
                                '20170925':'ubuntu1610-20170925',
                                // Latest auto image.  This will be used for transitioning
                                // to the auto images, at which point we will move back to
                                // the generic unversioned label except for special cases.
                                'latest-or-auto':'ubuntu1610-20170925',
                                // For outerloop runs.
                                'outer-latest-or-auto':'ubuntu1610-20170216-outer',
                                ],
                            // El Capitan
                            'OSX10.11' :
                                [
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
                            // High Sierra
                            'OSX10.13' :
                                [
                                // Generic version label
                                '' : 'osx-10.13',
                                // Latest auto image.
                                'latest-or-auto':'osx-10.13',
                                // For elevated runs
                                'latest-or-auto-elevated':'osx-10.13-elevated'
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
                                // win2016-20170303 + Python 3.2
                                '20170427' : 'win2012-20170809',
                                // win2016-20170303 + Python 3.2
                                '20170427-elevated' : 'win2012-20170809-elevated',
                                // the generic unversioned label except for special cases.
                                // Now contains VS2017
                                'latest-or-auto':'win2012-20170929',
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
                                'latest-or-auto-dev15-0':'win2016-20170712',
                                // For internal runs - Win2016 + VS15.1
                                'latest-dev15-1':'win2016-20170427',
                                // Win2016 + VS15.3 Preview1
                                'latest-or-auto-dev15-3':'win2016-20170507',
                                // Dev15 image
                                'latest-dev15':'auto-win2012-20160506',
                                // For internal runs
                                'latest-or-auto-internal':'auto-win2012-20160707-internal',
                                // For internal runs - Win2016 + VS15.RC2
                                'latest-or-auto-dev15-rc2-internal':'win2016-20170105-internal',
                                // For internal runs - Win2016 + VS15.RC4
                                'latest-or-auto-dev15-internal':'win2016-20170214-internal',
                                // For internal runs - Win2016 + VS15.0
                                'latest-or-auto-dev15-0-internal':'win2016-20170712-internal',
                                // For internal runs - Win2016 + VS15.1
                                'latest-dev15-1-internal':'win2016-20170427-internal',
                                // win2016-base + d15prerel-26423.1
                                'latest-d15prerel' : 'win2016-20170427-1',
                                // win2016-base + d15prerel-26423.1
                                'latest-d15prerel-internal' : 'win2016-20170531-internal',
                                // For internal runs which don't need/want the static 'windows-internal' pool
                                'latest-dev15-internal':'auto-win2012-20160707-internal',
                                // win2016-base + Dev15.3 preview 1
                                'latest-dev15-3-preview1' : 'win2016-20170510',
                                // win2016-base + Dev15.3 preview 1
                                'latest-dev15-3-preview1-internal' : 'win2016-20170510-internal',
                                // win2016-base + Dev15.3 preview 2
                                'latest-dev15-3-preview2' : 'win2016-20170613',
                                // win2016-base + Dev15.3 preview 2
                                'latest-dev15-3-preview2-internal' : 'win2016-20170613-internal',
                                // win2016-base + Dev15.3 preview 4
                                'latest-dev15-3-preview4' : 'win2016-20170717',
                                // win2016-base + Dev15.3 preview 4
                                'latest-dev15-3-preview4-internal' : 'win2016-20170717-internal',
                                // win2016-base + Dev15.3 preview 6
                                'latest-dev15-3-preview6' : 'win2016-20170731',
                                // win2016-base + Dev15.3 preview 6
                                'latest-dev15-3-preview6-internal' : 'win2016-20170731-internal',
                                // win2016-base + Dev15.3 preview 7
                                'latest-dev15-3-preview7' : 'win2016-20170802',
                                // win2016-base + Dev15.3 preview 7
                                'latest-dev15-3-preview7-internal' : 'win2016-20170802-internal',
                                // win2016-base + Dev15.3.4
                                'latest-dev15-3' : 'win2016-20170919',
                                // win2016-base + Dev15.3.4
                                'latest-dev15-3-internal' : 'win2016-20170919-internal',
                                // For elevated runs
                                'latest-or-auto-elevated':'win2012-20170929-elevated',
                                // For arm64 builds
                                'latest-arm64':'win2012-20170810',
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
                                // Docker 17.06.1-ee-2
                                'win2016-20170921':'win2016-20170921',
                                'latest-docker':'win2016-20170921',
                                // Latest auto image w/docker (move to latest-or-auto when possible)
                                'latest-containers':'win2016-20161018-1',
                                // Latest auto image.
                                'latest-or-auto':'auto-win2016-20160223'
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
                            'RHEL7.2' :
                                [
                                // 20170525 + clang 3.9
                                '20170928':'rhel72-20170928',
                                // Latest auto image.
                                'latest-or-auto':'rhel72-20170928',
                                // For outerloop runs.
                                'outer-latest-or-auto':'auto-rhel72-20160412.1outer'
                                ],
                            'CentOS7.1' :
                                [
                                // centos71-20170216 + clang 3.9
                                '20170926':'centos71-20170926',
                                // Latest auto image.
                                'latest-or-auto':'centos71-20170926',
                                // For outerloop runs.
                                'outer-latest-or-auto':'centos71-20170216-outer',
                                // For outerloop runs, using Linux kernel version 4.6.4
                                'outer-linux464': 'auto-auto-centos71-20160609.1-20160715outer'
                                ],
                            'Debian8.2' :
                                [
                                '20160323':'auto-deb82-20160323',
                                // Latest auto image.
                                'latest-or-auto':'auto-deb82-20160323'
                                ],
                            'Debian8.4' :
                                [
                                // 20170214 + clang 3.9
                                '20170925':'deb84-20170925',
                                // Latest auto image.
                                'latest-or-auto':'deb84-20170925',
                                // For outerloop runs
                                'outer-latest-or-auto':'deb84-20170214-outer'
                                ],
                            'Fedora24' :
                                [
                                // fedora24-20170420 + clang 3.9
                                '20170926':'fedora24-20170926',
                                // Latest auto image.
                                'latest-or-auto':'fedora24-20170926',
                                // For outerloop runs
                                'outer-latest-or-auto':'fedora24-20170420-outer'
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
                                'latest-or-auto':'!windowsnano16 && !performance && !dtap'
                                ]
                            ]
        def versionLabelMap = machineMap.get(osName, null)
        assert versionLabelMap != null : "Could not find os ${osName}"
        def machineLabel = versionLabelMap.get(version, null)
        assert machineLabel != null : "Could not find version ${version} of ${osName}"
        job.with {
            label(machineLabel)
        }

        // These are non-functioning images (were TP5), for now just disable the jobs till all the
        // groovy files can be updated.
        if (osName.equals('Windows 10')) {
            job.with {
                disabled(true)
            }
        }
    }

    /**
     * Performs standard job setup for a newly created job.
     * Includes: Basic parameters, Git SCM, and standard options
     *
     * @param job Job to set up.
     * @param project Name of project
     * @param isPR True if job is PR job, false otherwise.
     * @param defaultBranch If not a PR job, the branch that we should be building.
     */
    def static standardJobSetup(def job, String project, boolean isPR, String defaultBranch = '*/master') {
        String defaultRefSpec = getDefaultRefSpec(null)
        if (isPR) {
            defaultBranch = getDefaultBranchOrCommitPR(null)
        }
        standardJobSetupEx(job, project, isPR, defaultBranch, defaultRefSpec)
    }

    /**
     * Performs standard job setup for a newly created push job.
     * Includes: Basic parameters, Git SCM, and standard options
     *
     * @param job Job to set up.
     * @param project Name of project
     * @param defaultBranch Branch to build on push.
     */
    def static standardJobSetupPush(def job, String project, String defaultBranch = null) {
        defaultBranch = getDefaultBranchOrCommitPush(defaultBranch);
        standardJobSetupEx(job, project, false, defaultBranch, null);
    }

    /**
     * Performs standard job setup for a newly created push job.
     * Includes: Basic parameters, Git SCM, and standard options
     *
     * @param job Job to set up.
     * @param project Name of project
     * @param defaultBranchOrCommit Commit / branch to build.
     * @param defaultRefSpec the refs that Jenkins must sync on a PR job
     */
    def static standardJobSetupPR(def job, String project, String defaultBranchOrCommit = null, String defaultRefSpec = null) {
        defaultBranchOrCommit = getDefaultBranchOrCommitPR(defaultBranchOrCommit)
        defaultRefSpec = getDefaultRefSpec(defaultRefSpec)
        standardJobSetupEx(job, project, true, defaultBranchOrCommit, defaultRefSpec)
    }

    /**
     * Performs standard job setup for a newly created job.
     * Includes: Basic parameters, Git SCM, and standard options
     *
     * @param job Job to set up.
     * @param project Name of project
     * @param isPR True if job is PR job, false otherwise.
     * @param defaultBranchOrCommit Commit / branch to build.
     * @param defaultRefSpec the refs that Jenkins must sync on a PR job
     */
    def private static standardJobSetupEx(def job, String project, boolean isPR, String defaultBranchOrCommit, String defaultRefSpec) {
        Utilities.addStandardParametersEx(job, project, isPR, defaultBranchOrCommit, defaultRefSpec)
        Utilities.addScm(job, project, isPR)
        Utilities.addStandardOptions(job, isPR)
        addReproBuild(job)
    }

    /**
     * Set the job timeout to the specified value.
     *
     * @param job Input job to modify
     * @param jobTimeout Set the job timeout.
     */
    def static setJobTimeout(def job, int jobTimeout) {
        job.with {
            wrappers {
                timeout {
                    absolute(jobTimeout)
                }
            }
        }
    }

    /**
     * Adds a retention policy for artifacts
     *
     * @param job Job to modify
     * @param isPR True if the job is a pull request job, false otherwise.  If isPR is true,
     *             a more restrictive retention policy is use.
     */
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

    /**
     * Add standard options to a job.
     *
     * @param job Input job
     * @param isPR True if the job is a pull request job, false otherwise.
     */
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
        Utilities.setBuildEventWebHooks(job, ['helix-int-notification-url', 'helix-prod-notification-url'])
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

    /**
     * Sets up the job to fast exit if only certain paths were edited.
    * <p>
     * If only files in the paths were changed (these paths are evaluated as globs)
     * then the build exits early. Multiple calls to this function will replace the original
     * ignored paths.
     * </p>
     *
     * @param job Input job to modify
     * @param ignoredPaths Array of strings containing paths that should be ignored
     */
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

    /**
     * Adds an auto-retry to a job
     */
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
            'java.util.concurrent.ExecutionException: Invalid object ID',
            'hexadecimal value.*is an invalid character.', // This is here until NuGet cache corruption issue is root caused and fixed.
            'The plugin hasn\'t been performed correctly: Problem on deletion',
            'No space left on device'
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

    /**
     * Adds a github PR trigger for a job
     *
     * @param job Job to add the PR trigger for
     * @param contextString String to use as the context (appears in github as the name of the test being run).
     *                      If empty, the job name is used.
     * @param triggerPhraseString String to use to trigger the job.  If empty, the PR is triggered by default.
     * @param triggerOnPhraseOnly If true and trigger phrase string is non-empty, triggers only using the specified trigger
     *                            phrase.
     * @param permitAllSubmitters If true all PR submitters may run the job
     * @param permittedOrgs If permitAllSubmitters is false, at least permittedOrgs or permittedUsers should be non-empty.
     * @param permittedUsers If permitAllSubmitters is false, at least permittedOrgs or permittedUsers should be non-empty.
     * @param branchName If null, all branches are tested.  If not null, then is the target branch of this trigger
     */
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

    /**
     * Adds a github PR trigger only triggerable by member of certain organizations. Either permittedOrgs or
     * permittedUsers must be non-null.
     *
     * @param job Job to add the PR trigger for
     * @param  contextString String to use as the context (appears in github as the name of the test being run).
     *                       If empty, the job name is used.
     * @param triggerPhraseString String to use to trigger the job.  If empty, the PR is triggered by default.
     * @param triggerOnPhraseOnly If true and trigger phrase string is non-empty, triggers only using the specified trigger phrase.
     * @param permittedOrgs orgs permitted to trigger the job
     * @param permittedUsers users permitted to trigger the job
     */
    def static addPrivateGithubPRTrigger(def job, String contextString, String triggerPhraseString, boolean triggerPhraseOnly, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''
        assert triggerPhraseString != ''

        Utilities.addGithubPRTriggerImpl(job, null, contextString, triggerPhraseString, triggerPhraseOnly, false, permittedOrgs, permittedUsers)
    }

    /**
     * Adds a github PR trigger only triggerable by member of certain organizations
     *
     * @param job Job to add the PR trigger for
     * @param branchName If the target branch for the PR message matches this target branch, then the trigger is run.
     * @param contextString String to use as the context (appears in github as the name of the test being run).
     *                      If empty, the job name is used.
     * @param triggerPhraseString String to use to trigger the job.  If empty, the PR is triggered by default.
     * @param permittedOrgs If permitAllSubmitters is false, permittedOrgs should be non-empty list of organizations
     * @param branchName Branch that this trigger is specific to.  If a PR comes in from another branch, this trigger is ignored.
     */
    def static addPrivateGithubPRTriggerForBranch(def job, def branchName, String contextString, String triggerPhraseString, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''
        assert triggerPhraseString != ''

        Utilities.addGithubPRTriggerImpl(job, branchName, contextString, triggerPhraseString, true, false, permittedOrgs, permittedUsers)
    }

    /**
     * Adds a github PR trigger only triggerable by member of certain organizations
     *
     * @param job Job to add the PR trigger for
     * @param branchName If the target branch for the PR message matches this target branch, then the trigger is run.
     * @param contextString String to use as the context (appears in github as the name of the test being run).
     *                      If empty, the job name is used.
     * @param permittedOrgs If permitAllSubmitters is false, permittedOrgs should be non-empty list of organizations
     * @param branchName Branch that this trigger is specific to.  If a PR comes in from another branch, this trigger is ignored.
     */
    def static addDefaultPrivateGithubPRTriggerForBranch(def job, def branchName, String contextString, Iterable<String> permittedOrgs, Iterable<String> permittedUsers) {
        assert contextString != ''

        String triggerPhraseString = "(?i).*test\\W+${contextString}.*"

        Utilities.addGithubPRTriggerImpl(job, branchName, contextString, triggerPhraseString, false, false, permittedOrgs, permittedUsers)
    }

    /**
     * Adds a github PR trigger for a job that is specific to a particular branch
     *
     * @param job Job to add the PR trigger for
     * @param branchName If the target branch for the PR message matches this target branch, then the trigger is run.
     * @param contextString String to use as the context (appears in github as the name of the test being run).
     *                      If empty, the job name is used.
     * @param triggerPhraseString String to use to trigger the job.  If empty, the PR is triggered by default.
     * @param triggerOnPhraseOnly If true and trigger phrase string is non-empty, triggers only using the specified trigger
     *                            phrase.
     */
    def static addGithubPRTriggerForBranch(def job, String branchName, String contextString,
        String triggerPhraseString = '', boolean triggerOnPhraseOnly = true) {

        assert contextString != ''

        if (triggerPhraseString == '') {
            triggerOnPhraseOnly = false
            triggerPhraseString = "(?i).*test\\W+${contextString}.*"
        }

        Utilities.addGithubPRTriggerImpl(job, branchName, contextString, triggerPhraseString, triggerOnPhraseOnly, true, null, null)
    }

    /**
     * Adds a github PR trigger for a job
     *
     * @param job Job to add the PR trigger for
     * @param contextString String to use as the context (appears in github as the name of the test being run).
     *                      If empty, the job name is used.
     * @param triggerPhraseString String to use to trigger the job.  If empty, the PR is triggered by default.
     * @param triggerOnPhraseOnly If true and trigger phrase string is non-empty, triggers only using the specified trigger
     *                            phrase.
     */
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

    /**
     * Adds the standard parameters for PR and Push jobs.
     *
     * @param job Job to change
     * @param project Github project
     * @param isPR True if this is a PR job, false otherwise.
     * @param defaultBranch Branch to build by default if this is NOT a PR job. Defaults to *&#x2215;master.
     */
    def static addStandardParameters(def job, String project, boolean isPR, String defaultBranch = '*/master') {
        String defaultRefSpec = getDefaultRefSpec(null)
        if (isPR) {
            defaultBranch = getDefaultBranchOrCommitPR(null)
        }

        addStandardParametersEx(job, project, isPR, defaultBranch, defaultRefSpec)
    }

    /**
     * Adds the standard parameters for PR and Push jobs.
     *
     * @param job Job to set up.
     * @param project Name of project
     * @param isPR True if job is PR job, false otherwise.
     * @param defaultBranchOrCommit Commit / branch to build.
     * @param defaultRefSpec the refs that Jenkins must sync on a PR job
     */
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

    /**
     * Adds parameters to a non-PR job.  Right now this is only the git branch or commit.
     * This is added so that downstream jobs get the same hash as the root job
     */
    def private static addStandardNonPRParameters(def job, String project, String defaultBranch = '*/master') {
        job.with {
            parameters {
                stringParam('GitBranchOrCommit', defaultBranch, 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                booleanParam('ReproBuild', false, 'Check to enable repro functionality. This option is currently in development.')
                // Telemetry
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')
                stringParam('QualifiedRepoName', project, 'Combined Github/VSTS project/org and repo name')
                stringParam('BranchName', Utilities.getBranchName(defaultBranch), 'Branch name (without */)')
            }
        }
    }

    /**
     * Adds the private job/PR parameters to a job.  Note that currently this shouldn't used on a non-pr job because
     * push triggering may not work.
     */
    def static addStandardPRParameters(def job, String project, String defaultBranchOrCommit = null, String defaultRefSpec = null) {
        defaultBranchOrCommit = getDefaultBranchOrCommitPR(defaultBranchOrCommit)
        defaultRefSpec = getDefaultRefSpec(defaultRefSpec)

        job.with {
            parameters {
                stringParam('GitBranchOrCommit', defaultBranchOrCommit, 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                stringParam('GitRepoUrl', calculateGitURL(project), 'Git repo to clone.')
                stringParam('GitRefSpec', defaultRefSpec, 'RefSpec.  WHEN SUBMITTING PRIVATE JOB FROM YOUR OWN REPO, CLEAR THIS FIELD (or it won\'t find your code)')
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')
                booleanParam('ReproBuild', false, 'Check to enable repro functionality. This option is currently in development.')
                stringParam('QualifiedRepoName', project, 'Combined Github/VSTS project/org and repo name')
                stringParam('BranchName', Utilities.getBranchName(defaultBranchOrCommit), 'Branch name (without */)')
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
                            timeout(90)
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds private job/PR test SCM.  This is slightly different than normal
     * SCM since we use the parameterized fields for the refspec, repo, and branch
     */
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
                            timeout(90)
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds private permissions to a job, making it visible only to certain users
     *
     * @param job Job to modify
     * @param permitted Array of groups and users that are permitted to view the job.
     */
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

    /**
     * Add the ability to repro a failed job.
     *
     * @param job Job to modify
     */
    def static addReproBuild(def job) {
        job.with {
            publishers {
                reproToolPublisher {
                    StorageName("workspaceUpload")
                    StorageAccount("reproworkspace")
                    StorageContainer("workspace")
                    APIBaseURI("https://repro-tool.westus2.cloudapp.azure.com/")
                    CredentialsId("")
                }
            }
        }
    }

    /**
     * Archives data for a job when specific job result conditions are met.
     *
     * @param job Job to modify
     * @param settings Archival settings
     */
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

    /**
     * Archives data in Azure for a job when specific job result conditions are met.
     *
     * @param job Job to modify
     * @param storageAccount Storage account to use
     * @param settings Archival settings
     */
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

    /**
     * Archives data for a job
     *
     * @param job Job to modify
     * @param filesToArchive Files to archive. Comma separated glob syntax
     * @param filesToExclude Files to exclude from archival. Defaults to no files excluded. Comma separated glob syntax
     * @param doNotFailIfNothingArchived If true, and nothing is archived, will not fail the build.
     * @param archiveOnlyIfSuccessful If true, will only archive if the build passes. If false, then will archive always
     */
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

    /**
     * Adds a trigger that causes the job to run on a schedule.
     * If alwaysRuns is true, then this is a true cron trigger.  If false,
     * then only runs if source control has changed
     *
     * @param job Job to modify
     * @param cronString Cron job string indicating the frequency
     * @param alwaysRuns If true, the job will be a true cron job, if false, will run only when source has changed
     */
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

    /**
     * Adds xunit.NET v2 test results.
     *
     * @param job Job to modify
     * @param resultFilePattern Ant style pattern of test results. Defaults to **&#x2215;testResults.xml
     * @param skipIfNoTestFiles Do not fail the build if there were no test files found.
     */
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

    /**
     * Adds MSTest test results.
     *
     * @param job Job to modify
     * @param resultFilePattern Ant style pattern of test results.  Defaults to **&#x2215;testResults.xml
     * @param skipIfNoTestFiles - Do not fail the build if there were no test files found.
     */
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

    /**
     * Calls a web hook on Jenkins build events.  Allows our build monitoring jobs to be push notified
     * vs. polling.
     *
     * Each call to this overwrites the previous set of notifications
     *
     * @param job Job to add hook to
     * @param targetEndpoints List of credential IDs of the endpoints to run
     */
    private static void setBuildEventWebHooks(def job, def targetEndpoints) {
        job.with {
            properties {
                hudsonNotificationProperty {
                    endpoints {
                        targetEndpoints.each { targetEndpoint ->
                            // Use the secret endpoint to source the endpoint URL from credentials
                            endpoint {
                                urlInfo {
                                    urlType('SECRET')
                                    urlOrId(targetEndpoint)
                                }
                                event('all')
                                retries(3)
                            }
                        }
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

    /**
     * Adds cross integration to this repo
     */
    def static addCROSSCheck(def dslFactory, String project, String branch, boolean runByDefault = true) {
        def crossJob = dslFactory.job(Utilities.getFullJobName(project, 'CROSS_check', true)) {
            steps {
                // Download the tool
                batchFile("powershell -Command \"wget '%CROSS_SOURCE%' -OutFile cross-integration.zip\"")
                // Unzip it
                batchFile("powershell -Command \"Add-Type -Assembly 'System.IO.Compression.FileSystem'; [System.IO.Compression.ZipFile]::ExtractToDirectory('cross-integration.zip', 'cross-tool')\"")
                def orgName = Utilities.getOrgName(project)
                def repoName = Utilities.getProjectName(project)
                batchFile("cd cross-tool && cross --organization ${orgName} --repository ${repoName} --token %UPDATE_TOKEN% --writecomments --logfile cross-log.json %ghprbPullId% ")
            }

            // Ensure credentials are bound
            wrappers {
                // SAAS URL
                credentialsBinding {
                    string('CROSS_SOURCE', 'cross-sas-url')
                    string('UPDATE_TOKEN', 'cross-update-token')
                }
            }

            triggers {
                githubPullRequest {
                    useGitHubHooks()
                    permitAll()
                    extensions {
                        commitStatus {
                            context('CROSS Check')
                            statusUrl('https://wikilinkhere')
                        }
                    }
                    onlyTriggerPhrase(!runByDefault)
                    triggerPhrase('(?i).*test\\W+cross\\W+please.*')
                    whiteListTargetBranches([branch])
                }
            }
        }


        // Cross tool currently works on Windows only
        Utilities.setMachineAffinity(crossJob, 'Windows_NT', 'latest-or-auto')
        Utilities.addPrivatePermissions(crossJob, ['Microsoft'])
        Utilities.standardJobSetup(crossJob, project, true, "*/${branch}")
        Utilities.addArchival(crossJob, '**/cross-log.json')
    }

    /**
     * Creates the standard job view for a given folder. This does not create the folder.
     *
     * @param dslFactory The factory that is creating the view. In your primary groovy definitions, the parameter is 'this'
     * @param folderName The folder to create a view for.
     * @param jobName The name of the job, to use in customizing text. Defaults to folderName if not specified.
     * @param viewName The name to give the standard view. Defaults to 'Official Builds'.
     * @param filterRegex The regex that determines what jobs should display in the view.
     * @return The created view
     */
    def static addStandardFolderView(def dslFactory, String folderName, String jobName = null, String viewName = 'Official Builds', String filterRegex = /.*(?<!prtest)$/) {
        jobName = jobName ?: folderName
        // Create a view for all jobs in this folder that don't end with prtest
        return dslFactory.dashboardView("${folderName}/${viewName}") {
            recurse()
            jobs {
                regex(filterRegex)
            }
            statusFilter(StatusFilter.ENABLED)

            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
            }

            topPortlets {
                jenkinsJobsList {
                    displayName = "${jobName} jobs"
                }
            }

            rightPortlets {
                buildStatistics {
                    displayName 'Build Statistics'
                }
            }

            def createPortletId = {
                def random = new Random()
                return "dashboard_portlet_${random.nextInt(32000)}"
            }

            configure { view ->
                view / 'leftPortlets' / 'hudson.plugins.view.dashboard.stats.StatJobs' {
                    id createPortletId()
                    name 'Job Statistics'
                }

                def bottomPortlets = view / NodeBuilder.newInstance().bottomPortlets {}

                bottomPortlets << 'hudson.plugins.view.dashboard.core.UnstableJobsPortlet' {
                    id createPortletId()
                    name 'Unstable Jobs'
                    showOnlyFailedJobs 'false'
                    recurse 'true'
                }
                bottomPortlets << 'hudson.plugins.view.dashboard.test.TestStatisticsPortlet' {
                    id createPortletId()
                    name 'Test Statistics'
                    hideZeroTestProjects 'true'
                }
            }
        }
    }

    /**
     * Emits a helper job.  This job prints a help message to the GitHub comments section
     * Should only be emitted if the system has "ignore comments from bot" enabled.  The job is triggered
     * by commenting '@dotnet-bot help please'
     *
     * This function should be called at the *end* of the groovy file (it needs a list of the
     * generated jobs obtained through the reporting framework.
     *
     * Example
     * At the end of the netci.groovy:
     *  Utilities.createHelperJob(this, 'Welcome to CoreFX CI')
     *
     * Parameters:
     * @param dslFactory Factory object that can be used to create new jobs.  In your primary groovy
     *                   definitions, this parameter is "this".
     * @param project Current project (required)
     * @param branchName Branch this help message is defined for.
     * @param customHeader Custom header message prepended to the help message.
     * @param customFooter Footer message appended. Could be used to provide repo specific info (bug reports, known issues, etc.)
     */
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

            // Check Generate Disabled setting (for pr tests)
            if (GenerationSettings.isTestGeneration()) {
                disabled(true)
            }
        }
    }
}
