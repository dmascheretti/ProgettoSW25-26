package com.example.modelsInterface;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.models.Auto;
import com.example.models.Utente;

public interface AutoInterface {
	
	public CompletableFuture<Auto> verificaTarga(String targa);
	
	public CompletableFuture<Void> salvaAuto(Auto auto);
	
	public CompletableFuture<Void> deleteAuto(Auto auto);
	
	public CompletableFuture<Void> calcolaNuovoStato(Auto auto, LocalDateTime oraAttuale, double potenzaColonninaKw);
	
	public CompletableFuture<List<Auto>> listaAutoUtente(Utente u);
	
	public CompletableFuture<List<String>> getTargheUtente(Utente u);
	

}
