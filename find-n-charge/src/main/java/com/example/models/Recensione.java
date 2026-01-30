/**
 * Classe oggetto recensione
 */
package com.example.models;


public class Recensione {
    private String utente;
    private int stelle;
    private String colonnina;
    private String idPrenotazione;

    public Recensione(String utente, String colonnina, int stelle, String idPrenotazione) {
        this.utente = utente;
        this.stelle=stelle;
        this.setColonnina(colonnina);
        this.idPrenotazione=idPrenotazione;
    }
    
    public Recensione() {}

    // Getter e Setter
    public String getUtente() { return utente; }
    public void setUtente(String utente) { this.utente = utente; }
    

	public int getStelle() {
		return stelle;
	}

	public String getColonnina() {
		return colonnina;
	}

	public void setColonnina(String colonnina) {
		this.colonnina = colonnina;
	}

	public String getIdPrenotazione() {
		return idPrenotazione;
	}

	public void setIdPrenotazione(String idPrenotazione) {
		this.idPrenotazione = idPrenotazione;
	}
}
