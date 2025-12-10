/**
 * Classe oggetto prenotazione
 */
package com.example.models;


import com.example.enums.StatoPrenotazione;

public class Prenotazione {
	
	private String id;
	private String idColonnina;
	private String nomeColonnina;
	private String utente;
	private String data; 
	private String inizio;
	private String timestamp;
	private String targa;
	private String stato;

	public Prenotazione() {
		
	}
	
	public Prenotazione(String id, String idCol, String nomeCol, String utente, String localDate, String inizio, String ts, String targa) {
		this.id=id;
		this.setIDColonnina(idCol);
		this.setNomeColonnina(nomeCol);
		this.utente=utente;
		this.setData(localDate);
		this.setInizio(inizio);
		this.timestamp=ts;
		this.setTarga(targa);
		this.setStato(StatoPrenotazione.FUTURA.toString());
		
	}

	public String getIDColonnina() {
		return idColonnina;
	}

	public void setIDColonnina(String nomeColonnina) {
		this.idColonnina = nomeColonnina;
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

	public String getStato() {
		return stato;
	}

	public void setStato(String stato) {
		this.stato = stato;
	}

	public String getNomeColonnina() {
		return nomeColonnina;
	}

	public void setNomeColonnina(String nomeColonnina) {
		this.nomeColonnina = nomeColonnina;
	}
	
}