package com.example.models;

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
