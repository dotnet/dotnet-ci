/**
  * Retrieves the branch that the commit is associated with.
  * Because it's very difficult to tell branch, this just pulls the BranchName parameter.
  */
def call() {
    return env["BranchName"]
}