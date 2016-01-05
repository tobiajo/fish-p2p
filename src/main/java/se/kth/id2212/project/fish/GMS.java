package se.kth.id2212.project.fish;

import se.kth.id2212.project.fish.server.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

public class GMS implements Runnable {

    private int port;
    private ClientAddress clientAddress;
    private boolean join;
    private HashSet<ClientAddress> groupMembers;
    private HashMap<ClientAddress, Socket> groupMemberSockets;

    public GMS(int port) {
        this.port = port;
    }

    public GMS(int port, ClientAddress clientAddress) {
        this.port = port;
        join = true;
    }

    @Override
    public void run() {

        if (join) {
            // join group
            try {
                Socket socket = new Socket(clientAddress.getIp(), clientAddress.getPort());
                // receive list of group members => for each: create socket
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            for (; ; ) {
                getClientThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Thread getClientThread(Socket clientSocket) {
        return new Thread(() -> {
            for (; ;) {
                // receive queries...
            }
        });
    }
}
