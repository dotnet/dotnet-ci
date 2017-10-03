/**
  * Constructs the HelixSource submission parameter given the current environment.
  * @return Helix source
  */
def call() {
    def projectName = getRepo()
    def orgName = getOrganization()
    def branch = getBranch()
    if (isPR()) {
        return "pr/jenkins/${orgName}/${projectName}/${branch}/"
    }
    else {
        return "automated/jenkins/${orgName}/${projectName}/origin/${branch}/"
    }
}