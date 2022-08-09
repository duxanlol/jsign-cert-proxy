package net.sf.jsignpdf;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class Controller {

    final static String OUTPUT = "<html><head><title>Example</title></head><body><p>Worked!!!</p></body></html>";
    final static String OUTPUT_HEADERS = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html\r\n" +
            "Access-Control-Allow-Private-Network: true\r\n" +
            "Access-Control-Allow-Origin: http://xxvdv.cc:8085/\r\n" +
            "Content-Length: ";
    final static String OUTPUT_END_OF_HEADERS = "\r\n\r\n";

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8085, 1, InetAddress.getByName("127.0.0.1"));

        while (true) {
            System.out.println("Waiting for the client request");

            Socket socket = server.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String message = in.readLine();
            System.out.println("Message Received: " + message);
            if (message.contains("sign")){
                String documentIdentifier = message.substring(message.lastIndexOf("sign?"), message.lastIndexOf(" "));
                documentIdentifier = documentIdentifier.substring(5);
                System.out.println(documentIdentifier);
                new MainWindow(documentIdentifier);
            }

            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(
                            new BufferedOutputStream(socket.getOutputStream()), "UTF-8")
            );

            out.write(OUTPUT_HEADERS + OUTPUT.length() + OUTPUT_END_OF_HEADERS + OUTPUT);
            out.flush();

            out.close();
            in.close();
            socket.close();

        }
    }
}
