/**
  * Checkout the source indicated in the scm section of the pipeline.
  * Includes automatic retry.  The normal retry {} is problematic at least in some pipeline versions
  */
def call() {
    retry (10) {
        timeout(15) {
            checkout scm
        }
    }
}
