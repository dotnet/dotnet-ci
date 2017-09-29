package org.dotnet.helix;

import java.util.UUID

class NoopSender implements IHelixEventSender {
    public final String correlationId

    public String Send(HelixEventData data) {return ''}

    public IHelixEventSender GetForRelatedWorkItem(String workItemId) {return this}

    public NoopSender() { 
        this.correlationId = new UUID(0L, 0L)
    }
}