/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import static com.sun.net.httpserver.HttpServer.create;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import static java.lang.Integer.MAX_VALUE;
import java.net.InetSocketAddress;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
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
        server = create(new InetSocketAddress(port), 10);
        server.createContext(GZIP_CSV_URI, this::gzipCsv);
        server.createContext(ZIP_CSV_URI, this::zipCsv);
        server.createContext(TGZ_CSV_URI, this::tarGzipCsv);
        server.createContext("/", hx -> {
            hx.getResponseHeaders().add("Content-Type", "text/plain");
            hx.sendResponseHeaders(404, 0);
            try ( var os = hx.getResponseBody()) {
                os.write("Not Found".getBytes(ISO_8859_1));
            }
        });
        executorService = newFixedThreadPool(4);
        server.setExecutor(executorService);
        server.start();
    }

    @Override
    public void close() {
        server.stop(MAX_VALUE);
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(1, MINUTES);
        } catch (InterruptedException ex) {
            getLogger(TestFeedHttpServer.class.getName()).log(SEVERE, null, ex);
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
            getLogger(TestFeedHttpServer.class.getName()).log(SEVERE, null, ex);
        }
    }

    private void zipCsv(HttpExchange hx) throws IOException {
        hx.getResponseHeaders().add("Content-Type", "application/zip");
        hx.sendResponseHeaders(200, 0);

        try ( var zos = new ZipOutputStream(hx.getResponseBody())) {
            zos.putNextEntry(new ZipEntry("test.csv"));
            Writer sw = new OutputStreamWriter(zos);
            buildFile(sw);
            sw.flush();
            zos.closeEntry();
        } catch (IOException ex) {
            getLogger(TestFeedHttpServer.class.getName()).log(SEVERE, null, ex);
        }
    }

    private void tarGzipCsv(HttpExchange hx) throws IOException {
        hx.getResponseHeaders().add("Content-Type", "application/gzip");
        hx.sendResponseHeaders(200, 0);

        try ( var tos = new TarArchiveOutputStream(new GZIPOutputStream(hx.getResponseBody()))) {
            var csv = TestFeedHttpServer.this.buildFile();
            var entry = new TarArchiveEntry("test.csv");
            entry.setSize(csv.length);

            tos.putArchiveEntry(entry);
            tos.write(csv);
            tos.closeArchiveEntry();
        } catch (IOException ex) {
            getLogger(TestFeedHttpServer.class.getName()).log(SEVERE, null, ex);
        }
    }

    public byte[] buildFile() throws IOException {
        var o = new ByteArrayOutputStream();

        try ( var w = new OutputStreamWriter(o)) {
            buildFile(w);
        }

        return o.toByteArray();
    }

    public void buildFile(Writer sw) throws IOException {
        sw.write("123");
    }

}
