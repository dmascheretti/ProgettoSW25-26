package com.example.enums;

public enum TipoAuto {
	SPORTIVA("Sportiva (75 kWh)"),
	SUV("Suv (100 kWh)"), 
	BERLINA("Berlina (65 kWh)"), 
	UTILITARIA("Utilitaria (40 kWh)");
	
	private final String label;

    TipoAuto(String label) {
       
        this.label = label;
    }

    public String getLabel() { return label; }
}
