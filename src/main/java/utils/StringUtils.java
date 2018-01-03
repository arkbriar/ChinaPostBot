package utils;

import java.io.UnsupportedEncodingException;

/**
 * Created by Shunjie Ding on 03/01/2018.
 */
public final class StringUtils {
    public static String convertToUTF8(String str) {
        try {
            return new String(str.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        return "";
    }
}
