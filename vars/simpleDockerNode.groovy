import org.dotnet.ci.util.Agents
import org.dotnet.ci.util.Constants

// Example:
//
// simpleDockerNode('foo:bar') { <= braces define the closure, implicitly passed as the last parameter
//     checkout scm
//     sh 'echo Hello world'
// }

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  timeoutInMinutes - Timeout in minutes for the body
//  body - Closure, see example
def call(String dockerImageName, int timeoutInMinutes, Closure body) {
    simpleDockerNode(dockerImageName, 'latest', timeoutInMinutes, body)
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  body - Closure, see example
def call(String dockerImageName, Closure body) {
    simpleDockerNode(dockerImageName, 'latest', Constants.DEFAULT_PIPELINE_TIMEOUT, body)
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  hostVersion - Host VM version.  See Agents.getDockerMachineAffinity for explanation.
//  timeoutInMinutes - Timeout in minutes for the body
//  body - Closure, see example
def call(String dockerImageName, String hostVersion, Closure body) {
    simpleDockerNode(dockerImageName, hostVersion, Constants.DEFAULT_PIPELINE_TIMEOUT, body)
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  hostVersion - Host VM version.  See Agents.getDockerMachineAffinity for explanation.
//  timeoutInMinutes - Timeout in minutes for the body
//  body - Closure, see example
def call(String dockerImageName, String hostVersion, int timeoutInMinutes, Closure body) {
    call(dockerImageName, hostVersion, timeoutInMinutes, '', body)
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  hostVersion - Host VM version.  See Agents.getDockerMachineAffinity for explanation.
//  timeoutInMinutes - Timeout in minutes for the body
//  dockerArgs - additional docker parameters for the docker node
//  body - Closure, see example
def call(String dockerImageName, String hostVersion, String dockerArgs, Closure body) {
    call(dockerImageName, hostVersion, Constants.DEFAULT_PIPELINE_TIMEOUT, dockerArgs, body)
}

// Runs a set of functionality on the default node
// that supports docker.
// Parameters:
//  dockerImageName - Docker image to use
//  dockerRegistryUrl - Url of alternate container registry
//  registryCredentialsId - token for accessing registry at dockerRegistryUrl
//  hostVersion - Host VM version.  See Agents.getDockerMachineAffinity for explanation.
//  timeoutInMinutes - Timeout in minutes for the body
//  dockerArgs - additional docker parameters for the docker node
//  body - Closure, see example
def call(String dockerImageName, String dockerRegistryUrl, String registryCredentialsId, String hostVersion, String dockerArgs, Closure body) {
    call(dockerImageName, dockerRegistryUrl, registryCredentialsId, hostVersion, Constants.DEFAULT_PIPELINE_TIMEOUT, dockerArgs, body)
}

def call(String dockerImageName, String hostVersion, int timeoutInMinutes, String dockerArgs, Closure body) {
    call(dockerImageName, null, null, hostVersion, timeoutInMinutes, dockerArgs, body)
}

def call(String dockerImageName, String dockerRegistryUrl, String registryCredentialsId, String hostVersion, int timeoutInMinutes, String dockerArgs, Closure body) {
    node (Agents.getDockerAgentLabel(hostVersion)) {
        timestamps {
            timeout(timeoutInMinutes) {
                def dockerImageClosure = { 
                    def dockerImage = docker.image(dockerImageName) 
                    // Force pull.
                    retry (3) {
                        dockerImage.pull()
                    }
                    // Important!! The current docker plugin always wants to pass -u hostuid:hostgid
                    // to the docker run step.  This is designed to work well with some of the other
                    // infrastructure, but causes havoc in our case because there are no non-default (root)
                    // users in our container.  This bug is filed as https://issues.jenkins-ci.org/browse/JENKINS-38438.
                    // In the meantime, we can pass -u to the inside() step which will append to the command line and
                    // override the original -u.
                    dockerImage.inside('-u 0:0 ' + dockerArgs) {
                        try {
                            // Make the log folder
                            makeLogFolder()
                            // Run the body
                            body()
                        }
                        finally {
                            // Archive anything in the standard log folder
                            archiveLogs()

                            // Normally we would use the workspace cleanup plugin to do this cleanup.
                            // However in the latest versions, it utilizes the AsyncResourceDisposer.
                            // This means that we won't attempt to delete the workspace until after the container
                            // has exited.  Outside the container, we won't have permissions to delete the
                            // mapped files (they will be root). Instead, we use rm -rf * and then workspace cleanup.
                            // This could potentially fail if there were too many files in the workspace dir (many thousands)
                            // but this is unlikely.  This will probably eventually be fixed on the workspace cleanup
                            // side of things, so don't worry about it too much
                            echo "Cleaning workspace ${WORKSPACE}"
                            sh 'rm -rf ..?* .[!.]* *'
                            // Use the workspace cleaner now.
                            cleanWs deleteDirs: true, patterns: [[pattern: '**/*', type: 'INCLUDE']]
                        }
                    }
                }
                if (dockerRegistryUrl!=null && registryCredentialsId!=null)
                {
                    docker.withRegistry(dockerRegistryUrl, registryCredentialsId) {
                        dockerImageClosure()
                    }
                }
                else
                {
                    dockerImageClosure()
                }
            }
        }
    }
}