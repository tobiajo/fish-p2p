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
    private HashSet<ClientAddress> groupMembers = new HashSet<>();
    private HashMap<ClientAddress, Socket> groupMemberSockets = new HashMap<>();

    public GMS(int port) {
        this.port = port;
        join = false;
    }

    public GMS(int port, ClientAddress clientAddress) {
        this.clientAddress = clientAddress;
        this.port = port;
        join = true;
    }

    @Override
    public void run() {

        if (join) {
            joinGMS();
        } else {
            // First peer
            startGMS();
        }



    }

    private void startGMS() {
        System.out.println("Starting GMS service...");
        myAddress = new ClientAddress(DEFAULT_CLIENT_ADDRESS, port);
        // Add myself to group member list
        groupMembers.add(myAddress);
        startAcceptingMessages();

    }

    private void joinGMS() {
        // Joining an already existing GMS
        try {
            Socket socket = new Socket(clientAddress.getIp(), clientAddress.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Send join
            System.out.println("Joining GMS....");
            out.writeObject(new Message(MessageDescriptor.JOIN, null));
            out.flush();

            // Receive group member list
            System.out.println("   * Received information about other peers");
            groupMembers = (HashSet<ClientAddress>) in.readObject();

            // Find unique port within this GSM
            this.port = findAvailablePort(port);

            // Set this clients address
            myAddress = new ClientAddress(DEFAULT_CLIENT_ADDRESS, port);
            System.out.println("   * Local address: " + myAddress.getIp() + ":" + myAddress.getPort());

            // Start accept messages


            //Send ADD_JOIN message to first contact
            System.out.println("   * Sending ADD_JOIN request");
            out.writeObject(new Message(MessageDescriptor.ADD_JOIN, myAddress));

            //Receive ADD_OK from first contact
            Message m = (Message) in.readObject();
            if(m.getDescriptor().equals(MessageDescriptor.ADD_OK)) {
                System.out.println("   * Connection established to peer: " + clientAddress.getIp() + ":" + clientAddress.getPort());

                // Add first contact to socket list
                groupMemberSockets.put(clientAddress, socket);
            }


            // Start accepting connections
            startAcceptingMessages();

            Socket peerSocket;
            for(ClientAddress member : groupMembers) {

                if(sameClient(member, clientAddress)) {
                    continue;
                }

                //Open socket to group member
                peerSocket = new Socket(member.getIp(), member.getPort());

                //Add group member to group member socket list
                groupMemberSockets.put(member, peerSocket);
                ObjectOutputStream outS = new ObjectOutputStream(peerSocket.getOutputStream());
                ObjectInputStream inS = new ObjectInputStream(peerSocket.getInputStream());

                //Send ADD message to member
                System.out.println("   * Sending ADD request");
                outS.writeObject(new Message(MessageDescriptor.ADD, myAddress));

                //Receive ADD OK
                m = (Message) inS.readObject();
                if(m.getDescriptor().equals(MessageDescriptor.ADD_OK)) {
                    System.out.println("   * Connection established to peer: " + member.getIp() + ":" + member.getPort());
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Could not connect to host");
            e.printStackTrace();
        }

        // Add myself to group member list
        groupMembers.add(myAddress);


    }

    private boolean sameClient(ClientAddress clientOne, ClientAddress clientTwo) {
        boolean same = ((clientOne.getIp().equals(clientTwo.getIp())) && (clientOne.getPort() == clientTwo.getPort())) ? true : false;
        return same;
    }

    private int findAvailablePort(int suggestedPort) {
        boolean portFound = false;
        boolean portTaken = false;

        int port = suggestedPort;

        while(!portFound) {

            for(ClientAddress member : groupMembers) {
                if(member.getPort() == port) {
                    portTaken = true;
                    break;
                }
            }

            if(portTaken) {
                port++;
                portTaken = false;
            } else {
                portFound = true;
            }
        }
        return port;
    }

    private void startAcceptingMessages() {
        new Thread(() -> {
            // Accept incoming GMS message requests
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                System.out.println("   * Accepting connections");
                for (; ; ) {
                    getClientThread(serverSocket.accept()).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private Thread getClientThread(Socket clientSocket) throws IOException {
        return new Thread(() -> {
            ObjectOutputStream out;
            ObjectInputStream in;
            try {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                in = new ObjectInputStream(clientSocket.getInputStream());
                ClientAddress peerAddress;
                Socket peerSocket;

                for (; ;) {
                    // receive queries...
                    Message m = (Message) in.readObject();
                    switch(m.getDescriptor()) {
                        case ADD:
                            System.out.println("   * Received ADD request");
                            // Add member to group list
                            peerAddress = (ClientAddress) m.getContent();
                            groupMembers.add(peerAddress);
                            // Connect to member and add member to group socket list
                            peerSocket = new Socket(peerAddress.getIp(), peerAddress.getPort());
                            groupMemberSockets.put(peerAddress, peerSocket);
                            out.writeObject(new Message(MessageDescriptor.ADD_OK, null));
                            System.out.println("   * Connection established to peer: " + peerAddress.getIp() + ":" + peerAddress.getPort());
                            break;
                        case ADD_JOIN:
                            // SPECIAL CASE FOR ADD BETWEEN PEER CONNTECTING, who just did a join, TO GMS AND HOSTING PEER.
                            System.out.println("   * Received ADD_JOIN request");
                            peerAddress = (ClientAddress) m.getContent();
                            groupMembers.add(peerAddress);
                            // Connect to member and add member to group socket list
                            groupMemberSockets.put(peerAddress, clientSocket);
                            out.writeObject(new Message(MessageDescriptor.ADD_OK, null));
                            System.out.println("   * Connection established to peer: " + peerAddress.getIp() + ":" + peerAddress.getPort());
                            break;

                        case JOIN:
                            System.out.println("   * Received JOIN request");
                            // Send Member list to joining peer
                            out.writeObject(groupMembers);
                            out.flush();
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
