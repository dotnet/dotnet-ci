/**
  * Returns what version control host this pipeline is running in
  * @return True if this was triggered as part of a PR, false otherwise.
  */
def call() {
    def vcLocation = env["VersionControlLocation"]
    assert !isNullOrEmpty(vcLocation) : "Version control location parameter not set (VersionControlLocation)"
    return vcLocation
}