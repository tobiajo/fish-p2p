package se.kth.id2212.project.fish.server;

import se.kth.id2212.project.fish.common.FileAddress;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {

    public static final String USAGE = "Usage: java Server [server_port]";

    private static final int DEFAULT_SERVER_PORT = 6958; // FI5H => 6-9-5-8

    private int serverPort;
    private HashMap<Socket, ArrayList<String>> fileLists = new HashMap<>();

    public Server() {
        this(DEFAULT_SERVER_PORT);
    }

    public Server(int serverPort) {
        this.serverPort = serverPort;
    }

    public void run() {
        try {
            System.out.println("Opening server socket " + serverPort + "...");
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("\nFISH server ready.\n");
            for (; ; ) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(this, clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Server crashed: " + e.getMessage());
        }
    }

    public void addFileList(Socket clientSocket, ArrayList<String> fileList) {
        fileLists.put(clientSocket, fileList);
    }

    public void removeFileList(Socket clientSocket) {
        fileLists.remove(clientSocket);
    }

    public ArrayList<FileAddress> searchFileLists(Socket clientSocket, String request) {
        ArrayList<FileAddress> ret = new ArrayList<>();

        fileLists.keySet().forEach(socket -> {
            fileLists.get(socket).forEach(file -> {
                if (file.contains(request)) {
                    if (socket != clientSocket) {
                        ret.add(new FileAddress(socket.getInetAddress().getHostAddress(), socket.getPort(), file));
                    }
                }
            });
        });
        if (ret.size() == 0) return null;

        return ret;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            new Server().run();
        } else if (args.length == 1) {
            new Server(Integer.parseInt(args[0])).run();
        } else {
            System.out.println("FISH server: invalid number of arguments\n" + USAGE);
        }
    }
}
