// This file generates all of the root jobs in a repo.  Basic generators, cleaners, etc.
// Does not use any utility functionality to make setup easy

// Check incoming parameters
assert binding.variables.get("ServerName") != null : "Expected string parameter with name ServerName corresponding to name of server"
assert binding.variables.get("SDKImplementationBranch") != null : "Expected name of branch where SDK is implemented"
assert binding.variables.get("RepoListLocationBranch") != null : "Expected name of branch where repo list is located"
assert binding.variables.get("RepoListLocation") != null : "Expected path to repo list"
assert binding.variables.get("VersionControlLocation") != null && 
       (binding.variables.get("VersionControlLocation") == 'VSTS' || 
       binding.variables.get("VersionControlLocation") == 'GitHub') : "Expected what version control this server targets (VSTS or GitHub)"

boolean isVSTS = binding.variables.get("VersionControlLocation") == 'VSTS'

// Create a folder for the PR generation of the dotnet-ci generation
folder('GenPRTest') {}

// Create the main generator and its PR test version
[true, false].each { isPR ->

    def fullJobName = ''
    if (isPR) {
        fullJobName = "GenPRTest/dotnet_dotnet-ci_generator_prtest"
    }
    else {
        fullJobName = "dotnet_dotnet-ci_generator"
    }
    
    def newJob = job(fullJobName) {
        logRotator {
            daysToKeep(7)
        }

        if (isPR) {
            multiscm {
                git {
                    remote {
                        if (isVSTS) {
                            url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                            credentials('vsts-dotnet-ci-trusted-creds')
                            // TODO: Set refspec for VSTS PR
                        }
                        else {
                            github("dotnet/dotnet-ci")
                            // Set the refspec
                            refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                        }
                    }

                    branch("refs/heads/${RepoListLocationBranch}")

                    extensions {
                        relativeTargetDirectory('dotnet-ci-repolist')
                    }
                }
                git {
                    remote {
                        if (isVSTS) {
                            url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                            credentials('vsts-dotnet-ci-trusted-creds')
                            // TODO: Set refspec for VSTS PR
                        }
                        else {
                            github("dotnet/dotnet-ci")
                            // Set the refspec
                            refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                        }
                    }

                    if (isVSTS) {
                        // TODO: PR branch
                    }
                    else {
                        branch('${sha1}')
                    }
                    
                    // On older versions of DSL this is a top level git element called relativeTargetDir
                    extensions {
                        relativeTargetDirectory('dotnet-ci-sdk')
                    }
                }
            }
        }
        else {
            multiscm {
                git {
                    remote {
                        if (isVSTS) {
                            url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                            credentials('vsts-dotnet-ci-trusted-creds')
                        }
                        else {
                            github("dotnet/dotnet-ci")
                        }
                    }

                    branch("refs/heads/${RepoListLocationBranch}")
                    
                    // On older versions of DSL this is a top level git element called relativeTargetDir
                    extensions {
                        relativeTargetDirectory('dotnet-ci-repolist')
                    }
                }
                git {
                    remote {
                        if (isVSTS) {
                            url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                            credentials('vsts-dotnet-ci-trusted-creds')
                        }
                        else {
                            github("dotnet/dotnet-ci")
                        }
                    }

                    branch("refs/heads/${SDKImplementationBranch}")

                    extensions {
                        relativeTargetDirectory('dotnet-ci-sdk')
                    }
                }
            }
        }

        // Add a parameter which is the server name (incoming parameter to this job
        parameters {
            stringParam('ServerName', ServerName, "Server that this generator is running on")
            stringParam('RepoListLocation', "dotnet-ci-repolist/${RepoListLocation}", "Location of the repo list relative to the workspace root.")
            stringParam('VersionControlLocation', VersionControlLocation, "Type of version control this CI server deals with")
            stringParam('SDKImplementationBranch', SDKImplementationBranch, "Where the SDK is implemented.  This is the default utilities repo branch")
        }

        // No concurrency, throttle among the other generators.
        // Not entirely sure this is required, but this is how it is today
        concurrentBuild(false)
        throttleConcurrentBuilds {
            throttleDisabled(false)
            maxTotal(0)
            maxPerNode(1)
            categories(['job_generators'])
        }
        
        label('!windowsnano16 && !performance && !dtap')

        if (isPR) {
            // Trigger on a PR test from the dotnet-ci repo
            triggers {
                if (isVSTS) {
                    // TODO: PR trigger for VSTS when available.
                }
                else {
                    githubPullRequest {
                        useGitHubHooks()
                        permitAll()
                        admin('mmitche')

                        extensions {
                            commitStatus {
                                context("${ServerName} MetaGeneration Test")
                                updateQueuePosition(true)
                            }
                        }
                        onlyTriggerPhrase(true)
                        triggerPhrase('(?i).*@dotnet-bot\\W+test\\W+metagen.*')
                    }
                }
            }
        }
        else {
            // Trigger a build when a change is pushed.
            triggers {
                if (isVSTS) {
                    teamPushTrigger()
                }
                else {
                    githubPush()
                }
            }
        }

        // Step is "process job dsls"
        steps {
            jobDsl {
                // Generates the generator jobs
                targets('dotnet-ci-sdk/src/jobs/generation/MetaGenerator.groovy')

                // Additional classpath should point to the sdk repo
                additionalClasspath('dotnet-ci-sdk/src')

                // Fail the build if a plugin is missing
                failOnMissingPlugin(true)

                // Generate jobs relative to the seed job.
                lookupStrategy('SEED_JOB')

                // Run in the sandbox
                sandbox(true)

                removedJobAction('DISABLE')
                removedViewAction('DELETE')
            }
            
            // If this is a PR test job, we don't want the generated jobs
            // to actually trigger (say on a github PR, since that will be confusing
            // and wasteful.  We can accomplish this by adding another DSL step that does
            // nothing.  It will generate no jobs, but the remove action is DISABLE so the
            // jobs generated in the previous step will be disabled.

            if (isPR) {
                jobDsl {
                     scriptText('// Generate no jobs so the previously generated jobs are disabled')
                     useScriptText(true)

                     // Generate jobs relative to the seed job.
                     lookupStrategy('SEED_JOB')
                     removedJobAction('DISABLE')
                     removedViewAction('DELETE')

                     sandbox(true)
                }
            }
        }

        // If not PRtest, then trigger the cleaner.  Otherwise fine
        if (!isPR) {
            publishers {
                // After we're done, trigger the cleaner job
                downstreamParameterized {
                    trigger('generator_cleaner') {
                        condition('SUCCESS')
                        parameters {
                            predefinedProp('GeneratorBuildNumber', '${BUILD_NUMBER}')
                            predefinedProp('GeneratorJobName', '${JOB_NAME}')
                        }
                    }
                }
            }
        }
    }
}

// Create the job disabler
job('disable_jobs_in_folder') {
    logRotator {
        daysToKeep(7)
    }

    // Set up parameters
    parameters {
        booleanParam('DryRunOnly', true, 'Do not actually disable jobs, just indicate what jobs would be disabled')
        booleanParam('DisableSubFolderItems', false, 'Disable items in subfolders of the target folder')
        stringParam('FolderName', '', 'Folder to disable.  Root folder not allowed')
    }

    scm {
        git {
            remote {
                if (isVSTS) {
                    url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                    credentials('vsts-dotnet-ci-trusted-creds')
                }
                else {
                    github("dotnet/dotnet-ci")
                }
            }
            branch("refs/heads/${SDKImplementationBranch}")
        }
    }

    // We stream from the workspace since in the groovy 2.0 plugin, the scripts
    // read from disk always execute in the sandbox. This is not the case with inline scripts.
    // This is a bug.  https://issues.jenkins-ci.org/browse/JENKINS-43700
    steps {
        // Rather
        systemGroovy {
            source {
                stringSystemScriptSource {
                    script {
                        script (readFileFromWorkspace('scripts/disable_jobs_in_folder.groovy'))
                        // Don't execute in sandbox
                        sandbox (false)
                    }
                }
            }
        }
    }
}

// Create the workspace cleaner
job('workspace_cleaner') {
    logRotator {
        daysToKeep(7)
    }

    logRotator {
        daysToKeep(7)
    }

    scm {
        git {
            remote {
                if (isVSTS) {
                    url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                    credentials('vsts-dotnet-ci-trusted-creds')
                }
                else {
                    github("dotnet/dotnet-ci")
                }
            }
            branch("refs/heads/${SDKImplementationBranch}")
        }
    }

    triggers {
        cron('0 0 * * *')
    }
       
    label('!windowsnano16 && !performance && !dtap')

    // We stream from the workspace since in the groovy 2.0 plugin, the scripts
    // read from disk always execute in the sandbox. This is not the case with inline scripts.
    // This is a bug.  https://issues.jenkins-ci.org/browse/JENKINS-43700
    steps {
        // Rather
        systemGroovy {
            source {
                stringSystemScriptSource {
                    script {
                        script (readFileFromWorkspace('scripts/workspace_cleaner.groovy'))
                        // Don't execute in sandbox
                        sandbox (false)
                    }
                }
            }
        }
    }
}

// Create the system cleaner
job('system_cleaner') {
    logRotator {
        daysToKeep(7)
    }

    scm {
        git {
            remote {
                if (isVSTS) {
                    url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                    credentials('vsts-dotnet-ci-trusted-creds')
                }
                else {
                    github("dotnet/dotnet-ci")
                }
            }
            branch("refs/heads/${SDKImplementationBranch}")
        }
    }

    triggers {
        cron('0 0 * * *')
    }

    // We stream from the workspace since in the groovy 2.0 plugin, the scripts
    // read from disk always execute in the sandbox. This is not the case with inline scripts.
    // This is a bug.  https://issues.jenkins-ci.org/browse/JENKINS-43700
    steps {
        // Rather
        systemGroovy {
            source {
                stringSystemScriptSource {
                    script {
                        script (readFileFromWorkspace('scripts/system_cleaner.groovy'))
                        // Don't execute in sandbox
                        sandbox (false)
                    }
                }
            }
        }
    }
}

// Temporary workaround for a deadlock in JNA.  Remove the swap space monitor from the list of monitors in Jenkins if it exists.
// https://issues.jenkins-ci.org/browse/JENKINS-39179 and https://github.com/java-native-access/jna/issues/652
job('swap_space_monitor_remover') {
    logRotator {
        daysToKeep(7)
    }

    scm {
        git {
            remote {
                if (isVSTS) {
                    url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                    credentials('vsts-dotnet-ci-trusted-creds')
                }
                else {
                    github("dotnet/dotnet-ci")
                }
            }
            branch("refs/heads/${SDKImplementationBranch}")
        }
    }

    // Run hourly
    triggers {
        cron('@hourly')
    }

    // We stream from the workspace since in the groovy 2.0 plugin, the scripts
    // read from disk always execute in the sandbox. This is not the case with inline scripts.
    // This is a bug.  https://issues.jenkins-ci.org/browse/JENKINS-43700
    steps {
        // Rather
        systemGroovy {
            source {
                stringSystemScriptSource {
                    script {
                        script (readFileFromWorkspace('scripts/disable_swap_space_monitor.groovy'))
                        // Don't execute in sandbox
                        sandbox (false)
                    }
                }
            }
        }
    }
}

// Node cleaner.  This is due to a bug or two in the azure vm agents plugin, which will block removal of agents that go offline
// for non-user caused reasons.  This removes those nodes once a day
job('node_cleaner') {
    logRotator {
        daysToKeep(7)
    }

    // Source is just basic git for dotnet-ci
    scm {
        git {
            remote {
                if (isVSTS) {
                    url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                    credentials('vsts-dotnet-ci-trusted-creds')
                }
                else {
                    github("dotnet/dotnet-ci")
                }
            }
            branch("refs/heads/${SDKImplementationBranch}")
        }
    }

    // Run every hour
    triggers {
        cron('@hourly')
    }

    // We stream from the workspace since in the groovy 2.0 plugin, the scripts
    // read from disk always execute in the sandbox. This is not the case with inline scripts.
    // This is a bug.  https://issues.jenkins-ci.org/browse/JENKINS-43700
    steps {
        // Rather
        systemGroovy {
            source {
                stringSystemScriptSource {
                    script {
                        script (readFileFromWorkspace('scripts/remove_offline_nodes.groovy'))
                        // Don't execute in sandbox
                        sandbox (false)
                    }
                }
            }
        }
    }
}

// Create the generator cleaner job
job('generator_cleaner') {
    logRotator {
        daysToKeep(7)
    }
    
    // Source is just basic git for dotnet-ci
    scm {
        git {
            remote {
                if (isVSTS) {
                    url("https://mseng.visualstudio.com/Tools/_git/DotNet-CI-Trusted")
                    credentials('vsts-dotnet-ci-trusted-creds')
                }
                else {
                    github("dotnet/dotnet-ci")
                }
            }
            branch("refs/heads/${SDKImplementationBranch}")
        }
    }

    parameters {
        stringParam('GeneratorBuildNumber', '')
        stringParam('GeneratorJobName', '')
    }

    // We stream from the workspace since in the groovy 2.0 plugin, the scripts
    // read from disk always execute in the sandbox. This is not the case with inline scripts.
    // This is a bug.  https://issues.jenkins-ci.org/browse/JENKINS-43700
    steps {
        // Rather
        systemGroovy {
            source {
                stringSystemScriptSource {
                    script {
                        script (readFileFromWorkspace('scripts/generator_cleaner.groovy'))
                        // Don't execute in sandbox
                        sandbox (false)
                    }
                }
            }
        }
    }
}
