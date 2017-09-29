/* Creates a "sender" Map object, which is a dictionary with these values
 *  correlationId: value
 *  workItemId: value
 *  credentialsIdentifier: value
 *  Generates new correlation and workItem id's if they are not specified'
 */  
def call(String credentialsIdentifier, String correlationId = null, String workItemId = null ) {
    if(correlationId == null) {
        correlationId = generateRandomUUID()
    }
    if(workItemId == null) {
        workItemId = generateRandomUUID()
    }
    return Collections.synchronizedMap([correlationId: correlationId,
                                        workItemId: workItemId,
                                        credentialsIdentifier: credentialsIdentifier])
}