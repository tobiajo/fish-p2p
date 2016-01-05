package se.kth.id2212.project.fish.client;

import se.kth.id2212.project.fish.common.FileAddress;
import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;
import se.kth.id2212.project.fish.common.ProtocolException;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

    public static final String USAGE = "Usage: java Client [download_path] [shared_file_path] [server_address] [server_port]";

    private static final String DEFAULT_DOWNLOAD_PATH = "download";
    private static final String DEFAULT_SHARED_FILE_PATH = "shared";
    private static final String DEFAULT_SERVER_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 6958; // FI5H => 6-9-5-8


    private String sharedFilePath, downloadPath, serverAddress;
    private int serverPort;
    private Socket serverSocket;
    private ObjectInputStream inS;
    private ObjectOutputStream outS;

    public Client() {
        this(DEFAULT_DOWNLOAD_PATH, DEFAULT_SHARED_FILE_PATH, DEFAULT_SERVER_ADDRESS, DEFAULT_SERVER_PORT);
    }

    public Client(String downloadPath, String sharedFilePath, String serverAddress, int serverPort) {
        this.sharedFilePath = sharedFilePath;
        this.downloadPath = downloadPath;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public void run() {
        System.out.println("Connecting to " + serverAddress + ":" + serverPort + "...");

        if (connect() && register()) {
            int sharePort = serverSocket.getLocalPort();
            System.out.println("Opening share socket " + sharePort + "...");
            P2PHandler.getShareThread(sharedFilePath, sharePort).start();
            prompt();
            System.exit(0);
        }
    }

    private boolean connect() {
        try {
            serverSocket = new Socket(serverAddress, serverPort);
            outS = new ObjectOutputStream(serverSocket.getOutputStream());
            outS.flush();
            inS = new ObjectInputStream(serverSocket.getInputStream());
            System.out.println("Connected");
            return true;
        } catch (IOException e) {
            System.out.println("Connecting failed: " + e.getMessage());
            return false;
        }
    }

    private boolean register() {
        try {
            outS.writeObject(new Message(MessageDescriptor.REGISTER, getFileList(true)));
            Message m = (Message) inS.readObject();
            if (m.getDescriptor() != MessageDescriptor.REGISTER_OK) {
                throw new ProtocolException("Did not receive REGISTER_OK");
            }
            System.out.println("Registered");
            return true;
        } catch (IOException | ClassNotFoundException | ProtocolException e) {
            System.out.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public ArrayList<String> getFileList(boolean print) {
        ArrayList<String> ret = new ArrayList<>();

        if (print) System.out.println("Shared files:");
        File dir = new File(sharedFilePath);
        dir.mkdir();
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                if (print) System.out.println("  " + sharedFilePath + File.separator + f.getName());
                ret.add(f.getName());
            }
        }

        return ret;
    }

    private void prompt() {
        System.out.println("\nFISH client ready.");
        for (boolean stop = false; !stop; ) {
            System.out.print("\n1. Search\n2. Update\n3. Exit\n> ");
            switch (new Scanner(System.in).nextLine()) {
                case "1":
                    try {
                        search();
                    } catch (IOException | ClassNotFoundException | ProtocolException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "2":
                    try {
                        update();
                    } catch (IOException | ClassNotFoundException | ProtocolException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "3":
                    try {
                        unregister();
                    } catch (IOException | ClassNotFoundException | ProtocolException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    stop = true;
                    break;
                default:
                    System.out.println("Invalid input");
                    break;
            }
        }
    }

    private void search() throws IOException, ClassNotFoundException, ProtocolException {
        System.out.print("Request: ");
        String fileName = new Scanner(System.in).nextLine();
        outS.writeObject(new Message(MessageDescriptor.SEARCH, fileName));
        Message m = (Message) inS.readObject();
        if (m.getDescriptor() != MessageDescriptor.SEARCH_RESULT) {
            throw new ProtocolException("Did not receive SEARCH_RESULT");
        }
        if (m.getContent() != null) {
            System.out.println("Available at:");
            int i = 0;
            ArrayList<FileAddress> remoteClients = (ArrayList<FileAddress>) m.getContent();
            for (FileAddress fA : remoteClients) {
                System.out.println(++i + ". " + fA);
            }

            for (boolean stop = false; !stop; ) {
                System.out.print("Download from (0 = none): ");
                int input;
                try {
                    input = new Scanner(System.in).nextInt();
                } catch (InputMismatchException e) {
                    input = -1;
                }
                if (input == 0) {
                    stop = true;
                } else if (input >= 1 && input <= i) {
                    fetch(remoteClients.get(input - 1));
                    stop = true;
                } else {
                    System.out.println("Invalid input");
                }
            }
        } else {
            System.out.println("File not found");
        }
    }

    private void fetch(FileAddress fileAddress) throws IOException, ClassNotFoundException, ProtocolException {
        // connect
        Socket remoteClientSocket = new Socket(fileAddress.getIp(), fileAddress.getPort());
        ObjectOutputStream outRC = new ObjectOutputStream(remoteClientSocket.getOutputStream());
        outRC.flush();
        ObjectInputStream inRC = new ObjectInputStream(remoteClientSocket.getInputStream());

        // request
        outRC.writeObject(new Message(MessageDescriptor.FETCH_FILE, fileAddress.getFile()));
        System.out.println("Sent request");

        // receive
        File dir = new File(downloadPath);
        dir.mkdir();
        File file = new File(dir.getName() + File.separator + fileAddress.getFile());
        byte[] data = (byte[]) inRC.readObject();
        Files.write(file.toPath(), data);
        System.out.println("Received file");

        // disconnect
        inRC.close();
        outRC.close();
        remoteClientSocket.close();

        if (sharedFilePath.equals(downloadPath)) {
            // update file list
            update();
        }
    }

    private void update() throws IOException, ClassNotFoundException, ProtocolException {
        outS.writeObject(new Message(MessageDescriptor.UPDATE, getFileList(false)));
        Message m = (Message) inS.readObject();
        if (m.getDescriptor() != MessageDescriptor.UPDATE_OK) {
            throw new ProtocolException("Did not receive UPDATE_OK");
        }
        System.out.println("Updated file list");
    }

    private void unregister() throws IOException, ClassNotFoundException, ProtocolException {
        outS.writeObject(new Message(MessageDescriptor.UNREGISTER, null));
        Message m = (Message) inS.readObject();
        if (m.getDescriptor() != MessageDescriptor.UNREGISTER_OK) {
            throw new ProtocolException("Did not receive UNREGISTER_OK");
        }
        System.out.println("Unregistered");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            new Client().run();
        } else if (args.length == 4) {
            try {
                int serverPort = Integer.parseInt(args[3]);
                new Client(args[0], args[1], args[2], serverPort).run();
            } catch (NumberFormatException e) {
                System.out.println("FISH client: invalid server port\n" + USAGE);
            }
        } else {
            System.out.println("FISH client: invalid number of arguments\n" + USAGE);
        }
    }
}
