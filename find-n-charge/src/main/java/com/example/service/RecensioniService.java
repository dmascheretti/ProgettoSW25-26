package com.example.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.models.Auto;
import com.example.models.Prenotazione;
import com.example.models.Recensione;
import com.example.modelsInterface.RecensioniInterface;

@Service
public class RecensioniService {
	
	private final RecensioniInterface recensioniInterface;
	
	public RecensioniService(RecensioniInterface recensioniInterface) {
		this.recensioniInterface=recensioniInterface;
	}
	
	public CompletableFuture<Void> aggiungiRecensione(String utente, String colonnina, int stelle, Prenotazione prenotazione){
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		recensioniInterface.verificaRecensione(prenotazione).thenAccept(recensione -> {

			if (recensione != null) {
				future.completeExceptionally(new IllegalArgumentException("Hai già valutato questa prenotazione!"));
				return;
			}


			Recensione r=new Recensione(utente,colonnina,stelle,prenotazione.getId());

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


}
