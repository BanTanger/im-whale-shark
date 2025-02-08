package com.bantanger.im.codec.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/2 18:48
 */
public class Base64URL {
    public static byte[] base64EncodeUrl(byte[] input) {
        byte[] base64 = new Base64().encode(input);
        for (int i = 0; i < base64.length; ++i)
            switch (base64[i]) {
                case '+':
                    base64[i] = '*';
                    break;
                case '/':
                    base64[i] = '-';
                    break;
                case '=':
                    base64[i] = '_';
                    break;
                default:
                    break;
            }
        return base64;
    }

    public static byte[] base64DecodeUrlNotReplace(byte[] input) throws IOException {
        for (int i = 0; i < input.length; ++i)
            switch (input[i]) {
                case '*':
                    input[i] = '+';
                    break;
                case '-':
                    input[i] = '/';
                    break;
                case '_':
                    input[i] = '=';
                    break;
                default:
                    break;
            }
        return new Base64().decode(new String(input, StandardCharsets.UTF_8));
    }

    public static byte[] base64DecodeUrl(byte[] input) throws IOException {
        byte[] base64 = input.clone();
        for (int i = 0; i < base64.length; ++i)
            switch (base64[i]) {
                case '*':
                    base64[i] = '+';
                    break;
                case '-':
                    base64[i] = '/';
                    break;
                case '_':
                    base64[i] = '=';
                    break;
                default:
                    break;
            }
        return new Base64().decode(new String(input));
    }
}
