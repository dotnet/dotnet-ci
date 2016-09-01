// This file generates all of the root jobs in a repo.  Basic generators, cleaners, etc.
// Does not use any utility functionality to make setup easy


// Create the main generator.
job("dotnet_dotnet-ci_generator") {
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

            relativeTargetDir('dotnet-ci')
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

    // Trigger a build when a change is pushed
    triggers {
        githubPush()
    }

    // Step is "process job dsls"
    steps {
        dsl {
            // Loads netci.groovy
            external('dotnet-ci/jobs/generation/MetaGenerator.groovy')

            // Additional classpath should point to the utility repo
            additionalClasspath('dotnet-ci')

            // Generate jobs relative to the seed job.
            lookupStrategy('JENKINS_ROOT')

            removeAction('DISABLE')
            removeViewAction('DELETE')
        }
    }

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
