package se.kth.id2212.project.fish;

import java.io.Serializable;

public class ClientAddress implements Serializable {

    private String ip;
    private int port;

    public ClientAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
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
