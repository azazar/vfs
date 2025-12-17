/*
 * The MIT License
 *
 * Copyright 2016 Mikhail Yevchenko <m.ṥῥẚɱ.ѓѐḿởύḙ@azazar.com>.
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
 * A functional interface for processing byte data in chunks.
 * <p>
 * This interface is useful for streaming data processing where the entire content
 * doesn't need to be loaded into memory at once.
 * </p>
 *
 * @author Mikhail Yevchenko &lt;m.ṥῥẚɱ.ѓѐḿởύḙ@azazar.com&gt;
 */
public interface DataStreamProcessor {

    /**
     * Processes a chunk of byte data.
     *
     * @param data the byte array containing the data
     * @param ofs  the offset in the array where the data starts
     * @param len  the number of bytes to process
     */
    void process(byte[] data, int ofs, int len);

    /**
     * Processes an entire byte array.
     *
     * @param data the byte array to process
     */
    default void process(byte[] data) {
        process(data, 0, data.length);
    }

}
