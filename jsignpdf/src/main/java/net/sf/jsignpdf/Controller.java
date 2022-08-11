package net.sf.jsignpdf;

import net.sf.jsignpdf.types.Document;
import net.sf.jsignpdf.types.DocumentStatus;
import net.sf.jsignpdf.utils.RestClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Controller {
    private static int PORT = 8085;
    //final static String OUTPUT = "<html><head><title>Example</title></head><body><p>Worked!!!</p></body></html>";
    final static String OUTPUT_HEADERS = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: text/html\r\n" +
            "Access-Control-Allow-Private-Network: true\r\n" +
            "Access-Control-Allow-Origin: http://xxvdv.cc:8085/\r\n" +
            "Content-Length: ";
    final static String OUTPUT_END_OF_HEADERS = "\r\n\r\n";

    static Map<String, DocumentStatus> statusMap = new HashMap<>();

    public void notify(String a){
        System.out.println("NOTIFIED! "+ a);
    }

    public static void main(String[] args) throws IOException {
        Controller controller = new Controller();
        controller.init();
    }

    public void checkIfAlreadyRunning(){
        try {
            ServerSocket server = new ServerSocket(PORT);
            server.close();
        } catch (IOException e) {
            System.err.println("Application already running!");
            JOptionPane.showMessageDialog(null, "Program je već upaljen.", "Greška", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
    }

    public void init() throws IOException {
        checkIfAlreadyRunning();
        initTray();
        ServerSocket server = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"));

        while (true) {
            System.out.println("Waiting for the client request");
            Socket socket = server.accept();
            Thread t = new Thread(() -> {
                try {
                    handleRequest(socket);
                } catch (IOException e) {
                    System.out.println("exception");
                }finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("can't close socket");
                    }
                }
            });
            t.start();
        }
    }

    private void handleRequest(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        String message = in.readLine();
        System.out.println("Message Received: " + message);
        if (message != null) {
            if (message.contains("sign")) {
                String documentIdentifier = message.substring(message.lastIndexOf("sign?"), message.lastIndexOf(" "));
                documentIdentifier = documentIdentifier.substring(5);
                System.out.println(documentIdentifier);
                if (!documentIdentifier.isEmpty()) {
                    RestClient restClient = new RestClient();
                    byte[] file = restClient.getUnsignedDocument(documentIdentifier);
                    if(file != null && file.length != 0) {
                        MainWindow mainWindow = new MainWindow(new Document(file, documentIdentifier), this);

                    }else{
                        System.out.println("Fajl ne postoji");
                    }
                }
            }
        }

        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(
                        new BufferedOutputStream(socket.getOutputStream()), "UTF-8")
        );

        BufferedReader br = new BufferedReader( new FileReader("C:\\opt\\test\\sample.html" ));
        String line;
        String OUTPUT = "";
        while((line = br.readLine()) != null){

            OUTPUT += line + "\r\n";
        }
        out.write((OUTPUT_HEADERS + OUTPUT.length() + OUTPUT_END_OF_HEADERS + OUTPUT));
        out.flush();

        out.close();
        in.close();
        socket.close();
    }

    private static void initTray() throws IOException {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        SystemTray tray = SystemTray.getSystemTray();
        Image logoIcon = ImageIO.read(Controller.class.getResource("logo16-white-fill.png"));
        final PopupMenu popup = new PopupMenu();
        popup.add("Osluškuljem port " + PORT + ".");
        MenuItem miExit = new MenuItem("Exit");
        popup.addSeparator();
        popup.add(miExit);
        miExit.addActionListener(evt -> exit());
        final TrayIcon trayIcon = new TrayIcon(logoIcon, "DSPlatform", popup);
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println(e);
        }
    }

    private static void exit() {
        System.exit(0);
    }
}
