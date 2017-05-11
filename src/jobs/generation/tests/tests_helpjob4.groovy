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

TestUtilities.StartTest(out, "Help Job Creation - Global Job With Skipped Jobs Help Job")

def newJob = job(Utilities.getFullJobName(projectName, 'testJob', true)) {}
Utilities.setMachineAffinity(newJob, 'Windows_NT', 'latest-or-auto')
Utilities.standardJobSetup(newJob, projectName, true, "*/${branchName}")

// Use the trigger builder
TriggerBuilder builder = TriggerBuilder.triggerOnPullRequest()
builder.setGithubContext("PR Context")
builder.doNotTriggerForBranch('branchA')
builder.doNotTriggerForBranch('branchB')
builder.emitTrigger(newJob)

// Generate a help job and then check the basics.  We can't check all the job properties but
// even just basic generation is important

def generatedJobs = Utilities.createHelperJob(this, projectName, branchName, '', '')

def genJobName
// Should have two jobs, one for main and one for the global triggers
assert generatedJobs.size() == 2
assert generatedJobs[0].name == 'help_main'
genJobName = generatedJobs[0].name
// Use the reporting functionality to check some more
assert JobReport.Report.prTriggeredJobs.containsKey(genJobName)
assert JobReport.Report.prTriggeredJobs[genJobName].context == 'Help Message'
assert JobReport.Report.prTriggeredJobs[genJobName].originalTriggerPhrase == '(?i).*@dotnet-bot\\W+help.*'
assert JobReport.Report.prTriggeredJobs[genJobName].targetBranches.size() == 1
assert JobReport.Report.prTriggeredJobs[genJobName].targetBranches[0] == branchName
assert JobReport.Report.prTriggeredJobs[genJobName].skipBranches.size() == 0
assert JobReport.Report.prTriggeredJobs[genJobName].isDefault == false

assert generatedJobs[1].name == 'help_skips_branchA_branchB'
genJobName = generatedJobs[1].name
// Use the reporting functionality to check some more
assert JobReport.Report.prTriggeredJobs.containsKey(genJobName)
assert JobReport.Report.prTriggeredJobs[genJobName].context == 'Help Message'
assert JobReport.Report.prTriggeredJobs[genJobName].originalTriggerPhrase == '(?i).*@dotnet-bot\\W+help.*'
assert JobReport.Report.prTriggeredJobs[genJobName].targetBranches.size() == 0
assert JobReport.Report.prTriggeredJobs[genJobName].skipBranches.size() == 2
assert JobReport.Report.prTriggeredJobs[genJobName].skipBranches[0] == 'branchA'
assert JobReport.Report.prTriggeredJobs[genJobName].skipBranches[1] == 'branchB'
assert JobReport.Report.prTriggeredJobs[genJobName].isDefault == false

generatedJobs.each { jerb -> 
    TestUtilities.DisableJob(jerb)
}
TestUtilities.DisableJob(newJob)
TestUtilities.EndTest(out)