import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerClientHandler extends Thread {
    private final Socket s;
    private final int clientID;
    private final MessageProcessor mp;
    private final BlockingQueue<Message> clientMessageQueue;

    // New server client handler is created when a new client connects
    public ServerClientHandler(int clientID, Socket s, MessageProcessor mp, BlockingQueue<Message> cmq) {
        this.clientID = clientID;
        this.s = s;
        this.mp = mp;
        this.clientMessageQueue = cmq;
    }


    public void run() {
        ObjectMapper mapMsg = new ObjectMapper();
        //Thread to listen for messages sent by clients
        new Thread(() -> {
            try (PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true)) {
                while (!mp.getExit()) {
                    Message message = clientMessageQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        String json = mapMsg.writeValueAsString(message);
                        clientOut.println(json);
                    }
                }
                //If server exit is initialised, the server client handler will close socket
                s.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        String json;
        try (BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            while ((json = clientIn.readLine()) != null) {
                Message message = mapMsg.readValue(json, Message.class);
                mp.processMessage(clientID, message);
            }
        } catch (SocketException e) {
            System.out.println("CLIENT " + this.clientID + " DISCONNECTED");
        } catch (IOException e) {
            e.printStackTrace();

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
