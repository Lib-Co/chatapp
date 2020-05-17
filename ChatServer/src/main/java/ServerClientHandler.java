import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

// Allows multiple clients to connect
public class ServerClientHandler extends Thread {
    private final Socket s;
    private final int clientID;
    private final MessageProcessor mp;
    private final BlockingQueue<Message> clientMessageQueue;
    private volatile boolean exit = true;


    //
    public ServerClientHandler(int clientID, Socket s, MessageProcessor mp, BlockingQueue<Message> cmq) {
        this.clientID = clientID;
        this.s = s;
        this.mp = mp;
        this.clientMessageQueue = cmq;
    }


    public void run() {

        new Thread(() -> {
            try (PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true)) {
                while (!mp.getExit()) {
                    Message message = clientMessageQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        clientOut.println(message.senderUsername + ":  " + message.data);
                    }
                }
                //close socket
                s.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            //Sending to client
            // Stream to be sent from the server to the client across the socket
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
        } catch (SocketException e) {
            System.out.println("CLIENT " + this.clientID +  " DISCONNECTED");
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
