package com.example.modelsInterface;

import java.util.concurrent.CompletableFuture;

import com.example.models.Prenotazione;
import com.example.models.Recensione;

public interface RecensioniInterface {

	public CompletableFuture<Void> aggiungiRecensione(Recensione r);

	public CompletableFuture<Recensione> verificaRecensione(Prenotazione p);
	
	
	
}
