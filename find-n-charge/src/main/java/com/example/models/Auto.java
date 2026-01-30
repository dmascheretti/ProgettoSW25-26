/**
 * Classe per modellazione dell‘oggetto automobile: targa, proprietario, grandezza batteria in kWh e stato di carica della batteria in percentuale
 * @author claudiomorgera
 */
package com.example.models;
import java.util.Random;

import com.example.enums.TipoAuto;
public class Auto {

	

	private String targa;
	private String modello;
	private String tipo;
	private String proprietario;
	private  int capacitaBatteria;
	private double statoCaricaIniziale;
	private String inizioRicarica;
	private String oraUltimoControllo;
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
    	
    	TipoAuto tipoAuto= TipoAuto.getTipoFromString(tipo);
    	
    	if(tipoAuto != null) return tipoAuto.getCapacita();
    	
    	else return 0;
		
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

	public String getInizioRicarica() {
		return inizioRicarica;
	}

	public void setInizioRicarica(String inizioRicarica) {
		this.inizioRicarica = inizioRicarica;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getOraUltimoControllo() {
		return oraUltimoControllo;
	}

	public void setOraUltimoControllo(String oraUltimoControllo) {
		this.oraUltimoControllo = oraUltimoControllo;
	}

	
	
	
}
