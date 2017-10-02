/**
  * Make the log folder.
  */
def call() {
    String logFolder = getLogFolder()
    if (isUnix()) {
        sh "mkdir -p '${logFolder}'"
    }
    else {
        bat "if not exist \"${logFolder}\" mkdir \"${logFolder}\""
    }
}