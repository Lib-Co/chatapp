import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.StringConcatFactory;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerClientHandler extends Thread{

    private Socket s;
    private String user;

    public ServerClientHandler(Socket s, String user) {
        this.s = s;
        this.user = user;
    }

    public void run() {
        try {
            System.out.println(user + " has connected on " + s);
            BufferedReader clientIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter clientOut = new PrintWriter(s.getOutputStream(), true);
            String message;
            while ((message = clientIn.readLine()) != null) {
                System.out.println(user + " has sent: " + message);
                if (message.equals("quit")) {
                    System.out.println("Session has ended for " + user);
                }
                clientOut.println("sent");
                clientOut.flush();
                System.out.println("Server has sent a message to " + s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


