package net.sf.jsignpdf.types;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Document {
    private byte[] file;
    private String identifier;
}
