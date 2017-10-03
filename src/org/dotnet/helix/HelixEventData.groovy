package org.dotnet.helix;

import groovy.json.*

class HelixEventData {
    String type
    String correlationId
    String workItemId
    Map data

    public String toJson() {
        def eventDataMap = [:]
        eventDataMap.Type = type
        eventDataMap.CorrelationId = correlationId
        eventDataMap.WorkItemId = workItemId
        
        data.each { prop, val ->
            if(prop != "Properties") {
                eventDataMap.put(prop, val)
            }
        }
        if(data.Properties != null) {
            eventDataMap.put("Properties", new JsonBuilder(data.Properties).toString())
        }

        return new JsonBuilder(eventDataMap).toString()
    }
}