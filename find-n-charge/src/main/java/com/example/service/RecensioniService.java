/**
 * Gestione recensioni, validazione e chiamata a interfaccia
 */
package com.example.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Recensione;
import com.example.modelsInterface.RecensioniInterface;

@Service
public class RecensioniService {

	private final RecensioniInterface recensioniInterface;

	public RecensioniService(RecensioniInterface recensioniInterface) {
		this.recensioniInterface = recensioniInterface;
	}

	public CompletableFuture<Float> getValutazColonnina(Colonnina colonnina) {

		CompletableFuture<Float> future = new CompletableFuture<>();
		//qui id colonnina è presente perche quando scarico le colonnine faccio .setId
		recensioniInterface.getRecensioniColonnina(colonnina.getId()).thenAccept(recensioni -> {

			float somma = 0;

			if (recensioni.isEmpty()) {
				future.completeExceptionally(null);
				return;
			}
			for (Recensione r : recensioni) {
				somma = somma + r.getStelle();
			}

			future.complete(somma / recensioni.size());
		}).exceptionally(ex -> {
			future.completeExceptionally(ex);
			return null;
		});

		return future;
	}

	public CompletableFuture<Void> aggiungiRecensione(String utente, String colonnina, int stelle,
			Prenotazione prenotazione) {

		CompletableFuture<Void> future = new CompletableFuture<>();
		recensioniInterface.verificaRecensione(prenotazione).thenAccept(recensione -> {

			if (recensione != null) {
				future.completeExceptionally(new IllegalArgumentException("Hai già valutato questa prenotazione!"));
				return;
			}

			Recensione r = new Recensione(utente, colonnina, stelle, prenotazione.getId());

			recensioniInterface.aggiungiRecensione(r).thenRun(() -> {
				future.complete(null);
			}).exceptionally(ex -> {

				future.completeExceptionally(ex);
				return null;
			});

			// eccezione di verificaTarga
		}).exceptionally(ex -> {

			future.completeExceptionally(new RuntimeException("Errore verifica recensione"));
			return null;
		});

		// null se tutto è andato a buon fine
		return future;
	}
	
	public CompletableFuture<Recensione> getRecensionePrenotazione(Prenotazione p) {
	    return recensioniInterface.verificaRecensione(p);
	}

}
