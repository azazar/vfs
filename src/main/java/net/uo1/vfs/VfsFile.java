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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import static java.util.Arrays.copyOfRange;
import static java.util.Objects.requireNonNull;
import java.util.concurrent.Callable;
import static net.uo1.vfs.DataUrl.isDataUrl;
import static net.uo1.vfs.DataUrl.parse;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Represents a file that may be located within archives or accessed via URLs.
 * <p>
 * VfsFile provides a unified interface for accessing files from various sources:
 * <ul>
 *   <li>Local filesystem files</li>
 *   <li>Files within archives (ZIP, TAR, RAR, etc.)</li>
 *   <li>URLs (HTTP, HTTPS, etc.)</li>
 *   <li>Data URLs (embedded content)</li>
 * </ul>
 * </p>
 * <p>
 * Nested archive paths are supported using Apache Commons VFS2 style paths,
 * e.g., {@code /path/to/archive.zip!/inner/file.txt}
 * </p>
 *
 * @author Mikhail Yevchenko &lt;spam@azazar.com&gt;
 */
public class VfsFile {

    /**
     * Resolves an Apache Commons VFS2 style path to a VfsFile.
     * <p>
     * Supports paths like:
     * <ul>
     *   <li>{@code /local/path/file.txt}</li>
     *   <li>{@code http://example.com/file.zip!/inner.txt}</li>
     *   <li>{@code file:///path/archive.tar.gz!/dir/file.txt}</li>
     *   <li>{@code data:application/gzip;base64,...}</li>
     * </ul>
     * </p>
     *
     * @param path the VFS2 style path to resolve
     * @return a VfsFile representing the specified path
     * @throws IllegalArgumentException if the path cannot be parsed
     */
    public static VfsFile resolvePath(CharSequence path) throws IllegalArgumentException {
        var parsedPath = parsePath(path);

        Object first;

        if (isDataUrl(parsedPath[0])) {
            first = parse(parsedPath[0]);
        } else if (parsedPath[0].contains(":")) {
            try {
                first = java.net.URI.create(parsedPath[0]).toURL();
            } catch (MalformedURLException | IllegalArgumentException ex) {
                throw new IllegalArgumentException(ex);
            }
        } else {
            first = new File(parsedPath[0]);
        }

        return new VfsFile(first, copyOfRange(parsedPath, 1, parsedPath.length));
    }

    /**
     * Parses a VFS2 style path into its component parts.
     * <p>
     * The path is split on '!' characters to separate archive boundaries.
     * </p>
     *
     * @param path the path to parse
     * @return an array of path components
     * @throws IllegalArgumentException if the path is malformed
     */
    public static String[] parsePath(CharSequence path) throws IllegalArgumentException {
        var schemes = new ArrayList<CharSequence>();

        schemeSearchLoop:
        while (true) {
            for (var i = 0; i < path.length(); i++) {
                var c = path.charAt(i);

                if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                    continue;
                }

                if (i > 0) {
                    if (c == ':') {
                        schemes.add(path.subSequence(0, i));
                        path = path.subSequence(i + 1, path.length());
                        continue schemeSearchLoop;
                    }
                }

                break schemeSearchLoop;
            }

            break;
        }

        var paths = split(path.toString(), '!');

        if (schemes.size() >= 1) {
            var lastScheme = schemes.get(schemes.size() - 1);

            if ("file".equals(lastScheme) && paths[0].startsWith("//")) {
                paths[0] = paths[0].substring(2);
            } else {
                paths[0] = lastScheme + ":" + paths[0];
            }

        }

        return paths;
    }

    public final Object file;
    public final String[] archived;
    private Callable<InputStream> opener = new AutoOpener(this);
    private Long modified = null;

    /**
     * Creates a new VfsFile with the specified base file and archived path components.
     *
     * @param file     the base file object (File, URL, DataUrl, or byte[])
     * @param archived the path components within archives (may be empty)
     */
    public VfsFile(Object file, String... archived) {
        this.file = requireNonNull(file);
        this.archived = requireNonNull(archived);
    }

    /**
     * Returns the last (innermost) path component.
     * <p>
     * For archived files, returns the path within the archive.
     * For regular files, returns the file path.
     * </p>
     *
     * @return the last path component
     */
    public String getLastPath() {
        if (archived.length == 0) {
            if (file instanceof File) {
                return ((File) file).getPath();
            }

            return file.toString();
        } else {
            return archived[archived.length - 1];
        }
    }

    /**
     * Returns the filename (without directory path) of the innermost file.
     *
     * @return the filename
     */
    public String getLastName() {
        if (archived.length == 0) {
            if (file instanceof File) {
                return ((File) file).getName();
            } else {
                return file.toString();
            }
        }

        var name = archived[archived.length - 1];
        var i = name.lastIndexOf('/');
        if (i != -1) {
            name = name.substring(i + 1);
        }

        i = name.lastIndexOf('\\');
        if (i != -1) {
            name = name.substring(i + 1);
        }

        return name;
    }

    /**
     * Returns the opener used to create input streams for this file.
     *
     * @return the current opener
     */
    public Callable<InputStream> getOpener() {
        return opener;
    }

    /**
     * Sets a custom opener for this file.
     *
     * @param opener the opener to use, or {@code null} to disable
     */
    public void setOpener(Callable<InputStream> opener) {
        this.opener = opener;
    }

    /**
     * Opens an input stream for reading this file's content.
     * <p>
     * For archived files, this will navigate through the archive hierarchy
     * to locate and decompress the target file.
     * </p>
     *
     * @return an input stream for reading the file content
     * @throws IOException if an I/O error occurs
     */
    public InputStream open() throws IOException {
        if (archived.length == 0) {
            if (file instanceof DataUrl) {
                return new ByteArrayInputStream(((DataUrl) file).getContent());
            } else if (file instanceof File) {
                return new FileInputStream((File) file);
            } else if (file instanceof URL) {
                return Vfs.getUrlOpener().open((URL) file);
            } else {
                throw new IllegalStateException(file.getClass().toString());
            }
        }

        try {
            return opener.call();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Reads and returns the entire file content as a byte array.
     *
     * @return the file content as bytes
     * @throws IOException if an I/O error occurs
     */
    public byte[] getContent() throws IOException {
        if (archived.length == 0) {
            if (file instanceof byte[]) {
                return (byte[]) file;
            } else if (file instanceof DataUrl) {
                return ((DataUrl) file).getContent();
            }
        }

        try ( var is = open()) {
            return toByteArray(is);
        }
    }

    /**
     * Processes the file content in chunks using the specified processor.
     * <p>
     * This is more memory-efficient than {@link #getContent()} for large files.
     * </p>
     *
     * @param dsp the data stream processor to handle the content
     * @throws IOException if an I/O error occurs
     */
    public void process(DataStreamProcessor dsp) throws IOException {
        try ( var is = open()) {
            var buf = new byte[65536];
            int nr;

            while ((nr = is.read(buf)) != -1) {
                dsp.process(buf, 0, nr);
            }
        }
    }

    /**
     * Reads the file content as a UTF-8 encoded string.
     *
     * @return the file content as a string
     * @throws IOException if an I/O error occurs
     */
    public String getContentAsUTF8String() throws IOException {
        return new String(getContent(), UTF_8);
    }

    /**
     * Reads the file content as a string with the specified character encoding.
     *
     * @param charset the character encoding to use
     * @return the file content as a string
     * @throws IOException if an I/O error occurs
     */
    public String getContentAsString(Charset charset) throws IOException {
        return new String(getContent(), charset);
    }

    /**
     * Checks if this file is a native filesystem file (not archived or remote).
     *
     * @return {@code true} if this is a local filesystem file, {@code false} otherwise
     */
    public boolean isNative() {
        return archived.length == 0 && file instanceof File;
    }

    @Override
    public String toString() {
        var b = new StringBuilder();
        b.append(file.toString());
        for (var af : archived) {
            b.append('/').append(af);
        }
        return b.toString();
    }

    /**
     * Returns the last modification time of this file.
     *
     * @return the modification time in milliseconds since epoch, or {@code null} if unknown
     */
    public Long lastModified() {
        if (modified != null) {
            return modified;
        }

        return isNative() ? ((File) file).lastModified() : null;
    }

    /**
     * Sets the last modification time for this file.
     *
     * @param modified the modification time in milliseconds since epoch
     */
    public void setLastModified(Long modified) {
        this.modified = modified;
    }

}
