/**
  * Retrieves the user email that this job was associated with.
  * This is:
  *     dotnet-bot@microsoft.com (default) if triggered by a user.
  *     logged in github user's email - If triggered manually through the UI
  *     pr originator (ghprbPullAuthorEmail) - If triggered via the PR builder (even if triggered by another user)
  *     pr originator's github username + @github.login - If triggered via the PR builder and email is not available
  * @return User associated with this run.
  */
def call() {
    def isPRJob = isPR();
    if (isPRJob) {
        def versionControlLocation = getVersionControlLocation()
        if (versionControlLocation == "GitHub") {
            def ghPullUserEmail = env["ghprbPullAuthorEmail"]
            if (isNullOrEmpty(ghPullUserEmail)) {
                ghPullUserEmail = getUser() + "@github.login"
            }
            assert !isNullOrEmpty(ghPullUserEmail) : "Could not locate the pull author email."
            return ghPullUserEmail
        } else if (versionControlLocation == "VSTS") {
            assert false : "NYI VSTS PR"
        } else {
            assert false : "Unknown version control location"
        }
    }
    else {
        // Do some digging to determine how the job was launched.  There are a couple easy possibilities:
        
        // user ID cause -> manually launched runs
        def userIdCause = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)

        if (userIdCause != null) {
            def userId = userIdCause.getUserId()
            def userIdEmail = hudson.model.User.get(userId).getProperty(hudson.tasks.Mailer.class).getAddress()
            if (isNullOrEmpty(userIdEmail)) {
                userIdEmail = userId + "@github.login"
            }
            return userIdEmail
        }
        else {
            // Return dotnet-bot as the default
            return 'dotnet-bot@microsoft.com'
        }
    }
}
