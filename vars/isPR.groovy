/**
  * Determines whether the pipeline was invoked as part of a PR or not.
  * @return True if this was triggered as part of a PR, false otherwise.
  */
def call() {
    def repository = env["ghprbGhRepository"]
    return (!isNullOrEmpty(repository))
}