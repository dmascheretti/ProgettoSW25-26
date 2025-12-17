/**
 * Classe PrenotazioneService per seprarare il ruolo di prenotazione dalla classe MapView
 * 
 * @author Davide Mascheretti
 */
package com.example.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.enums.StatoColonnina;
import com.example.enums.StatoPrenotazione;
import com.example.models.Auto;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.example.modelsInterface.PrenotazioniInterface;

@Service
public class PrenotazioniService  {
	private final PrenotazioniInterface prenotazioniInterface;

	public PrenotazioniService(PrenotazioniInterface prenotazioniInterface) {
		this.prenotazioniInterface = prenotazioniInterface;
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

		prenotazioniInterface.cercaPrenotazione(c, data, orario).thenAccept(ris -> {

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

	public CompletableFuture<Boolean> prenota(Colonnina c, Utente u, String data, String orario, String auto) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();

		if (c.getStato().equals(StatoColonnina.GUASTA.toString())) {
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

			Prenotazione p = new Prenotazione(generaId(), c.getId(), c.getNome(), u.getUsername(), data, orario,
					LocalDate.now().toString(), auto);

			// Chiamo funzione del database, se salvataggio completato future.complete(true)

			prenotazioniInterface.salvaPrenotazione(p).thenRun(() -> future.complete(true)).exceptionally(ex -> {
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

	public CompletableFuture<Void> aggiornaStato(Prenotazione p, StatoPrenotazione stato) {

		CompletableFuture<Void> future = new CompletableFuture<>();
		prenotazioniInterface.aggiornaStato(p, stato).thenRun(() -> {
			future.complete(null);
		}).exceptionally(e -> {
			future.completeExceptionally(e);
			return null;
		});

		return future;
	}

	public CompletableFuture<Prenotazione> inCarica(Auto a) {
		return prenotazioniInterface.inCarica(a);
	}

	public CompletableFuture<List<String>> getSlotOccupati(String idColonnina, String data) {

		return prenotazioniInterface.getSlotOccupati(idColonnina, data);
	}

	public CompletableFuture<List<Prenotazione>> getUtenteReservation(String u) {

		return prenotazioniInterface.getUtenteReservation(u);
	}

	public CompletableFuture<Boolean> cancellaPrenotazione(Prenotazione p) {

		return prenotazioniInterface.cancellaPrenotazione(p);
	}
	
	public CompletableFuture<Integer> contaPrenotazioni() {
		return prenotazioniInterface.contaPrenotazioni() ;
	}
	public CompletableFuture<Integer> contaPrenotazioniNuove() {
		return prenotazioniInterface.contaPrenotazioniNuove() ;
	}
	
	public CompletableFuture<Integer[]> contaPrenotazioniGiorni() {
		return prenotazioniInterface.contaPrenotazioniGiorni();
	}

	public CompletableFuture<List<Prenotazione>> getAllReservation() {

		return prenotazioniInterface.getAllReservation();
	}
}
