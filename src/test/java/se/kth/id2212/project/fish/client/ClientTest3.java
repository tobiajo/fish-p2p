package se.kth.id2212.project.fish.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientTest3 {
    public static void main(String[] args) throws UnknownHostException {
        Client.main(new String[] {"shared3", "shared3", "7003", InetAddress.getLocalHost().getHostAddress(), "7002"});
    }
}
