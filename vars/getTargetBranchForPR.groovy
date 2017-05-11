/**
  * Retrieves the target branch for a PR.
  * @return PR target branch, asserts if not PR.
  */
def call() {
    assert isPR() : "Not a PR"
    def targetBranch = env["ghprbTargetBranch"]
    assert !isNullOrEmpty(targetBranch) : "Could not find target branch"
    return targetBranch
}