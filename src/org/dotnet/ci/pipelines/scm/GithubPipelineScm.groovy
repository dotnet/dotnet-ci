package org.dotnet.ci.pipelines.scm;

import jobs.generation.Utilities

class GithubPipelineScm implements PipelineScm {
    private String _project
    private String _branch
    private String _credentialsId

    public GithubPipelineScm(String project, String branch, String credentialsId) {
        _project = project
        _branch = branch
        _credentialsId = credentialsId
    }

    public GithubPipelineScm(String project, String branch) {
        _project = project
        _branch = branch
    }

    public String getBranch() {
        return _branch
    }

    public String getScmType() {
        return "GitHub"
    }

    /**
     * Emits the source control setup for a PR job
     *
     * @param job Pipeline job to apply the source control setup to
     * @param pipelineFile File containing the pipeline script, relative to repo root
     */
    void emitScmForPR(def job, String pipelineFile) {
        job.with {
            // Set up parameters for this job
            parameters {
                stringParam('sha1', '', 'Incoming sha1 parameter from the GHPRB plugin.')
                stringParam('GitBranchOrCommit', '${sha1}', 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                stringParam('GitRepoUrl', Utilities.calculateGitURL(this._project), 'Git repo to clone.')
                stringParam('GitRefSpec', '+refs/pull/*:refs/remotes/origin/pr/*', 'RefSpec.  WHEN SUBMITTING PRIVATE JOB FROM YOUR OWN REPO, CLEAR THIS FIELD (or it won\'t find your code)')
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${_project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')
                // Project name (without org)
                stringParam('GithubProjectName', Utilities.getProjectName(_project), 'Project name ')
                // Org name (without repo)
                stringParam('GithubOrgName', Utilities.getOrgName(_project), 'Project name passed to the DSL generator')
                stringParam('QualifiedRepoName', this._project, 'Combined GitHub org and repo name')
                stringParam('BranchName', Utilities.getBranchName(this._branch), 'Branch name (without */)')
            }

            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                // Sets up the project field to the non-parameterized version
                                github(this._project)
                                // Set the refspec to be the parmeterized version
                                refspec('${GitRefSpec}')
                                // Set URL to the parameterized version
                                url('${GitRepoUrl}')

                                if (this._credentialsId != null) {
                                    credentials(this._credentialsId)
                                }
                            }

                            // Set the branch
                            branch('${GitBranchOrCommit}')
                            
                            // Raise the clone timeout
                            extensions {
                                cloneOptions {
                                    timeout(30)
                                }
                            }
                        }
                    }
                    scriptPath(pipelineFile)
                }
            }
        }
    }

    // Emits the source control setup for a non-PR job
    // Parameters:
    //  job - Job to emit scm for
    //  pipelineFile - File containing the pipeline script, relative to repo root
    void emitScmForNonPR(def job, String pipelineFile) {
        job.with {
            // Set up parameters for this job
            parameters {
                stringParam('GitBranchOrCommit', "*/${this._branch}", 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${_project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')
                // Project name (without org)
                stringParam('GithubProjectName', Utilities.getProjectName(_project), 'Project name ')
                // Org name (without repo)
                stringParam('GithubOrgName', Utilities.getOrgName(_project), 'Project name passed to the DSL generator')
                stringParam('QualifiedRepoName', this._project, 'Combined GitHub org and repo name')
                stringParam('BranchName', Utilities.getBranchName(this._branch), 'Branch name (without */)')
            }

            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                github(this._project)

                                if (this._credentialsId != null) {
                                    credentials(this._credentialsId)
                                }
                            }

                            branch('${GitBranchOrCommit}')
                            
                            // Raise up the timeout
                            extensions {
                                cloneOptions {
                                    timeout(30)
                                }
                            }
                        }
                    }
                    scriptPath(pipelineFile)
                }
            }
        }
    }
}
