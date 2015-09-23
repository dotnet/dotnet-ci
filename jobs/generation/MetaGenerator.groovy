// This project will generate new job generators based on changes to the repolist.txt, which will
// in turn cause those generators to run over the job generation in the local repos.

import jobs.generation.Utilities

// About this generator:
//
//  This generator is designed to run every time there is a change to
//    * The dotnet-ci repo's generation dir
//    
//  A rerun of this could mean any number of things:
//    * Updated repolist.txt
//    * Updated utilities
//
//  Rerunning this generator will not automatically rerun all of the generators
//  below it (by design).  Users making changes to the overall generation structure
//  should recognize when there is a functional change being made and rerun all of the
//  generated jobs.

streamFileFromWorkspace('dotnet-ci/jobs/data/repolist.txt').eachLine { line ->
    // Skip comment lines
    boolean skip = (line ==~ / *#.*/);
    line.trim()
    skip |= (line == '')
    if (!skip) {
        def project = line
      
       	// Create a folder for the project
        // Folders do not have a way to not inherit the top level permissions
     	// currently.  This is problematic since it means that we can't put 
      	// names of non-open jobs in the folder name.  For now, label Private.
      	// Fix for the folder should be relatively trivial.
        def generatorFolder = Utilities.getFolderName(project)
        def generatorPRTestFolder = "${generatorFolder}/GenPRTest"
        
      	folder(generatorFolder) {}
        
        // Create a Folder for generator PR tests under that.
        folder(generatorPRTestFolder) {}
        
        [true, false].each { isPRTest ->
            def jobGenerator = job(Utilities.getFullJobName(project, 'generator', isPRTest, isPRTest ? generatorPRTestFolder : generatorFolder)) {
                // Need multiple scm's
                multiscm {
                    git {
                        remote {
                            github('dotnet/dotnet-ci')
                        }
                        relativeTargetDir('dotnet-ci')
                        branch('*/master')
                    }
                    // 
                    git {
                        remote {
                            github(project)
                            
                            if (isPRTest) {
                                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                            }
                        }
                        // Want the relative to be just the project name
                        relativeTargetDir(Utilities.getProjectName(project))
                        
                        // If PR, change to ${sha1} 
                        if (isPRTest) {
                            branch('${sha1}')
                        }
                        else {
                            branch('*/master')
                        }
                    }
                }
                
                // Add in the job generator logic
                
                steps {
                    dsl {
                        // Loads netci.groovy
                        external(Utilities.getProjectName(project) + '/netci.groovy')
                        
                        // Additional classpath should point to the utility repo
                        additionalClasspath('dotnet-ci')
                        
                        // Generate jobs relative to the seed job.        
                        lookupStrategy('SEED_JOB')
                        
                        // PR tests should do nothing with the other jobs.
                        // Non-PR tests should disable the jobs, which will get cleaned
                        // up later.
                        if (isPRTest) {
                            removeAction('IGNORE')
                        }
                        else {
                            removeAction('DISABLE')
                        }
                        removeViewAction('DELETE')
                    }
                    
                    // If this is a PR test job, we don't want the generated jobs
                    // to actually trigger (say on a github PR, since that will be confusing
                    // and wasteful.  We can accomplish this by adding another DSL step that does
                    // nothing.  It will generate no jobs, but the remove action is DISABLE so the
                    // jobs generated in the previous step will be disabled.
                    
                    if (isPRTest) {
                        dsl {
                             text('// Generate no jobs so the previously generated jobs are disabled')
                        
                             // Generate jobs relative to the seed job.        
                             lookupStrategy('SEED_JOB')
                             removeAction('DISABLE')
                             removeViewAction('DELETE')
                        }
                    }
                }
            }
            
            // Enable the standard options
            Utilities.addStandardOptions(jobGenerator)
            
            jobGenerator.with {
                // Disable concurrency
                concurrentBuild(false)
            }
            
            if (isPRTest) {
                // Enable the github PR trigger, but add a trigger phrase so
                // that it doesn't build on every change.
                Utilities.addGithubPRTrigger(jobGenerator, jobGenerator.name, 'test ci please')
            }
            else {
                // Enable the github push trigger
                Utilities.addGithubPushTrigger(jobGenerator)
            }
        }
    }
}