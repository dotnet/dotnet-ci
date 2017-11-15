// Import the utility functionality.

import jobs.generation.Utilities;

def project = QualifiedRepoName
def branch = BranchName

[true, false].each { isPR ->
    def newJob = job(Utilities.getFullJobName(project, "FooBarTest", isPR)) {
        steps {
            batchFile("echo Hello World")
        }
    }

    Utilities.setMachineAffinity(newJob, "Windows_NT", 'latest-or-auto')
    Utilities.standardJobSetup(newJob, project, isPR, "*/${branch}")

    // After calling standardJobSetup, modify the job so it checks out to a subdir
    Utilities.addScmInSubDirectory(newJob, project, isPR, 'mysubdir')
}