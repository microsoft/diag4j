package com.microsoft.azure.agent.plugin.agent.entity;

public class PodInfo {
    private String name;
    private String podIp;
    private String phase;
    private String isAttach;

    public PodInfo(String name, String podIp, String phase, String isAttach) {
        this.name = name;
        this.podIp = podIp;
        this.phase = phase;
        this.isAttach = isAttach;
    }

    public String getName() {
        return name;
    }

    public String getPodIp() {
        return podIp;
    }

    public String getPhase() {
        return phase;
    }

    public String isAttach() {
        return isAttach;
    }
}
