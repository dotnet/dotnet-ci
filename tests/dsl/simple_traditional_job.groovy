// Import the utility functionality.

import jobs.generation.Utilities;

def project = QualifiedRepoName
def branch = TargetBranchName

[true, false].each { isPR ->
    def newJob = job(Utilities.getFullJobName(project, "FooBarTest", isPR)) {
        steps {
            if (os == 'Windows_NT') {
                batchFile('''call "C:\\Program Files (x86)\\Microsoft Visual Studio 14.0\\Common7\\Tools\\VsDevCmd.bat" && build.cmd''')
            }
            else {
                shell('''./build.sh''')
            }
        }
    }

    Utilities.setMachineAffinity(newJob, os, 'latest-or-auto')
    Utilities.standardJobSetup(newJob, project, isPR, "*/${branch}")
}