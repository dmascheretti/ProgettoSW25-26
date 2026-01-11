package com.example.models;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.example.enums.StatoPrenotazione;

class PrenotazioneTest {

    @Test
    void testStatoIniziale() {
    
        Prenotazione p = new Prenotazione("ID1", "COL1", "Colonnina A", "Mario", "2025-01-01", "10:00", "timestamp", "AB123CD");
        
        assertEquals(StatoPrenotazione.FUTURA.toString(), p.getStato());
    }
}