/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Global configuration and utility class for the Virtual File System.
 * <p>
 * Provides static configuration for HTTP settings (user agent, timeouts)
 * and customizable URL openers for handling different protocols.
 * </p>
 *
 * @author m
 */
public class Vfs {
    
    private static String USER_AGENT = "VFS/1.0 (Java; +https://github.com/azazar/vfs/)";
    private static int HTTP_TIMEOUT = 600000;
    
    private static StreamOpener<URL> HTTP_URL_OPENER = url -> {
        var reqUrl = url;
        
        for(int i = 0; i < 10; i++) {
            var conn = (HttpURLConnection) reqUrl.openConnection();

            conn.setInstanceFollowRedirects(true);

            if (Vfs.getDefaultUserAgent() != null) {
                conn.setRequestProperty("User-Agent", getDefaultUserAgent());
            }

            if (Vfs.getDefaultHttpTimeout() > 0) {
                conn.setConnectTimeout(Vfs.getDefaultHttpTimeout());
                conn.setReadTimeout(Vfs.getDefaultHttpTimeout());
            }
            
            var status = conn.getResponseCode();

            if (status == 200)
                return conn.getInputStream();
            
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                try {
                    reqUrl = reqUrl.toURI().resolve(conn.getHeaderField("Location")).toURL();
                } catch (java.net.URISyntaxException ex) {
                    throw new IOException("Invalid redirect URL: " + conn.getHeaderField("Location"), ex);
                }
                
                continue;
            }
            
            throw new IOException("Invalid response code received for " + reqUrl);
        }
        
        throw new IOException("Too many redirects");
    };

    private static StreamOpener<URL> URL_OPENER = url -> {
            var proto = url.getProtocol();

            if ("http".equals(proto) || "https".equals(proto)) {
                return getHttpUrlOpener().open(url);
            }
            else {
                return url.openStream();
            }
    };

    /**
     * Returns the default User-Agent header used for HTTP requests.
     *
     * @return the default user agent string
     */
    public static String getDefaultUserAgent() {
        return USER_AGENT;
    }

    /**
     * Sets the default User-Agent header for HTTP requests.
     *
     * @param userAgent the user agent string to use
     */
    public static void setDefaultUserAgent(String userAgent) {
        USER_AGENT = userAgent;
    }

    /**
     * Returns the default timeout for HTTP connections in milliseconds.
     *
     * @return the timeout in milliseconds
     */
    public static int getDefaultHttpTimeout() {
        return HTTP_TIMEOUT;
    }

    /**
     * Sets the default timeout for HTTP connections.
     *
     * @param timeout the timeout in milliseconds
     */
    public static void setDefaultHttpTimeout(int timeout) {
        HTTP_TIMEOUT = timeout;
    }

    /**
     * Returns the current HTTP URL opener used for http/https protocols.
     *
     * @return the HTTP URL opener
     */
    public static StreamOpener<URL> getHttpUrlOpener() {
        return HTTP_URL_OPENER;
    }

    /**
     * Sets a custom HTTP URL opener for http/https protocols.
     *
     * @param opener the stream opener to use for HTTP URLs
     */
    public static void setHttpUrlOpener(StreamOpener<URL> opener) {
        Vfs.HTTP_URL_OPENER = opener;
    }

    /**
     * Returns the current general URL opener.
     *
     * @return the URL opener
     */
    public static StreamOpener<URL> getUrlOpener() {
        return URL_OPENER;
    }

    /**
     * Sets a custom general URL opener.
     *
     * @param opener the stream opener to use for URLs
     */
    public static void setUrlOpener(StreamOpener<URL> opener) {
        Vfs.URL_OPENER = opener;
    }

}
