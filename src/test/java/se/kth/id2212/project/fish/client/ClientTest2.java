package se.kth.id2212.project.fish.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientTest2 {
    public static void main(String[] args) throws UnknownHostException {
        Client.main(new String[]{"shared2", "shared2", "7002", InetAddress.getLocalHost().getHostAddress(), "7001"});
    }
}
