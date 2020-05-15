import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

//allows multiple clients to connect
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

    //listening for messages and broadcasting to the client that sent it (not broadcasting to multiple clients)
    //receives messages and sends sent back
    public void run() {
        try {
            System.out.println(user + " has connected on " + s);
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String data;

            new Thread(() -> {
                try {
                    PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true);
                    while (true) {
                        Message message = clientMessageQueue.take();
                        clientOut.println(user + " sent:  " + message.data);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

            }).start();

            /*
            Need to separate out the reading in and sending out of messages
             */
            while ((data = clientIn.readLine()) != null) {
                Instant arrivalTime = Instant.now();
                Message message = new Message(data, arrivalTime, id);
                System.out.println(arrivalTime + ": " + user + " has sent: " + data);
                mp.processMessage(message);
            }
            //Clients are able to end their session by entering "quit"
            if (data != null && data.equals("quit")) {
                System.out.println("Session has ended for " + user);
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
