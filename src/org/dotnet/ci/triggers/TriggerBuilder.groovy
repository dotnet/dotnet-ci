package org.dotnet.ci.triggers;

interface TriggerBuilder {
    boolean isPRTrigger()
    void emitTrigger(def job)
}