
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class TCPClient {

    private static final int BUFFER_SIZE = 1024;
    private static byte[] fromServerBuffer;
    private static int fromServerLength;

    public static String askServer(String hostname, int port, String toServer) throws IOException {
        byte [] fromClientBuffer = (toServer + "\r\n").getBytes(StandardCharsets.UTF_8);
        fromServerBuffer = new byte[BUFFER_SIZE];
        StringBuilder result = new StringBuilder();

        Socket cSocket = new Socket(hostname, port);
        cSocket.getOutputStream().write(fromClientBuffer);
        cSocket.setSoTimeout(1000);

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
        fromServerBuffer = new byte[BUFFER_SIZE];
        StringBuilder result = new StringBuilder();

        Socket cSocket = new Socket(hostname, port);
        cSocket.getOutputStream().write(new byte[]{});
        cSocket.setSoTimeout(5000);

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


