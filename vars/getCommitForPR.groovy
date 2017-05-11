/**
  * Retrieves the commit checked out in a PR.
  * If not a PR, then asserts.
  * @return ghprbActualCommit if a PR
  */
def call() {
    assert isPR() : "Not a PR"
    def commitSha = env["ghprbActualCommit"]
    assert !isNullOrEmpty(commitSha) : "Could not find commit sha"
    return commitSha
}