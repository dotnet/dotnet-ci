package org.dotnet.ci.pipelines.scm;

import jobs.generation.Utilities

class VSTSPipelineScm implements PipelineScm {
    private String _project
    private String _branch
    private String _credentials
    private String _collection

    public VSTSPipelineScm(String project, String branch, String credentials, String collection) {
        _project = project
        _branch = branch
        _credentials = credentials
        _collection = collection
    }

    public String getBranch() {
        return _branch
    }

    public String getScmType() {
        return "VSTS"
    }

    private String getGitUrl() {
        return Utilities.calculateVSTSGitURL(this._collection, this._project)
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
                stringParam('VSTSCollectionName', this._collection, 'VSTS collection name')
                stringParam('VSTSCredentialsId', this._credentials, 'VSTS credentials id')
                stringParam('VSTSRepoUrl', this.getGitUrl(), 'VSTS repo to clone.')
                stringParam('vstsRefspec', '', 'VSTS refspec')
                stringParam('vstsBranchOrCommit', '*/${this._branch}', 'VSTS commit hash')
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${_project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')

                stringParam('QualifiedRepoName', this._project, 'Combined GitHub org and repo name')
                stringParam('RepoName', Utilities.getRepoName(this._project), 'Repo name')
                stringParam('OrgOrProjectName', Utilities.getOrgOrProjectName(this._project), 'Organization/VSTS project name')
                stringParam('BranchName', Utilities.getBranchName(this._branch), 'Branch name')
                booleanParam('AutoSaveReproEnv', false, 'Save Repro Environment automatically for this job.')
                stringParam('VersionControlLocation', 'VSTS', 'Where the version control sits (VSTS or GitHub)')
            }

            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                // Sets up the project field to the non-parameterized version
                                url(this.getGitUrl())
                                // Set the refspec to be the parmeterized version
                                refspec('${vstsRefspec}')
                                // Set URL to the parameterized version
                                url('${VSTSRepoUrl}')
                                // Set the credentials, which are always required
                                credentials(this._credentials)
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
                stringParam('VSTSCollectionName', this._collection, 'VSTS collection name')
                stringParam('VSTSCredentialsId', this._credentials, 'VSTS credentials id')
                stringParam('VSTSRepoUrl', this.getGitUrl(), 'VSTS repo to clone.')
                stringParam('vstsRefspec', '+refs/heads/*:refs/remotes/origin/*', 'VSTS refspec')
                stringParam('vstsBranchOrCommit', "*/${this._branch}", 'VSTS commit hash')

                stringParam('GitBranchOrCommit', "*/${this._branch}", 'Git branch or commit to build.  If a branch, builds the HEAD of that branch.  If a commit, then checks out that specific commit.')
                stringParam('DOTNET_CLI_TELEMETRY_PROFILE', "IsInternal_CIServer;${_project}", 'This is used to differentiate the internal CI usage of CLI in telemetry.  This gets exposed in the environment and picked up by the CLI product.')
                stringParam('QualifiedRepoName', this._project, 'Combined GitHub org and repo name')
                stringParam('RepoName', Utilities.getRepoName(this._project), 'Repo name')
                stringParam('OrgOrProjectName', Utilities.getOrgOrProjectName(this._project), 'Organization/VSTS project name')
                stringParam('BranchName', Utilities.getBranchName(this._branch), 'Branch name')
                booleanParam('AutoSaveReproEnv', false, 'Save Repro Environment automatically for this job.')
                stringParam('VersionControlLocation', 'VSTS', 'Where the version control sits (VSTS or GitHub)')
            }

            definition {
                cpsScm {
                    scm {
                        git {
                            remote {
                                url(this.getGitUrl())
                                // Set the credentials, which are always required
                                credentials(this._credentials)
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
