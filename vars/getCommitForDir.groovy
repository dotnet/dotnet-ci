/**
  * Retrieves the commit checked out in a specific directory.
  * Must be called under a workspace.  Uses git to determine the checked out
  * commit.
  * @return The commit checked out in a specific directory, fails the step otherwise.
  */
def call() {
    def value
    if (isUnix()) {
        def output = sh 'git rev-parse HEAD', returnStdout: true
    }
    else {
        def output = bat 'git rev-parse HEAD', returnStdout: true
    }
    return output
}