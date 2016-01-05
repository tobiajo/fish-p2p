package se.kth.id2212.project.fish.client;

import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;
import se.kth.id2212.project.fish.common.ProtocolException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class P2PHandler implements Runnable {

    private String sharedFilePath;
    private Socket remoteClientSocket;


    private P2PHandler(String sharedFilePath, Socket remoteClientSocket) {
        this.sharedFilePath = sharedFilePath;
        this.remoteClientSocket = remoteClientSocket;
    }

    @Override
    public void run() {
        try {
            // connect
            ObjectOutputStream out = new ObjectOutputStream(remoteClientSocket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(remoteClientSocket.getInputStream());

            // receive message
            Message request = (Message) in.readObject();
            if (request.getDescriptor() != MessageDescriptor.FETCH_FILE) {
                throw new ProtocolException("Did not receive FETCH_FILE");
            }

            // reply
            File file = new File(sharedFilePath + File.separator + request.getContent());
            byte[] data = Files.readAllBytes(file.toPath());
            out.writeObject(data);

            // disconnect
            in.close();
            out.close();
            remoteClientSocket.close();
        } catch (IOException | ClassNotFoundException | ProtocolException e) {
            /* nothing */
        }
    }

    public static Thread getShareThread(String sharedFilePath, int sharePort) {
        return new Thread(() -> {
            try {
                ServerSocket shareSocket = new ServerSocket(sharePort);
                while (true) {
                    Socket remoteClientSocket = shareSocket.accept();
                    new Thread(new P2PHandler(sharedFilePath, remoteClientSocket)).start();
                }
            } catch (IOException e) {
                System.out.println("Share thread crashed: " + e.getMessage());
                System.exit(1);
            }
        });
    }
}
