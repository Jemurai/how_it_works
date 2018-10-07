package com.jemurai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

class Totp {
    private static final Logger log = LoggerFactory.getLogger(Totp.class);
    private static final int SEED_LENGTH_IN_BYTES = 64;
    private static final int POWER = 1000000;
    private static final int PERIOD = 30;
    private static final int DIGITS = 6;

    static String generateSeed() {
        SecureRandom random = new SecureRandom();
        byte[] randomBytes = new byte[SEED_LENGTH_IN_BYTES];
        random.nextBytes(randomBytes);

        return printHexBinary(randomBytes);
    }

    static String generateInstance(final String seed) {
        return generateInstance(seed, counterToBytes());
    }

    static String generateInstance(final String seed, final byte[] counter) {
        byte[] key = hexToBytes(seed);
        byte[] result = hash(key, counter);

        if (result == null) {
            throw new RuntimeException("Could not produce OTP value");
        }

        int offset = result[result.length - 1] & 0xf;
        int binary = ((result[offset]     & 0x7f) << 24) |
                     ((result[offset + 1] & 0xff) << 16) |
                     ((result[offset + 2] & 0xff) << 8)  |
                     ((result[offset + 3] & 0xff));

        StringBuilder code = new StringBuilder(Integer.toString(binary % POWER));
        while (code.length() < DIGITS) code.insert(0, "0");

        return code.toString();
    }

    static byte[] hexToBytes(String hex) {
        byte[] bArray = new BigInteger("10" + hex,16).toByteArray();
        byte[] ret = new byte[bArray.length - 1];
        if (ret.length >= 0) System.arraycopy(bArray, 1, ret, 0, ret.length);
        return ret;
    }

    private static byte[] counterToBytes() {
        return counterToBytes(System.currentTimeMillis() / 1000L);
    }

    private static byte[] counterToBytes(long time) {
        long counter = time / PERIOD;
        byte[] buffer = new byte[Long.SIZE / Byte.SIZE];
        for (int i = 7; i >= 0; i--) {
            buffer[i] = (byte)(counter & 0xff);
            counter = counter >> 8;
        }
        return buffer;
    }

    private static byte[] hash(final byte[] key, final byte[] message) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(key, "RAW");
            hmac.init(keySpec);
            return hmac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}