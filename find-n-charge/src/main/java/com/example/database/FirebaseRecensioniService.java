package com.example.database;

import java.util.concurrent.CompletableFuture;

import com.example.models.Colonnina;
import com.example.models.Recensione;
import com.example.models.Utente;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseRecensioniService {

	private final DatabaseReference recensioni;

	public FirebaseRecensioniService() {
		this.recensioni = FirebaseDatabase.getInstance().getReference("recensioni");
		
	}
	
	/**
	 * Aggiunge recensione sotto il nodo recensioni/colonnina
	 * @param u Utente che ha scritto la recensione
	 * @param c Colonnina selezionata
	 * @param mess Messaggio 
	 * @param stelle Valutazione
	 * @return
	 */
	public CompletableFuture<Void> aggiungiRecensione(Utente u, Colonnina c, String mess, int stelle) {
		CompletableFuture<Void> recensione = new CompletableFuture<>();
		
		Recensione r=new Recensione(u.getUsername(),mess,stelle);
		
		recensioni.child(c.getNome()).push().setValue(r, (databaseError, ref) -> {
			if (databaseError != null) {
				// errore --> chiama eccezione
				recensione.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				recensione.complete(null);
			}
		});
		return recensione; 
}

}