// This file generates all of the root jobs in a repo.  Basic generators, cleaners, etc.
// Does not use any utility functionality to make setup easy

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
            scm {
                git {
                    remote {
                        github("dotnet/dotnet-ci")

                        // Set the refspec
                        refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    }

                    branch('${sha1}')
                    
                    relativeTargetDir('dotnet-ci')
                }
            }
        }
        else {
            // Source is just basic git for dotnet-ci
            scm {
                git {
                    remote {
                        github("dotnet/dotnet-ci")
                    }
                    branch("*/master")

                    relativeTargetDir('dotnet-ci')
                }
            }
        }

        // Add a parameter which is the server name (incoming parameter to this job
        parameters {
            stringParam('ServerName', ServerName, "Server that this generator is running on")
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
                // Loads netci.groovy
                external('dotnet-ci/jobs/generation/MetaGenerator.groovy')

                // Additional classpath should point to the utility repo
                additionalClasspath('dotnet-ci')

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
