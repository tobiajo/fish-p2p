package se.kth.id2212.project.fish.client.gms;

import se.kth.id2212.project.fish.client.Client;
import se.kth.id2212.project.fish.client.ClientAddress;
import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;
import se.kth.id2212.project.fish.common.ProtocolException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

public class GroupMembershipService implements Runnable {

    private Client client;
    private int clientPort;
    private ClientAddress anyMember;
    private HashSet<String> groupMembers;

    public GroupMembershipService(Client client, int clientPort) {
        this.client = client;
        this.clientPort = clientPort;
        groupMembers = new HashSet<>();
    }

    public GroupMembershipService(Client client, int clientPort, ClientAddress anyMember) {
        this.client = client;
        this.clientPort = clientPort;
        this.anyMember = anyMember;
        groupMembers = new HashSet<>();
    }

    @Override
    public void run() {
        if (anyMember != null) {
            Message reply = request(new Message(MessageDescriptor.REQUEST_MEMBERS, null), anyMember);
            if (reply == null) {
                System.out.println("Could not receive group members from " + anyMember);
                System.exit(0);
            }
            groupMembers = (HashSet<String>) reply.getContent();
            groupMembers.add(anyMember.toString());
            System.out.println("Members: " + groupMembers);
            broadcast(new Message(MessageDescriptor.ADD_ME, clientPort));
        }

        try {
            System.out.println("Opening server port " + clientPort + "...");
            ServerSocket serverSocket = new ServerSocket(clientPort);
            synchronized (client) {
                client.notify();
            }
            for (; ; ) {
                new Thread(new RequestHandler(client, this, serverSocket.accept())).start();
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

    public void addGroupMember(ClientAddress member) {
        groupMembers.add(member.toString());
    }

    public HashSet<String> getGroupMembers() {
        return groupMembers;
    }
}
