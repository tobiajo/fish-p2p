package se.kth.id2212.project.fish.common;

import java.io.File;
import java.io.Serializable;

public class FileAddress implements Serializable {

    private String ip;
    private int port;
    private String file;

    public FileAddress(String ip, int port, String file) {
        this.ip = ip;
        this.port = port;
        this.file = file;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getFile() {
        return file;
    }

    @Override
    public String toString() {
        return getIp() + ":" + getPort() + File.separator + getFile();
    }
}
