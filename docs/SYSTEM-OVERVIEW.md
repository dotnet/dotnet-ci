# .NET CI Overview

This document gives an overall layout of the .NET CI System.  How it's created, run, updated, etc.

## CI Workflow

Perhaps the best way to explain how the whole thing is put together is to explain what happens when someone wants to build X in CI on a per-commit and per-PR basis from a new repo.  Let's start from the beginning.  Let's say @mmitche wants to start a new project.  He creates a new repo in Github (let's call it dotnet/testproject), brings up a basic C# project, and decides it needs to be built when someone creates or updates a new PR, or when someone pushes a new commit.

The first thing that happens is that mmitche forks and clones the dotnet/dotnet-ci repo.  This repo contains a file that has a list of the repositories currently watched by the CI system.  He adds a line to that file indicating the repo name, the branch that he wants tested, and an optional folder name.  Github is set up to send a message to the CI server whenever a new commit is pushed to that repository (this is in the repo settings).  Upon merge, the CI server receives this message.

The basic unit of execution in Jenkins is the 'job'.  A job defines a set of work on a node.  The set of work might be to clone the dotnet/testproject repo, call build.cmd in the root, then archive all .dll files generated under bin/.  Jobs can be set up with a variety of triggers.  Triggers based on daily cron jobs, Github messages, source control polling, etc.

There are a few special jobs in Jenkins.  The primary ones are 'dotnet-ci_generator' and 'dotnet-ci-internal_generator'.  These jobs form the root of the CI system.  The 'dotnet-ci_generator' job is set up to trigger a build when a new commit is pushed to dotnet-ci.  This job clones the dotnet/dotnet-ci repo and runs something called a job-dsl build step.

Jenkins is configurable in lots of ways.  When projects and numbers of jobs are small, the primary way of configuring the system is do so through the UI.  The configuration UI is rich and usable, but as the size of a system increases, you quickly run into problems.  How do I change all jobs running for a repo to archive less data per run?  This is relatively simple if you have two jobs, but if you have 200, you're left with attempting to script the changes.  This is entirely possible.  The scripting interface of Jenkins is essentially as rich as the Java API itself (they interface together).  However, when you try this you quickly realize the tedium in doing this whenever you need to make wide-scale changes.  You might end up with 100 different custom scripts to do daily tasks.  There are other issues with a manually configured Jenkins.  How is configuration backed up and versioned?  Backing up the entire Jenkins on a daily basis is impossible.  The full size of the data is many terabytes.  Backing up might take a full day.  Backing up the configuration alone is problematic too  While the configuration data is simple xml, there may be version specific information in each of the files.  For instance, if two versions of the Git plugin may use different settings and have differing xml entries.  These entries may be tagged with version information about the Git plugin.  What do we do if we need to downgrade a plugin?

Enter Job DSL.  It forms the basis of a lot of the .NET CI functionality.  Essentially it's a build step that runs a script that can create jobs and other constructs within Jenkins.  A basic entry might look like:

    job('my-new-job') {
        steps {
            batchFile('echo Hello World')
        }
    }
    
This code, when run by the plugin, creates the xml associated with a new job called my-new-job that simply calls a batch file containing "echo Hello World".  This xml is then fed into Jenkins through an import API and the job appears.  There are tons of elements to job dsl, and plugins can extend it within the plugin source to ensure that the generated.  The language has the full scripting power of groovy, so there is a lot of flexibility in what it can do.  The Job DSL also keeps track of previous runs, which means it knows what Jenkins elements it generated before.  It can disable/delete removed items, update existing items, etc.  For more info, see here: https://jenkinsci.github.io/job-dsl-plugin/.

Back to the workflow.  So the repo line was added to the repo list text file and commited.  This caused the special dotnet-ci_generator to run. This runs a job-dsl step that reads the text file from the cloned repo and processes each line.  For each line that mentions a repo, it generates a folder for the repo and a new generator for that repo/branch combo.  That generator watches dotnet-ci and the target repo for changes, and is set up to run a Job DSL processing step on a specific file in the repo, called 'netci.groovy'.  This file contains code to generate job definitions for the repo/branch combo.  This means a few things:
* Job definitions are backed up and versioned in the repository.
* In case of server loss, starting from a single manually configured job (the root 'meta' generator) we can regenerate all of the jobs for all repos with only a few commands.
* Making wide-scale changes to a repository's jobs is now a lot easier.

So, now that dotnet/testproject has a new folder for its jobs, .NET CI is now watching the repo for changes.  @mmitche adds the netci.groovy file to the root of the dotnet/testproject repo and begins to write the CI definition.  While Job DSL is very powerful, it can also be a little verbose at times.  There is a lot of shared functionality between all jobs.  For instance, most jobs have timeouts of a few hours, most jobs want to clean the workspace before and after each run, and most jobs want some kind of retention policy on stored artifacts.  Enter the Utility functionality.  The netci.groovy files usually import a set of Utility functionality which exists in the dotnet-ci repository.  This contains a basic set of functionality commonly used by all CI definitions, packaged as a set of methods.  For example:

    Utilities.addGithubPRTrigger(job, "Innerloop Windows Build and Test")
    
This takes the job passed in, adds a Github build trigger that runs by default on every PR and prints "Innerloop Windows Build and Test" in the Github PR check status box.

Having finished the new CI definition, @mmitche submits a new PR for the netci.groovy file.  One thing to note is that the netci code isn't processed for PR.  Only committed code affects the jobs that run in the system.  The reasons for this are mostly technical, though there is some indication that it could be done.  So how does @mmitche test the new netci.groovy before merging?  Sadly, there isn't a way to test the jobs by running them, but it is possible to at least test the generation itself.  When the root meta-generator reads the repo list file, it also generates a special folder and generator under dotnet/testimpact's spot in CI, called GenPRTest.  This generator is triggered for a PR when a special phrase is commented in Jenkins (@dotnet-bot test ci).  This new generator processes the PR's netci, placing all generated jobs under the GenPRTest folder, then immediately disables them (so that they don't begin to be triggered).  If the generation is succesful, it means at least the jobs will succesfully regenerate when the PR is merged.  The launched GenPRTest generator job can also examined to view the generated jobs for accuracy.  As a side note, there is also a way to generate jobs locally on your machine direct to xml.  Diffing the output XML can be invaluable.

Upon merge, the dotnet/testimpact generator will run, read the netci.groovy file, and generate all the desired jobs and real work on the project can begin.

Let's say that the job added runs on Linux, builds the product and runs the tests.  When @mmitche creates a new PR that needs to be tests, Github sends a message to the CI server indicating various information about the pull request (hash, ID, user, etc.).  The CI server has a plugin waiting for such a message (the Github pull request builder) at a predetermined URL, and upon receiving it, determines what jobs it should build based on that message (matches the repo name, permissions, whether the job should only be launched for specific phrases, etc.).  It finds the job, sends status back to Github that it is queuing a new build (so that Github can update the UI), and queues the build.  Jobs are generally tied to types of machines.  They are often set up with a spefic label expression which indicates what nodes the build can execute on.  Each node in turn has a set of labels that are assigned.  A job might have label expression ```ubuntu && x64``` which will cause the CI system to attempt to find a node that has both the 'ubuntu' label and the 'x64' label.  This need for a node might be fulfilled by a cloud service.  Cloud services are designed to dynamically allocate nodes as necessary.  The CI system asks the cloud if it can satisfy the label expression, and if the cloud service says yes, the scheduler asks the cloud to allocate the node and then waits.  In .NET CI, we use the Azure Cloud plugin for a lot of dynamic node allocation.  It would spin up a new node, give it the appropriate labels and connect it to the CI server.  Jenkins would then have a new node on which to schedule work.

One the new build of the job is assigned a machine, it makes Github API calls to update its status and starts to run.  Upon completion, the status is updated one more time in Github and the run is complete.

Let's say @mmitche wanted a bit more testing.  He created a code coverage job that runs nightly, but also added a version that can be triggered on demand.  Much of the way pull requests interact with the CI system is through comments.  Comments can act as trigger phrases, so he added a special trigger phrase (@dotnet-bot test code coverage) to the code coverage job.  Commenting this phrase will queue the new job, update the status in Github, and run the workflow.

## More in-depth technical information

### General components

The following components are involved in the day-to-day operation of the .NET CI system:

* CI Server - Jenkins server that serves as the center and controller for the entire system.
* Github - Though not truly a CI component, Github sends messages to the CI Server to indicate that certain actions have been taken.  Some examples: When a PR is created, when a commit is pushed, when a comment is added to a pull request etc.  These messages are processed by plugins in the CI server to trigger actions, like new builds.
* Nodes - These nodes attach to Jenkins to do the actual work of testing.  These are rougly divides into 3 categories: automatically allocated VMs, statically allocated VMs, and static machines.  
* Azure - Azure is the primary home the CI Server and additional nodes.  The CI server also talks to Azure to allocate new nodes on demand.
* Controller Repos - There are two special repositories which contain information that drives the CI: dotnet/dotnet-ci and dotnet/dotnet-ci-internal.  These contain lists of repositories that the CI is responsible for, as well as additional scripting and utility functionality.

### Complex workflows and orchestration