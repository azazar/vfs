/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
public class InputStreamWithCloseHookTest {

    public static void main(String[] args) throws IOException {
        //StructuredFile f = new StructuredFile(new File("/tmp/1.zip"), "1");
        StructuredFile f = new StructuredFile(new File("/tmp/export.csv.zst"), "export.csv");

        try (InputStream i = f.open()) {
            System.out.println(IOUtils.toString(i, StandardCharsets.UTF_8));
        }

        StructuredFileScanner scanner = new StructuredFileScanner(file -> {
            try {
                System.out.println(file.getContentAsUTF8String());
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        scanner.scan(new File("/tmp/textfiles.zip"));

    }
    
}
