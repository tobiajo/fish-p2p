package se.kth.id2212.project.fish.client.gms;

import se.kth.id2212.project.fish.client.Client;
import se.kth.id2212.project.fish.client.ClientAddress;
import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;

public class RequestHandler implements Runnable {

    private Client client;
    private GroupMembershipService gms;
    private Socket socket;

    public RequestHandler(Client client, GroupMembershipService gms, Socket socket) {
        this.client = client;
        this.gms = gms;
        this.socket = socket;
    }

    @Override
    public void run() {
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
                    client.getFileList().forEach(file -> {
                        if (file.contains(query)) matchedFiles.add(file);
                    });
                    oos.writeObject(new Message(MessageDescriptor.OK, matchedFiles));
                    break;
                case FETCH:
                    File file = new File(client.getSharedFilePath() + File.separator + request.getContent());
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
    }
}
