/**
  * Retrieves the branch that the commit is associated with.
  * Just like getCommit, this has some subtlety.
  * Becuase we can sync multiple repos, this command is sometimes dependent
  * on what directory we are in.  For a PR, it retrieves the pr target branch, regardless of directory.
  * If not in a PR, retrieves the branch of the current directory. In some cases, 
  * @return Commit for this pipeline invocation.
  */
def call() {
    if (isPR()) {
        return getTargetBranchForPR()
    }
    else {
        // This may not be reliable, but we can try to use GitBranchOrCommit.
        def branch = env["GitBranchOrCommit"]
        // This could theoretically be just a hash, but use it anyway.
        // It's likely it's prefixed with */, strip that off.
        if (branch.indexOf("*/") == 0) {
            return branch.substring(2)
        }
    }    
}