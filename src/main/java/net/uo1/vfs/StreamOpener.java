/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A functional interface for opening input streams from various data sources.
 * <p>
 * This interface abstracts the mechanism for obtaining an {@link InputStream}
 * from different types of references (URLs, files, etc.).
 * </p>
 *
 * @param <T> the type of data reference used to open the stream
 * @author m
 */
public interface StreamOpener<T> {

    /**
     * Opens an input stream for the specified data reference.
     *
     * @param dataRef the reference to the data source
     * @return an input stream for reading the data
     * @throws IOException           if an I/O error occurs
     * @throws FileNotFoundException if the referenced resource doesn't exist
     */
    InputStream open(T dataRef) throws IOException, FileNotFoundException;
    
}
