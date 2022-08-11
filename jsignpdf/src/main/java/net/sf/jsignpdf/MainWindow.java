package net.sf.jsignpdf;

import net.sf.jsignpdf.preview.Pdf2Image;
import net.sf.jsignpdf.preview.SelectionImage;
import net.sf.jsignpdf.types.*;
import net.sf.jsignpdf.utils.GuiUtils;
import net.sf.jsignpdf.utils.KeyStoreUtils;
import net.sf.jsignpdf.utils.RestClient;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Observable;
import java.util.Set;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class MainWindow {

    private JFrame frame;
    private BasicSignerOptions anOptions;
    private SelectionImage selectionImage;
    private JButton sign;
    private JButton previousPage;
    private JButton nextPage;
    private PdfExtraInfo extraInfo;
    private PageInfo pdfPageInfo;
    private Pdf2Image p2i;
    private Document document;
    private RestClient restClient;
    private int numberOfPages = -1;
    private Integer pageNr = null;
    private Controller observer;
    private DocumentStatus documentStatus;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
//                    MainWindow window = new MainWindow("uverenje.pdf");
//                    window.frame.setVisible(true);
//                    window.frame.toFront();
//                    window.frame.requestFocus();
//                    window.frame.setAlwaysOnTop(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    public MainWindow(Document document, Controller observer) {
        this.observer = observer;
        setDocumentStatus(DocumentStatus.INITIALIZING);
        this.document = document;
        initialize(document);
    }

    private void loadPdf(){
        anOptions = new BasicSignerOptions();
        restClient = new RestClient();
        anOptions.setInFileBytes(document.getFile());
        selectionImage = new SelectionImage();
        p2i = new Pdf2Image(anOptions);
        extraInfo = new PdfExtraInfo(anOptions);
        numberOfPages = extraInfo.getNumberOfPages();
        pageNr = null;
        if (numberOfPages > 0){
            pageNr = 1;
        }
        pdfPageInfo = extraInfo.getPageInfo(pageNr);
    }

    private void initialize(Document document) {
        loadPdf();
        frame = new JFrame();
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        frame.setTitle(document.getIdentifier());
        Container pane = frame.getContentPane();

        pane.setPreferredSize(new Dimension(600, 800));
        selectionImage.setPreferredSize(new Dimension(600, 800));
        pane.add(selectionImage, BorderLayout.CENTER);

        sign = new JButton("Potpiši");
        previousPage = new JButton("Prethodna");
        nextPage = new JButton("Sledeća");
        setIcons();
        JPanel southPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new Insets(5, 10, 10, 5);
        southPanel.add(previousPage, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        southPanel.add(sign, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        southPanel.add(nextPage, gridBagConstraints);
        pane.add(southPanel, BorderLayout.PAGE_END);

        updateRendering();
        setActionListeners();

        frame.setSize(new Dimension(600, 800));
        GuiUtils.center(frame);
        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
        frame.setAlwaysOnTop(true);
        setDocumentStatus(DocumentStatus.WORKING);
    }

    private void preDispose() {
        if(documentStatus != DocumentStatus.DONE)
        setDocumentStatus(DocumentStatus.CANCELED);
    }

    private void sign() {
        Float[] coords = selectionImage.getRelRect().getCoords();
        frame.dispose();
        String tmpdir = System.getProperty("java.io.tmpdir");
        System.out.println(tmpdir);
        anOptions.setOutFile(tmpdir + "signed_" + document.getIdentifier());
        anOptions.setPage(pageNr);
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
            System.out.println(anOptions.getOutFile());
            restClient.uploadSignedDocument(anOptions.getOutFile());
            setDocumentStatus(DocumentStatus.DONE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setIcons() {
        try {
            Image backIcon = ImageIO.read(getClass().getResource("prev16.png"));
            Image nextIcon = ImageIO.read(getClass().getResource("next16.png"));
            Image logoIcon = ImageIO.read(getClass().getResource("logo-white-fill.png"));
            frame.setIconImage(logoIcon);
            previousPage.setIcon(new ImageIcon(backIcon));
            nextPage.setIcon(new ImageIcon(nextIcon));
            previousPage.setText("");
            nextPage.setText("");
        } catch (Exception e) {
            System.out.println("Nisam nasao ikonice.");
        }
    }

    private void setActionListeners(){
        nextPage.addActionListener(evt -> nextPageActionPerformed());
        previousPage.addActionListener(evt -> previousPageActionPerformed());
        sign.addActionListener(evt -> sign());
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                preDispose();
            }
        });
    }

    private void nextPageActionPerformed(){
        pageNr++;
        updateRendering();
    }

    private void previousPageActionPerformed(){
        pageNr--;
        updateRendering();
    }

    private void updateRendering(){
        if (pageNr != null) {
            previousPage.setEnabled(pageNr > 1);
            nextPage.setEnabled(pageNr < numberOfPages);
            if (pageNr <= 0 || pageNr > numberOfPages) {
                pageNr = numberOfPages;
            }
            final BufferedImage buffImg = p2i.getImageUsingPdfBox(pageNr);
            if (buffImg != null) {
                selectionImage.setImage(buffImg);
            }
            pageNrChanged();
        }
    }

    protected void pageNrChanged() {
        if (numberOfPages < 1)
            return;
        if (pageNr != null) {
            if (pageNr <= 0 || pageNr > numberOfPages) {
                pageNr = numberOfPages;
            }
            pdfPageInfo = extraInfo.getPageInfo(pageNr);
        }

    }

    public void setDocumentStatus(DocumentStatus documentStatus) {
        this.documentStatus = documentStatus;
        observer.notify(documentStatus.toString());
    }

    public void setObserver(Controller observer) {
        this.observer = observer;
    }
}
