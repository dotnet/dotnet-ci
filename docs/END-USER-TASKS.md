# End User Tasks

This document describes various tasks end users of the system may want to perform.  For information on writing CI scripting, see WRITING-NETCI.md

## The Github comment system and CI

Today, the primary way that users interact with the CI system doing pull requests is through the comment system.  Comments are fed back into the CI system, which then looks to see whether the commented phrases matches known sets of phrases which signal different things.  Most of the time these signals are job triggers.

While not required, when commenting to interact with the CI system, it's best practice to prefix your comment with '@dotnet-bot ' to avoid confusion.  For example, '@dotnet-bot test this please'.  Most phrases are not case, space, or line sensitive, and can be combined with other phrases in the same commment.

## Re-Queuing PR testing

PR testing might need to be re-queued for various reasons:

* A necessary fix got merged and you want to ensure your PR now passes
* Infrastructure issues (disk space, connection, etc.)
* Transient test issues.

** First and foremost, before you decide to re-queue testing, examine whether the failure you are trying to clear should instead be investigated and fixed.  Mashing retest phrases until you get what you want (green) doesn't help anyone in the long term**

There are two primary ways to do re-testing: general re-test and specific re-tests.  A general re-test will re-queue all jobs that run by default, while a specific test will only run those jobs associated with the phrase.

* 'test this' tells the CI system to rerun all testing associated by default with the PR (optional legs will not automatically re-queue).  At the time of writing, this will **not** cancel previous jobs, if any are running, though that functionality is in the works.
    Example: @dotnet-bot test this please
* 'test <job name from Github status>' tells the CI system to re-queue a job that was run by default.  If a created job was given no custom test phrase, the pull request context phrase (for instance, 'Ubuntu x64 Checked Build and Test') will automatically be associated with the job.  Commenting test followed by the context phrase will requeue just that job.  Again, this phrase is case insensitive.
    Example: @dotnet-bot test Ubuntu x64 Checked Build and Test please
    
## Queuing optional testing

There is LOTS of optional testing in Jenkins.  In general, most jobs that are created have a way to run them at PR time.  For coreclr, this might translate out to hundreds of potential jobs.  You can choose testing beyond the defaults to suit your PR's needs.  At the time of writing, there is no formal method of finding all the test phrases besides looking at the netci.groovy file in a repo.  The addGithubPullRequestTrigger (and addGithubPullRequestTriggerForBranch) take an optional parameter after the context which indicates the test phrase.  If the test phrase is supplied, commenting that phrase will queue the job.

Example:

    Utilities.addGithubPRTrigger(job, "${os} ${architecture} ${configuration} Build", '(?i).*test\\W+suse.*')
    
In this, saying 'test suse' Will queue up OpenSUSE testing.  Presumably in this case, the same test phrase was used across all configurations and architectures, so each job that was given that phrase will be queued.

In the future, there are plans for easy ways to determine what the phrases are, or avoid them altogether where possible.
