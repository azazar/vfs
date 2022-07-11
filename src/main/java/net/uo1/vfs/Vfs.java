/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

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
        var conn = (HttpURLConnection) url.openConnection();

        if (Vfs.getDefaultUserAgent() != null) {
            conn.setRequestProperty("User-Agent", getDefaultUserAgent());
        }

        if (Vfs.getDefaultHttpTimeout() > 0) {
            conn.setConnectTimeout(Vfs.getDefaultHttpTimeout());
            conn.setReadTimeout(Vfs.getDefaultHttpTimeout());
        }

        return conn.getInputStream();
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
