import org.dotnet.helix.*
def call(Map senderMap) {
    return call(senderMap.correlationId, senderMap.workItemId, senderMap.credentialsIdentifier)
}
def call(String credentialsIdentifier) {
    return call(null, null, credentialsIdentifier)
}
def call(String correlationId, String credentialsIdentifier) {
    return call(correlationId, null, credentialsIdentifier)
}

/* Create a helix event sender which is used to send events to the endpoint specified in the
 * credentialsIdentifier
 */
def call(String correlationId, String workItemId, String credentialsIdentifier) {
    withCredentials([string(credentialsId: credentialsIdentifier, variable: 'helixEndpoint')]) {
        HelixEvent.Initialize(env.helixEndpoint)
        if(correlationId != null) {
            if(workItemId != null) {
                return HelixEvent.CreateSender(correlationId, workItemId)
            }
            else {
                return HelixEvent.CreateSender(correlationId)
            }
        }
        else {
            return HelixEvent.CreateSender()
        }
    }
}