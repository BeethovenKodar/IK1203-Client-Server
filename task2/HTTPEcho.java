
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class HTTPEcho {

    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String STATUS_CODE = "200 OK";
    private static final String CONTENT_LENGTH = "Content-Length: ";
    private static String DATA = "Data: ";
    private static int LENGTH = 0;

    public static void main(String[] args) {
        int port = 0;
        int BUFFER_SIZE = 1024;

        //parse the port the socket should be listening on
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println("usage: java HTTPEcho <port>");
        }

        try {
            ServerSocket welcomeSocket = new ServerSocket(port);
            while(true) {
                Socket connectionSocket = welcomeSocket.accept();
                connectionSocket.setSoTimeout(2000);
                StringBuilder input = new StringBuilder();
                StringBuilder response = new StringBuilder();

                int fromClientLength;
                byte[] fromClientBuffer = new byte[BUFFER_SIZE];
                try {
                    while(true) {
                        fromClientLength = connectionSocket.getInputStream().read(fromClientBuffer);
                        input.append(new String(fromClientBuffer, 0, fromClientLength, StandardCharsets.UTF_8));
                        if (input.charAt(input.length() - 2) == '\r' && input.charAt(input.length() - 1) == '\n')
                            break;
                        fromClientBuffer = new byte[BUFFER_SIZE];
                    }
                } catch (SocketTimeoutException ignored) {}

                DATA = input.toString();
                LENGTH = DATA.length();

                //create the response
                response.append(HTTP_VERSION).append(" ").append(STATUS_CODE).append("\r\n");
                response.append(CONTENT_LENGTH).append(LENGTH).append("\r\n\r\n");
                response.append(DATA);

                //write the response to the client
                byte[] toClientBuffer = response.toString().getBytes(StandardCharsets.UTF_8);
                connectionSocket.getOutputStream().write(toClientBuffer);
                connectionSocket.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

