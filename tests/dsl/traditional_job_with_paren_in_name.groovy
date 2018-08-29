// Import the utility functionality.

import jobs.generation.Utilities;

def project = QualifiedRepoName
def branch = BranchName

[true, false].each { isPR ->
    def jobName = Utilities.getFullJobName(project, "FooBarTest (Name - Config=0)", isPR)
    def newJob = job(jobName) {
        steps {
            batchFile("echo Hello World")
        }
    }

    Utilities.setMachineAffinity(newJob, "Windows_NT", 'latest-or-auto')
    Utilities.standardJobSetup(newJob, project, isPR, "refs/heads/${branch}")
	Utilities.addGithubPRTrigger(newJob, jobName)
}