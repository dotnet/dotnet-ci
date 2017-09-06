/**
  * Retrieves the number of the PR.
  * @return ghprbPullId if a PR (GitHub).  VSTS NYI
  */
def call() {
    assert isPR() : "Not a PR"
    def versionControlLocation = getVersionControlLocation()
    if (versionControlLocation == "GitHub") {
        def pullId = env["ghprbPullId"]
        assert !isNullOrEmpty(pullId) : "Could not find pull id"
        return pullId
    } else if (versionControlLocation == "VSTS") {
        assert false : "NYI VSTS PR"
    } else {
        assert false : "Unknown version control location"
    }
}