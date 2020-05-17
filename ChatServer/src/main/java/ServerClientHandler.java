import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

// Allows multiple clients to connect
public class ServerClientHandler extends Thread {
    private Socket s;
    private String user;
    private MessageProcessor mp;
    private int id;
    BlockingQueue<Message> clientMessageQueue;


    //
    public ServerClientHandler(Socket s, String u, int id, MessageProcessor mp, BlockingQueue<Message> cmq) {
        this.s = s;
        this.user = u;
        this.id = id;
        this.mp = mp;
        this.clientMessageQueue = cmq;
    }

    //
    public void run() {
        try {
            System.out.println(user + " has connected on " + s);
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()));

            // Stream to be sent from the server to the client across the socket
            new Thread(() -> {
                try {
                    PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true);
                    while (true) {
                        Message message = clientMessageQueue.take();
                        if (message.data != null) {
                            clientOut.println(user + ":  " + message.data);
                        }
                        else {
                            clientOut.println("You are quitting the program");
                        }
                        }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

            }).start();


            String json;
            ObjectMapper mapMsg = new ObjectMapper ();

            while ((json = clientIn.readLine()) != null) {
                Message message = mapMsg.readValue(json, Message.class);
                message.arrivalTime = Instant.now();
                message.id = id;
                System.out.println(user + ": " + json);
                //Clients are able to end their session by entering "quit"
                if (message.messageType.equals(Message.Type.QUIT)) {
                    System.out.println("Session ending for " + user);
                    break;
                }
                mp.processMessage(message);

            }


        } catch (IOException e) {
            e.printStackTrace();

            // Close the socket
        } finally {
            try {
                this.s.close();

                System.out.println("Closed: " + s);
            } catch (IOException e) {
                System.out.println("Error closing socket for " + user);

            }

        }
    }
}

// new to create new thread to wait for exit
// so that it won't accept any new clients and will shut down existing sockets
