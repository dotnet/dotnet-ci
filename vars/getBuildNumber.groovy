/**
  * Retrieves formatted official build number.
  * Depends on the Version Number Plugin (https://wiki.jenkins.io/display/JENKINS/Version+Number+Plugin)
  * to provide these variable values
  */
def call() {
    // The VersionNumber method takes a format string, and returns a conforming build number
    def versionNumber = VersionNumber('${BUILD_YEAR}${BUILD_MONTH, XX}${BUILD_DAY, XX}-${BUILDS_TODAY, XX}')
    assert versionNumber != null : "Version number is not a valid format, is the 'Version Number Plugin' installed?"
    return versionNumber
}