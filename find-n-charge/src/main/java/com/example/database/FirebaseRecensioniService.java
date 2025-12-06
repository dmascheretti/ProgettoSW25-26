package com.example.database;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Repository;
import com.example.models.Prenotazione;
import com.example.models.Recensione;
import com.example.modelsInterface.RecensioniInterface;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

@Repository
public class FirebaseRecensioniService implements RecensioniInterface {

	private final DatabaseReference recensioni;

	public FirebaseRecensioniService() {
		this.recensioni = FirebaseDatabase.getInstance().getReference("recensioni");

	}

	/**
	 * Aggiunge recensione sotto il nodo recensioni/colonnina
	 * 
	 * @param u      Utente che ha scritto la recensione
	 * @param c      Colonnina selezionato
	 * @param stelle Valutazione
	 * @return
	 */
	public CompletableFuture<Void> aggiungiRecensione(Recensione recensione) {
		CompletableFuture<Void> future = new CompletableFuture<>();


		recensioni.child(recensione.getColonnina()).child(recensione.getIdPrenotazione()).setValue(recensione, (databaseError, ref) -> {
			if (databaseError != null) {
				// errore --> chiama eccezione
				future.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				future.complete(null);
			}
		});
		return future;
	}
	
	public CompletableFuture<Recensione> verificaRecensione(Prenotazione p) {
		CompletableFuture<Recensione> future = new CompletableFuture<>();

	
		DatabaseReference userRef = recensioni.child(p.getIDColonnina()).child(p.getId());
		userRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override

			public void onDataChange(DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists()) {
					
					Recensione recensione = dataSnapshot.getValue(Recensione.class);
					
					future.complete(recensione);
				} else {
					future.complete(null);
				}
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				System.err.println("Errore nel database " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}
		});

		return future;
	}

}