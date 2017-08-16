// Import the utility functionality.

import jobs.generation.Utilities;

def project = QualifiedRepoName
def branch = TargetBranchName

[true, false].each { isPR ->
    def newJob = job(Utilities.getFullJobName(project, "FooBarTest", isPR)) {
        steps {
            batchFile("echo Hello World")
        }
    }

    Utilities.setMachineAffinity(newJob, os, 'latest-or-auto')
    Utilities.standardJobSetup(newJob, project, isPR, "*/${branch}")
}