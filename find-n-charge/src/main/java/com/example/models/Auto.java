/**
 * @author claudiomorgera
 * Classe per modellazione dell‘oggetto automobile: targa, proprietario, grandezza batteria in kWh e stato di carica della batteria in percentuale
 */
package com.example.models;

public class Auto {

	

	private String targa;
	private String modello;
	private String tipo;
	private Utente proprietario;
	private  int capacitaBatteria;
	private int statoCarica;
	
	public Auto(){
		}
	
	public Auto(String targa, String modello, String tipo, Utente proprietario) {
		this.targa = targa;
		this.setModello(modello);
		this.proprietario = proprietario;
		this.capacitaBatteria = setCapacitaBatteria(tipo);
	}

	public String getTarga() {
		return targa;
	}

	public void setTarga(String targa) {
		this.targa = targa;
	}

	public Utente getProprietario() {
		return proprietario;
	}

	public void setProprietario(Utente proprietario) {
		this.proprietario = proprietario;
	}

	public int getCapacitaBatteria() {
		return capacitaBatteria;
	}
	
    public int setCapacitaBatteria(String tipo) {
    	
    	switch(tipo) {
    	
    	case "Berlina": return 65;
    	
    	case "Suv": return 100;
    	
    	case "Utilitaria": return 40;
    	
    	case "Sportiva": return 75;
    	
    	default: return 0;	
    	
    		
    	}
		
	}

	public int getStatoCarica() {
		return statoCarica;
	}

	public void setStatoCarica(int statoCarica) {
		this.statoCarica = statoCarica;
	}

	public String getModello() {
		return modello;
	}

	public void setModello(String modello) {
		this.modello = modello;
	}
	
	
	
}
