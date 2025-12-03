package com.example.modelsInterface;

import com.example.models.Prenotazione;
import com.example.models.Utente;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface UtentiInterface {
	
	CompletableFuture<Utente> verificaUtente(String username);

	CompletableFuture<Void> salvaUtente(Utente utente);

	CompletableFuture<Void> cambiaPassword(Utente utente, String nuovaPassword);

	CompletableFuture<Void> cambiaMail(Utente utente, String nuovaEmail);
	
	CompletableFuture<List<Prenotazione>> getUtenteReservation(String username);

	CompletableFuture<Void> cancellaUtente(Utente utente);

	CompletableFuture<List<Utente>> getAllUtenti();

	CompletableFuture<Integer> contaUtentiNuovi();
	
	CompletableFuture<Integer> contaUtenti();

}