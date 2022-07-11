/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author m
 */
public class Vfs {
    
    private static String USER_AGENT = "VFS/1.0 (Java; +https://github.com/azazar/vfs/)";
    private static int HTTP_TIMEOUT = 600;
    
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
                reqUrl = new URL(reqUrl, conn.getHeaderField("Location"));
                
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

    public static String getDefaultUserAgent() {
        return USER_AGENT;
    }

    public static void setDefaultUserAgent(String userAgent) {
        USER_AGENT = userAgent;
    }

    public static int getDefaultHttpTimeout() {
        return HTTP_TIMEOUT;
    }

    public static void setDefaultHttpTimeout(int timeout) {
        HTTP_TIMEOUT = timeout;
    }

    public static StreamOpener<URL> getHttpUrlOpener() {
        return HTTP_URL_OPENER;
    }

    public static void setHttpUrlOpener(StreamOpener<URL> opener) {
        Vfs.HTTP_URL_OPENER = opener;
    }

    public static StreamOpener<URL> getUrlOpener() {
        return URL_OPENER;
    }

    public static void setUrlOpener(StreamOpener<URL> opener) {
        Vfs.URL_OPENER = opener;
    }

}
