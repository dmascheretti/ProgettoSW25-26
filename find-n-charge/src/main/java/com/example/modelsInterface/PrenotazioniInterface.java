package com.example.modelsInterface;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.enums.StatoPrenotazione;
import com.example.models.Auto;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;

public interface PrenotazioniInterface {

	public CompletableFuture<Void> salvaPrenotazione(Prenotazione p);
	
	public CompletableFuture<Boolean> cancellaPrenotazione(Prenotazione p);
	
	public CompletableFuture<Prenotazione> cercaPrenotazione(Colonnina c, String data, String ora);
	
	public CompletableFuture<List<Prenotazione>> getUtenteReservation(String username);
	
	public CompletableFuture<List<Prenotazione>> getAllReservation();
	
	public CompletableFuture<List<String>> getColonnineSlot(String ora);
	
	public CompletableFuture<Integer> contaPrenotazioni() ;
	
	public CompletableFuture<Integer> contaPrenotazioniNuove();
	
	public CompletableFuture<List<String>> getSlotOccupati(String colonnina, String data) ;
	
	public CompletableFuture <Boolean> inCarica(Auto a);
	
	public CompletableFuture<Void> aggiornaStato(Prenotazione p, StatoPrenotazione stato);
	
	public CompletableFuture<Integer[]> contaPrenotazioniGiorni();
	
		
		
	
	
	
}
