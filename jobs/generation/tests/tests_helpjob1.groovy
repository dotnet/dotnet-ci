// Tests for the .NET CI System
// This is effectively a netci.groovy file.  You can test new functionality by pointing a new line in the
// repo list against a branch of utilities.
// Rules about this file:
// 1) All jobs must be disable (to avoid weird launching scenarios. To do this, after, call "DisableJob(myJob)" after generation
// 2) All files must be named tests-<test name here>.groovy

// START - Put in all Files
package jobs.generation.tests

import jobs.generation.*

// The input project name (e.g. dotnet/corefx)
def projectName = GithubProject
// The input branch name (e.g. master)
def branchName = GithubBranchName
// END - PUT IN ALL FILES

TestUtilities.StartTest(out, "Help Job Creation - Current Branch Specific")

def newJob = job(Utilities.getFullJobName(projectName, 'testJob', true)) {}
Utilities.setMachineAffinity(newJob, 'Windows_NT', 'latest-or-auto')
Utilities.standardJobSetup(newJob, projectName, true, "*/${branchName}")

// Use the trigger builder
TriggerBuilder builder = TriggerBuilder.triggerOnPullRequest()
builder.setGithubContext("PR Context")
builder.setCustomTriggerPhrase('trigger phrase')
builder.triggerForBranch(branchName)
builder.emitTrigger(newJob)

// Generate a help job and then check the basics.  We can't check all the job properties but
// even just basic generation is important

def generatedJobs = Utilities.createHelperJob(this, projectName, branchName, '', '')

assert generatedJobs.size() == 1
assert generatedJobs[0].name == 'help_main'
// Use the reporting functionality to check some more
assert JobReport.Report.prTriggeredJobs.containsKey(generatedJobs[0].name)
assert JobReport.Report.prTriggeredJobs[generatedJobs[0].name].context == 'Help Message'
assert JobReport.Report.prTriggeredJobs[generatedJobs[0].name].originalTriggerPhrase == '(?i).*@dotnet-bot\\W+help.*'
assert JobReport.Report.prTriggeredJobs[generatedJobs[0].name].targetBranches.size() == 1
assert JobReport.Report.prTriggeredJobs[generatedJobs[0].name].targetBranches[0] == branchName
assert JobReport.Report.prTriggeredJobs[generatedJobs[0].name].skipBranches.size() == 0
assert JobReport.Report.prTriggeredJobs[generatedJobs[0].name].isDefault == false

generatedJobs.each { jerb -> 
    TestUtilities.DisableJob(jerb)
}
TestUtilities.DisableJob(newJob)
TestUtilities.EndTest(out)