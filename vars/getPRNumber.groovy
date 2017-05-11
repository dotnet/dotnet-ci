/**
  * Retrieves the number of the PR.
  * @return ghprbPullId if a PR
  */
def call() {
    assert isPR() : "Not a PR"
    def pullId = env["ghprbPullId"]
    assert !isNullOrEmpty(pullId) : "Could not find pull id"
    return pullId
}