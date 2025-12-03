package com.example.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.database.FirebaseAutoService;
import com.example.models.Auto;
import com.example.models.Utente;

@Service
public class AutoService {

	private FirebaseAutoService firebaseAutoService;

	@Autowired
	public AutoService(FirebaseAutoService firebaseAutoService) {
		this.firebaseAutoService = firebaseAutoService;
	}
	
	public CompletableFuture<List<String>> getTargheUtente(Utente u){
		
		CompletableFuture <List<String>> future= new CompletableFuture<>();
		
		firebaseAutoService.getTargheUtente(u).thenAccept(lista->{
			future.complete(lista);
		})
		.exceptionally(e -> {
			future.completeExceptionally(e);
			return null;
		});
		
		return future;
	}
	
public CompletableFuture<List<Auto>> getAutoUtente(Utente u){
		
		CompletableFuture <List<Auto>> future= new CompletableFuture<>();
		
		firebaseAutoService.listaAutoUtente(u).thenAccept(lista->{
			future.complete(lista);
		})
		.exceptionally(e -> {
			future.completeExceptionally(e);
			return null;
		});
		
		return future;
	}

public CompletableFuture<Void> aggiungiAuto(String targa, String modello, String tipo, Utente u) {

	CompletableFuture<Void> future = new CompletableFuture<>();
	firebaseAutoService.verificaTarga(targa).thenAccept(autoTrovata -> {

		// Auto già trovata con questa targa, invio eccezione
		if (autoTrovata != null) {
			future.completeExceptionally(new IllegalArgumentException("Targa già esistente"));
			return;
		}


		// Oggetto auto locale
		Auto a = new Auto(targa, modello, tipo, u.getUsername());

		// Chiamo firebase per salvare auto nel database
		firebaseAutoService.salvaAuto(a).thenRun(() -> {
			future.complete(null);
			// eccezione di salvaAuto
		}).exceptionally(ex -> {

			future.completeExceptionally(ex);
			return null;
		});

		// eccezione di verificaTarga
	}).exceptionally(ex -> {

		future.completeExceptionally(new RuntimeException("Errore verifica targa"));
		return null;
	});

	// null se tutto è andato a buon fine
	return future;
}

}
