package com.example.modelsInterface;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.enums.StatoColonnina;
import com.example.models.Colonnina;

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
