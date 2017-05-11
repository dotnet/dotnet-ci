/**
  * Retrieves the org associated the repository containing this pipeline.
  * This info is read from the input 'GitOrgName' parameter.  If this is not
  * specified, then asserts.
  * @return Org associated with this pipeline.
  */
def call() {
    def githubOrgName = env["GithubOrgName"]
    assert !isNullOrEmpty(githubOrgName) : "Could not find GithubOrgName parameter"
    return githubOrgName
}