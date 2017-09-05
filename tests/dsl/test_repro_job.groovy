import jobs.generation.Utilities;

def project = QualifiedRepoName
def branch = BranchName

// Generate a job to test ReproTool Plugin
def reproJob = job(Utilities.getFullJobName(project, 'simple_repro', false)) {
    steps {
        batchFile('fail.cmd')
    }
    label('test-vm')
}
Utilities.standardJobSetup(reproJob, project, false, "*/${branch}")
