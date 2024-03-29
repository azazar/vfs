/*
 * PROPRIETARY/CONFIDENTIAL
 */
package net.uo1.vfs;

import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

/**
 *
 * @author m
 */
public class DataUrl {

    public static boolean isDataUrl(String url) {
        return url != null && url.startsWith("data:");
    }

    public static DataUrl parse(String url) {
        if (!url.startsWith("data:")) {
            throw new IllegalArgumentException(url);
        }

        var data = url.substring(5);
        String contentType = null;

        var commaIndex = data.indexOf(',');
        var base64 = false;

        if (commaIndex != -1) {
            var semicolonIndex = data.indexOf(';');

            if (semicolonIndex != -1) {
                var header = data.substring(semicolonIndex + 1, commaIndex);

                base64 = "base64".equals(header);

                if (semicolonIndex > 0) {
                    contentType = data.substring(0, semicolonIndex);
                }
            } else {
                if (commaIndex > 0) {
                    contentType = data.substring(0, commaIndex);
                }
            }

            data = data.substring(commaIndex + 1);
        }

        byte[] bytes;

        if (base64) {
            bytes = getDecoder().decode(data);
        } else {
            bytes = decode(data, US_ASCII).getBytes(US_ASCII);
        }

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
        var url = new StringBuilder(5 + (contentType == null ? 0 : contentType.length() + 1) + content.length * 2 + (base64 ? 7 : 0));

        url.append("data:");

        if (contentType != null) {
            url.append(contentType);
        }

        if (base64) {
            url.append(";base64");
        }

        if (contentType != null || base64) {
            url.append(',');
        }

        if (base64) {
            url.append(getEncoder().encodeToString(content));
        } else {
            url.append(encode(new String(content, UTF_8), UTF_8));
        }

        return url.toString();
    }

}
