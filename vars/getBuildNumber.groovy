/**
  * Retrieves formatted official build number.
  */
def call() {
    def versionNumber = VersionNumber('${BUILD_YEAR}${BUILD_MONTH, "XX"}${BUILD_DAY, "XX"}-${BUILDS_TODAY, "XX"}')
    return versionNumber
}