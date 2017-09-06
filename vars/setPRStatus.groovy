// Sets a commit status for a PR.  Relies on the following:
// pipeline must have been triggered by the GHPRB triggered
// other parameters (auth, etc.) must be present in environment and valid
// if they are not there, this step is skipped, commit status is echoed.
// This file is a workaround to the inability seamlessly set this today.
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.jenkinsci.plugins.ghprb.GhprbGitHubAuth;
import org.jenkinsci.plugins.ghprb.GhprbTrigger;
import org.kohsuke.github.GHCommitState;

def call(String context, String state, String url, String subMessage = '') {
    // Validate the state
    assert (state == "PENDING" || state == "SUCCESS" || state == "FAILURE" || state == "ERROR") : "Valid states are PENDING, SUCCESS, FAILURE and ERROR, was ${state}"
    GHCommitState ghState = GHCommitState.valueOf(state)

    // Gather required parameters.  If missing, echo to let the
    // owner know
    def credentialsId = env["ghprbCredentialsId"]
    if (isNullOrEmpty(credentialsId)) {
        echo "Could not find credentials ID (ghprbCredentialsId), ${context} (${subMessage}) is ${state}, see ${url}"
        return
    }

    // Grab the repository associated
    def repository = env["ghprbGhRepository"]
    if (isNullOrEmpty(repository)) {
        echo "Could not find repository name (ghprbGhRepository), ${context} (${subMessage}) is ${state}, see ${url}"
        return
    }
    // Find the commit commitSha
    def commitSha = env["ghprbActualCommit"]
    if (isNullOrEmpty(commitSha)) {
        echo "Could not find sha (ghprbActualCommit), ${context} (${subMessage}) is ${state}, see ${url}"
        return
    }

    try {
        GhprbGitHubAuth auth = GhprbTrigger.getDscp().getGitHubAuth(credentialsId);
        GitHub gh = auth.getConnection(currentBuild.rawBuild.getParent());
        // Null out the auth object so that Jenkins doesn't potentially ask to serialize it
        auth = null
        GHRepository ghRepository = gh.getRepository(repository);
        // Null out the gh object so that Jenkins doesn't potentially ask to serialize it
        gh = null
        // Create the state
        ghRepository.createCommitStatus(commitSha, ghState, url, subMessage, context);
        // Null out the ghRepository object so that Jenkins doesn't potentially ask to serialize it
        ghRepository = null
    }
    catch (e) {
        echo "Failed to create commit status for sha ${commitSha}: ${e.getMessage()}"
    }
}
