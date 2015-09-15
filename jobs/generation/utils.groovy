// This file contains utilities used in job creation throughout the dotnet-ci ecosystem.

// Returns the expected full name for a job given the desired job name
// and the project name.

def getFullJobName(def project, def jobName) {
  def projectJobName = project.replace('/', '_')
  
  if (jobName == '') {
    return projectJobName
  }
  else {
    return "${projectJobName}_${jobName}"
  }
}

// Add the standard options to a job.  Unless you
// have a good reason, you should call this on your job.
// job - Input job

def addStandardOptions(def job) {
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

// Adds a push trigger to a given job.

def addGithubPushTrigger(def job) {
  job.with {
   triggers {
      githubPush()
    } 
  }
}

// Sets up a build flow to build all jobs in jobList in parallel

def addBuildJobsInParallel(def buildFlowJob, def jobList) {
  def buildString = '';
  jobList.each { currentJob ->
    buildString += "  { build('${currentJob.name}') },\n"
  }
  buildFlowJob.with {
    buildFlow("""parallel (
${buildString}
)""")
  }
}