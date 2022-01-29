
import java.net.*;
import java.io.*;

public class ConcHTTPAsk {

    public static void main(String[] args) {
        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            System.out.println("usage: HTTPAsk <port>");
        }

        try {
            ServerSocket wSocket = new ServerSocket(port);
            while(true) {
                Socket cSocket = wSocket.accept();
                MyRunnable runnable = new MyRunnable(cSocket);
                new Thread(runnable).start();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}

