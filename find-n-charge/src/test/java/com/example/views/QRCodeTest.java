package com.example.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.example.util.QRCode;

class QRCodeTest {

	@Test
	void testGeneraQRCode() throws Exception {
		
		String testo = "NuovoCodiceQRCode";
		BufferedImage risultato = QRCode.generaQR(testo);
		
		assertNotNull(risultato, "L'immagine generata non deve essere null");
		assertEquals(300, risultato.getWidth(), "La larghezza deve essere 300px");
        assertEquals(300, risultato.getHeight(), "L'altezza deve essere 300px");
	}
	
	/**
	 * Controllo anche con stringhe corte
	 * @throws Exception
	 */
	@Test
    void testGeneraQRBreve() throws Exception {
        BufferedImage risultato = QRCode.generaQR("A");
        assertNotNull(risultato, "L'immagine generata non deve essere null");
        assertEquals(300, risultato.getWidth(), "La larghezza deve essere 300px");
        assertEquals(300, risultato.getHeight(), "L'altezza deve essere 300px");    
    }
	
	/**
	 * Controllo anche con stringhe vuote
	 * @throws Exception
	 */
	@Test
    void testGeneraQRVuoto() throws Exception {
        assertThrows(Exception.class, () -> {
            QRCode.generaQR("");
        }, "L'input null dovrebbe sollevare un'eccezione");
          
    }
	
	/**
	 * Controllo anche con stringhe vuote
	 * @throws Exception
	 */
	@Test
    void testGeneraQRNull() throws Exception {
        assertThrows(Exception.class, () -> {
            QRCode.generaQR(null);
        }, "L'input null dovrebbe sollevare un'eccezione");
          
    }
	
}
