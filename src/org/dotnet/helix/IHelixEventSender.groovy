package org.dotnet.helix;

interface IHelixEventSender {
    String Send(HelixEventData data)
    IHelixEventSender GetForRelatedWorkItem(String workItemId)
    String correlationId
}