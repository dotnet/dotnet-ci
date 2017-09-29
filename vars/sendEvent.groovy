import org.dotnet.helix.*

def call(String type, Map data, Map senderMap, Integer maxRetries = 3, Integer waitTimeInMilliseconds = 1000) {
    def sender = createSender(senderMap)
    def eventData = new HelixEventData()
    eventData.type = type
    eventData.data = data

    return Send(sender, eventData, 0, maxRetries, waitTimeInMilliseconds)
}

/**
 *  Send event data with retry logic, if not succesful after 'maxRetries', ignore the failure and continue 
 */
def Send(IHelixEventSender sender, HelixEventData eventData, Integer attempt, Integer maxRetries, Integer waitTimeInMilliseconds) {
    try {
        return sender.Send(eventData)
    }
    catch(Exception) {
        if(attempt < maxRetries) {
            println "Send event failed, will retry in ${waitTimeInMilliseconds} ms (retry ${attempt + 1} / ${maxRetries})"
            Thread.sleep(waitTimeInMilliseconds)
            return Send(data, attempt + 1, maxRetries)
        }
    }
}