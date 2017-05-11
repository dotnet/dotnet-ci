/**
  * Retrieves the user that this job was associated with.
  * This is:
  *     dotnet-bot (default) if triggered by a user.
  *     logged in github user - If triggered manually through the UI
  *     pr originator (ghprbPullAuthorLogin) - If triggered via the PR builder (even if triggered by another user)
  * @return User associated with this run.
  */
def call() {
    def isPRJob = isPR();
    if (isPRJob) {
        def ghPullUser = env["ghprbPullAuthorLogin"]
        assert !isNullOrEmpty(ghPullUser) : "Could not locate the pull author login "
        return ghPullUser
    }
    else {
        assert false : "getUser() nyi for non-PR"
    }
}