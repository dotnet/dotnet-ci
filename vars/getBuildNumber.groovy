/**
  * Retrieves formatted official build number.
  */
def call() {
    println ("${BUILD_YEAR}")
    println ("${BUILD_MONTH}")
    println ("${BUILD_DAY}")
    println ("${BUILDS_TODAY}")
    return "${BUILD_YEAR}"
}