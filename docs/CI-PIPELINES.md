# .NET CI Pipelines

This document details the new pipeline workflow available on some .NET CI Jenkins servers.  It covers an overview of the system, how to create pipelines, and how to tell Jenkins to run your pipeline.

# Overview

.NET CI uses Jenkins to enable CI for OSS projects in .NET Core (and a few others).  Traditionally, we have primarily used Jenkins 'freestyle' jobs to do most of the work.  These jobs are fairly ridgid in their workflow, made up of several predefined phases.  Their entire workflow is 'statically' configured in Jenkins by the Job DSL plugin and a commit to the definition script (typically called netci.groovy) is required to update that workflow.

While useful, these freestyle jobs have a number of drawbacks:
* It's not possible to change the workflow of the job in a PR.
* Multi-machine workflows require linking several freestyle jobs together.  This is difficult to define in the job dsl script (netci.groovy) and prone to error.
* The job dsl definition script (netci.groovy) file tends to become very difficult to decipher over time.  They tend to grow into a spaghetti code mass.

Jenkins Pipelines were introduced a few years back and became a 'first class' citizen in Jenkins with the Jenkins 2.0 overhaul.  They solve many of the issues with freestyle jobs. A pipeline job reads a script from source control and executes it.  This script interacts with the Jenkins CI system to allocate nodes, run commands, gather results, etc.

# .NET CI System Lifecycle - FreeStyle vs. Pipelines

Assuming some familiarity with the existing .NET CI system (netci.groovy), this section details the current FreeStyle (netci.groovy) lifecycle and how the Jenkins pipeline workflow is different.

## Freestyle Jobs Today

1. User adds an entry to the [repo list](../data/repolist.txt).
2. Each jenkins server notices a change to the [repo list](../data/repolist.txt) and reads the file.  It creates a folder for each entry in the list that is targeted at the server and a 'generator' job which watches the repo/branch combo for the changes in the CI definition file (usually netci.groovy)
3. When a new CI definition file is committed to the repo, Jenkins runs the netci.groovy file.  This file is effectively a script that sets up state in Jenkins.  In this case, the state consists of the set of jobs, their triggers, views, etc. that are defined for the repo.
4. The created jobs start waiting for their triggers (PR test, PR comment, etc.)
5. Upon triggering, Jenkins reads the set of steps preconfigured in the job (source to clone, build steps to run, test results to publish) and executes them on the target machine pool configured in the job.

## The New World Of Pipelines

1. User adds an entry to the [repo list](../data/repolist.txt).
2. Each jenkins server notices a change to the [repo list](../data/repolist.txt) and reads the file.  It creates a folder for each entry in the list that is targeted at the server and a 'generator' job which watches the repo/branch combo for the changes in a 'pipeline declaration' file (usually pipelines.groovy)
3. When a new pipeline declaration file is committed to the repo, Jenkins runs it. The pipeline declaration file is similar to the traditional CI definition file, but is more barebones.  It notes the pipeline script to run in the repo, the parameters to pass it, and the triggers on which to run it.  The entire workload logic is contained within the pipeline itself, which is read from source control.
4. The created pipeline jobs start waiting for their triggers (PR test, PR comment, etc.)
5. Upon triggering, Jenkins reads the specified pipeline script, typically from source control, passes it the default set of parameters configured in the pipeline declaration file, and executes the pipeline.

## What Does A Pipeline CI Declaration Look Like?

The new pipeline declaration files are similar in form to the traditional CI definitions, with much of the complexity taken out.

```
// Import the pipeline declaration classes.
import org.dotnet.ci.pipelines.Pipeline

// Declare a new pipeline.
def windowsPipeline = Pipeline.createPipeline(this, 'windows-pipeline.groovy')

// Over the array of 'Debug' and 'Release' configurations, generate a pipeline
// job that runs windows-pipeline on each push, passing in the parameter
// 'Configuration' with the value of 'configuration'
['Debug', 'Release'].each { configuration ->
    windowsPipeline.triggerPipelineOnPush(['Configuration':configuration])
}

// Over the array of 'Debug' and 'Release' configurations, generate a pipeline
// job that runs linux-pipeline.groovy on each PR, passing in the parameter
// 'Configuration' with the value of 'configuration'
def linuxPipeline = Pipeline.createPipeline(this, 'linux-pipeline.groovy')
// Pass Debug/Release configuration
['Debug', 'Release'].each { configuration ->
    linuxPipeline.triggerPipelineOnEveryPR(['Configuration':configuration])
}
```

## What Does A Pipeline Script Look Like?

A simple Jenkins pipeline script looks something like this:

```
String stringToPrint = 'foo2'
// 'node' indicates to Jenkins that the enclosed block runs on a node that matches
// the label 'windows-with-vs'
node ('windows-with-vs') {
    // 'stage' is primarily for visualization.  All steps within the stage block
    // show up in the UI under the heading 'Say Hello'.
    stage ('Say Hello') {
        // 'echo' prints the string to the pipeline log file
        // The echoed string is a multi-line string with no inline
        // replacement (single quotes)
        echo '''Hello World.
        This is a Jenkins pipeline.
        '''
    }
    stage ('Run some command') {
        // 'bat' Executes a batch script with the enclodes text.
        // In this case, the enclosed string is a multiline string with
        // inline replacement (double quotes).  The expression inside of ${} will
        // be replaced. Here, it's the stringToPrintVariable.
        bat """type foo > bar.txt
        type ${stringToPrint} >> bar.txt
        """
    }
    stage ('Archive artifacts') {
        // 'archiveArtifacts' Archives artifacts to the server for
        // later.
        archiveArtifacts allowEmptyArchive: false, artifacts: 'bar.txt'
    }
}
```

## What Can Be Changed In A PR? What Can't Be Changed?

In the traditional CI definition, not much could be changed in a PR.  Changes to the CI definition did not take effect until after commit.  There are ways to compile the definition (@dontet-bot test ci please), then manually enable and run new/modified jobs, but they are clunky.

Pipelines improve upon this substantially.  Becuase the pipeline script, where most of the workflow logic is defined, is read from source control, a pull request will pick up any changes to the script.  Unfortunately, the pipeline declaration is still run and created on commit, so the following changes will not show up in your PR, though they can still be verified with '@dotnet-bot test ci please'

* Changing file names of pipelines
* Adding/removing pipelines
* Changing/adding triggers for pipelines
* Parameters passed to pipelines

# Onboarding Your Project onto Pipelines

The following details the process by which you can onboard your projects onto the new pipelines.

1. Make a PR that adds an entry to the repo list for pipelines.  At the time of writing, the repo lists live in the dev/pipelinesupport branch of dotnet/dotnet-ci and dotnet/dotnet-ci-internal.  dotnet-ci should be used for OSS pipelines, dotnet-ci-internal for VSTS/private pipelines.
    1. [OSS](https://github.com/dotnet/dotnet-ci/blob/dev/pipelinesupport/data/repolist.txt)
    2. [VSTS/Internal](https://github.com/dotnet/dotnet-ci-internal/blob/dev/pipelinesupport/data/repolist.txt)
    3. The entry should have the following characteristics, in addition to the other typical parameters:
        * OSS GitHub pipelines
            * server=dotnet-ci3
            * utilitiesRepoBranch=dev/pipelinesupport
            * definitionScript=<path to declaration script in your repo>
        * VSTS pipelines
            * The first entry, is <project>/<repo-name>
            * server=dotnet-vsts
            * credentials=<please see mmitche for info, but generally
            * collection=<project collection server, mseng or devdiv>
            * utilitiesRepoBranch=dev/pipelinesupport
            * definitionScript=<path to declaration script in your repo>
2. Add your pipeline declaration script at the indicated place in your repo.   For a sample starter, please see the [sample](sample-declaration.groovy).  For a list of triggers, please see [Pipeline.groovy](../src/org/dotnet/ci/pipelines/Pipeline.groovy).  Methods that set up triggers for pipelines start with 'trigger'.  The triggers are also listed [below](#available-triggers).  It's recommended that you check-in your pipeline declaration prior to writing your pipeline script, with manual (triggerPipelineManually) or GitHub comment (triggerPipelineOnGithubPRComment) triggers enabled.  Then verification/development of your new pipeline can be done in PR or by manually launching it against your dev branch/fork.
3. Write your pipeline.  Please see [below](#writing-net-ci-pipelines) for information.

# Writing .NET CI Pipeline Declarations

Writing the CI declarations is quite simple.  After the initial import, you declare your pipeline to Jenkins.  The `this` pointer passed is the context that the CI declaration script is executing in.  It's used to generate jobs in Jenkins for each call to one of the `trigger*` methods.

```
// Declare a new pipeline.
def windowsPipeline = Pipeline.createPipeline(this, 'windows-pipeline.groovy')
```
Then simply determine what parameters you wish to pass to this pipeline script and what triggers you wish to use and make the appropriate calls to `trigger*` methods.

```
windowsPipeline.triggerPipelineOnPush(['Configuration':configuration])
```

## Available Triggers
* triggerPipelineOnEveryPR (GitHub, VSTS support coming soon) - Triggers the pipeline on every PR.
    `myPipeline.triggerPipelineOnEveryPR('GitHub Status Check Name', ['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelineOnEveryGithubPR (GitHub) - Triggers the pipeline on every PR.
    `myPipeline.triggerPipelineOnEveryGithubPR('GitHub Status Check Name', ['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelineOnEveryGithubPR (GitHub) - Triggers the pipeline on every PR with a custom re-trigger phrase
    `myPipeline.triggerPipelineOnEveryGithubPR('GitHub Status Check Name', ".*test\\W+my\\W+job.*", ['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelineOnGithubPRComment (GitHub) - Triggers the pipeline on a PR only when the status check text is commented.
    `myPipeline.triggerPipelineOnGithubPRComment('GitHub Status Check Name', ['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelineOnGithubPRComment (GitHub) - Triggers the pipeline on a PR only when a specific phrase is commented.
    `myPipeline.triggerPipelineOnGithubPRComment('GitHub Status Check Name', ".*test\\W+my\\W+job.*", ['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelineOnPush (GitHub/VSTS) - Triggers a pipeline on a push
    `myPipeline.triggerPipelineOnPush(['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelineOnVSTSPush (VSTS) - Triggers a pipeline on a push (VSTS specific)
    `myPipeline.triggerPipelineOnPush(['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelineOnGithubPush (GitHub) - Triggers a pipeline on a push (GitHub specific)
    `myPipeline.triggerPipelineOnPush(['paramA':'valueA', 'paramB,valueB'])`
* triggerPipelinePeriodically (GitHub/VSTS) - Triggers a pipeline on a shedule, specified by cron job syntax.
    `myPipeline.triggerPipelineOnPush('@hourly', ['paramA':'valueA', 'paramB,valueB'])`

# Writing .NET CI Pipelines

Writing .NET CI pipelines is significantly more straightforward than the old .NET CI job definitions.  Each pipeline should start with the following:

```
@Library('dotnet-ci') _
```

This pulls in the .NET CI SDK and imports all the global methods and utility functionality.

## Incoming Parameters

Parameters passed to the pipeline are implicitly added to the environment.  They are also accessible via local variables and the `params` local. For instance, if a pipeline had a String parameter called 'Configuration' set to 'Debug', the following would be true:
```
assert env['Configuration'] == Configuration
assert params.Configuration == env['Configuration']
```
Note that there is an important caveat to this.  Accessing variables via the local actually implicitly accesses the environment map.  Just like on a typical OS, the environment is simply a list of strings.  Therefore, if a boolean parameter is passed in, the local will refer to the string value of the boolean, not the true/false value.  
```
assert env['IsMyBooleanParam'] == true // Incorrect usage
assert IsMyBooleanParam == true // Incorrect usage
assert params.IsMyBooleanParam == true // Correct usage
```
Therefore, as a best practice when dealing with incoming parameters, accessing them as `params.ParamName` is safer and clearer.

### Default Parameters/Environment Variables

Jenkins injects a number of default parameters into pipelines, and the .NET CI SDK injects a few others.  Here are some of them:
* WORKSPACE - Workspace path on the node (only valid inside the a node block)
* BUILD_ID - ID of the build (name/number combo)
* BUILD_NUMBER - Number of the build
* GitBranchOrCommit (added by .NET CI) - Branch/commit to build
* RepoName (added by .NET CI) - Repository, with org/project name
* OrgOrProjectName (added by .NET CI) - GitHub org or VSTS Project name containing the repo

## Node/Docker Blocks

Node blocks wrap functionality the runs on an agent with a workspace.  Inside a node block you can run batch scripts, archive data, run tests, etc.  In .NET CI, you are encouraged to use two wrappers around Jenkins node functionality: `simpleNode` and `simpleDockerNode`.  These node blocks handle cleanup, label assignment, and a few other tasks for you.

```
// simpleNode allocates a new node with the OS as the first parameter and
// the version as the second.  These node names are resolved from the
// src/org/dotnet/ci/util/Agents.groovy in `getAgentLabel`.
// Users of the traditional CI job definitions will remember this as
// Utilities.setMachineAffinity.
simpleNode('Windows_NT','latest') {
    echo 'Hello World'
}
```
Docker is also supported:
```
// simpleDockerNode allocates a new node and runs docker on it, downloading the specified image and executing the enclosed block inside the container.  Cleanup is done for you.
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    echo 'Hello World'
}
```

## Checking Out Source
Source is not automatically checked out into the workspace at the allocation of a node, but there is an easy step for this. `checkout scm` checks out the same source that was used to checkout the pipeline script.  So if a pipeline was triggered on every PR run, Jenkins will read the pipeline script from the PR branch, and `checkout scm` would pull the pipeline script's repository at the same branch/commit.

Example:
```
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    stage ('Checking out Source') {
        // This step checks out the same source control that was configured
        // for the pipeline script itself.  'scm' is a variable representing the 
        // source control settings for the pipeline
        checkout scm
    }
}
```

You can also check out arbitrary repos using the `git` step.  These repos can even be checked out along side the regular scm.

Example:
```
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    stage ('Checking out Source') {
        // This step checks out the same source control that was configured
        // for the pipeline script itself.  'scm' is a variable representing the 
        // source control settings for the pipeline
        checkout scm
    }
    stage ('Checkout corefx') {
        // Creates a directory called corefx under the workspace, and all
        // commands under this have the 'corefx' current working directory
        dir('corefx') {
            git 'https://github.com/dotnet/coreclr'
        }
        dir('coreclr at another branch') {
            git url: 'https://github.com/dotnet/coreclr', branch: 'release/2.0.0'
        }
    }
}
```
## Parallelism
Jenkins pipelines allow for parallelism using the `parallel` statement.  The parallel statement takes a map of the names of the parallel branch of workflow (e.g. 'Build x64') to each corresponding workflow.  For example, a workflow that echoes two messages in parallel might look like:
```
parallel (
    'Hello World" : { echo 'Hello World'},
    'Hello Universe" : { echo 'Hello Universe'}
)
```
A similar workflow might be:
```
def parallelWork = [
    "x64 build" : {
        simpleNode('Windows_NT', 'latest') {
            checkout scm
            echo 'Build x64'
        }
    },

    "x86 build" : {
        simpleNode('Windows_NT', 'latest') {
            checkout scm
            echo 'Build x86'
        }
    }
]

parallel parallelWork
```
Failures in either branch will 
## Timeouts
Timeouts can be specified for blocks of pipeline code:
```
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    // If the enclosed block doesn't complete within 5 minutes,
    // then the pipeline will fail.
    timeout(5) {
        sh './build.sh'
        sh './test.sh'
    }
}
```
## Retries
Retries can be implemented fairly simply too.  HEre 
```
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    // If the enclosed block doesn't complete within 5 minutes,
    // due to the timeout, then the block will fail.  The block will retry up to 3
    // times before giving up.
    retry(3) {
        timeout(5) {
            sh './build.sh'
            sh './test.sh'
        }
    }
}
```
## Errors and error handling
Each pipeline step can have errors which will affect the outcome of the pipeline.  For instance, using `sh './build.sh'` will fail the current pipeline, or the current branch of a parallel workflow if build.sh returns a non-zero exit code.  Errors can also be thrown explicitly using the `error` step.
```
    simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
        if (params.Outerloop) {
            error 'Outerloop not supported in this docker container'.
        }
    }
```
Errors, which are effectively exceptions, can also be caught, rethrown, etc.  Just like in typical Java/Groovy/C# code, we can use `try/catch/finally` blocks:

```
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    timeout(5) {
        try {
            // We expect that publish.sh will fail right now, because the
            // publishing service is unavailable due to an outage.  It's not
            // critical for our build, so just continue.
            sh './publish.sh'
        }
        catch(err) {
            echo 'Publishing service down, continuing build!'
            // Log the failure so we can check it out.
            archiveArtifacts allowEmptyArchive: true, artifacts: "msbuild.log"
        }
    }
}
```
Maybe we want to do some logging upon failure:
```
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    try {
        // This step really mucks up the temp dir.  Make sure we clean this up
        // before continuing onto testing.
        sh './build.sh'
    }
    finally {
        sh './clean-temp.sh'
    }
    sh './test.sh'
}
```
And maybe we even want to log and rethrow:
```
simpleDockerNode('microsoft/dotnet-buildtools-prereqs:rhel7_prereqs_2') {
    try {
        // This step tends to fail, but we don't know why. Make sure we
        // log on failure, but propagate the error outward.
        sh './build.sh'
    }
    catch(e) {
        archiveArtifacts allowEmptyArchive: true, artifacts: "msbuild.log"
        throw e
    }
}
```
## Multi-machine workflows and stashing
One of the strengths of the Jenkins pipelines is the ability to orchestrate multiple machines in the same script. To do this, we simply write a pipeline that has multiple node blocks.  We can pass files between blocks any way we'd like (e.g. Azure Blob Storage), but one easy way is to use the `stash` and `unstash` steps to create a small temporary archive. Note that caution should be used to limit archive size.
```
simpleNode('Windows_NT','latest') {
    // Build some stuff
    bat 'build.cmd'
    // Stash this for a subsequent Linux job
    stash name: 'nt-build-artifacts', includes: 'bin/foo.dll'
}

simpleNode('Ubuntu14.04','latest') {
    dir ('linux-bin') {
        // Unstash to a different directory
        unstash 'nt-build-artifacts'
    }
    // Build some stuff
    sh './build.sh linux-bin/foo.dll'
}
```

## .NET CI SDK Utility Functionality
The .NET CI SDK, which is imported at the start of your pipelines, contains a number of utility steps, sometimes called 'global variables' in pipelines, which make life easier.  Each step is implemented in its own script file under the [vars](../vars) directory in this repository.  Most utility steps should be fairly self explanatory.  Please see the [defining global variables](https://jenkins.io/doc/book/pipeline/shared-libraries/#defining-global-variables) for info on how they work.  Some interesting ones are highlighted here
* getUser() - Attempts to get the user who started the run.  Can be useful for some reporting/submission steps
* getCommit() - Attempts to get the commit for this run.  For a PR, this will be the PR source commit.  For a push/manually triggered run it's the commit of the repo in the directory which getCommit() is called.  This is becuase a job can check out multiple repositories
* getLogFolder() - Gets the name of the log folder automatically created in the workspace when `simpleNode` or `simpleDockerNode` is used.  This folder is automatically archived before existing the node.
* getHelixSource() - For Helix submissions, attempts to construct a helix source for the run that can be passed to the Helix submission script.
* waitForHelixRuns(helixRunsBlob) - Given the json output of SubmittedHelixRuns.txt, wait for all of the runs to finish and report their status.  It does not need to be called inside a node block, and this should be avoided to improve efficiency.
    ```
    def logFolder = getLogFolder()
    def submittedHelixJson = readJSON file: "${logFolder}/SubmittedHelixRuns.txt"
    waitForHelixRuns(submittedHelixJson)
    ```
## .NET CI Pipeline Examples

* [CoreFX Linux pipeline](https://github.com/dotnet/corefx/blob/master/buildpipeline/linux.groovy).
* [CoreFX Windows pipeline](https://github.com/dotnet/corefx/blob/master/buildpipeline/windows.groovy).
* [CoreFX OSX pipeline](https://github.com/dotnet/corefx/blob/master/buildpipeline/osx.groovy).
* [Pipeline tests (written in pipeline)](../tests/pipeline/pipeline-tests.groovy)

# Additional Resources

Note that if you come across "Declarative Pipeline" references, these are not a pipeline form that we utilize today.  We use Scripted Pipelines.

* [Jenkins pipeline tutorial](https://github.com/jenkinsci/pipeline-plugin/blob/master/TUTORIAL.md)
* [Jenkins official pipeline tour](https://jenkins.io/doc/book/pipeline/)
* [Jenkins pipeline shared library globals](https://jenkins.io/doc/book/pipeline/shared-libraries/#defining-global-variables)
* [Pipeline syntax reference](https://jenkins.io/doc/book/pipeline/syntax/) - See bottom of page for Scripted syntax reference.
* [Pipeline Steps Reference For Installed Plugins](https://ci3.dot.net/pipeline-syntax/globals)