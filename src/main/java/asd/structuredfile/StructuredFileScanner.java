/*
 * The MIT License
 *
 * Copyright 2015 Mikhail Yevchenko <spam@azazar.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package asd.structuredfile;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Mikhail Yevchenko <spam@azazar.com>
 */
public class StructuredFileScanner {

    private static final Logger LOG = Logger.getLogger(StructuredFileScanner.class.getName());

    protected Consumer<StructuredFile> consumer;

    public StructuredFileScanner(Consumer<StructuredFile> consumer) {
        this.consumer = consumer;
    }

    public void scan(StructuredFile file) throws IOException {
        scan(file, new AutoStream(file::open));
    }

    public void scan(StructuredFile file, InputStream in) throws IOException {
        String p = file.getLastPath().toLowerCase();
        
        if (p.endsWith(".zip")) {
            if (file.isNative()) {
                final ZipFile zf = new ZipFile((File)file.file);
                zf.stream().parallel().forEach(ze -> {
                    try {
                        StructuredFile f = new StructuredFile(file.file, ze.getName());
                        f.setLastModified(ze.getTime());
                        f.setOpener(
                            () -> zf.getInputStream(ze)
                        );
                        scan(f, new AutoStream(
                            () -> zf.getInputStream(ze)
                        ), true);
                    } catch (IOException | RuntimeException ex) {
                        Logger.getLogger(StructuredFileScanner.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            } else {
                try (ZipInputStream zis = new ZipInputStream(in)) {
                    ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        String[] deepPath = new String[file.archived.length + 1];
                        System.arraycopy(file.archived, 0, deepPath, 0, file.archived.length);
                        deepPath[file.archived.length] = e.getName();
                        StructuredFile df = new StructuredFile(file.file, deepPath);
                        df.setLastModified(e.getTime());
                        df.setOpener(() -> {
                            df.setOpener(null);
                            return new InputStream() {

                                private InputStream wrapped = zis;

                                @Override
                                public int read() throws IOException {
                                    return wrapped.read();
                                }

                                @Override
                                public int read(byte[] b, int off, int len) throws IOException {
                                    return wrapped.read(b, off, len);
                                }

                                @Override
                                public int read(byte[] b) throws IOException {
                                    return wrapped.read(b);
                                }

                                @Override
                                public int available() throws IOException {
                                    return wrapped.available();
                                }

                                @Override
                                public void close() throws IOException {
                                    wrapped = null;
                                }

                            };
                        });

                        scan(df, zis);

                        df.setOpener(null);
                    }
                } catch (IOException | RuntimeException ex) {
                    LOG.log(Level.SEVERE, "Error scanning " + file, ex);
                }
            }
            return;
        }
        
        if (p.endsWith(".rar")) {
            File tempFile = null;
            if (!file.isNative()) {
                tempFile = File.createTempFile("dfs", ".rar");
            }
            
            if (tempFile != null) {
                try (FileOutputStream o = new FileOutputStream(tempFile)) {
                    IOUtils.copy(file.open(), o);
                }
            }
            
            try (Archive a = new Archive(tempFile == null ? (File)file.file : tempFile)) {
                String[] deepPath = new String[file.archived.length + 1];
                System.arraycopy(file.archived, 0, deepPath, 0, file.archived.length);

                a.getFileHeaders().stream().forEach(fh -> {
                    deepPath[file.archived.length] = fh.getFileNameString();
                    StructuredFile df = new StructuredFile(file.file, deepPath);
                    df.setLastModified(fh.getMTime().getTime());

                    df.setOpener(
                        () ->  a.getInputStream(fh)
                    );

                    try {
                        scan(df);
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, "Error scanning " + file, ex);
                    }

                    df.setOpener(null);
                });

            } catch (RarException | IOException | RuntimeException ex) {
                LOG.log(Level.SEVERE, "Error scanning " + file, ex);
            } finally {
                if (tempFile != null) {
                    tempFile.delete();
                }
            }
            
            return;
        }

        consumer.accept(file);
    }
    
    public void scan(StructuredFile file, InputStream in, boolean closeStream) throws IOException {
        try {
            scan(file, in);
        } finally {
            if (closeStream)
                in.close();
        }
    }

    public void scan(File file) throws IOException {
        if (file.isDirectory()) {
            Arrays.asList(file.listFiles()).parallelStream().forEach(t -> {
                try {
                    scan(t);
                } catch (IOException | RuntimeException ex) {
                    Logger.getLogger(StructuredFileScanner.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            return;
        }
        
        scan(new StructuredFile(file));
    }
    
}
