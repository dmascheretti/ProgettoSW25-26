/**
 * Classe oggetto prenotazione
 */
package com.example.models;

import java.time.LocalTime;
import java.util.Date;
import org.threeten.bp.LocalDate;

public class Prenotazione {
	
	private String id;
	private String nomeColonnina;
	private String utente;
	private String data; 
	private String inizio;
	private String timestamp;
	private String targa;

	public Prenotazione() {
		
	}
	
	public Prenotazione(String id, String nome ,String utente, String localDate, String inizio, String ts) {
		this.id=id;
		this.setNomeColonnina(nome);
		this.utente=utente;
		this.setData(localDate);
		this.setInizio(inizio);
		this.timestamp=ts;
		this.setTarga("AAAAAA");
		
	}

	public String getNomeColonnina() {
		return nomeColonnina;
	}

	public void setNomeColonnina(String nomeColonnina) {
		this.nomeColonnina = nomeColonnina;
	}

	public String getUtente() {
		return utente;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getInizio() {
		return inizio;
	}

	public void setInizio(String inizio) {
		this.inizio = inizio;
	}

	public String getTimestamp() {
		return timestamp;
}
	public String getId() {
		return id;
	}

	public String getTarga() {
		return targa;
	}

	public void setTarga(String targa) {
		this.targa = targa;
	}
	
}