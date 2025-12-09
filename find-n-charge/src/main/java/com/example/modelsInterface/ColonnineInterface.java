package com.example.modelsInterface;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.models.Colonnina;
import com.example.models.StatoColonnina;

public interface ColonnineInterface {

	public CompletableFuture<Void> salvaColonnina(Colonnina colonnina);
	
	public CompletableFuture<Void> cambiaStatoColonnina(String c, StatoColonnina stato) ;
	
	public CompletableFuture<List<Colonnina>> getAllColonnine();
	
	public CompletableFuture<List<Colonnina>> cercaColonnine(String query);
	
	public CompletableFuture<Integer> contaColonnine() ;
	
	public CompletableFuture<Integer> contaColonnineLG(StatoColonnina stato);
	
	public CompletableFuture<List<String>> getColonnineInCarica();
	
	public CompletableFuture<List<String>> getColonnineSlot(String ora);
	
	
	
}
