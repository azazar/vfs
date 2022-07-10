/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
class AutoOpener implements Callable<InputStream> {

    private static final Logger LOG = Logger.getLogger(AutoOpener.class.getName());

    private final VfsFile file;

    AutoOpener(VfsFile file) {
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

        int i = filename.lastIndexOf('/');

        if (i != -1) {
            filename = filename.substring(i + 1);
        }

        if (filename.equals(internal[0] + ".gz")) {
            return openWrappedStream(new GZIPInputStream(in), internal[0], ArrayUtil.shift(internal));
        }

        if (filename.equals(internal[0] + ".bz2")) {
            return openWrappedStream(new BZip2CompressorInputStream(in), internal[0], ArrayUtil.shift(internal));
        }

        if (filename.equals(internal[0] + ".zst")) {
            return openWrappedStream(new ZstdCompressorInputStream(in), internal[0], ArrayUtil.shift(internal));
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
                            } catch (IOException ex) {
                                LOG.log(Level.SEVERE, null, ex);
                            }
                        });
                    }
                }
            } finally {
                if (zis != null) {
                    zis.close();
                }
            }
        }

        if (filename.endsWith(".tar") || filename.endsWith(".tgz")) {
            TarArchiveInputStream zis = new TarArchiveInputStream(filename.endsWith(".tgz") ? new GZIPInputStream(in) : in);

            try {
                TarArchiveEntry ze;

                while ((ze = zis.getNextTarEntry()) != null) {
                    if (ze.getName().equals(internal[0])) {
                        InputStream w = zis;
                        zis = null;
                        return new InputStreamWithCloseHook(openWrappedStream(w, internal[0], ArrayUtil.shift(internal)), () -> {
                            try {
                                w.close();
                            } catch (IOException ex) {
                                LOG.log(Level.SEVERE, null, ex);
                            }
                        });
                    }
                }
            } finally {
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

        if (file.file instanceof byte[]) {
            in = new ByteArrayInputStream((byte[]) file.file);
            filename = "file";
        } else if (file.file instanceof DataUrl) {
            in = new ByteArrayInputStream(((DataUrl) file.file).getContent());
            switch (((DataUrl) file.file).getContentType()) {
                case "application/gzip": filename = "file.gz"; break;
                case "application/bzip2": filename = "file.bz2"; break;
                case "application/zip": filename = "file.zip"; break;
                default: filename = "file"; break;
            }
        } else if (file.file instanceof File) {
            in = new FileInputStream((File) file.file);
            filename = ((File) file.file).getName();
        } else if (file.file instanceof URL) {
            in = ((URL) file.file).openStream();
            filename = ((URL) file.file).getFile();
        } else {
            throw new IllegalStateException(file.file.toString());
        }

        return openWrappedStream(in, filename, file.archived);
    }

}
