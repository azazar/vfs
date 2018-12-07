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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Mikhail Yevchenko <spam@azazar.com>
 */
public class StructuredFile {
    
    public final Object file;
    public final String[] archived;

    public StructuredFile(Object file, String... archived) {
        this.file = Objects.requireNonNull(file);
        this.archived = Objects.requireNonNull(archived);
    }

    public String getLastPath() {
        if (archived.length == 0) {
            if (file instanceof File)
                return ((File)file).getPath();
            
            return file.toString();
        }
        else {
            return archived[archived.length - 1];
        }
    }

    public String getLastName() {
        if (archived.length == 0) {
            if (file instanceof File)
                return ((File)file).getName();
            else
                return file.toString();
        }

        String name = archived[archived.length - 1];
        
        int i = name.lastIndexOf('/');
        if (i != -1)
            name = name.substring(i + 1);
        
        i = name.lastIndexOf('\\');
        if (i != -1)
            name = name.substring(i + 1);
        
        return name;
    }

    private Callable<InputStream> opener = new AutoOpener(this);

    public Callable<InputStream> getOpener() {
        return opener;
    }

    public void setOpener(Callable<InputStream> opener) {
        this.opener = opener;
    }
    
    public InputStream open() throws IOException {
        if (archived.length == 0) {
            if (file instanceof File)
                return new FileInputStream((File)file);
            else if (file instanceof URL)
                return ((URL)file).openStream();
            else
                throw new IllegalStateException(file.getClass().toString());
        }

        try {
            return opener.call();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }
    
    public byte[] getContent() throws IOException {
        try (InputStream is = open()) {
            return IOUtils.toByteArray(is);
        }
    }
    
    public void process(DataStreamProcessor dsp) throws IOException {
        try (InputStream is = open()) {
            byte[] buf = new byte[65536];
            int nr;
            
            while ((nr = is.read(buf)) != -1) {
                dsp.process(buf, 0, nr);
            }
        }
    }

    public String getContentAsUTF8String() throws IOException {
        return new String(getContent(), StandardCharsets.UTF_8);
    }

    public String getContentAsString(Charset charset) throws IOException {
        return new String(getContent(), charset);
    }
    
    public boolean isNative() {
        return archived.length == 0 && file instanceof File;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(file.toString());
        for (String af : archived) {
            b.append('/').append(af);
        }
        return b.toString();
    }

    private Long modified = null;
    
    public Long lastModified() {
        if (modified != null)
            return modified;

        return isNative() ? ((File)file).lastModified() : null;
    }
    
    public void setLastModified(Long modified) {
        this.modified = modified;
    }

}
