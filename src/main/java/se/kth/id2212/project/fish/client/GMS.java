package se.kth.id2212.project.fish.client;

import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;


public class GMS implements Runnable {

    private String DEFAULT_CLIENT_ADDRESS = "127.0.0.1";
    private int port;
    private ClientAddress clientAddress;
    private ClientAddress myAddress;
    private boolean join;
    private HashSet<ClientAddress> groupMembers;
    private HashMap<ClientAddress, Socket> groupMemberSockets;

    public GMS(int port) {
        this.port = port;
    }

    public GMS(int port, ClientAddress clientAddress) {
        this.clientAddress = clientAddress;
        this.port = port;
        join = true;
    }

    @Override
    public void run() {

        if (join) {
            // Joining an already existing GMS
            try {
                Socket socket = new Socket(clientAddress.getIp(), clientAddress.getPort());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                ////////// Receive list of group members => for each: create socket //////////////
                // Send join
                out.writeObject(new Message(MessageDescriptor.JOIN, null));

                // receive group member list
                groupMembers = (HashSet<ClientAddress>) in.readObject();

                // find unique port within this GSM
                port = findAvailablePort(port);

                // Set this clients address
                myAddress = new ClientAddress(DEFAULT_CLIENT_ADDRESS, port);



                Socket peerSocket;
                for(ClientAddress member : groupMembers) {

                    //Open socket to group member
                    peerSocket = new Socket(member.getIp(), member.getPort());

                    //Add group member to group member list
                    groupMemberSockets.put(member, peerSocket);
                    ObjectOutputStream outS = new ObjectOutputStream(socket.getOutputStream());
                    outS.flush();
                    ObjectInputStream inS = new ObjectInputStream(socket.getInputStream());

                    //Send ADD message to member
                    outS.writeObject(new Message(MessageDescriptor.ADD, myAddress));

                    //Receive ADD OK
                    Message m = (Message) inS.readObject();
                    if(m.getDescriptor().equals(MessageDescriptor.ADD_OK)) {
                        System.out.println("Connection established to the following peer: " + member.getIp() + ":" + member.getPort());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Could not connect to host");
                e.printStackTrace();
            }

        } else {
            // TODO: If first peer in GMS
        }

        // Accept incoming GMS message requests
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            for (; ; ) {
                getClientThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int findAvailablePort(int suggestedPort) {
        boolean portFound = false;
        boolean portTaken = false;


        while(!portFound) {

            for(ClientAddress member : groupMembers) {
                if(member.getPort() == suggestedPort) {
                    portTaken = true;
                }

            }

            if(portTaken) {
                suggestedPort++;
            } else {
                portFound = true;
            }
        }
        return suggestedPort;
    }



    private Thread getClientThread(Socket clientSocket) throws IOException {
        return new Thread(() -> {
            ObjectOutputStream out;
            ObjectInputStream in;
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                out.flush();
                in = new ObjectInputStream(clientSocket.getInputStream());
                ClientAddress peerAddress;
                Socket peerSocket;

                for (; ;) {
                    // receive queries...
                    Message m = (Message) in.readObject();
                    switch(m.getDescriptor()) {
                        case ADD:
                            // Add member to group list
                            peerAddress = (ClientAddress) m.getContent();
                            // Connect to member and add member to group socket list
                            peerSocket = new Socket(peerAddress.getIp(), peerAddress.getPort());
                            groupMemberSockets.put(peerAddress, peerSocket);
                            out.writeObject(new Message(MessageDescriptor.ADD_OK, null));
                            break;
                        case JOIN:
                            // Send Member list to joining peer
                            out.writeObject(groupMembers);
                            break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
