package com.qdc.lims.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Utility class for generating QR codes as Base64-encoded PNG images.
 */
public class QrCodeUtil {

    /**
     * Generates a QR code for the given text and returns it as a Base64-encoded PNG image string.
     *
     * @param text the text to encode in the QR code
     * @param width the width of the QR code image
     * @param height the height of the QR code image
     * @return a Base64-encoded PNG image string, or an empty string if generation fails
     */
    public static String generateBase64Qr(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            byte[] pngData = pngOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);

        } catch (Exception e) {
            e.printStackTrace();
            return ""; // Return empty if failed
        }
    }
}