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

TestUtilities.StartTest(out, "PR Trigger Creation - Branch Specific Non Default - Legacy")

def newJob = job(Utilities.getFullJobName(projectName, 'testJob', true)) {}
Utilities.setMachineAffinity(newJob, 'Windows_NT', 'latest-or-auto')
Utilities.standardJobSetup(newJob, projectName, true, "*/${branchName}")
Utilities.addGithubPRTriggerForBranch(newJob, branchName, 'PR Context', 'trigger phrase')

// Check using the job report utilities
assert JobReport.Report.prTriggeredJobs.containsKey(newJob.name)
assert JobReport.Report.prTriggeredJobs[newJob.name].context == 'PR Context'
assert JobReport.Report.prTriggeredJobs[newJob.name].triggerPhrase == 'trigger phrase'
assert JobReport.Report.prTriggeredJobs[newJob.name].targetBranches.size() == 1
assert JobReport.Report.prTriggeredJobs[newJob.name].targetBranches[0] == branchName
assert JobReport.Report.prTriggeredJobs[newJob.name].skipBranches.size() == 0
assert JobReport.Report.prTriggeredJobs[newJob.name].isDefault == false

TestUtilities.DisableJob(newJob)
TestUtilities.EndTest(out)