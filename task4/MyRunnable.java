import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyRunnable implements Runnable {

    private static final int BUFFER_SIZE = 1024;
    private Socket cSocket;

    public MyRunnable(Socket cSocket) {
        this.cSocket = cSocket;
    }

    @Override
    public void run() {
        try {
            final List<String> PARAM_TYPES = Arrays.asList("hostname", "port", "string");
            String REQ_TYPE = null;
            String CMD = null;
            List<String> PARAMS = new ArrayList<>();
            String HTTP_VERSION = null;
            String STATUS_LINE = null;
            cSocket.setSoTimeout(2000);

            StringBuilder req = new StringBuilder();
            try {
                int fromClientLength;
                byte[] fromClientBuffer = new byte[BUFFER_SIZE];
                while ((fromClientLength = cSocket.getInputStream().read(fromClientBuffer)) != -1) {
                    req.append(new String(fromClientBuffer, 0, fromClientLength, StandardCharsets.UTF_8));
                    fromClientBuffer = new byte[BUFFER_SIZE];
                }
            } catch (SocketTimeoutException ignored) {}

            try {
                String reqLine = req.toString().split("\n", 2)[0];
                if (reqLine.equals("GET /favicon.ico HTTP/1.1\r")) {
                    STATUS_LINE = "404 Not Found";
                    HTTP_VERSION = "HTTP/1.1";
                } else {
                    HTTP_VERSION = reqLine.split(" ")[2];                               // -> HTTP/1.1
                    REQ_TYPE = reqLine.split(" ")[0];                                   // -> GET
                    CMD = reqLine.split(" ")[1].split("\\?")[0];                 // -> /ask
                    String parameters = reqLine.split(" ")[1].split("\\?")[1];
                    for (String param : parameters.split("&")) {                        // -> extract [par1, par2, ...]
                        if (!PARAM_TYPES.contains(param.split("=")[0]))
                            STATUS_LINE = "400 Bad Request";
                        PARAMS.add(param.split("=")[1]);
                    }
                }
            } catch (NullPointerException | IndexOutOfBoundsException ex) {
                STATUS_LINE = "400 Bad Request";
            }

            if (STATUS_LINE == null) {
                boolean validPort;
                try {
                    Integer.parseInt(PARAMS.get(1));
                    validPort = true;
                } catch (NumberFormatException nfe) {
                    validPort = false;
                }
                if (!REQ_TYPE.equals("GET") || !CMD.equals("/ask") || !validPort || PARAMS.size() < 2 || !HTTP_VERSION.contains("HTTP/1.1"))
                    STATUS_LINE = "400 Bad Request";
            }

            byte[] toClientBuffer;
            if (STATUS_LINE == null) {
                String serverResponse;
                try {
                    System.out.println("1 " + PARAMS.get(1));
                    if (PARAMS.size() == 3) serverResponse = TCPClient.askServer(
                            PARAMS.get(0), Integer.parseInt(PARAMS.get(1)), PARAMS.get(2));
                    else serverResponse = TCPClient.askServer(
                            PARAMS.get(0), Integer.parseInt(PARAMS.get(1)));
                    STATUS_LINE = "200 OK";
                    System.out.println("2 " + PARAMS.get(1));
                    String response = HTTP_VERSION + " " + STATUS_LINE + "\r\n" +
                            "Content-Length: " + serverResponse.length() + "\r\n\r\n" +
                            serverResponse;
                    toClientBuffer = response.getBytes(StandardCharsets.UTF_8);
                } catch (UnknownHostException | ConnectException ex) {
                    STATUS_LINE = "404 Error Not Found";
                    String DATA = "\n\n\n\t\t" + STATUS_LINE + "\n" +
                            "\t\tusage: hostname:port/ask?hostname=___&port=___&string=___, string is optional\n" +
                            "\n\t\tInformation:\n" +
                            "\t\t400 - Check that the URL is correctly constructed\n" +
                            "\t\t404 - Check that the hostname and port is valid";
                    String response = HTTP_VERSION + " " + STATUS_LINE + "\r\n" +
                            "Content-Length: " + DATA.length() + "\r\n\r\n" +
                            DATA;
                    toClientBuffer = response.getBytes(StandardCharsets.UTF_8);
                }
            } else {
                String DATA = "\n\n\n\t\t" + STATUS_LINE + "\n" +
                        "\t\tusage: hostname:port/ask?hostname=___&port=___&string=___, string is optional\n" +
                        "\n\t\tInformation:\n" +
                        "\t\t400 - Check that the URL is correctly constructed\n" +
                        "\t\t404 - Check that the hostname and port is valid";
                String response = HTTP_VERSION + " " + STATUS_LINE + "\r\n" +
                        "Content-Length: " + DATA.length() + "\r\n\r\n" +
                        DATA;
                toClientBuffer = response.getBytes(StandardCharsets.UTF_8);
            }

            cSocket.getOutputStream().write(toClientBuffer);
            cSocket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
