package se.kth.id2212.project.fish.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ClientTest4 {
    public static void main(String[] args) throws UnknownHostException {
        Client.main(new String[]{"shared4", "shared4", "7004", InetAddress.getLocalHost().getHostAddress(), "7003"});
    }
}
