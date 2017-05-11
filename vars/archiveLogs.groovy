/**
  * Archive the auto-archived log folder
  */
def call() {
    String logFolder = getLogFolder()
    echo "Archiving logs in ${logFolder}/**"
    archiveArtifacts allowEmptyArchive: true, artifacts: "${logFolder}/**"
}