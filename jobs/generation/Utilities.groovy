package jobs.generation;

class Utilities {

  def static getFolderName(def project) {
    return project.replace('/', '_')
  }
  
  def static getFullJobName(def project, def jobName, def isPR, def folder = '') {
    def jobSuffix = ''
    if (isPR) { 
      jobSuffix = '_prtest'
    }
    
    def folderPrefix = ''
    if (folder != '') {
        folderPrefix = "${folder}/"
    }

    if (jobName == '') {
      return "${folderPrefix}innerloop${jobSuffix}"
    }
    else {
      return "${folderPrefix}${jobName}${jobSuffix}"
    }
  }
  
  // Given the github full project name (e.g. dotnet/coreclr), get the
  // project name (coreclr)
  //
  // Parameters:
  //  project: Qualified project name
  //
  // Returns: Project name
  def static getProjectName(def project) {
    return project.split('/')[1];
  }
  
  // Given the github full project name (e.g. dotnet/coreclr), get the
  // org name (dotnet)
  //
  // Parameters:
  //  project: Qualified project name
  //
  // Returns: Org name
  def static getOrgName(def project) {
    return project.split('/')[0];
  }

  // Do the standard job setup
  // job - Input job
  // url - Github URL of project
  // 

  def static addStandardOptions(def job) {
    job.with {
      // Enable concurrent builds 
      concurrentBuild()

      // Enable the log rotator

      logRotator {    
        artifactDaysToKeep(7)
        daysToKeep(21)
        artifactNumToKeep(25)
      }

      wrappers {
        timeout {
          absolute(120)
        }
        timestamps()
      }
    }
  }

  def static addGithubPushTrigger(def job) {
    job.with {
      triggers {
        githubPush()
      } 
    }
  }

  def static addGithubPRTrigger(def job, def contextString = '') {
    def commitContext = contextString
    if (commitContext == '') {
        commitContext = job.name
    }
    
    job.with {
      triggers {
        pullRequest {
          useGitHubHooks()
          admin('Microsoft')
          admin('mmitche')
          permitAll()            
          extensions {
            commitStatus {
              context(commitContext)
            }
          }
        }
      }
    }
  }

  def static calculateGitURL(def project, def protocol = 'https') {
    // Example: git://github.com/dotnet/corefx.git 
  
    return "${protocol}://github.com/${project}.git"
  }

  // Adds parameters to a non-PR job.  Right now this is only the git branch or commit.
  // This is added so that downstream jobs get the same hash as the root job
  def static addStandardNonPRParameters(def job, def defaultBranch = '*/master') {
    job.with {
      parameters {
        stringParam('GitBranchOrCommit', defaultBranch, 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
      }
    }
  }

  // Adds the private job/PR parameters to a job.  Note that currently this shouldn't used on a non-pr job because
  // push triggering may not work.
  
  def static addStandardPRParameters(def job, def projectName, def protocol = 'https') {
    job.with {
      parameters {
        stringParam('GitBranchOrCommit', '${sha1}', 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
        stringParam('GitRepoUrl', calculateGitURL(projectName, protocol), 'Git repo to clone.')
        stringParam('GitRefSpec', '+refs/pull/*:refs/remotes/origin/pr/*', 'RefSpec.  WHEN SUBMITTING PRIVATE JOB FROM YOUR OWN REPO, CLEAR THIS FIELD (or it won\'t find your code)')
      }
    }
  }
}