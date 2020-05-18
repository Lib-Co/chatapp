import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.*;

public class ChatClient {

    private Socket clientSocket;
    String user;
    volatile boolean isConnected;
    boolean isBot;
    InputStream inputStream;
    PrintWriter botPrintWriter;

    public ChatClient(String host, int port, String user, boolean isBot) {
        try {
            clientSocket = new Socket(host, port);
            isConnected = true;
            this.user = user;
            //Normal clients read from stdin, while chatbot clients use an alternative InputStream
            if (isBot) {
                this.isBot = true;
                inputStream = new PipedInputStream();
                PipedOutputStream outputStream = new PipedOutputStream((PipedInputStream) inputStream);
                botPrintWriter = new PrintWriter(outputStream, true);
            } else {
                inputStream = System.in;
            }
        } catch (IOException e) {
            System.out.println("Unable to connect. ChatApp server is not running.");
        }
        if (clientSocket == null) {
            System.exit(1);
        }
    }

    private void sendLoginMessage(PrintWriter serverOut, ObjectMapper mapper) throws JsonProcessingException {
        Message msg = new Message(Message.Type.LOGIN, user, "");
        serverOut.println(mapper.writeValueAsString(msg));
    }

    public void connect() {
        ObjectMapper mapMsg = new ObjectMapper();
        //Sending messages to the server
        new Thread(() -> {
            try {
                BufferedReader userIn = new BufferedReader(new InputStreamReader(inputStream));
                PrintWriter serverOut = new PrintWriter(clientSocket.getOutputStream(), true);
                sendLoginMessage(serverOut, mapMsg);
                while (isConnected) {
                    String data = userIn.readLine();
                    Message msg;
                    if (data.startsWith("@")) {
                        //Removing the @ character (required format of a private message) in order to read recipient username
                        int spaceIndex = data.indexOf(' ');
                        if (spaceIndex != -1 && spaceIndex + 1 < data.length()) {
                            String recipientUsername = data.substring(1, spaceIndex);
                            String prvMsg = data.substring(spaceIndex + 1);
                            //Create a private message and add tag for client to indicate that they received a private message
                            msg = new Message(Message.Type.PRIVATE, user, prvMsg);
                            msg.recipientUsername = recipientUsername;
                            msg.tag = "[private]";
                        } else {
                            System.out.println("Private message is invalid because it is empty. Please use a space after the username and type a message.");
                            continue;
                        }

                    } else if (data.equals("quit")) {
                        msg = new Message(Message.Type.QUIT, user, data);
                        isConnected = false;
                        int exitStatus = isConnected ? 1 : 0;
                        System.exit(exitStatus);
                    } else {
                        msg = new Message(Message.Type.BROADCAST, user, data);
                    }
                    String json = mapMsg.writeValueAsString(msg);
                    serverOut.println(json);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        //Receiving messages from the server
        new Thread(() -> {
            try {
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while (isConnected) {
                    String json = serverIn.readLine();
                    if (json != null) {
                        Message message = mapMsg.readValue(json, Message.class);
                        //Chatbots will retrieve pre-scripted messages from the botMsg list
                        if (isBot && message.messageType == Message.Type.PRIVATE) {
                            generateMessage(message);
                        }

                        String output = null;
                        switch (message.messageType) {
                            case BROADCAST:
                                output = message.senderUsername + ":  " + message.data;
                                break;
                            //Private messages will display a tag to indicate to the recipient
                            case PRIVATE:
                                output = message.senderUsername + " " + message.tag + ": " + message.data;
                                break;
                        }
                        if (output != null) {
                            System.out.println(output);
                        }
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    //Pre-scripted messages for chatbots
    public List<String> botMsg = new ArrayList<>(Arrays.asList(" Hello", " How are you?", " It's cold", " I don't understand", " Oh really?", " Nice to meet you", "  The end"));

    private void generateMessage(Message message) {
        Random rand = new Random();
        String randomMsg = botMsg.get(rand.nextInt(botMsg.size()));
        botPrintWriter.println("@" + message.senderUsername + randomMsg);
    }

    //Optional parameters. Please see README.md for details
    public static void main(String[] args) {
        Options options = new Options();

        Option host = new Option("cca", "host", true, "IP address of server - if not entered, default of localhost will be assigned");
        options.addOption(host);

        Option port = new Option("ccp", "port", true, "Connection port - if not entered, default of 14001 will be assigned");
        options.addOption(port);

        Option user = new Option("u", "user", true, "Username selection - this is required to make a connection. Usernames must be unique");
        options.addOption(user);

        Option chatbot = new Option("cb", "chatbot", false, "Add chatbot to session - if not entered, default of no chatbot is assigned");
        options.addOption(chatbot);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            String hostArg = cmd.getOptionValue("host", "localhost");
            int portArg = Integer.parseInt(cmd.getOptionValue("port", "14001"));
            String userArg = cmd.getOptionValue("user", "DefaultUser");
            boolean isBot = false;
            if (cmd.hasOption("chatbot")) {
                isBot = true;
            }

            new ChatClient(hostArg, portArg, userArg, isBot).connect();

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ChatClient", options);
            // Exit the program if cmd line arguments are not entered correctly
            System.exit(1);
        }
    }
}

