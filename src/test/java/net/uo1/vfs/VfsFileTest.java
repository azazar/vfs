/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author m
 */
public class VfsFileTest {
    
    static TestFeedHttpServer server;
    static VfsFile[] testFiles;
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        server = new TestFeedHttpServer(8888);

        testFiles = new VfsFile[] {
            VfsFile.resolvePath(server.getGzipUrl()),
            VfsFile.resolvePath(server.getTarGzipUrl()),
            VfsFile.resolvePath(server.getZipUrl()),
        };
    }
    
    @AfterClass
    public static void tearDownClass() {
        server.close();
    }
    
    @Test
    public void testReadDataUrl() throws IOException {
        VfsFile vFile = VfsFile.resolvePath(dataUrl("test"));
        
        assertArrayEquals("test".getBytes(StandardCharsets.US_ASCII), vFile.getContent());
    }

    @Test
    public void testReadGzipDataUrl() throws IOException {
        VfsFile vFile = VfsFile.resolvePath("gz:" + new DataUrl("application/gzip", true, gzip("test")).toString() + "!file");
        
        assertArrayEquals("test".getBytes(StandardCharsets.US_ASCII), vFile.getContent());
    }

    @Test
    public void testParsePath() {
        String[] parsedPath;

        parsedPath = VfsFile.parsePath("data:test");
        
        assertEquals(1, parsedPath.length);
        assertEquals("data:test", parsedPath[0]);

        parsedPath = VfsFile.parsePath("gz:http://example.org/test.csv.gz!test.csv");
        
        assertEquals(2, parsedPath.length);
        assertEquals("http://example.org/test.csv.gz", parsedPath[0]);
        assertEquals("test.csv", parsedPath[1]);
    }
    
    private byte[] gzip(String data) {
        return gzip(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] gzip(byte[] bytes) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (GZIPOutputStream gos = new GZIPOutputStream(bos)) {
            IOUtils.copy(bis, gos);
        } catch (IOException ex) {
            Logger.getLogger(VfsFileTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return bos.toByteArray();
    }
    
    private String dataUrl(String data) {
        return dataUrl(data.getBytes(StandardCharsets.UTF_8));
    }

    private String dataUrl(byte[] bytes) {
        return "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);
    }

}
