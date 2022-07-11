/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author m
 * @param <T>
 */
public interface StreamOpener<T> {
    
    InputStream open(T dataRef) throws IOException, FileNotFoundException;
    
}
