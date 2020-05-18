import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerClientHandler extends Thread {
    private final Socket s;
    private final int clientID;
    private final MessageProcessor mp;
    private final BlockingQueue<Message> clientMessageQueue;
    private volatile boolean exit = true;
    private int serverRunning;


    // New server client handler is created when a new client connects
    // Handle
    public ServerClientHandler(int clientID, Socket s, MessageProcessor mp, BlockingQueue<Message> cmq) {
        this.clientID = clientID;
        this.s = s;
        this.mp = mp;
        this.clientMessageQueue = cmq;
    }


    public void run() {
        ObjectMapper mapMsg = new ObjectMapper();

        //Listening for messages added to the client's message queue
        new Thread(() -> {
            try (PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true)) {
                while (!mp.getExit()) {
                    Message message = clientMessageQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (message != null) {

                        String json = mapMsg.writeValueAsString(message);
                        clientOut.println(json);
                    }
                }
                // If server exit is initialised, the server client handler will close socket
                s.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        String json;
        //String to be sent to the server from the client across the socket
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            //Receiving from client
            while ((json = clientIn.readLine()) != null) {
                Message message = mapMsg.readValue(json, Message.class);
                mp.processMessage(clientID, message);
            }
        } catch (SocketException e) {
            //System.out.println("CLIENT " + this.clientID +  " DISCONNECTED");
            System.out.println("null from client");
        } catch (IOException e) {
            e.printStackTrace();

        //Close the socket
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
