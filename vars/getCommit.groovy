/**
  * Retrieves the commit that the run should be associated with.
  * This is an interesting concept which requires some thought.
  * Becuase we can sync multiple repos, this command is sometimes dependent
  * on what directory we are in.  For a PR, it retrieves the pr commit, regardless of directory.
  * If not in a PR, retrieves the commit of the current directory. In almost all cases this
  * will return the commit that a user would expect. In cases where multiple repos are cloned,
  * then this would return the PR commit instead of the current directory. In those cases, using
  * getCommitForDir and getCommitForPR can disambiguate.
  * @return Commit for this pipeline invocation.
  */
def call() {
    if (isPR()) {
        return getCommitForPR()
    }
    else {
        return getCommitForDir()
    }    
}