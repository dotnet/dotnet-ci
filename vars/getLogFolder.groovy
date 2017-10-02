/**
  * Get the folder where logs should be placed to be automatically archived.  This is a relative path
  * not ending in / 
  * @return Log folder.  Creates if necessary
  */
def call() {
    return "netci-archived-logs"
}