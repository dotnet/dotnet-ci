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
  
  // Does simple setup for a job.  Used for easy setup when building a
  // bunch of jobs in a loop
  // 
  // Parameters:
  //  job - Job to modify
  //  project - Project the job belongs to
  //  isPR - True if the job is a PR job, false otherwise
  //  prContext - If the job is a PR job, it will get a PR trigger.  This context will
  //              show up in github for the PR.  If left blank, will use the job name.
  def static simpleInnerLoopJobSetup(def job, def project, def isPR, def prContext = '') {
    if (isPR) {
      if (prContext == '') {
        prContext = job.name
      }
      Utilities.addPRTestSCM(job, project)
      Utilities.addStandardPRParameters(job, project)
      Utilities.addGithubPRTrigger(job, prContext)
    }
    else {
      Utilities.addScm(job, project)
      Utilities.addStandardNonPRParameters(job)
      Utilities.addGithubPushTrigger(job)
    }
    
    Utilities.addStandardOptions(job)
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
      
      // Add option to ignore changes to netci.groovy when building
      configure {
        it / 'buildWrappers' / 'ruby-proxy-object' {
          'ruby-object' ('ruby-class': 'Jenkins::Plugin::Proxies::BuildWrapper', pluginid: 'pathignore') {
             pluginid(pluginid: 'pathignore', 'ruby-class': 'String', 'pathignore' )
             object('ruby-class': 'PathignoreWrapper', pluginid: 'pathignore') {
               ignored__paths(pluginid: 'pathignore', 'ruby-class': 'String', 'netci.groovy')
               invert__ignore(pluginid: 'pathignore', 'ruby-class': 'FalseClass')
             }
          }
        }
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

  // Adds a github PR trigger for a job
  // Parameters:
  //    job - Job to add the PR trigger for
  //    contextString - String to use as the context (appears in github as the name of the test being run).
  //                    If empty, the job name is used.
  //    triggerPhraseString - String to use to trigger the job.  If empty, the PR is triggered by default.
  //
  def static addGithubPRTrigger(def job, def contextString = '',  def triggerPhraseString = '') {
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
          
          if (triggerPhraseString != '') {
            onlyTriggerPhrase()
            triggerPhrase(triggerPhraseString)
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
  
  def static addScm(def job, def project, def buildBranch = '${GitBranchOrCommit}') {
    job.with {
      scm {
        git {
          wipeOutWorkspace()

          remote {
            github(project)
          }

          branch(buildBranch)
        }
      }
    }
  }
  
  // Adds private job/PR test SCM.  This is slightly different than normal
  // SCM since we use the parameterized fields for the refspec, repo, and branch
  def static addPRTestSCM(def job, def project) {
    job.with {
      scm {
        git {
          wipeOutWorkspace()

          remote {
            github(project)

            // Set the refspec
            refspec('${GitRefSpec}')

            // Reset the url to the parameterized version
            url('${GitRepoUrl}')
          }

          branch('${GitBranchOrCommit}')
        }
      }
    }
  }
  
  def static addArchival(def job, def filesToArchive, def filesToExclude = '',
        def doNotFailIfNothingArchived = false, def archiveOnlyIfSuccessful = true) {

        job.with {
            publishers {
                archiveArtifacts {
                    pattern(filesToArchive)
                    exclude(filesToExclude)
                    onlyIfSuccessful(archiveOnlyIfSuccessful)
                    allowEmpty(doNotFailIfNothingArchived)
                }
            }
        }
    }

    def static addHtmlPublisher(def job, def reportDir, def name, def reportHtml, def keepAllReports = true) {
        job.with {
            publishers {
                publishHtml {
                    report(reportDir) {
                        reportName(name)
                        keepAll(keepAllReports)
                        reportFiles(reportHtml)
                    }
                }
            }
        }
    }

    def static addPeriodicTrigger(def job, def cronString) {
        job.with {
            triggers {
                cron(cronString)
            }
        }
    }
    
    // Adds xunit.NET v2 test results.
    // TODO: Once native support for this type appears in the plugin (should be next version),
    // this code can be simplified.
    // Parameters:
    //
    //  job - Job to modify
    //  resultFilePattern - Ant style pattern of test results.  Defaults to **/testResults.xml
    //  skipIfNoTestFiles - Do not fail the build if there were no test files found.
    def static addXUnitDotNETResults(def job, def resultFilePattern = '**/testResults.xml', def skipIfNoTestFiles = false) {
        job.with {
            configure { node ->
                node / 'publishers' << {
                    'xunit'('plugin': 'xunit@1.97') {
                        'types' {
                            'XUnitDotNetTestType' {
                                'pattern'(resultFilePattern)
                                'skipNoTestFiles'(skipIfNoTestFiles)
                                'failIfNotNew'(true)
                                'deleteOutputFiles'(true)
                                'stopProcessingIfError'(true)
                            }
                        }
                        'thresholds' {
                            'org.jenkinsci.plugins.xunit.threshold.FailedThreshold' {
                                'unstableThreshold'('')
                                'unstableNewThreshold'('')
                                'failureThreshold'('')
                                'failureNewThreshold'('')
                            }
                            'org.jenkinsci.plugins.xunit.threshold.SkippedThreshold' {
                                'unstableThreshold'('')
                                'unstableNewThreshold'('')
                                'failureThreshold'('')
                                'failureNewThreshold'('')
                            }
                        }
                        'thresholdMode'('1')
                        'extraConfiguration' {
                            testTimeMargin('3000')
                        }
                    }
                }
            }
        }
    }
}
