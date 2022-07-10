/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 *
 * @author m
 */
class TestFeedHttpServer implements AutoCloseable {

    public static final String GZIP_CSV_URI = "/test.csv.gz";
    public static final String ZIP_CSV_URI = "/test.zip";
    public static final String TGZ_CSV_URI = "/test.tgz";

    private final HttpServer server;
    private final int port;
    private final ExecutorService executorService;

    public TestFeedHttpServer(int port) throws IOException {
        this.port = port;
        server = HttpServer.create(new InetSocketAddress(port), 10);
        server.createContext(GZIP_CSV_URI, this::gzipCsv);
        server.createContext(ZIP_CSV_URI, this::zipCsv);
        server.createContext(TGZ_CSV_URI, this::tarGzipCsv);
        server.createContext("/", hx -> {
            hx.getResponseHeaders().add("Content-Type", "text/plain");
            hx.sendResponseHeaders(404, 0);
            try ( OutputStream os = hx.getResponseBody()) {
                os.write("Not Found".getBytes(StandardCharsets.ISO_8859_1));
            }
        });
        executorService = Executors.newFixedThreadPool(4);
        server.setExecutor(executorService);
        server.start();
    }

    @Override
    public void close() {
        server.stop(Integer.MAX_VALUE);
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestFeedHttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getUrl() {
        return "http://localhost:" + port;
    }

    public String getGzipUrl() {
        return "gz:" + getUrl() + GZIP_CSV_URI + "!test.txt";
    }

    public String getZipUrl() {
        return "zip:" + getUrl() + GZIP_CSV_URI + "!test.txt";
    }

    public String getTarGzipUrl() {
        return "tgz:" + getUrl() + GZIP_CSV_URI + "!test.txt";
    }

    private void gzipCsv(HttpExchange hx) throws IOException {
        hx.getResponseHeaders().add("Content-Type", "application/gzip");
        hx.sendResponseHeaders(200, 0);

        try ( Writer sw = new OutputStreamWriter(new GZIPOutputStream(hx.getResponseBody()))) {
            buildFile(sw);
        } catch (IOException ex) {
            Logger.getLogger(TestFeedHttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void zipCsv(HttpExchange hx) throws IOException {
        hx.getResponseHeaders().add("Content-Type", "application/zip");
        hx.sendResponseHeaders(200, 0);

        try ( ZipOutputStream zos = new ZipOutputStream(hx.getResponseBody())) {
            zos.putNextEntry(new ZipEntry("test.csv"));
            Writer sw = new OutputStreamWriter(zos);
            buildFile(sw);
            sw.flush();
            zos.closeEntry();
        } catch (IOException ex) {
            Logger.getLogger(TestFeedHttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void tarGzipCsv(HttpExchange hx) throws IOException {
        hx.getResponseHeaders().add("Content-Type", "application/gzip");
        hx.sendResponseHeaders(200, 0);

        try ( TarArchiveOutputStream tos = new TarArchiveOutputStream(new GZIPOutputStream(hx.getResponseBody()))) {
            byte[] csv = TestFeedHttpServer.this.buildFile();

            TarArchiveEntry entry = new TarArchiveEntry("test.csv");
            entry.setSize(csv.length);

            tos.putArchiveEntry(entry);
            tos.write(csv);
            tos.closeArchiveEntry();
        } catch (IOException ex) {
            Logger.getLogger(TestFeedHttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public byte[] buildFile() throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();

        try ( OutputStreamWriter w = new OutputStreamWriter(o)) {
            buildFile(w);
        }

        return o.toByteArray();
    }

    public void buildFile(Writer sw) throws IOException {
        sw.write("123");
    }

}
