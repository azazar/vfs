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
package net.uo1.vfs;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import java.io.File;
import static java.io.File.createTempFile;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.System.arraycopy;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import static org.apache.commons.io.IOUtils.copy;

/**
 *
 * @author Mikhail Yevchenko <spam@azazar.com>
 */
public class VfsScanner implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(VfsScanner.class.getName());
    
    protected volatile VfsInterruptException interruptException = null;

    protected Consumer<VfsFile> consumer;
    protected ExecutorService executor;

    public VfsScanner(Consumer<VfsFile> consumer, ExecutorService executor) {
        this.consumer = consumer;
        this.executor = executor;
    }

    public VfsScanner(Consumer<VfsFile> consumer) {
        this.consumer = consumer;
        this.executor = Executors.newWorkStealingPool();
    }

    public void scan(VfsFile file) throws IOException {
        scan(file, new AutoStream(file::open));
    }

    public void scan(VfsFile file, InputStream in) throws IOException {
        if (interruptException != null) {
            throw interruptException;
        }

        var p = file.getLastPath().toLowerCase();

        if (p.endsWith(".gz") || p.endsWith(".zst")) {
            LOG.warning("Scanning through GZip or ZStd files not yet implemented");
        }

        if (p.endsWith(".zip")) {
            if (file.isNative()) {
                final var zf = new ZipFile((File) file.file);
                for (Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zf.entries(); interruptException == null && e.hasMoreElements();) {
                    ZipEntry ze = e.nextElement();

                    executor.submit(() -> {
                        try {
                            var f = new VfsFile(file.file, ze.getName());
                            f.setLastModified(ze.getTime());
                            f.setOpener(
                                    () -> zf.getInputStream(ze)
                            );
                            scan(f, new AutoStream(
                                    () -> zf.getInputStream(ze)
                            ), true);
                        } catch (VfsInterruptException ex) {
                            interruptException = ex;

                            throw ex;
                        } catch (IOException | RuntimeException ex) {
                            LOG.log(Level.SEVERE, null, ex);
                        }
                    });
                }
                
                if (interruptException != null)
                    throw interruptException;
            } else {
                try ( var zis = new ZipInputStream(in)) {
                    ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        var deepPath = new String[file.archived.length + 1];
                        arraycopy(file.archived, 0, deepPath, 0, file.archived.length);
                        deepPath[file.archived.length] = e.getName();
                        var df = new VfsFile(file.file, deepPath);
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
                } catch (VfsInterruptException ex) {
                    interruptException = ex;
                    throw ex;
                } catch (IOException | RuntimeException ex) {
                    LOG.log(Level.SEVERE, "Error scanning " + file, ex);
                }
            }
            return;
        }

        if (p.endsWith(".rar")) {
            File tempFile = null;
            if (!file.isNative()) {
                tempFile = createTempFile("dfs", ".rar");
            }

            if (tempFile != null) {
                try ( var o = new FileOutputStream(tempFile)) {
                    copy(file.open(), o);
                }
            }

            try ( var a = new Archive(tempFile == null ? (File) file.file : tempFile)) {
                var deepPath = new String[file.archived.length + 1];
                arraycopy(file.archived, 0, deepPath, 0, file.archived.length);
                
                for(FileHeader fh : a.getFileHeaders()) {
                    if (interruptException != null) {
                        throw interruptException;
                    }

                    deepPath[file.archived.length] = fh.getFileNameString();
                    var df = new VfsFile(file.file, deepPath);
                    df.setLastModified(fh.getMTime().getTime());

                    df.setOpener(
                            () -> a.getInputStream(fh)
                    );

                    Runnable scanTask = () -> {
                        try {
                            scan(df);
                        } catch (VfsInterruptException ex) {
                            interruptException = ex;
                            throw ex;
                        } catch (IOException | RuntimeException ex) {
                            LOG.log(Level.SEVERE, "Error scanning " + file, ex);
                        } finally {
                            df.setOpener(null);
                        }
                    };
                    
                    if (tempFile == null) {
                        executor.submit(scanTask);
                    }
                    else {
                        scanTask.run();
                    }
                }

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

    public void scan(VfsFile file, InputStream in, boolean closeStream) throws IOException {
        try {
            scan(file, in);
        } finally {
            if (closeStream) {
                in.close();
            }
        }
    }

    public void scan(File file) throws IOException {
        if (interruptException != null) {
            throw interruptException;
        }

        if (file.isDirectory()) {
            for(File t : file.listFiles()) {
                if (interruptException != null) {
                    throw interruptException;
                }

                executor.submit(() -> {
                    try {
                        scan(t);
                    } catch (VfsInterruptException ex) {
                        interruptException = ex;
                        throw ex;
                    } catch (IOException | RuntimeException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                });
            }
            return;
        }

        scan(new VfsFile(file));
    }
    
    public void stop() {
        throw interruptException = new VfsInterruptException();
    }
    
    @Override
    public void close() {
        executor.close();
    }

}
