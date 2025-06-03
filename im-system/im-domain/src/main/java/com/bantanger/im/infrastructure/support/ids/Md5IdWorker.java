package com.bantanger.im.infrastructure.support.ids;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;

/**
 * SDK 端所生成的 clientMsgID
 * @author BanTanger 半糖
 * @Date 2023/7/26 12:56
 */
@Slf4j
public class Md5IdWorker {
    public static String getMsgID(String sendID) {
        String t = Long.toString(getCurrentTimestampByNano());
        return md5(t + sendID + new Random().nextLong());
    }

    public static long getCurrentTimestampByNano() {
        return System.nanoTime();
    }

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(s.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

//    public static void main(String[] args) {
//        String sendID = "senderID";
//        String msgID = getMsgID(sendID);
//        System.out.println("Generated msgID: " + msgID);
//    }
}