/**
  * Retrieves the commit checked out in a specific directory.
  * Must be called under a workspace.  Uses git to determine the checked out
  * commit.
  * @return The commit checked out in a specific directory, fails the step otherwise.
  */
def call() {
    if (isUnix()) {
        // Prefix with @ to avoid the command in the output
        def output = sh script: '@git rev-parse HEAD', returnStdout: true
        echo output
        return output
    }
    else {
        // Prefix with @ to avoid the command in the output
        def output = bat script: '@git rev-parse HEAD', returnStdout: true
        echo output
        return output
    }
}