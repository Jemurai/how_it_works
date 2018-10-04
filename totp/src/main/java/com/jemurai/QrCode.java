package com.jemurai;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.commons.codec.binary.Base32;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import static com.jemurai.Totp.hexToBytes;

class QrCode {
    static void generate(String applicationName, String issuer, String path) {
        try {
            String seed = Totp.generateSeed();
            Base32 base32 = new Base32();
            String secret = base32.encodeAsString(hexToBytes(seed)).replace("=", "");
            String qrdata = String.format("otpauth://totp/%s?secret=%s&issuer=%s", applicationName, secret, issuer);
            System.out.println(seed);
            System.out.println(qrdata);
            generateQRCodeImage(qrdata, 350, 350, path);
        } catch (WriterException e) {
            System.out.println("Could not generate QR Code, WriterException :: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Could not generate QR Code, IOException :: " + e.getMessage());
        }
    }

    private static void generateQRCodeImage(String text, int width, int height, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }
}
