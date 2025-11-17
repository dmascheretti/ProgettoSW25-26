package com.example.models;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Recensione {
    private String utente;
    private String messaggio;
    private String ora;
    private int stelle;


    public Recensione() {}

    public Recensione(String utente, String messaggio, int stelle) {
        this.utente = utente;
        this.messaggio = messaggio;
        this.ora = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.stelle=stelle;
    }

    // Getter e Setter
    public String getUtente() { return utente; }
    public void setUtente(String utente) { this.utente = utente; }

    public String getMessaggio() { return messaggio; }
    public void setMessaggio(String messaggio) { this.messaggio = messaggio; }

    public String getOra() { return ora; }
    public void setOra(String ora) { this.ora = ora; }

	public int getStelle() {
		return stelle;
	}
}
