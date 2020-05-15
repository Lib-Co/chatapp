import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {

    private Socket clientSocket;

    private boolean isConnected() {
        return false;
    }

    public ChatClient(String address, int port){
        try {
            clientSocket = new Socket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(){

        new Thread(() -> {
            try {
                BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter serverOut = new PrintWriter(clientSocket.getOutputStream(), true);
                String in = "";
                while (!in.equals("quit")) {
                    in = userIn.readLine();
                    serverOut.println(in);
                }
                System.out.println("Program Terminated");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while (true) {
                    String line = serverIn.readLine();
                    System.out.println(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();



    }



    public static void main(String[] args) {
        new ChatClient("localhost", 14001).connect();
    }
}

// closing the client cleanly = terminal input "quit"
// need to tell server so server gets rid of the connection to close socket
