/**
  * Retrieves the commit checked out in a PR.
  * If not a PR, then asserts.
  * @return ghprbActualCommit if a PR if GitHub (VSTS NYI)
  */
def call() {
    assert isPR() : "Not a PR"
    def versionControlLocation = getVersionControlLocation()
    if (versionControlLocation == "GitHub") {
        def commitSha = env["ghprbActualCommit"]
        assert !isNullOrEmpty(commitSha) : "Could not find commit sha"
        return commitSha
    } else if (versionControlLocation == "VSTS") {
        assert false : "NYI VSTS PR commit"
    } else {
        assert false : "Unknown version control location"
    }
}