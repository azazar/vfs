/*
 * PROPRIETARY/CONFIDENTIAL
 */
package asd.structuredfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
public class InputStreamWithCloseHookTest {

    public static void main(String[] args) throws IOException {
        StructuredFile f = new StructuredFile(new File("/tmp/1.zip"), "1");
        
        try (InputStream i = f.open()) {
            System.out.println(IOUtils.toString(i));
        }
    }
    
}
