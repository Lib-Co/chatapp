import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.*;


public class ChatServer extends Thread implements MessageProcessor {

    private ServerSocket in;
    private Map<String, Integer> userNameMap = new HashMap<>();
    private Map<Integer, ServerClientHandler> clientHandlers = new HashMap<>();
    private Map<Integer, Queue<Message>> messageQueueMap = new HashMap<>();
    private volatile boolean exit = false;

    public ChatServer(int port) {
        try {
            in = new ServerSocket(port);
            //Thread to listen for "exit" on server
            new Thread(() -> {
                try (BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in))) {
                    while (true) {
                        String in = userIn.readLine();
                        if (in.equals("EXIT")) {
                            System.out.println(("Server commencing exit"));
                            exit();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getExit() {
        return exit;
    }

    //Process messages from client based on message type
    public synchronized void processMessage(int clientID, Message message) {
        //Check the message type and add to client queues as required
        Message.Type type = message.messageType;
        switch (type) {
            case QUIT:
                messageQueueMap.remove(clientID);
                System.out.println(message.senderUsername + " has quit");
                break;
            case PRIVATE:
                System.out.println(message.senderUsername + ": " + message.data);
                String recipient = message.recipientUsername;
                //Check if intended recipient exists
                if (!userNameMap.containsKey(recipient)) {
                    System.out.println("Specified user is not connected to server. Cannot send message.");
                    break;
                }
                int recipientID = userNameMap.get(recipient);
                Queue<Message> recipientQueue = messageQueueMap.get(recipientID);
                if (recipientQueue != null) {
                    recipientQueue.add(message);
                }
                break;
            case BROADCAST:
                System.out.println(message.senderUsername + ": " + message.data);
                //Iterate through queues to check that client id of the sender does not equal id of the message queue before adding message
                //Ensures senders do not receive their own messages as echos from the server
                for (Map.Entry<Integer, Queue<Message>> queueEntry : messageQueueMap.entrySet()) {
                    if (!queueEntry.getKey().equals(clientID)) {
                        queueEntry.getValue().add(message);
                    }
                }
                break;
            case LOGIN:
                userNameMap.put(message.senderUsername, clientID);
                System.out.println(message.senderUsername + " has connected successfully");
                break;
        }

    }

    public void exit() throws IOException {
        exit = true;
        in.close();
    }

    //Accepting clients and creating a new ServerClientHandler for each new client with unique client ID
    public void run() {
        int currentID = 0;

        try {
            while (!exit) {
                Socket s = in.accept();
                BlockingQueue<Message> broadcastQueue = new LinkedBlockingQueue<>();
                ServerClientHandler c = new ServerClientHandler(currentID, s, this, broadcastQueue);
                clientHandlers.put(currentID, c);
                messageQueueMap.put(currentID, broadcastQueue);
                c.start();
                currentID++;
            }
        } catch (SocketException e) {
            System.out.println("SERVER CLOSING");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Optional parameters. Please see README.md for details
    public static void main(String[] args) {
        Options options = new Options();

        Option port = new Option("csp", "port", true, "Connection port - if not entered, default of 14001 will be assigned");
        options.addOption(port);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            int portArg = Integer.parseInt(cmd.getOptionValue("port", "14001"));

            new ChatServer(portArg).start();

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ChatClient", options);
            // Exit the program if cmd line arguments are not entered correctly
            System.exit(1);
        }
    }
}

