# .NET CI Classic Jobs

This document details classic style .NET CI jobs.

## Overview

The classic style of job, typically called a freestyle job, is what .NET CI has used for quite a long time as its primary job type.  Unlike the newer pipeline model, the set of steps that a job will take, the machine it will run on, etc. are 'baked' into Jenkins when the job is created.  While this works fine, making changes to a classic freestyle job is more difficult because changes will not be reflected within a PR. Aside from a few helpers (like the ability to 'test generate a CI file'), changes to classic jobs must be commited to Jenkins internal state before the effects are seen.

## Classic (Freestyle) Workflow Today

1. User adds an entry to the [repo list](../data/repolist.txt).
2. Each jenkins server notices a change to the [repo list](../data/repolist.txt) and reads the file.  It creates a folder for each entry in the list that is targeted at the server and a 'generator' job which watches the repo/branch combo for the changes in the CI definition file (usually netci.groovy)
3. When a new CI definition file is committed to the repo, Jenkins runs the netci.groovy file.  This file is effectively a script that sets up state in Jenkins.  In this case, the state consists of the set of jobs, their triggers, views, etc. that are defined for the repo.
4. The created jobs start waiting for their triggers (PR test, PR comment, etc.)
5. Upon triggering, Jenkins reads the set of steps preconfigured in the job (source to clone, build steps to run, test results to publish) and executes them on the target machine pool configured in the job.

## What Does a Classic CI Definition Look Like?

Below is presented a 'classic' CI definition, annotated.

```
// Import the utility functionality.
import jobs.generation.Utilities;

// Defines a the new of the repo, used elsewhere in the file
def project = GithubProject
def branch = GithubBranchName

// Generate the builds for debug and release, commit and PRJob
[true, false].each { isPR -> // Defines a closure over true and false, value assigned to isPR
    ['Debug', 'Release'].each { configuration ->
        
        // Determine the name for the new job.  The first parameter is the project,
        // the second parameter is the base name for the job, and the last parameter
        // is a boolean indicating whether the job will be a PR job.  If true, the
        // suffix _prtest will be appended.
        def newJobName = Utilities.getFullJobName(project, configuration, isPR)
        
        // Define build string
        def buildString = """call \"C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\Common7\\Tools\\VsDevCmd.bat\" && build.cmd ${configuration}"""

        // Create a new job with the specified name.  The brace opens a new closure
        // and calls made within that closure apply to the newly created job.
        def newJob = job(newJobName) {
            // This opens the set of build steps that will be run.
            steps {
                // Indicates that a batch script should be run with the build string (see above)
                // Also available is:
                // shell (for unix scripting)
                batchFile(buildString)
            }
        }
        
        Utilities.setMachineAffinity(newJob, 'Windows_NT', 'latest-or-auto')
        
        // This call performs remaining common job setup on the newly created job.
        // It does the following:
        //   1. Sets up source control for the project.
        //   2. Adds standard options for build retention and timeouts
        //   3. Adds standard parameters for PR and push jobs.
        //      These allow PR jobs to be used for simple private testing, for instance.
        // See the documentation for this function to see additional optional parameters.
        Utilities.standardJobSetup(newJob, project, isPR, "*/${branch}")
        
        // The following two calls add triggers for push and PR jobs
        // In Github, the PR trigger will appear as "Windows Debug" and "Windows Release" and will be run
        // by default
        if (isPR) {
            Utilities.addGithubPRTriggerForBranch(newJob, branch, "Windows ${configuration}")
        }
        else {
            Utilities.addGithubPushTrigger(newJob)
        }
    }
}
```

## What kind of functionality is available in a classic definition?

Below is presented a limited list of available functionality.  For a full listing, see [the Utilities class](../src/jobs/generation/Utilities.groovy) as well as [the job dsl reference](https://ci2.dot.net/plugin/job-dsl/api-viewer/index.html)

## Build functionality

## Triggers