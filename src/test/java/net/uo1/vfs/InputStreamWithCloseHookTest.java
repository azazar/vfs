/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.File;
import java.io.IOException;
import static java.lang.System.out;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
public class InputStreamWithCloseHookTest {

    public static void main(String[] args) throws IOException {
        //StructuredFile f = new StructuredFile(new File("/tmp/1.zip"), "1");
        var f = new VfsFile(new File("/tmp/export.csv.zst"), "export.csv");

        try ( var i = f.open()) {
            out.println(IOUtils.toString(i, UTF_8));
        }

        try (var scanner = new VfsScanner(file -> {
            try {
                out.println(file.getContentAsUTF8String());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        })) {
            scanner.scan(new File("/tmp/textfiles.zip"));
        }

    }

}
