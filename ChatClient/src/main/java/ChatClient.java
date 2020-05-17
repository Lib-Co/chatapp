import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.*;

public class ChatClient {

    private Socket clientSocket;
    String user;
    boolean isConnected;
    public Map<Integer, String> map = new HashMap<Integer, String>();

    private boolean isConnected() {
        return isConnected;
    }

    public ChatClient(String host, int port, String user) {
        try {
            clientSocket = new Socket(host, port);
            isConnected = true;
            this.user = user;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLoginMessage(PrintWriter serverOut, ObjectMapper mapper) throws JsonProcessingException {
        Message msg = new Message(Message.Type.LOGIN, user, "");
        serverOut.println(mapper.writeValueAsString(msg));
    }

    public void connect() {

        //Sending
        new Thread(() -> {
            ObjectMapper mapMsg = new ObjectMapper();
            try {
                BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter serverOut = new PrintWriter(clientSocket.getOutputStream(), true);
                sendLoginMessage(serverOut, mapMsg);
                boolean running = true;
                while (running && isConnected) {
                    String data = userIn.readLine();
                    Message msg;
                    if (data.startsWith("@")) {
                        //Add to the message recipient field
                        //set message type to private

                        int spaceIndex = data.indexOf(' ');
                        String recipientUsername = data.substring(1, spaceIndex);
                        String prvMsg = data.substring(spaceIndex+1);

                        msg = new Message(Message.Type.PRIVATE, user, prvMsg);
                        msg.recipientUsername = recipientUsername;

                    }
                    else if (data.equals("quit")) {
                        msg = new Message(Message.Type.QUIT, user, data);
                        //temp measure to stop null print out after user types quit
                        isConnected = false;
                        running = false;
                    }
                    else {
                        msg = new Message(Message.Type.BROADCAST, user, data);
                    }
                    String json = mapMsg.writeValueAsString(msg);
                    //Need to add recipient username here if private message
                    serverOut.println(json);
                }

                // Add functionality to wait for server to close socket and permit exit
                //System.out.println("Program Terminated");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        //Receiving
        new Thread(() -> {
            //
            try {
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while (isConnected) {
                    // Need to add check if the server has sent 'quit ok' message
                    // If 'quit ok' message received, break loop and close the socket from client side
                    // Currently client continues to print null after server has closed socket
                    String line = serverIn.readLine();
                    if (line != null) {
                        System.out.println(line);
                    }
                    else {
                        clientSocket.close();
                        System.out.println("Server socket has been closed. Client socket now also closed");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

    }


    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option host = new Option("cca", "host", true, "IP address of server - if not entered, default of localhost will be assigned");
        host.setRequired(false);

        options.addOption(host);

        Option port = new Option("csp", "port", true, "connection port - if not entered, default of 14001 will be assigned");
        port.setRequired(false);
        options.addOption(port);

        Option user = new Option("u", "user", true, "username selection - this is required to make a connection. usernames must be unique");
        user.setRequired(true);
        options.addOption(user);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            String hostArg = cmd.getOptionValue("host", "localhost");
            int portArg = Integer.parseInt(cmd.getOptionValue("port","14001"));
            String userArg = cmd.getOptionValue("user");

            new ChatClient(hostArg, portArg, userArg).connect();

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ChatClient", options);
            // Exit the program if cmd line arguments
            System.exit(1);
        }

    }

}



// closing the client cleanly = terminal input "quit"
// need to tell server so server gets rid of the connection to close socket
