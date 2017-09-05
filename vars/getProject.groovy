/**
  * Retrieves the project associated with this pipeline.
  * This info is read from the input 'GitProjectName' parameter.  If this is not
  * specified, then asserts.
  * @return Repository associated with this run.
  */
def call() {
    def githubProjectName = env["ProjectName"]
    if(isNullOrEmpty(githubProjectName)) {
        githubProjectName = env["GithubProjectName"]
    }
    assert !isNullOrEmpty(githubProjectName) : "Could not find ProjectName (or GithubProjectName) parameter"
    return githubProjectName
}