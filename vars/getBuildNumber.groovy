/**
  * Retrieves formatted official build number.
  */
def call() {
    return '${BUILD_YEAR}${BUILD_MONTH, "XX"}${BUILD_DAY, "XX"}-${BUILDS_TODAY, "XX"}''
}