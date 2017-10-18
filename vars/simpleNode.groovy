import org.dotnet.ci.util.Agents
import org.dotnet.ci.util.Constants

// Example:
//
// simpleNode('osx-10.12') { <= braces define the closure, implicitly passed as the last parameter
//     checkout scm
//     sh 'echo Hello world'
// }

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  label - label to use
//  timeoutInMinutes - Timeout in minutes for the body of the node.
//  body - Closure, see example
def call(String label, int timeoutInMinutes, Closure body) {
    node (label) {
        // Clean.  Currently processes are killed at the end of the node block, but we don't have an easy way to run the cleanup
        // after the node block exits currently.  Cleaning at the start should be sufficient.
        cleanWs deleteDirs: true, patterns: [[pattern: '**/*', type: 'INCLUDE']]
        // Make the log folder
        makeLogFolder()
        // Wrap in a try finally that cleans up the workspace
        try {
            timestamps {
                // Wrap in the default timeout of 120 mins
                timeout(timeoutInMinutes) {
                    body()
                }
            }
        }
        finally {
            try {
                // Archive anything in the standard log folder
                archiveLogs()
            }
            catch (e) {
                echo "Error during cleanup: ${e}"
            }
            
            // Clean the workspace.  Note that because processes might not yet be exited (e.g. VCBSCompiler.exe)
            // this could fail.  At the end of this node block, processes WILL be killed, but at this time it's a bit
            // late for workspace cleanup.
            try {
                if (!isUnix()) {
                    // For now, kill the most common culprit (return status instead of failing if the process wasn't found
                    bat script: 'taskkill /F /IM VBCSCompiler.exe', returnStatus: true
                }
                cleanWs deleteDirs: true, patterns: [[pattern: '**/*', type: 'INCLUDE']]
            }
            catch (e) {
                echo "Some files could not be cleaned up because of running processes.  These processes will be killed immediately and cleanup will happen before the node upon node-reuse"
            }
        }
    }
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  label - label to use
//  body - Closure, see example
def call(String label, Closure body) {
    simpleNode(label, Constants.DEFAULT_PIPELINE_TIMEOUT, body)
}

// Example:
//
// simpleNode('OSX10.12', 'latest') { <= braces define the closure, implicitly passed as the last parameter
//     checkout scm
//     sh 'echo Hello world'
// }

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  osName - Docker image to use
//  version - Version of the OS image.  See Agents.getMachineAffinity
//  body - Closure, see example
def call(String osName, String version, Closure body) {
    simpleNode(Agents.getAgentLabel(osName, version), Constants.DEFAULT_PIPELINE_TIMEOUT, body)
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  osName - Docker image to use
//  version - Version of the OS image.  See Agents.getMachineAffinity
//  timeoutInMinutes - Timeout in minutes for the body of the node
//  body - Closure, see example
def call(String osName, String version, int timeoutInMinutes, Closure body) {
    simpleNode(Agents.getAgentLabel(osName, version), timeoutInMinutes, body)
}