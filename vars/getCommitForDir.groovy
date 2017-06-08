/**
  * Retrieves the commit checked out in a specific directory.
  * Must be called under a workspace.  Uses git to determine the checked out
  * commit.
  * @return The commit checked out in a specific directory, fails the step otherwise.
  */
def call() {
    if (isUnix()) {
        def output = sh script: 'git rev-parse HEAD', returnStdout: true
    }
    else {
        def output = bat script: 'git rev-parse HEAD', returnStdout: true
    }
    return output
}