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

/**
 * Exception thrown to interrupt a {@link VfsScanner} scanning operation.
 * <p>
 * This is a runtime exception that can be thrown from within a consumer callback
 * to signal that scanning should stop. The scanner will propagate this exception
 * after attempting to terminate pending operations.
 * </p>
 *
 * @author Mikhail Yevchenko &lt;spam@azazar.com&gt;
 */
public class VfsInterruptException extends RuntimeException {

    /**
     * Creates a new instance of <code>VfsInterruptException</code> without
     * detail message.
     */
    public VfsInterruptException() {
        super("Vfs scanning was interrupted");
    }

    /**
     * Constructs an instance of <code>VfsInterruptException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public VfsInterruptException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public VfsInterruptException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an instance with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public VfsInterruptException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an instance with full control over exception behavior.
     *
     * @param message            the detail message
     * @param cause              the cause of this exception
     * @param enableSuppression  whether suppression is enabled
     * @param writableStackTrace whether the stack trace should be writable
     */
    public VfsInterruptException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
