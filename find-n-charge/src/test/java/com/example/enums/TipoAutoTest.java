package com.example.enums;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TipoAutoTest {

    @Test
    void testGetTipoFromString_Valido() {
        
        TipoAuto tipo = TipoAuto.getTipoFromString("Suv (100 kWh)");
        assertEquals(TipoAuto.SUV, tipo);
    }

    @Test
    void testGetTipoFromString_Minuscolo() {
        TipoAuto tipo = TipoAuto.getTipoFromString("suv (100 kwh)");
        assertEquals(TipoAuto.SUV, tipo);
    }

    @Test
    void testGetTipoFromString_NonValido() {
        TipoAuto tipo = TipoAuto.getTipoFromString("Tipo inesistente");
        assertNull(tipo);
    }
    
    @Test
    void testCapacitaCorretta() {

        assertEquals(100, TipoAuto.SUV.getCapacita());
        assertEquals(40, TipoAuto.UTILITARIA.getCapacita());
    }
}