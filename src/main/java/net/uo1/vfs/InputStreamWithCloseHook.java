/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Mikhail Yevchenko <123@ǟẓåẓạŗ.ćọ₥>
 */
class InputStreamWithCloseHook extends InputStream {
    
    private final InputStream wrapped;
    private final Runnable hook;

    public InputStreamWithCloseHook(InputStream wrapped, Runnable hook) {
        if (wrapped == null) {
            throw new NullPointerException();
        }
        this.wrapped = wrapped;
        this.hook = hook;
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException {
        wrapped.reset();
    }

    @Override
    public synchronized void mark(int readlimit) {
        wrapped.mark(readlimit);
    }

    @Override
    public void close() throws IOException {
        try {
            wrapped.close();
        }
        finally {
            try {
                hook.run();
            }
            catch (Exception ex) {
                if (ex instanceof IOException) {
                    throw (IOException) ex;
                }
                throw new IOException(ex);
            }
        }
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
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
    public int read() throws IOException {
        return wrapped.read();
    }
    
}
