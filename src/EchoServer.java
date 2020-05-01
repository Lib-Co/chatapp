import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer extends Thread{

    private ServerSocket in;
    private int i;

    public EchoServer (int port) {
        try {
            in = new ServerSocket(port);
            i = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while(true) {
                Socket s = in.accept();
                new ServerClientHandler(s, "Client " + i).start();
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new EchoServer(14001).start();
    }
}
