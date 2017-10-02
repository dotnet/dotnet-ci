/**
  * Retrieves the org associated the repository containing this pipeline.
  * This info is read from the input 'OrgOrProjectName' parameter.  If this is not
  * specified, then asserts.
  * @return Org/VSTS project associated with this pipeline.
  */
def call() {
    def githubOrgName = env["OrgOrProjectName"]
    assert !isNullOrEmpty(githubOrgName) : "Could not find OrgOrProjectName parameter"
    return githubOrgName
}