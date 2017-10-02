/**
  * Determines whether the pipeline was invoked as part of a PR or not.
  * @return True if this was triggered as part of a PR, false otherwise.
  */
def call() {
    def versionControlLocation = getVersionControlLocation()
    if (versionControlLocation == "GitHub") {
        def repository = env["ghprbGhRepository"]
        return (!isNullOrEmpty(repository))
    } else if (versionControlLocation == "VSTS") {
        // PR is not supported at the moment, so just return false for now
        return false
    } else {
        assert false : "Unknown version control location"
    }
}