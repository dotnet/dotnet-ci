/**
  * Retrieves the commit checked out in a specific directory.
  * Must be called under a workspace.  Uses git to determine the checked out
  * commit.
  * @return The commit checked out in a specific directory, fails the step otherwise.
  */
def call() {
    def output
    if (isUnix()) {
        // Prefix with @ to avoid the command in the output
        output = sh script: 'git rev-parse HEAD', returnStdout: true
        
    }
    else {
        // Prefix with @ to avoid the command in the output
        output = bat script: '@git rev-parse HEAD', returnStdout: true
    }

    // Trim the output
    return output.trim()
}