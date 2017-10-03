import org.dotnet.helix.*

def call(String credentialsIdentifier) {
    return call(null, null, credentialsIdentifier)
}

def call(String correlationId, String credentialsIdentifier) {
    return call(correlationId, null, credentialsIdentifier)
}

def call(String correlationId, String workItemId, String credentialsIdentifier) {
    withCredentials([string(credentialsId: credentialsIdentifier, variable: 'HelixEndpoint')]) {
        def sender = createSender(correlationId, workItemId, credentialsIdentifier)
        return [CorrelationId: sender.CorrelationId,
                WorkItemId: sender.WorkItemId,
                CredentialsIdentifier: credentialsIdentifier]
    }
}