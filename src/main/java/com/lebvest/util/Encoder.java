package com.lebvest.util;

public class Encoder {

    public static String encodeUrl(String key) {
        return key
                .replace(" ", "%20")
                .replace("(", "%28")
                .replace(")", "%29")
                .replace("'", "%27")
                .replace("!", "%21");
    }
}
