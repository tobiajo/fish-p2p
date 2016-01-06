package se.kth.id2212.project.fish.client;

import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;
import se.kth.id2212.project.fish.common.ProtocolException;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GroupMembershipService implements Runnable {

    private String sharedFilePath;
    private int clientPort;
    private ClientAddress anyMember;
    private HashSet<String> groupMembers;

    public GroupMembershipService(String sharedFilePath, int clientPort) {
        this.sharedFilePath = sharedFilePath;
        this.clientPort = clientPort;
        groupMembers = new HashSet<>();
    }

    public GroupMembershipService(String sharedFilePath, int clientPort, ClientAddress anyMember) {
        this.sharedFilePath = sharedFilePath;
        this.clientPort = clientPort;
        this.anyMember = anyMember;
        groupMembers = new HashSet<>();
    }

    @Override
    public void run() {
        if (anyMember != null) {
            joinGroup();
        }

        try {
            System.out.println("Opening server port " + clientPort + "...");
            ServerSocket serverSocket = new ServerSocket(clientPort);
            synchronized (this) {
                notify();
            }
            for (; ; ) {
                Socket socket = serverSocket.accept();
                spawnRequestHandler(socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<ClientAddress, Message> broadcast(Message message) {
        HashMap<ClientAddress, Message> replies = new HashMap<>();

        groupMembers.forEach(memberString -> {
            ClientAddress member = new ClientAddress(memberString);
            Message reply = request(message, member);
            if (reply != null) {
                replies.put(member, reply);
            } else {
                groupMembers.remove(memberString);
            }
        });

        return replies;
    }

    public Message request(Message message, ClientAddress member) {
        Message reply;

        try {
            // connect
            Socket socket = new Socket(member.getIp(), member.getPort());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // request-receive
            oos.writeObject(message);
            reply = (Message) ois.readObject();
            if (reply.getDescriptor() != MessageDescriptor.OK) {
                throw new ProtocolException("Did not receive OK");
            }

            // disconnect
            ois.close();
            oos.close();
            socket.close();
        } catch (IOException | ClassNotFoundException | ProtocolException e) {
            reply = null;
        }

        return reply;
    }

    private void joinGroup() {
        if (anyMember != null) {
            Message reply = request(new Message(MessageDescriptor.MEMBERS, null), anyMember);
            if (reply == null) {
                System.out.println("Could not receive group members from " + anyMember);
                System.exit(0);
            }
            groupMembers = (HashSet<String>) reply.getContent();
            groupMembers.add(anyMember.toString());
            System.out.println("Members: " + groupMembers);
            broadcast(new Message(MessageDescriptor.ADD, clientPort));
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
                    case MEMBERS:
                        oos.writeObject(new Message(MessageDescriptor.OK, groupMembers));
                        break;
                    case ADD:
                        groupMembers.add((new ClientAddress(socket.getInetAddress().getHostAddress(), (int) request.getContent()).toString()));
                        oos.writeObject(new Message(MessageDescriptor.OK, null));
                        break;
                    case SEARCH:
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
                        oos.writeObject(new Message(MessageDescriptor.OK, data));
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
