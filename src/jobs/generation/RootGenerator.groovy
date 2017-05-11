// This file generates all of the root jobs in a repo.  Basic generators, cleaners, etc.
// Does not use any utility functionality to make setup easy

// By default we use the master branch (empty impl version)
def sdkImplBranchName = 'master'
// But, if the SDKImplementationVersion variable was set, then that gets passed down.
// It's the branch suffix we use, so 
def differentSDKImplVersion = binding.variables.get("SDKImplementationBranch")
if (differentSDKImplVersion) {
    sdkImplBranchName = differentSDKImplVersion
}

def repoListLocationBranchName = 'master'
// But, if the SDKImplementationVersion variable was set, then that gets passed down.
// It's the branch suffix we use, so 
def differentRepoListLocationBranchName = binding.variables.get("RepoListLocationBranch")
if (differentRepoListLocationBranchName) {
    repoListLocationBranchName = differentRepoListLocationBranchName
}


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
                        github("dotnet/dotnet-ci")

                        // Set the refspec
                        refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    }

                    branch("*/${repoListLocationBranchName}")
                    
                    // On older versions of DSL this is a top level git element called relativeTargetDir
                    extensions {
                        relativeTargetDirectory('dotnet-ci-repolist')
                    }
                }
                git {
                    remote {
                        github("dotnet/dotnet-ci")

                        // Set the refspec
                        refspec('+refs/pull/*:refs/remotes/origin/pr/*')
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

                    branch("*/${repoListLocationBranchName}")
                    
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
            stringParam('RepoListLocation', 'dotnet-ci-repolist/data/repolist.txt', "Location of the repo list relative to the workspace root.")
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
                github('dotnet/dotnet-ci')
            }
            branch("*/${sdkImplBranchName}")
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
                github('dotnet/dotnet-ci')
            }
            branch("*/${sdkImplBranchName}")
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
                github('dotnet/dotnet-ci')
            }
            branch("*/${sdkImplBranchName}")
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
            branch("*/${sdkImplBranchName}")
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

// Creates a job that pulls and updates the Azure VM templtes from
// the checked in information.
job('populate_azure_vm_templates') {
    logRotator {
        daysToKeep(7)
    }
    
    // Source is just basic git for dotnet-ci
    scm {
        git {
            remote {
                github("dotnet/dotnet-ci")
            }
            branch("*/${sdkImplBranchName}")
        }
    }

    parameters {
        stringParam('CloudSubscriptionCredentialsId', 'dotnet-social-cloud-vms')
        stringParam('VmTemplateDeclarations', 'data/azure-vm-templates.txt')
        booleanParam('TestOnly', true)
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
                        script (readFileFromWorkspace('scripts/populate-azure-vm-templates.groovy'))
                        // Don't execute in sandbox (needs approval)
                        sandbox (false)
                    }
                }
            }
        }
    }
}