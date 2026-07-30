package com.example.gym_management_and_fitness_tracking_system;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Generates a real, scannable QR Code Bitmap using ZXing BarcodeEncoder.
 */
public class QrGenerator {

    /**
     * Generates a proper QR Code bitmap for the given content at the specified pixel size.
     * Returns null if encoding fails.
     */
    public static Bitmap generateQrBitmap(String content, int size) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, size, size);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
