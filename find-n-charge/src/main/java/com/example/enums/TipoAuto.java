package com.example.enums;

import java.util.Arrays;

public enum TipoAuto {
	SPORTIVA("Sportiva (75 kWh)", 75),
	SUV("Suv (100 kWh)", 100), 
	BERLINA("Berlina (65 kWh)", 65), 
	UTILITARIA("Utilitaria (40 kWh)", 40);
	
	private final String label;
	private final int capacita;

    TipoAuto(String label, int capacita) {
       
        this.label = label;
        this.capacita=capacita;
    }

    public String getLabel() { return label; }
    
    public int getCapacita() { return capacita;}
    
    /**
     * Da stringa label trova il tipoAuto (con label + capacita)
     * @param label
     * @return
     */
    public static TipoAuto getTipoFromString (String label) {
    	return Arrays.stream(TipoAuto.values())
                .filter(t -> t.label.equalsIgnoreCase(label))
                .findFirst()
                .orElse(null); 
}
    }
    
