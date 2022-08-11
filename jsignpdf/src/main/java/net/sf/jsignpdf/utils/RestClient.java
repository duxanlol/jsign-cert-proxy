package net.sf.jsignpdf.utils;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import java.io.File;
import java.io.IOException;

public class RestClient {

    private static final String TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0aW5nIiwiaWF0IjoxNjYwMjE4MTc1fQ.xXiGypug_C--5br4ADoZ-LOeyJ6Zaxvnm7Z6kdvHpSc";
    private static final String DOWNLOAD_UNSIGNED_URI
            = "http://localhost:8080/downloadFile";

    private static final String UPLOAD_SIGNED_URI
            = "http://localhost:8080/uploadSignedFile";

    public byte[] getUnsignedDocument(String documentIdentifier) {
        try{
            return ClientBuilder.newClient()
                    .target(DOWNLOAD_UNSIGNED_URI)
                    .path(documentIdentifier)
                    .request(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.AUTHORIZATION, TOKEN)
                    .get(byte[].class);
        } catch (Exception e) {
            return null;
        }

    }

    public boolean uploadSignedDocument(String filePath) throws IOException {
        final Client upload = ClientBuilder.newBuilder()
                .register(MultiPartFeature.class)
                .build();
        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file",
                new File(filePath),
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
        multiPart.bodyPart(fileDataBodyPart);

        final WebTarget target = upload.target(UPLOAD_SIGNED_URI);
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.AUTHORIZATION, TOKEN)
                .post(Entity.entity(multiPart, multiPart.getMediaType()));

//        System.out.println(response.getStatus() + ">>> "
//                + response.getStatusInfo() + " <<<" + response);
        if (response.getStatus() == 200) return true;
        return false;

    }

    public static void main(String[] args) throws IOException {
        RestClient client = new RestClient();
//        client.uploadSignedDocument("C:\\opt\\test\\asdf.pdf");

    }
}
