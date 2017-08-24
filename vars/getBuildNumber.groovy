import org.jvnet.hudson.tools.versionnumber
/**
  * Retrieves formatted official build number.
  */
def call() {
    VersionNumberBuilder versionNumberBuilder = new VersionNumberBuilder(
      '${BUILD_YEAR}${BUILD_MONTH, "XX"}${BUILD_DAY, "XX"}-${BUILDS_TODAY, "XX"}', 
      null,
      "BUILDNUMBER",
      null,
      null,
      null,
      null,
      null,
      null,
      false)
    return versionNumberBuilder.getVersionNumberString()
}