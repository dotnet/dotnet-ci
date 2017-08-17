import jobs.generation.Utilities;

(ServerName != 'dotnet-ci4') { return } 
def project = GithubProject
def branch = GithubBranchName

// Generate a job to test ReproTool Plugin
def reproJob = job(Utilities.getFullJobName(project, 'simple_repro', false)) {
    steps {
        batchFile('fail.cmd')
    }
}
Utilities.standardJobSetup(reproJob, project, false, "*/${branch}")
