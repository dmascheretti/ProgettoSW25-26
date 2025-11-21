/**
 * Classe per generare QR code data la stringa che identifica la prenotazione univocamente presente nel database
 * @author Davide Mascheretti
 */
package com.example;

import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Service
public class QRCode {

	public static BufferedImage generaQR(String text) throws Exception {
		int width = 300; // DA MODIFICARE IN BASE ALLE DIMENSIONI
		int height = 300; // DA MODIFICARE IN BASE ALLE DIMENSIONI

		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

		return MatrixToImageWriter.toBufferedImage(bitMatrix);
	}
}