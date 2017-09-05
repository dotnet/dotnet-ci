import org.dotnet.ci.util.Agents

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
//  imageVersion - Version of the OS image.  See Agents.getMachineAffinity
//  body - Closure, see example
def call(String osName, version, Closure body) {
    node (Agents.getAgentLabel(osName, version)) {
        // Clean.  Currently processes are killed at the end of the node block, but we don't have an easy way to run the cleanup
        // after the node block exits currently.  Cleaning at the start should be sufficient.
        step([$class: 'WsCleanup'])
        // Make the log folder
        makeLogFolder()
        // Wrap in a try finally that cleans up the workspace
        try {
            timestamps {
                // Wrap in the default timeout of 120 mins
                timeout(120) {
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
                step([$class: 'WsCleanup'])
            }
            catch (e) {
                echo "Some files could not be cleaned up because of running processes.  These processes will be killed immediately and cleanup will happen before the node upon node-reuse"
            }
        }
    }
}
