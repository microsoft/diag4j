package com.microsoft.azure.agent.plugin.agent;

public class UrlConfig {
    private static String agentPort = "8081";

    public static void setAgentPort(String port) {
        agentPort = port;
    }

    public static String getAgentPort() {
        return agentPort;
    }

    public static String getAttachInfoUrl() {
        return "http://localhost:" + agentPort + "/attach/info";
    }

    public static String getAttachUrl() {
        return "http://localhost:" + agentPort + "/attach";
    }

    public static String getAddLogsUrl() {
        return "http://localhost:" + agentPort + "/logs/add";
    }

    public static String getRemoveLogsUrl() {
        return "http://localhost:" + agentPort + "/logs/remove";
    }
}
