package se.kth.id2212.project.fish.client;

import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;

public class RequestServer implements Runnable {

    private GroupMembershipService gms;
    private String sharedFilePath;
    private int clientPort;

    public RequestServer(GroupMembershipService gms, String sharedFilePath, int clientPort) {
        this.gms = gms;
        this.sharedFilePath = sharedFilePath;
        this.clientPort = clientPort;
    }

    @Override
    public void run() {
        try {
            System.out.println("Opening server port " + clientPort + "...");
            ServerSocket serverSocket = new ServerSocket(clientPort);
            synchronized (gms) {
                gms.notify();
            }
            for (; ; ) {
                Socket socket = serverSocket.accept();
                spawnRequestHandler(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void spawnRequestHandler(Socket socket) {
        new Thread(() -> {
            try {
                // connect
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.flush();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

                // receive-reply
                Message request = (Message) ois.readObject();
                switch (request.getDescriptor()) {
                    case REQUEST_MEMBERS:
                        oos.writeObject(new Message(MessageDescriptor.OK, gms.getGroupMembers()));
                        break;
                    case ADD_ME:
                        gms.addGroupMember(new ClientAddress(socket.getInetAddress().getHostAddress(), (Integer) request.getContent()));
                        oos.writeObject(new Message(MessageDescriptor.OK, null));
                        break;
                    case QUERY:
                        String query = (String) request.getContent();
                        ArrayList<String> matchedFiles = new ArrayList<>();
                        getFileList().forEach(file -> {
                            if (file.contains(query)) matchedFiles.add(file);
                        });
                        oos.writeObject(new Message(MessageDescriptor.OK, matchedFiles));
                        break;
                    case FETCH:
                        File file = new File(sharedFilePath + File.separator + request.getContent());
                        byte[] data = Files.readAllBytes(file.toPath());
                        oos.writeObject(data);
                        break;
                    default:
                        oos.writeObject(new Message(MessageDescriptor.ERROR, null));
                        break;
                }

                // disconnect
                ois.close();
                oos.close();
                socket.close();
            } catch (IOException | ClassNotFoundException e) {
            /* nothing */
            }
        }).start();
    }

    private ArrayList<String> getFileList() {
        ArrayList<String> ret = new ArrayList<>();

        File dir = new File(sharedFilePath);
        dir.mkdir();
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                ret.add(f.getName());
            }
        }

        return ret;
    }
}
