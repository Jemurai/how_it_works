package com.jemurai.howitworks.totp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

class QrCode {
    private static final int WIDTH = 350;
    private static final int HEIGHT = 350;

    static void generate(final String applicationName, final String issuer, final String path, final String secret) {
        try {
            String qrdata = String.format("otpauth://totp/%s?secret=%s&issuer=%s", applicationName, secret, issuer);
            generateQRCodeImage(qrdata, path);
        } catch (WriterException | IOException e) {
            System.out.println("Could not generate QR Code: " + e.getMessage());
        }
    }

    private static void generateQRCodeImage(final String text, final String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }
}
