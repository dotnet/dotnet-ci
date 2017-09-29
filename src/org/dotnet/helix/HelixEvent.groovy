package org.dotnet.helix;

import java.util.UUID

class HelixEvent {
    private static String s_connectionString
    private static String s_stagingConnectionString

    static void Initialize(String eventHubConnectionString, String eventHubStagingConnectionString = null) {
        s_connectionString = eventHubConnectionString
        s_stagingConnectionString = eventHubStagingConnectionString
    }

    public static IHelixEventSender CreateSender(String correlationId, String workItemId) {
        if(IsNullOrWhitespace(s_connectionString)) {
            return s_nullSender
        }
        return new HelixEventSender(correlationId, workItemId, s_connectionString, s_stagingConnectionString)
    }

    public static IHelixEventSender CreateSender(String correlationId) {
        if(IsNullOrWhitespace(s_connectionString)) {
            return s_nullSender
        }
        return new HelixEventSender(correlationId, UUID.randomUUID().toString(), s_connectionString, s_stagingConnectionString)
    }

    public static IHelixEventSender CreateSender() {
        if(IsNullOrWhitespace(s_connectionString)) {
            return s_nullSender
        }
        return new HelixEventSender(UUID.randomUUID().toString(), UUID.randomUUID().toString(), s_connectionString, s_stagingConnectionString)
    }

    private static Boolean IsNullOrWhitespace(String testString) {
        return testString == null || testString.isEmpty() || testString.trim().isEmpty()
    }

    private static IHelixEventSender s_nullSender = new NoopSender()
}