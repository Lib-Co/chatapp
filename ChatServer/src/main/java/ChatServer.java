import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class ChatServer extends Thread implements MessageProcessor {

    private ServerSocket in;
    private Map<String, Integer> userNameMap = new HashMap<>();
    private Map<Integer, ServerClientHandler> clientHandlers = new HashMap<>();
    private Map<Integer, Queue<Message>> messageQueueMap = new HashMap<>();
    private volatile boolean exit = false;

    public ChatServer(int port) {
        try {
            in = new ServerSocket(port);

            //creating new thread to listen for "exit" on server
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

    // Method to retrieve message from ServerClientHandler and add to message store
    public synchronized void processMessage(int clientID, Message message) {
        // check the message Type
        Message.Type type = message.messageType;
        switch (type) {
            case QUIT:
                messageQueueMap.remove(clientID);
                System.out.println(message.senderUsername + " has quit");
                break;
            case PRIVATE:
                System.out.println(message.senderUsername + ": " + message.data);
                String recipient = message.recipientUsername;
                int recipientID = userNameMap.get(recipient);
                Queue<Message> recipientQueue = messageQueueMap.get(recipientID);
                if (recipientQueue != null) {
                    recipientQueue.add(message);
                }
                break;
            case BROADCAST:
                System.out.println(message.senderUsername + ": " + message.data);
                // Iterate through queues to check that message id does not equal id of message store before adding to list
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

//    private void notifyExit() throws IOException {
//        exit = true;
//        Message exitNotify = new Message(Message.Type.BROADCAST, "ChatApp", "Server is shutting down, your connection will be closed shortly");
//        for (Map.Entry<Integer, Queue<Message>> queueEntry : messageQueueMap.entrySet()) {
//            queueEntry.getValue().add(exitNotify);
//        }
//        in.close();
//    }

    public void exit() throws IOException {
        exit = true;
        in.close();
    }

    // Accepting clients
    // Creating a new ServerClientHandler for each new client and assigning Client number
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

    public static void main(String[] args) {
        new ChatServer(14001).start();
    }
}


// need to enter exit on server side to shut down all client connections cleanly
// exit cmd will be entered here
// inform clients that server is shutting down + close all sockets
// clients will need to listen for this message, so can close socket from client side as well
// use try and catch first to ensure clean exit
// could use System.exit as final termination

// ** also need to remove clients from broadcast lists if their programs crash ans client leaves without netering "quit"
