package se.kth.id2212.project.fish.client;

import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;
import se.kth.id2212.project.fish.common.ProtocolException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

public class GroupMembershipService {

    private int clientPort;
    private HashSet<String> groupMembers;

    public GroupMembershipService(int clientPort) {
        this.clientPort = clientPort;
        groupMembers = new HashSet<>();
    }

    public GroupMembershipService(int clientPort, ClientAddress anyMember) {
        this.clientPort = clientPort;
        groupMembers = new HashSet<>();
        joinGroup(anyMember);
    }

    private void joinGroup(ClientAddress anyMember) {
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
