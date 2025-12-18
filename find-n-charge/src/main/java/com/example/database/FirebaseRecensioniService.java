package com.example.database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Repository;

import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Recensione;
import com.example.models.Utente;
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
	 * @param recensione da aggiungere al DB
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
	
	/**
	 * Verifiche che l'utente non abbia già inserito la valutazione per quella prenotazione
	 * 
	 * @param p Prenotazione da verificare 
	 * @return Recensione da inserire se passa la verifica
	 * 
	 */
	public CompletableFuture<Recensione> verificaRecensione(Prenotazione prenotazione) {
		CompletableFuture<Recensione> future = new CompletableFuture<>();

	
		DatabaseReference userRef = recensioni.child(prenotazione.getIDColonnina()).child(prenotazione.getId());
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

	/**
	 * Ottiene lista delle recensioni per una colonnina
	 * 
	 * @param colonninaID id univoco della colonnina
	 * @return Lista di recensioni
	 */
	public CompletableFuture<List<Recensione>> getRecensioniColonnina(String colonninaID) {
		// Oggetto che permette al programma di non fermarsi perchè sa che conterrà la
		// lista che sta cercando
		CompletableFuture<List<Recensione>> future = new CompletableFuture<>();

		// Legge il nodo recensioni
		recensioni.child(colonninaID).addListenerForSingleValueEvent(new ValueEventListener() {

			// Se le legge senza problemi
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				List<Recensione> listaRec = new ArrayList<>();
				if (dataSnapshot.exists()) {
					for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
						Recensione r = snapshot.getValue(Recensione.class); // Acquisisce tutti i dati delle recensioni
						if (r != null) {
							listaRec.add(r);
						}
					}
				}
				future.complete(listaRec); // Restituisce la lista (piena o vuota)
			}

			// Se trova errori
			@Override
			public void onCancelled(DatabaseError databaseError) {
				System.err.println("Errore nel caricamento: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}
		});
		return future;
	}

}