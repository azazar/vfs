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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 *
 * @author Mikhail Yevchenko <spam@azazar.com>
 */
public class AutoStream extends InputStream {
    
    protected Callable<InputStream> opener;
    protected InputStream wrapped;

    public AutoStream(Callable<InputStream> opener) {
        this.opener = opener;
    }
    
    protected void openIfNecessary() throws IOException {
        if (wrapped != null)
            return;
        try {
            wrapped = opener.call();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public int available() throws IOException {
        openIfNecessary();
        return wrapped.available();
    }

    @Override
    public long skip(long n) throws IOException {
        openIfNecessary();
        return wrapped.skip(n);
    }

    @Override
    public synchronized void reset() throws IOException {
        openIfNecessary();
        wrapped.reset();
    }

    @Override
    public boolean markSupported() {
        return wrapped == null ? false : wrapped.markSupported();
    }

    @Override
    public synchronized void mark(int readlimit) {
        if (wrapped != null)
            wrapped.mark(readlimit);
    }

    @Override
    public int read(byte[] buf, int ofs, int len) throws IOException {
        openIfNecessary();
        return wrapped.read(buf, ofs, len);
    }

    @Override
    public int read(byte[] buf) throws IOException {
        openIfNecessary();
        return wrapped.read(buf);
    }

    @Override
    public int read() throws IOException {
        openIfNecessary();
        return wrapped.read();
    }

    @Override
    public void close() throws IOException {
        if (wrapped == null)
            return;
        wrapped.close();
    }

}
