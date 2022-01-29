
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HTTPAsk {
    private static final int BUFFER_SIZE = 1024;

    private static String REQ_TYPE = null;
    private static String CMD = null;
    private static List<String> PARAMS = new ArrayList<>();
    private static String HTTP_VERSION = null;
    private static String STATUS_LINE = null;

    private static void reset() {
        REQ_TYPE = null;
        CMD = null;
        PARAMS = new ArrayList<>();
        HTTP_VERSION = null;
        STATUS_LINE = null;
    }

    public static void main(String[] args) {
        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            System.out.println("usage: HTTPAsk <port>");
        }

        try {
            ServerSocket wSocket = new ServerSocket(port);
            while(true) {
                Socket cSocket = wSocket.accept();
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
                parseRequest(req.toString()); //parse the parameters needed for TCPClient
                if (STATUS_LINE == null)
                    evaluateParsed(); //evaluate if correct format on request
                byte[] toClientBuffer;
                if (STATUS_LINE == null) {
                    String serverResponse;
                    try { //interaction as client with TCPClient
                        if (PARAMS.size() == 3) serverResponse = TCPClient.askServer(
                                PARAMS.get(0), Integer.parseInt(PARAMS.get(1)), PARAMS.get(2));
                        else serverResponse = TCPClient.askServer(
                                PARAMS.get(0), Integer.parseInt(PARAMS.get(1)));
                        STATUS_LINE = "200 OK";
                        toClientBuffer = createSuccessfulHTTPResponse(serverResponse);
                    } catch (UnknownHostException | ConnectException ex) {
                        STATUS_LINE = "404 Error Not Found";
                        toClientBuffer = createFailedHTTPResponse();
                    }
                } else { //400 Bad Request
                    toClientBuffer = createFailedHTTPResponse();
                }

                //write response to the client (browser)
                cSocket.getOutputStream().write(toClientBuffer);
                cSocket.close();
                reset();
            }
        } catch (IOException  ex) {
            ex.printStackTrace();
        }
    }

    private static byte[] createFailedHTTPResponse() {
        String DATA = "\n\n\n\t\t" + STATUS_LINE + "\n" +
                "\t\tusage: hostname:port/ask?hostname=___&port=___&string=___, string is optional\n" +
                "\n\t\tInformation:\n" +
                "\t\t400 - Check that the URL is correctly constructed\n" +
                "\t\t404 - Check that the hostname and port is valid";
        String response = HTTP_VERSION + " " + STATUS_LINE + "\r\n" +
                "Content-Length: " + DATA.length() + "\r\n\r\n" +
                DATA;
        return response.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] createSuccessfulHTTPResponse(String DATA) {
        String response = HTTP_VERSION + " " + STATUS_LINE + "\r\n" +
                "Content-Length: " + DATA.length() + "\r\n\r\n" +
                DATA;
        return response.getBytes(StandardCharsets.UTF_8);
    }

    private static void evaluateParsed() {
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

    static final List<String> PARAM_TYPES = Arrays.asList("hostname", "port", "string");
    private static void parseRequest(String req) {
        try {
            String reqLine = req.split("\n", 2)[0];                       // -> isolate line of interest
            if (reqLine.equals("GET /favicon.ico HTTP/1.1\r")) {
                STATUS_LINE = "404 Not Found";
                HTTP_VERSION = "1.1";
                return;
            }
            //GET /ask?hostname=time.nist.gov&port=13 HTTP/1.1
            HTTP_VERSION = reqLine.split(" ")[2];                               // -> HTTP/1.1
            REQ_TYPE = reqLine.split(" ")[0];                                   // -> GET
            CMD = reqLine.split(" ")[1].split("\\?")[0];                 // -> /ask
            String parameters = reqLine.split(" ")[1].split("\\?")[1];
            for (String param : parameters.split("&")) {                        // -> extract [par1, par2, ...]
                if (!PARAM_TYPES.contains(param.split("=")[0]))
                    STATUS_LINE = "400 Bad Request";
                PARAMS.add(param.split("=")[1]);
            }
        } catch (NullPointerException | IndexOutOfBoundsException ex) {
            STATUS_LINE = "400 Bad Request";
        }
    }

}

