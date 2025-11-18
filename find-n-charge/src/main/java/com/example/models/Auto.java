/**
 * @author claudiomorgera
 * Classe per modellazione dell‘oggetto automobile: targa, proprietario, grandezza batteria in kWh e stato di carica della batteria in percentuale
 */
package com.example.models;

public class Auto {

	

	private String targa;
	private Utente proprietario;
	private int capacitaBatteria;
	private int statoCarica;
	
	public Auto(){
		}
	
	public Auto(String targa, Utente proprietario, int capacitaBatteria, int statoCarica) {
		this.targa = targa;
		this.proprietario = proprietario;
		this.capacitaBatteria = capacitaBatteria;
		this.statoCarica = statoCarica;
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

	public void setCapacitaBatteria(int capacitaBatteria) {
		this.capacitaBatteria = capacitaBatteria;
	}

	public int getStatoCarica() {
		return statoCarica;
	}

	public void setStatoCarica(int statoCarica) {
		this.statoCarica = statoCarica;
	}
	
	
}
