package se.kth.id2212.project.fish.client;

import se.kth.id2212.project.fish.client.gms.GroupMembershipService;
import se.kth.id2212.project.fish.common.Message;
import se.kth.id2212.project.fish.common.MessageDescriptor;
import se.kth.id2212.project.fish.common.FileAddress;
import se.kth.id2212.project.fish.common.ProtocolException;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {
    public static final String USAGE = "New group: java Client [download_path] [shared_file_path] [local_port]\n" +
            " Existing: java Client [download_path] [shared_file_path] [local_port] [member_ip] [member_port]";

    private String sharedFilePath, downloadPath;
    private int localPort;
    private ClientAddress anyGroupMember;
    private GroupMembershipService gms;

    public Client(String downloadPath, String sharedFilePath, int localPort) {
        this(downloadPath, sharedFilePath, localPort, null);
    }

    public Client(String downloadPath, String sharedFilePath, int localPort, ClientAddress anyGroupMember) {
        this.sharedFilePath = sharedFilePath;
        this.downloadPath = downloadPath;
        this.localPort = localPort;
        this.anyGroupMember = anyGroupMember;
    }

    public void run() {
        if (anyGroupMember == null) {
            System.out.println("Creates a new group");
            gms = new GroupMembershipService(this, localPort);
        } else {
            System.out.println("Joins an existing group");
            gms = new GroupMembershipService(this, localPort, anyGroupMember);
        }

        new Thread(gms).start();

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                // nothing
            }
        }

        prompt();
        System.exit(0);
    }

    public ArrayList<String> getFileList() {
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

    public String getSharedFilePath() {
        return sharedFilePath;
    }

    private void prompt() {
        System.out.println("\nFISH client ready.");
        for (boolean stop = false; !stop; ) {
            System.out.print("\n1. Search\n2. Exit\n> ");
            switch (new Scanner(System.in).nextLine()) {
                case "1":
                    try {
                        search();
                    } catch (IOException | ClassNotFoundException | ProtocolException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "2":
                    stop = true;
                    break;
                default:
                    System.out.println("Invalid input");
                    break;
            }
        }
    }

    private void search() throws IOException, ClassNotFoundException, ProtocolException {
        System.out.print("Query: ");
        String query = new Scanner(System.in).nextLine();

        HashMap<ClientAddress, Message> replies = gms.broadcast(new Message(MessageDescriptor.QUERY, query));

        final int[] i = {0};
        ArrayList<FileAddress> matchedFiles = new ArrayList<>();
        replies.forEach((member, message) -> {
            ((ArrayList<String>) message.getContent()).forEach(file -> {
                matchedFiles.add(new FileAddress(member.getIp(), member.getPort(), file));
                System.out.println(++i[0] + ". " + matchedFiles.get(i[0] - 1));
            });
        });
        if (i[0] == 0) {
            System.out.println("File not found");
        } else {
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
                } else if (input >= 1 && input <= i[0]) {
                    fetch(matchedFiles.get(input - 1));
                    stop = true;
                } else {
                    System.out.println("Invalid input");
                }
            }
        }
    }

    private void fetch(FileAddress fileAddress) throws IOException, ClassNotFoundException, ProtocolException {
        // connect
        Socket remoteClientSocket = new Socket(fileAddress.getIp(), fileAddress.getPort());
        ObjectOutputStream outRC = new ObjectOutputStream(remoteClientSocket.getOutputStream());
        outRC.flush();
        ObjectInputStream inRC = new ObjectInputStream(remoteClientSocket.getInputStream());

        // request
        outRC.writeObject(new Message(MessageDescriptor.FETCH, fileAddress.getFile()));
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
    }

    public static void main(String[] args) {
        if (args.length == 3) {
            new Client(args[0], args[1], Integer.parseInt(args[2])).run();
        } else if (args.length == 5) {
            new Client(args[0], args[1], Integer.parseInt(args[2]), new ClientAddress(args[3], Integer.parseInt(args[4]))).run();
        } else {
            System.out.println("FISH client: invalid number of arguments\n" + USAGE);
        }
    }
}
