package com.example.database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.example.models.Colonnina;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class FirebaseColonnineService {

	private final DatabaseReference colonnine;

	public FirebaseColonnineService() {
		this.colonnine = FirebaseDatabase.getInstance().getReference("colonnine");

	}

	public CompletableFuture<Void> salvaColonnina(Colonnina colonnina) {
		CompletableFuture<Void> futureColonnina = new CompletableFuture<>();
		colonnine.child(colonnina.getId()).setValue(colonnina, (databaseError, ref) -> {
			if (databaseError != null) {
				// errore --> chiama eccezione anche in RegisterView
				futureColonnina.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				futureColonnina.complete(null);
			}
		});
		return futureColonnina; // qui il thenRun() in registrazione capisce che ha finito e prosegue con
		// esecuzione
	}

	public CompletableFuture<Void> cambiaStatoColonnina(String c, String stato) {
		CompletableFuture<Void> futureColonnina = new CompletableFuture<>();
		colonnine.child(c).child("stato").setValue(stato, (databaseError, ref) -> {
			if (databaseError != null) {
				// errore --> chiama eccezione anche in RegisterView
				futureColonnina.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				futureColonnina.complete(null);
			}
		});
		return futureColonnina; // qui il thenRun() in registrazione capisce che ha finito e prosegue con
		// esecuzione
	}

	public CompletableFuture<List<Colonnina>> getAllColonnine() {

		// Oggetto che permette al programma di non fermarsi perchè sa che conterrà la
		// lista che sta cercando
		CompletableFuture<List<Colonnina>> future = new CompletableFuture<>();

		// Legge il nodo colonnine
		colonnine.addListenerForSingleValueEvent(new ValueEventListener() {

			// Se li legge senza problemi
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) { // Istantanea dei dati
				List<Colonnina> listaColonnine = new ArrayList<>();
				if (dataSnapshot.exists()) {
					for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
						Colonnina colonnina = snapshot.getValue(Colonnina.class); // Acquisisce tutti i dati delle
																					// colonnine
						if (colonnina != null) {
							colonnina.setId(snapshot.getKey()); // Salva la chiave univoca come id
							listaColonnine.add(colonnina);
						}
					}
				}
				future.complete(listaColonnine); // Restituisce la lista (piena o vuota)
			}

			// Se trova errori
			@Override
			public void onCancelled(DatabaseError databaseError) {
				System.err.println("Errore nel caricamento colonnine: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}
		});
		return future;
	}

	/**
	 * Funzione necessaria per filtrare le colonnine in base alla query. Prende
	 * tutte le colonnine e la filtra in base ai parametri ricercati dall'utente.
	 * 
	 * @param query
	 * @return lista delle colonnine filtrata
	 */

	public CompletableFuture<List<Colonnina>> cercaColonnine(String query) {
		String filtro = query == null ? "" : query.toLowerCase();

		return getAllColonnine().thenApply(lista -> lista.stream().filter(c -> {
			String nome = c.getNome() != null ? c.getNome().toLowerCase() : "";
			String indirizzo = c.getIndirizzo() != null ? c.getIndirizzo().toLowerCase() : "";
			String comune = c.getComune() != null ? c.getComune().toLowerCase() : "";
			return nome.contains(filtro) || indirizzo.contains(filtro) || comune.contains(filtro);
		}).collect(Collectors.toList()));
	}

	/**
	 * Conta il numero delle colonnine presenti nel database. Conta i figli del nodo
	 * colonnine.
	 * 
	 * @return future.complete(count) --> numero di colonnine,
	 *         future.complete(eccezione) --> errore
	 */

	public CompletableFuture<Integer> contaColonnine() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		colonnine.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count = 0;

				if (snapshot.exists()) {
					count = (int) snapshot.getChildrenCount();
				}

				future.complete(count);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// Gestione errore standard, come negli altri metodi
				System.err.println("Errore nel contare le colonnine: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}

		});
		return future;

	}

	/**
	 * Conta il numero di colonnine presenti nel database che sono libere o guaste.
	 * Conta i figli del nodo colonnine che hanno stato uguale a libero o guasto (in
	 * base allo stato passato come string msg)
	 * 
	 * @param msg Stringa che rappresenta lo stato : può essere "Libera" o "Guasta"
	 * @return future.complete(count) --> numero di colonnine con stato=msg,
	 *         future.complete(eccezione) --> errore
	 */

	public CompletableFuture<Integer> contaColonnineLG(String msg) {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		Query colonnineCount = colonnine.orderByChild("stato").equalTo(msg);
		colonnineCount.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count = 0;

				if (snapshot.exists()) {
					count = (int) snapshot.getChildrenCount();
				}

				future.complete(count);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// Gestione errore standard, come negli altri metodi
				System.err.println("Errore nel contare le colonnine: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}

		});
		return future;

	}

}
