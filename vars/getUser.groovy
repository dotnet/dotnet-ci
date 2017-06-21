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
        assert !isNullOrEmpty(ghPullUser) : "Could not locate the pull author login."
        return ghPullUser
    }
    else {
        // Do some digging to determine how the job was launched.  There are a couple easy possibilities:
        
        // user ID cause -> manually launched runs
        def userIdCause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)

        if (userIdCause != null) {
            return userIdCause.getUserId()
        }
        else {
            // Return dotnet-bot as the default
            return 'dotnet-bot'
        }
    }
}