/**
 * Classe PrenotazioneService per seprarare il ruolo di prenotazione dalla classe MapView
 * 
 * @author Davide Mascheretti
 */
package com.example.util;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import com.example.database.FirebaseService;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;

public class PrenotazioneService {
	private final FirebaseService firebaseService;

	public PrenotazioneService(FirebaseService fb) {
		this.firebaseService = fb;
	}

	/**
	 * Verifica l'esistenza del nodo chiamando la funzione da FirebaseService
	 * 
	 * @param c      colonnina
	 * @param data
	 * @param orario
	 * @return CompletableFuture true/false in base al risultato di
	 *         cercaPrenotazione
	 */


	private String generaId() {
		 String prenotazioneId = java.util.UUID.randomUUID().toString();
		 return prenotazioneId;
	}
	
	public CompletableFuture<Boolean> verifica(Colonnina c, String data, String orario) {
		
		
		CompletableFuture<Boolean> future = new CompletableFuture<>();

		firebaseService.cercaPrenotazione(c, data, orario).thenAccept(ris -> {

			/*
			 * Slot occupato
			 */
			if (ris != null) {

				future.complete(false);
			}

			/*
			 * Slot libero
			 */
			else
				future.complete(true);

		}).exceptionally(ex -> {
			future.complete(false);
			return null;
		});

		return future;
	}

	/**
	 * 
	 * Salva la prenotazione nel databse dopo aver verificato lo slot data e orario
	 * 
	 * @param c      Colonnina
	 * @param u      Utente corrente
	 * @param data
	 * @param orario
	 * @return CompletableFuture in base al successo o errore della funzione
	 */

	public CompletableFuture<Boolean> prenota(Colonnina c, Utente u, String data, String orario) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		
		if(c.getStato().equals("Manutenzione")) {
			future.complete(false);
			return future;
		}

		verifica(c, data, orario).thenAccept(ris -> {
			/*
			 * Se verifica restituisce future.complete (false) --> non salvo la prenotazione
			 */
			if (!ris) {
				future.complete(false);
				return;
			}

			/*
			 * Se verifica restituisce future.complete (true) --> salvo la prenotazione
			 */

			Prenotazione p = new Prenotazione(generaId(),c.getId(), u.getUsername(), data, orario, LocalDate.now().toString());

			// Chiamo funzione del database, se salvataggio completato future.complete(true)

			firebaseService.salvaPrenotazione(p).thenRun(() -> future.complete(true)).exceptionally(ex -> {
				future.complete(false);
				return null;
			});

		})
				// eccezioni di verifica(c, data, orario)
				.exceptionally(ex -> {
					future.complete(false);
					return null;
				});
		return future;
	}
	
	

}
