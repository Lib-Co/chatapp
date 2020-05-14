import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {

    private Socket server;

    public ChatClient(String address, int port){
        try {
            server = new Socket(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(){
        try {
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter serverOut = new PrintWriter(server.getOutputStream(), true);
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(server.getInputStream()));
            String in = "";
            while (!in.equals("quit")) {
                in = userIn.readLine();
                serverOut.println(in);
                String back = serverIn.readLine();
                System.out.println(back);
            }
            if (in.equals("quit")){
                System.out.println("Program Terminated");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatClient("localhost", 14001).connect();
    }
}

// closing the client cleanly = terminal input "quit"
// need to tell server so server gets rid of the connection to close socket
