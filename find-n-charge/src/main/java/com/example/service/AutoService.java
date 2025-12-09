package com.example.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.database.FirebaseAutoService;
import com.example.models.Auto;
import com.example.models.Utente;
import com.example.modelsInterface.AutoInterface;

@Service
public class AutoService {

	private AutoInterface autoInterface;

	@Autowired
	public AutoService(AutoInterface autoInterface) {
		this.autoInterface = autoInterface;
	}
	
	public CompletableFuture<List<String>> getTargheUtente(Utente u){
		
		CompletableFuture <List<String>> future= new CompletableFuture<>();
		
		autoInterface.getTargheUtente(u).thenAccept(lista->{
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
		
		autoInterface.listaAutoUtente(u).thenAccept(lista->{
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
	autoInterface.verificaTarga(targa).thenAccept(autoTrovata -> {

		// Auto già trovata con questa targa, invio eccezione
		if (autoTrovata != null) {
			future.completeExceptionally(new IllegalArgumentException("Targa già esistente"));
			return;
		}


		// Oggetto auto locale
		Auto a = new Auto(targa, modello, tipo, u.getUsername());

		// Chiamo firebase per salvare auto nel database
		autoInterface.salvaAuto(a).thenRun(() -> {
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

public CompletableFuture<Void> eliminaAuto(Auto a) {
	CompletableFuture<Void> future = new CompletableFuture<>();

    autoInterface.deleteAuto(a)
        .thenRun(() -> future.complete(null))
        .exceptionally(ex -> {
            future.completeExceptionally(ex);
            return null;
        });

    return future;

}

}
