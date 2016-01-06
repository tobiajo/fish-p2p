package se.kth.id2212.project.fish.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientTest5 {
    public static void main(String[] args) throws UnknownHostException {
        Client.main(new String[]{"shared5", "shared5", "7005", InetAddress.getLocalHost().getHostAddress(), "7004"});
    }
}
