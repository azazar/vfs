/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 *
 * @author m
 */
public class DataUrl {
    
    public static boolean isDataUrl(String url) {
        return url != null && url.startsWith("data:");
    }
    
    public static DataUrl parse(String url) {
        if (!url.startsWith("data:"))
            throw new IllegalArgumentException(url);

        String data = url.substring(5);
        String contentType = null;

        int commaIndex = data.indexOf(',');

        boolean base64 = false;

        if (commaIndex != -1) {
            int semicolonIndex = data.indexOf(';');

            if (semicolonIndex != -1) {
                String header = data.substring(semicolonIndex + 1, commaIndex);

                base64 = "base64".equals(header);
                
                if (semicolonIndex > 0)
                    contentType = data.substring(0, semicolonIndex);
            }
            else {
                if (commaIndex > 0)
                    contentType = data.substring(0, commaIndex);
            }

            data = data.substring(commaIndex + 1);
        }

        byte[] bytes;

        if (base64)
            bytes = Base64.getDecoder().decode(data);
        else
            bytes = URLDecoder.decode(data, StandardCharsets.US_ASCII).getBytes(StandardCharsets.US_ASCII);

        return new DataUrl(contentType, base64, bytes);
    }
    
    private final String contentType;
    private final boolean base64;
    private final byte[] content;

    public DataUrl(String contentType, boolean base64, byte[] content) {
        this.contentType = contentType;
        this.base64 = base64;
        this.content = content;
    }

    public DataUrl(String contentType, byte[] content) {
        this.contentType = contentType;
        this.base64 = true;
        this.content = content;
    }

    public DataUrl(byte[] content) {
        this.contentType = null;
        this.base64 = true;
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isBase64() {
        return base64;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public String toString() {
        StringBuilder url = new StringBuilder(5 + (contentType == null ? 0 : contentType.length() + 1) + content.length * 2 + (base64 ? 7 : 0));
        
        url.append("data:");
        
        if (contentType != null) {
            url.append(contentType);
        }
        
        if (base64)
            url.append(";base64");
        
        if (contentType != null || base64) {
            url.append(',');
        }
        
        if (base64)
            url.append(Base64.getEncoder().encodeToString(content));
        else
            url.append(URLEncoder.encode(new String(content, StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        
        return url.toString();
    }

}
