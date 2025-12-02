/**
 * @author claudiomorgera
 * Classe per modellazione dell‘oggetto automobile: targa, proprietario, grandezza batteria in kWh e stato di carica della batteria in percentuale
 */
package com.example.models;
import java.util.Random;

import java.time.LocalDateTime;

public class Auto {

	

	private String targa;
	private String modello;
	private String tipo;
	private String proprietario;
	private  int capacitaBatteria;
	private double statoCaricaIniziale;
	private LocalDateTime inizioRicarica;
	private Colonnina colonna;
	private static final Random rand = new Random();
	
	public Auto(String targa, String modello, String tipo, String proprietario) {
		this.targa = targa;
		this.setModello(modello);
		this.proprietario = proprietario;
		this.setTipo(tipo);
		this.capacitaBatteria = setCapacitaBatteria(tipo);
		this.setStatoCarica(rand.nextInt(81)+20);
	}

	public Auto(){
		}
	
	
	public String getTarga() {
		return targa;
	}

	public void setTarga(String targa) {
		this.targa = targa;
	}

	public String getProprietario() {
		return proprietario;
	}

	public void setProprietario(String proprietario) {
		this.proprietario = proprietario;
	}

	public int getCapacitaBatteria() {
		return capacitaBatteria;
	}
	
    public int setCapacitaBatteria(String tipo) {
    	
    	switch(tipo) {
    	
    	case "Berlina (65 kWh)": return 65;
    	
    	case "Suv (100 kWh)": return 100;
    	
    	case "Utilitaria (40 kWh)": return 40;
    	
    	case "Sportiva (75 kWh)": return 75;
    	
    	default: return 0;	
    	
    		
    	}
		
	}

	public double getStatoCarica() {
		return statoCaricaIniziale;
	}

	public void setStatoCarica(double statoCarica) {
		this.statoCaricaIniziale = statoCarica;
	}

	public String getModello() {
		return modello;
	}

	public void setModello(String modello) {
		this.modello = modello;
	}

	public LocalDateTime getInizioRicarica() {
		return inizioRicarica;
	}

	public void setInizioRicarica(LocalDateTime inizioRicarica) {
		this.inizioRicarica = inizioRicarica;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Colonnina getColonna() {
		return colonna;
	}
	
	
	
}
