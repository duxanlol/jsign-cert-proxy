package net.sf.jsignpdf;

import net.sf.jsignpdf.preview.Pdf2Image;
import net.sf.jsignpdf.preview.SelectionImage;
import net.sf.jsignpdf.types.*;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.RestClient;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Set;

import static java.lang.System.out;

public class MainWindow {

    private JFrame frame;
    private BasicSignerOptions options;
    private SelectionImage selectionImage;
    private JButton sign;
    private JButton previousPage;
    private JButton nextPage;
    private PdfExtraInfo extraInfo;
    private PageInfo pdfPageInfo;

    public static void main(String[] args) {

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow("uverenje.pdf");
                    window.frame.setVisible(true);
                    window.frame.toFront();
                    window.frame.requestFocus();
                    window.frame.setAlwaysOnTop(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public MainWindow(String documentIdentifier) {
        initialize(documentIdentifier);
    }

    private void initialize(String documentIdentifier) {
        BasicSignerOptions anOptions = new BasicSignerOptions();
        RestClient restClient = new RestClient();
        byte[] file = restClient.getUnsignedDocument(documentIdentifier);
        anOptions.setInFileBytes(file);
        selectionImage = new SelectionImage();
        Pdf2Image p2i = new Pdf2Image(anOptions);
        extraInfo = new PdfExtraInfo(anOptions);
        selectionImage.setImage(p2i.getImageUsingPdfBox(1));
        pdfPageInfo = extraInfo.getPageInfo(1);

        frame = new JFrame();
        Container pane = frame.getContentPane();
        pane.setPreferredSize(new Dimension(600, 800));

        selectionImage.setPreferredSize(new Dimension(600, 800));
        pane.add(selectionImage, BorderLayout.CENTER);

        sign = new JButton("Potpiši");
        previousPage = new JButton("<");
        nextPage = new JButton(">");
        pane.add(previousPage, BorderLayout.WEST);
        pane.add(nextPage, BorderLayout.EAST);
        sign.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Float[] coords = selectionImage.getRelRect().getCoords();

                frame.dispose();

                anOptions.setOutFile("C:\\opt\\test\\signed_" + documentIdentifier);
                final Set<String> tmpKsTypes = KeyStoreUtils.getKeyStores();
                if (tmpKsTypes.contains(Constants.KEYSTORE_TYPE_WINDOWS_MY)) {
                    anOptions.setKsType(Constants.KEYSTORE_TYPE_WINDOWS_MY);
                }
                anOptions.setVisible(true);
                anOptions.setPositionLLX(coords[0] * pdfPageInfo.getWidth());
                anOptions.setPositionLLY(coords[1] * pdfPageInfo.getHeight());
                anOptions.setPositionURX(coords[2] * pdfPageInfo.getWidth());
                anOptions.setPositionURY(coords[3] * pdfPageInfo.getHeight());

                SignerLogic signerLogic = new SignerLogic(anOptions);
                signerLogic.signFile();
                try {
                    out.println(anOptions.getOutFile());
                    restClient.uploadSignedDocument(anOptions.getOutFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        pane.add(sign, BorderLayout.SOUTH);


        frame.setSize(new Dimension(600, 800));
        frame.setVisible(true);
    }

}
