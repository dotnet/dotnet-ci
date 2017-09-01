/**
  * Checkout the source indicated in the scm section of the pipeline.
  * Includes automatic retry.  The normal retry {} is problematic at least in some pipeline versions
  */
def call() {
    int checkoutTries = 10
    while (checkoutTries > 0) {
        checkoutTries--
        try {
            checkout scm
        } catch (e) {
            if (checkoutTries > 0) {
                echo "Failed to checkout, retrying. (${checkoutTries} attempts remaining)"
            } else {
                echo "Failed to checkout, Aborting. No attempts remaining"
            }
        }
    }
}