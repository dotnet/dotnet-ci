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

TestUtilities.StartTest(out, "PR Trigger Creation - Multiple Skipped Branches - New")

def newJob = job(Utilities.getFullJobName(projectName, 'testJob', true)) {}
Utilities.setMachineAffinity(newJob, 'Windows_NT', 'latest-or-auto')
Utilities.standardJobSetup(newJob, projectName, true, "*/${branchName}")

// Use the trigger builder
TriggerBuilder builder = TriggerBuilder.triggerOnPullRequest()
builder.setGithubContext("PR Context")
builder.triggerByDefault()
builder.setCustomTriggerPhrase('trigger phrase')
builder.doNotTriggerForBranches(['branchA', 'branchB'])
builder.doNotTriggerForBranch('branchC')
builder.doNotTriggerForBranch('branchD')
builder.doNotTriggerForBranches(['branchE', 'branchF'])
builder.emitTrigger(newJob)

// Check using the job report utilities
assert JobReport.Report.prTriggeredJobs.containsKey(newJob.name)
assert JobReport.Report.prTriggeredJobs[newJob.name].context == 'PR Context'
assert JobReport.Report.prTriggeredJobs[newJob.name].triggerPhrase == 'trigger phrase'
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches.size() == 6
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches[0] == 'branchA'
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches[1] == 'branchB'
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches[2] == 'branchC'
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches[3] == 'branchD'
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches[4] == 'branchE'
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches[5] == 'branchF'
assert JobReport.Report.prTriggeredJobs[newJob.name].targetBranches.size() == 0
assert JobReport.Report.prTriggeredJobs[newJob.name].isDefault == true

TestUtilities.DisableJob(newJob)
TestUtilities.EndTest(out)