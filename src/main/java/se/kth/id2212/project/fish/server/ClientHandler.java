package se.kth.id2212.project.fish.server;

import se.kth.id2212.project.fish.common.FileAddress;
import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;
import se.kth.id2212.project.fish.common.ProtocolException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Server server;
    private Socket clientSocket;

    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        if (connect() && register()) serve();
        server.removeFileList(clientSocket);
        clientPrint("Unregistered");
        disconnect();
    }

    private void clientPrint(String msg) {
        System.out.println("(" + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + ") " + msg);
    }

    private boolean connect() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
            clientPrint("Connected");
            return true;
        } catch (IOException e) {
            clientPrint("Connecting failed");
            return false;
        }
    }

    private boolean register() {
        try {
            Message m = (Message) in.readObject();
            if (m.getDescriptor() != MessageDescriptor.REGISTER) {
                throw new ProtocolException("Did not receive REGISTER");
            }
            ArrayList<String> sharedFiles = (ArrayList<String>) m.getContent();
            server.addFileList(clientSocket, sharedFiles);
            out.writeObject(new Message(MessageDescriptor.REGISTER_OK, null));
            clientPrint("Registered");
            return true;
        } catch (IOException | ClassNotFoundException | ProtocolException e) {
            clientPrint("Registration failed");
            return false;
        }
    }

    private void serve() {
        for (boolean stop = false; !stop; ) {
            try {
                Message m = (Message) in.readObject();
                switch (m.getDescriptor()) {
                    case SEARCH:
                        search((String) m.getContent());
                        break;
                    case UNREGISTER:
                        out.writeObject(new Message(MessageDescriptor.UNREGISTER_OK, null));
                        stop = true;
                        break;
                    case UPDATE:
                        update((ArrayList<String>) m.getContent());
                        break;
                    default:
                        throw new ProtocolException("Received " + m.getDescriptor().name());
                }
            } catch (IOException e) {
                clientPrint("Connection lost: " + e.getMessage());
                stop = true;
            } catch (ClassNotFoundException | ProtocolException e) {
                clientPrint("Error: " + e.getMessage());
            }
        }
    }

    private void search(String request) throws IOException {
        ArrayList<FileAddress> result = server.searchFileLists(clientSocket, request);
        out.writeObject(new Message(MessageDescriptor.SEARCH_RESULT, result));
        clientPrint("Searched");
    }

    private void update(ArrayList<String> sharedFiles) throws IOException {
        server.addFileList(clientSocket, sharedFiles);
        out.writeObject(new Message(MessageDescriptor.UPDATE_OK, null));
        clientPrint("Updated");
    }

    private boolean disconnect() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            clientPrint("Disconnected");
            return true;
        } catch (IOException e) {
            clientPrint("Disconnecting failed");
            return false;
        }
    }
}
