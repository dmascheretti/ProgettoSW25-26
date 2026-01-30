/**
 * Enumerativi per la gestione degli stati della prenotazione
 */
package com.example.enums;

public enum StatoPrenotazione {
	FUTURA("Futura"),
	IN_CARICA("In carica"), 
	PASSATA("Passata"), 
	ATTIVA("Attiva");

	private final String label;

	StatoPrenotazione(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

}
