// This file generates all of the root jobs in a repo.  Basic generators, cleaners, etc.
// Does not use any utility functionality to make setup easy

// By default we use the master branch (empty impl version)
def sdkImplBranchSuffix = ''
// But, if the SDKImplementationVersion variable was set, then that gets passed down.
// It's the branch suffix we use, so 
def differentSDKImplVersion = binding.variables.get("SDKImplementationVersion")
if (differentSDKImplVersion) {
    sdkImplBranchSuffix = "-${differentSDKImplVersion}"
}

// Grab the branch name for the sdk impl.  Metagenerator is run on master. 
def sdkImplBranchName = "master${sdkImplBranchSuffix}"

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

        // Multi-scm.  Master pulls the repo list (kept in one place), the sdk implementation etc. is
        // pulled from master<sdk impl suffix>
        if (isPR) {
            multiscm {
                git {
                    remote {
                        github("dotnet/dotnet-ci")

                        // Set the refspec
                        refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                    }

                    branch("*/master")
                    
                    // On older versions of DSL this is a top level git element called relativeTargetDir
                    extensions {
                        relativeTargetDirectory('dotnet-ci-repolist')
                    }
                }
                git {
                    remote {
                        github("dotnet/dotnet-ci")

                        // Set the refspec
                        refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                    }

                    branch('${sha1}')
                    
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
                        github("dotnet/dotnet-ci")
                    }

                    // Repolist is always on master
                    branch("*/master")
                    
                    // On older versions of DSL this is a top level git element called relativeTargetDir
                    extensions {
                        relativeTargetDirectory('dotnet-ci-repolist')
                    }
                }
                git {
                    remote {
                        github("dotnet/dotnet-ci")
                    }

                    branch("*/${sdkImplBranchName}")
                 
                    // On older versions of DSL this is a top level git element called relativeTargetDir
                    extensions {
                        relativeTargetDirectory('dotnet-ci-sdk')
                    }
                }
            }
        }

        // Add a parameter which is the server name (incoming parameter to this job
        parameters {
            stringParam('ServerName', ServerName, "Server that this generator is running on")
            stringParam('SDKImplementationBranchSuffix', sdkImplBranchSuffix, "Suffix of branch for the metageneration that should be used for the SDK implementation")
            stringParam('RepoListLocation', 'dotnet-ci-repolist/jobs/data/repolist.txt', "Location of the repo list relative to the workspace root.")
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
        else {
            // Trigger a build when a change is pushed
            triggers {
                githubPush()
            }
        }

        // Step is "process job dsls"
        steps {
            dsl {
                // Generates the generator jobs
                external('dotnet-ci-sdk/jobs/generation/MetaGenerator.groovy')

                // Additional classpath should point to the sdk repo
                additionalClasspath('dotnet-ci-sdk')

                // Generate jobs relative to the seed job.
                lookupStrategy('SEED_JOB')

                removeAction('DISABLE')
                removeViewAction('DELETE')
            }
            
            // If this is a PR test job, we don't want the generated jobs
            // to actually trigger (say on a github PR, since that will be confusing
            // and wasteful.  We can accomplish this by adding another DSL step that does
            // nothing.  It will generate no jobs, but the remove action is DISABLE so the
            // jobs generated in the previous step will be disabled.

            if (isPR) {
                dsl {
                     text('// Generate no jobs so the previously generated jobs are disabled')

                     // Generate jobs relative to the seed job.
                     lookupStrategy('SEED_JOB')
                     removeAction('DISABLE')
                     removeViewAction('DELETE')
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
                github('dotnet/dotnet-ci')
            }
            branch("*/master")
        }
    }

    steps {
        systemGroovyScriptFile('jobs/scripts/disable_jobs_in_folder.groovy')
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
                github('dotnet/dotnet-ci')
            }
            branch("*/master")
        }
    }

    triggers {
        cron('0 0 * * *')
    }

    steps {
        systemGroovyScriptFile('jobs/scripts/workspace_cleaner.groovy')
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
                github('dotnet/dotnet-ci')
            }
            branch("*/master")
        }
    }

    triggers {
        cron('0 0 * * *')
    }

    steps {
        systemGroovyScriptFile('jobs/scripts/system_cleaner.groovy')
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
                github("dotnet/dotnet-ci")
            }
            branch("*/master")
        }
    }

    parameters {
        stringParam('GeneratorBuildNumber', '')
        stringParam('GeneratorJobName', '')
    }

    steps {
        systemGroovyScriptFile('jobs/scripts/generator_cleaner.groovy')
    }
}

// Create the temporary backlog cleaner. This cleans up the asynchronous backlog which
// causes memory leaks due to a problem with the workspace cleaner plugin.
job('temporary_backlog_cleaner') {
    logRotator {
        daysToKeep(7)
    }
    
    // Source is just basic git for dotnet-ci
    scm {
        git {
            remote {
                github("dotnet/dotnet-ci")
            }
            branch("*/master")
        }
    }
    
    triggers {
        cron('@hourly')
    }

    steps {
        systemGroovyScriptFile('jobs/scripts/backlog_cleaner.groovy')
    }
}
