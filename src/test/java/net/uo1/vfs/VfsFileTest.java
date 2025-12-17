/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static net.uo1.vfs.VfsFile.parsePath;
import static net.uo1.vfs.VfsFile.resolvePath;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

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

        testFiles = new VfsFile[]{
            resolvePath(server.getGzipUrl()),
            resolvePath(server.getTarGzipUrl()),
            resolvePath(server.getZipUrl()),};
    }

    @AfterClass
    public static void tearDownClass() {
        server.close();
    }

    @Test
    public void testReadDataUrl() throws IOException {
        var vFile = resolvePath(dataUrl("test"));

        assertArrayEquals("test".getBytes(US_ASCII), vFile.getContent());
    }

    @Test
    public void testReadGzipDataUrl() throws IOException {
        var vFile = resolvePath("gz:" + new DataUrl("application/gzip", true, gzip("test")).toString() + "!file");

        assertArrayEquals("test".getBytes(US_ASCII), vFile.getContent());
    }

    @Test
    public void testReadZipDataUrl() throws IOException {
        var vFile = resolvePath("zip:" + new DataUrl("application/zip", true, zip("test", "test.txt")).toString() + "!test.txt");

        assertArrayEquals("test".getBytes(US_ASCII), vFile.getContent());
    }

    @Test
    public void testParsePath() {
        String[] parsedPath;

        parsedPath = parsePath("data:test");

        assertEquals(1, parsedPath.length);
        assertEquals("data:test", parsedPath[0]);

        parsedPath = parsePath("gz:http://example.org/test.csv.gz!test.csv");

        assertEquals(2, parsedPath.length);
        assertEquals("http://example.org/test.csv.gz", parsedPath[0]);
        assertEquals("test.csv", parsedPath[1]);
    }

    private byte[] gzip(String data) {
        return gzip(data.getBytes(UTF_8));
    }

    private byte[] gzip(byte[] bytes) {
        var bos = new ByteArrayOutputStream();
        try ( var gos = new GZIPOutputStream(bos)) {
            gos.write(bytes);
        } catch (IOException ex) {
            getLogger(VfsFileTest.class.getName()).log(SEVERE, null, ex);
        }

        return bos.toByteArray();
    }
    
    private byte[] zip(String data, String filename) {
        return zip(data.getBytes(US_ASCII), filename);
    }

    private byte[] zip(byte[] data, String filename) {
        var bos = new ByteArrayOutputStream();
        try ( var gos = new ZipOutputStream(bos)) {
            var entry = new ZipEntry(filename);
            entry.setSize(data.length);
            gos.putNextEntry(entry);
            gos.write(data);
            gos.closeEntry();
        } catch (IOException ex) {
            getLogger(VfsFileTest.class.getName()).log(SEVERE, null, ex);
        }

        return bos.toByteArray();
    }

    private String dataUrl(String data) {
        return dataUrl(data.getBytes(UTF_8));
    }

    private String dataUrl(byte[] bytes) {
        return "data:application/octet-stream;base64," + getEncoder().encodeToString(bytes);
    }

}
