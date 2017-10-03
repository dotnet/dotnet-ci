package org.dotnet.helix;

// ToDo: probably need to remove this and add the jars to jenkins as we don't want external dependencies
@Grab(group='com.microsoft.azure', module='azure-eventhubs', version='0.14.5')

import com.microsoft.azure.eventhubs.*

class HelixEventSender implements IHelixEventSender {
    String correlationId
    String workItemId

    final EventHubClient _sender
    final EventHubClient _stagingSender
    
    public HelixEventSender(String correlationId,
                            String workItemId,
                            String eventHubConnectionString,
                            String stagingEventHubConnectionString = null) {
        this.correlationId = correlationId
        this.workItemId = workItemId
        _sender = EventHubClient.createFromConnectionString(eventHubConnectionString).get()
        if(stagingEventHubConnectionString != null) {
            _stagingSender = EventHubClient.createFromConnectionString(stagingEventHubConnectionString).get()
        }
    }
    
    String Send(HelixEventData data) {
        if (data == null) throw new Exception("HelixEventData is null")
        
        data.correlationId = correlationId
        data.workItemId = workItemId

        def jsonString = ''
        try {
            jsonString = data.toJson()
            EventData eventData = new EventData(data.toJson().getBytes())
            if(_sender != null) {
                _sender.send(eventData)
            }
            if(_stagingSender != null) {
                _stagingSender.send(eventData)
            }
        }
        catch(Exception e) {
            println e.getMessage()
            println e.getLocationText()
            throw e
        }
        return jsonString
    }
    
    IHelixEventSender GetForRelatedWorkItem(String workItemId) {
        return new HelixEventSender(correlationId, workItemId, _sender, _stagingSender)
    }
} 
