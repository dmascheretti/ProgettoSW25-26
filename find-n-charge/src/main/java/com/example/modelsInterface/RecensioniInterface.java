package com.example.modelsInterface;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Recensione;

public interface RecensioniInterface {

	public CompletableFuture<Void> aggiungiRecensione(Recensione r);
	
	public CompletableFuture<List<Recensione>> getRecensioniColonnina(String colonninaID);
	
	public CompletableFuture<Recensione> verificaRecensione(Prenotazione p);
	
}
