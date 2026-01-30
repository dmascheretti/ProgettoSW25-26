/**
 * Enumerativi per la gestione dei vari stati della colonnine
 * 
 */

package com.example.enums;

public enum StatoColonnina {
    LIBERA("Libera"), // Codice per DB + Label per UI
    IN_CARICA("In Carica"),
    PRENOTATA("Prenotata"),
    GUASTA("Guasta");


    private final String label;

    StatoColonnina(String label) {
       
        this.label = label;
    }

    public String getLabel() { return label; }
}