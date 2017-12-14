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
def project = QualifiedRepoName
def branch = BranchName

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
        
        Utilities.setMachineAffinity(newJob, 'Windows_NT', 'latest')
        
        // This call performs remaining common job setup on the newly created job.
        // It does the following:
        //   1. Sets up source control for the project.
        //   2. Adds standard options for build retention and timeouts
        //   3. Adds standard parameters for PR and push jobs.
        //      These allow PR jobs to be used for simple private testing, for instance.
        // See the documentation for this function to see additional optional parameters.
        Utilities.standardJobSetup(newJob, project, isPR, "refs/heads/${branch}")
        
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

## General setup for a classic style job

```
// Import the utility functionality.
import jobs.generation.Utilities

// Grab the incoming parameters
def project = QualifiedRepoName
def branch = BranchName

// Create the name for a new job
def newJobName = Utilities.getFullJobName(project, "Linux_Debug", false /* not a PR */)
// Create the job with the name and add some build steps to it
def newJob = job(newJobName) {
    steps {
        shell('echo Hello World')
    }
}
// Call standard job setup to give the job default options, source control, etc.
Utilities.standardJobSetup(newJob, project, , false /* not a PR */, "refs/heads/${branch}")
// Set what type of machine it runs on
Utilities.setMachineAffinity(newJob, 'Ubuntu14.04', 'latest')
// And add some kind of trigger
Utilities.addGithubPushTrigger(newJob)
```

## What kind of functionality is available in a classic definition?

Below is presented a limited list of available functionality.  For a full listing, see [the Utilities class](../src/jobs/generation/Utilities.groovy) as well as [the job dsl reference](https://ci2.dot.net/plugin/job-dsl/api-viewer/index.html)

### Incoming parameters 

The generator job recieves a number of incoming parameters.  On all jobs, you'll find
* QualifiedRepoName - For VSTS repos, this is 'Project/RepoName', for GitHub this is 'Org/RepoName'.  This is typically used to pass information to the various utilities that create job functionality, like cloning source code.
* BranchName - Typically used to pass information to the various utilities that create job functionality, like cloning source code.  Also used to make triggers branch specific.
* GithubProject (*Deprecated*) - Same as QualifiedRepoName
* GithubBranchName (*Deprecated*) - Same as BranchName

### Build/Job construction functionality

* Utilities.getFullJobName(projectName, jobName, isPRJob) - Constructs a full job name based.  
* Utilities.setMachineAffinity(job, osName, version) - Sets 'job' to execute on a machine label obtained by looking up OS 'osName' at version 'version' in the image map.  Typically, the version is specific as 'latest'. The image map is located at https://github.com/dotnet/dotnet-ci/blob/master/src/org/dotnet/ci/util/Agents.groovy#L36. ***Note: This area of CI will come under change in the near future, and setMachineAffinity may change significantly.***
* Utilities.standardJobSetup(job project, isPR, branch) - Call after constructing a new job (see above).  Sets up the default options for a job, including source code.  'branch' should be passed in the form */branch (see above).
* Utilities.setJobTimeout(job, jobTimeoutInMinutes) - Must be called after standardJobSetup.  Sets the job timeout in minutes
* Utilities.addArchival(job, archivalSettings) - Adds archival with the set of specified settings.  A bit easier to use than the old style below.  Please see [ArchivalSettings](https://github.com/dotnet/dotnet-ci/blob/master/src/jobs/generation/ArchivalSettings.groovy) for complete information on constructing an archival setup. Example:
    ```
    def archivalSettings = new ArchivalSettings()
    archivalSettings.addFiles("**/artifacts/**")
    archivalSettings.excludeFiles("**/artifacts/${configName}/obj/**")
    archivalSettings.excludeFiles("**/artifacts/${configName}/tmp/**")
    archivalSettings.excludeFiles("**/artifacts/${configName}/VSSetup.obj/**")
    archivalSettings.setFailIfNothingArchived()
    archivalSettings.setArchiveOnFailure()

    Utilities.addArchival(job, archivalSettings)
    ```
* Utilities.addArchival(job, filesToArchive, filesToExclude (optional), doNotFailedIfNothingArchived (optional, defaults to false), archiveOnlyIfSuccessful (optional, defaults to true)) (*Deprecated*) - Adds archival to a job.  Files are archived on the Jenkins server based on the retention policy for the job.  filesToArchive and filesToExclude are glob syntax.
    
    ```
    Utilities.addArchival(newJob, '*.binlog', '', true, false) // archive *.binlog, don't exclude anything, don't fail if there are no files, archive in case of failure too
    ```
* Utilities.addXUnitDotNETResults(job, resultsFilePattern, skipIfNoTestFiles (optional, false)) - Finds and archives xunit results at the end of the job.  The results file pattern is a glob.  If skipIfNoTestFiles is left at false and there are no test result files, the job result will be failed.
* Utilities.addMSTestResults(job, resultsFilePattern, skipIfNoTestFiles (optional, false)) - Finds and archives MSTest results at the end of the job.  The results file pattern is a glob.  If skipIfNoTestFiles is left at false and there are no test result files, the job result will be failed.

### Triggers

* Utilities.addGithubPushTrigger(job) - For all source code branches cloned in the job, runs the job if any of them receives a push notification from GitHub.
* Utilities.addGithubPRTriggerForBranch(job, branchName, context, triggerPhraseString (regex, optional), triggerOnPhraseOnly (boolean, optional)) - Upon a PR to 'branchName', the job will be launched.  On GitHub a status check will appear in the PR with the text in 'context'.  If ommitted, the triggerPhrase becomes 'test ${context}'.  If specified, then by default the job will only run when a comment matches the trigger phrase regex.  This can be overriden by passing false for the optional 'triggerOnPhraseOnly'.
* Utilities.addVSTSPushTrigger(job) - For all source code branches cloned in the job, runs the job if any of them receives a push notification from VSTS.
* Utilities.addVSTSPRTrigger(job, branchName, contextString) - Triggers the job on every VSTS PR to branch 'branchName'. 'contextString' is displayed in the VSTS UI.
* Utilities.addPeriodicTrigger(job, cronString, alwaysRuns (optional, defaults to false)) - Triggers a job periodically, using the cronString.  If alwaysRuns is false, then the job will only run if the source code changes.