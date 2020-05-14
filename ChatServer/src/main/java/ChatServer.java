import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;


public class ChatServer extends Thread {

    //
    private ServerSocket in;
    private int i;

    private Boolean exit;


    public ChatServer(int port) {
        try {
            in = new ServerSocket(port);
            i = 1;
            exit = false;

            //creating new thread to listen for "exit" on server
            new Thread(() -> {
                try {
                    BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
                    while (true) {
                        String in = userIn.readLine();
                        if (in.equals("EXIT")) {
                            //or can add synchronized block
                            System.out.println(("Server commencing exit"));
                            synchronized (exit) {
                                exit = true;
                                break;
                            }
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

    //thread safe: blocks multiple threads accessing it at same time
    public synchronized boolean getExit() {
        return exit;
    }

    //accepting clients
    public void run() {
        try {
            while (!getExit()) {
                Socket s = in.accept();
                new ServerClientHandler(s, "Client " + i).start();
                i++;
            }

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
