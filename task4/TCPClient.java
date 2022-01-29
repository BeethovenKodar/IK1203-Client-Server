
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class TCPClient {

    private static final int BUFFER_SIZE = 1024;

    public static String askServer(String hostname, int port, String toServer) throws IOException {
        byte [] fromClientBuffer = (toServer + "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] fromServerBuffer = new byte[BUFFER_SIZE];
        StringBuilder result = new StringBuilder();

        Socket cSocket = new Socket(hostname, port);
        cSocket.getOutputStream().write(fromClientBuffer);
        cSocket.setSoTimeout(2000);

        int fromServerLength;
        try {
            while ((fromServerLength = cSocket.getInputStream().read(fromServerBuffer)) != -1) {
                result.append(new String(fromServerBuffer, 0, fromServerLength, StandardCharsets.UTF_8));
                fromServerBuffer = new byte[BUFFER_SIZE];
            }
        } catch (SocketTimeoutException ignored) {}
        cSocket.close();

        if (result.length() == 0) return null;
        else return new String(result);
    }

    public static String askServer(String hostname, int port) throws IOException {
        byte[] fromServerBuffer = new byte[BUFFER_SIZE];
        StringBuilder result = new StringBuilder();

        Socket cSocket = new Socket(hostname, port);
        cSocket.getOutputStream().write(new byte[]{});
        cSocket.setSoTimeout(2000);

        int fromServerLength;
        try {
            while ((fromServerLength = cSocket.getInputStream().read(fromServerBuffer)) != -1) {
                result.append(new String(fromServerBuffer, 0, fromServerLength, StandardCharsets.UTF_8));
                fromServerBuffer = new byte[BUFFER_SIZE];
            }
        } catch (SocketTimeoutException ignored) {}
        cSocket.close();

        if (result.length() == 0) return null;
        else return new String(result);
    }
}


