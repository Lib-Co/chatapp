import java.io.*;
import java.net.Socket;

//allows multiple clients to connect
public class ServerClientHandler extends Thread {
    private Socket s;
    private String user;
    //protected List<ServerClientHandler> clients;

    //echo the client message
    public ServerClientHandler(Socket s, String user) {
        this.s = s;
        this.user = user;
    }

    /*public class SendMessage extends Thread {
        protected List<ServerClientHandler> clients;
        protected String userInput;
        private String message;

        public SendMessage(List<ServerClientHandler> clients) {
            this.clients = clients;
            this.message = null;
            this.start();
        }*/

        //listening for messages and broadcasting to the client that sent it (not broadcasting to multiple clients)
        //receives messages and sends sent back
        public void run() {
            try {
                System.out.println(user + " has connected on " + s);
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
                PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true);
                //clients = Collections.synchronizedList(new ArrayList<ServerClientHandler>());
                //this.start();
                String message;
                /*
                Need to separate out the reading in and sending out of messages
                 */
                while ((message = clientIn.readLine()) != null) {
                    System.out.println(user + " has sent: " + message);
                    clientOut.println("sent");
                    clientOut.flush();
                    System.out.println("Server has sent a message to " + s);
                }
                //Clients are able to end their session by entering "quit"
                if (message != null && message.equals("quit")) {
                    System.out.println("Session has ended for " + user);
                }

            } catch (IOException e) {
                e.printStackTrace();

                // Close the socket
            } finally {
                try {
                    this.s.close();
                } catch (IOException e) {

                }
                System.out.println("Closed: " + s);
            }
        }
    }



// new to create new thread to wait for exit
// so that it won't accept any new clients and will shut down existing sockets
