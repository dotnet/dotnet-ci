/**
  * Retrieves the project associated with this pipeline.
  * This info is read from the input 'RepoName' parameter.  If this is not
  * specified, then asserts.
  * @return Repository associated with this run.
  */
def call() {
    def repoName = env["RepoName"]
    assert !isNullOrEmpty(repoName) : "Could not find RepoName parameter"
    return repoName
}