package utils;

import java.io.UnsupportedEncodingException;

/**
 * Created by Shunjie Ding on 03/01/2018.
 */
public final class StringUtils {

    private static final String OS_NAME = System.getProperty("os.name");

    public static String convertToGBKIfOSisWindows(String str) {
        // Return str if os is not windows
        if (!OS_NAME.equals("Windows")) {
            return str;
        }

        if (str == null) {
            return null;
        }
        try {
            return new String(str.getBytes(), "GBK");
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        return "";
    }
}
