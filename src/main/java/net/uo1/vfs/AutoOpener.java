/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import com.github.luben.zstd.ZstdInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
class AutoOpener implements Callable<InputStream> {

    private static final Logger LOG = Logger.getLogger(AutoOpener.class.getName());
    
    private final StructuredFile file;

    AutoOpener(StructuredFile file) {
        this.file = file;
    }

    @Override
    public InputStream call() throws Exception {
        return open();
    }
    
    InputStream openWrappedStream(InputStream in, String filename, String[] internal) throws IOException {
        if (internal.length == 0) {
            return in;
        }

        if (filename.equals(internal[0] + ".gz")) {
            return openWrappedStream(new GZIPInputStream(in), internal[0], ArrayUtil.shift(internal));
        }

        if (filename.equals(internal[0] + ".zst")) {
            return openWrappedStream(new ZstdInputStream(in), internal[0], ArrayUtil.shift(internal));
        }

        if (filename.endsWith(".zip")) {
            ZipInputStream zis = new ZipInputStream(in);
            
            try {
                ZipEntry ze;
                
                while ((ze = zis.getNextEntry()) != null) {
                    if (ze.getName().equals(internal[0])) {
                        InputStream w = zis;
                        zis = null;
                        return new InputStreamWithCloseHook(openWrappedStream(w, internal[0], ArrayUtil.shift(internal)), () -> {
                            try {
                                w.close();
                            }
                            catch (IOException ex) {
                                LOG.log(Level.SEVERE, null, ex);
                            }
                        });
                    }
                }
            }
            finally {
                if (zis != null) {
                    zis.close();
                }
            }
        }
        
        throw new FileNotFoundException(internal[0]);
    }
    

    InputStream open() throws IOException {
        InputStream in;
        String filename;

        if (file.file instanceof File) {
            in = new FileInputStream((File) file.file);
            filename = ((File) file.file).getName();
        }
        else if (file.file instanceof URL) {
            in = ((URL)file.file).openStream();
            filename = ((URL)file.file).getFile();
        }
        else {
            throw new IllegalStateException(file.file.toString());
        }

        return openWrappedStream(in, filename, file.archived);
    }

}
