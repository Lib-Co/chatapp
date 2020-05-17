import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;

// Allows multiple clients to connect
public class ServerClientHandler extends Thread {
    private final Socket s;
    private final int clientID;
    private final MessageProcessor mp;
    private final BlockingQueue<Message> clientMessageQueue;


    //
    public ServerClientHandler(int clientID, Socket s, MessageProcessor mp, BlockingQueue<Message> cmq) {
        this.clientID = clientID;
        this.s = s;
        this.mp = mp;
        this.clientMessageQueue = cmq;
    }

    //
    public void run() {
        try {
            // Stream to be sent from the server to the client across the socket
            new Thread(() -> {
                try {
                    PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true);
                    while (true) {
                        Message message = clientMessageQueue.take();
                        clientOut.println(message.senderUsername + ":  " + message.data);
                        //TODO: Need to break out of this loop
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

            }).start();

            BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String json;
            ObjectMapper mapMsg = new ObjectMapper();

            //Receiving from client
            while ((json = clientIn.readLine()) != null) {
                Message message = mapMsg.readValue(json, Message.class);
                message.arrivalTime = Instant.now();
                //Prints out to the server console
//                System.out.println(user + ": " + json);
                mp.processMessage(clientID, message);
            }

        } catch (IOException e) {
            e.printStackTrace();

            // Close the socket
        } finally {
            try {
                this.s.close();
                System.out.println("Closed: " + s);
            } catch (IOException e) {
                System.out.println("Error closing socket for " + clientID);
            }
        }
    }
}

// new to create new thread to wait for exit
// so that it won't accept any new clients and will shut down existing sockets
