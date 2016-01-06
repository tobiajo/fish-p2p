package se.kth.id2212.project.fish.client;

import java.io.Serializable;

public class ClientAddress implements Serializable {

    private String ip;
    private int port;

    public ClientAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public ClientAddress(String ipColonPort) {
        String[] split = ipColonPort.split(":");
        this.ip = split[0];
        this.port = Integer.parseInt(split[1]);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return getIp() + ":" + getPort();
    }
}
