package com.appsandlabs.telugubeats.helpers;

/**
 * Created by abhinav on 1/23/16.
 */
public class DebugUtils {
    public static String intToString(int number, int groupSize) {
        StringBuilder result = new StringBuilder();

        for(int i = 31; i >= 0 ; i--) {
            int mask = 1 << i;
            result.append((number & mask) != 0 ? "1" : "0");

            if (i % groupSize == 0)
                result.append(" ");
        }
        result.replace(result.length() - 1, result.length(), "");

        return result.toString();
    }
}
