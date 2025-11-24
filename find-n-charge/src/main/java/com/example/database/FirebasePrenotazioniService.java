package com.example.database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class FirebasePrenotazioniService {
	private final DatabaseReference prenotazioni;

	public FirebasePrenotazioniService() {
		this.prenotazioni = FirebaseDatabase.getInstance().getReference("prenotazioni");

	}

	/**
	 * Salva all'interno del database la prenotazione con sottodonodo di
	 * "prenotazioni" del tipo nome colnonnina + data + orario (sottoforma di
	 * stringa)
	 * 
	 * Se trova errore nel caricamento il futurePrenotazione viene completato con un
	 * eccezione, altrimenti viene completato con null.
	 * 
	 * @param p Prenotazione da salvare nel database
	 * @return futurePrenotazione (completato con null o con eccezione)
	 */

	public CompletableFuture<Void> salvaPrenotazione(Prenotazione p) {

		CompletableFuture<Void> futurePrenotazione = new CompletableFuture<>();
		/*
		 * Salva nel db prenotazioni/"nome colonnina + data + orario"
		 */
		prenotazioni.child(p.getNomeColonnina() + " " + p.getData() + " " + p.getInizio()).setValue(p,
				(databaseError, ref) -> {
					if (databaseError != null) {
						// errore --> chiama eccezione anche in RegisterView
						futurePrenotazione.completeExceptionally(new RuntimeException(databaseError.getMessage()));
					} else {
						futurePrenotazione.complete(null);
					}
				});
		return futurePrenotazione; // qui il thenRun() in registrazione capisce che ha finito e prosegue con
		// esecuzione
	}

	public CompletableFuture<Void> cancellaPrenotazione(Prenotazione p) {

		CompletableFuture<Void> futurePrenotazione = new CompletableFuture<>();
		/*
		 * Salva nel db prenotazioni/"nome colonnina + data + orario"
		 */
		prenotazioni.child(p.getNomeColonnina() + " " + p.getData() + " " + p.getInizio()).setValue(null,
				(databaseError, ref) -> {
					if (databaseError != null) {
						// errore --> chiama eccezione anche in RegisterView
						futurePrenotazione.completeExceptionally(new RuntimeException(databaseError.getMessage()));
					} else {
						futurePrenotazione.complete(null);
					}
				});
		return futurePrenotazione; // qui il thenRun() in registrazione capisce che ha finito e prosegue con
		// esecuzione
	}

	/**
	 * Controlla nel database se esiste una prenotazione con la stessa colonnina,
	 * data e orario passato nella funzione.
	 * 
	 * Se non esiste siginifica che lo slot e libero. Crea allora la prenotazione e
	 * completa il future. Se esiste significa che lo slot è occupato e la
	 * prenotazione non può andare a buon fine.
	 * 
	 * @param c    colonnina della prenotazione
	 * @param data della prenotazione
	 * @param ora  della prenotazione
	 * @return future completato o con null (slot occupato, prenotazione non
	 *         possibile) o con p (slot libero, prenotazione possibile)
	 */

	public CompletableFuture<Prenotazione> cercaPrenotazione(Colonnina c, String data, String ora) {
		CompletableFuture<Prenotazione> future = new CompletableFuture<>();

		/*
		 * Verifico l'esistenza di un nodo nel database
		 */
		DatabaseReference userRef = prenotazioni.child(c.getId() + " " + data + " " + ora);
		userRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override

			public void onDataChange(DataSnapshot dataSnapshot) {
				/*
				 * se esiste quel nodo entra nell'if, altrimenti restituisce
				 * future.complete(null)
				 */
				if (dataSnapshot.exists()) {
					// slot occupato, prenotazione non possibile
					Prenotazione p = dataSnapshot.getValue(Prenotazione.class);
					future.complete(p);
				} else {
					// slot libero, prenotazione possibile
					future.complete(null);
				}
			}

			/*
			 * gestione errori di sistema e database, vado nella gestione eccezioni
			 */
			@Override
			public void onCancelled(DatabaseError databaseError) {
				System.err.println("Errore nel database " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}
		});

		return future;
	}

	/**
	 * Restituisce una lista filtrata di prenotazioni in base all'utente che viene
	 * passato. La funzione cerca nel database sotto al nodo prenotazione tutte le
	 * prenotazione che tra le informiazioni hanno come username il nome passato.
	 * Quindi prenotazioni/numprenotazione/utenteUSername deve essere uguale a
	 * username.
	 * 
	 * L'utilizzo di completableFuture permette di lavorare in maniera asincorna e
	 * ottenere un risultato solo dopo aver analizzato tutto il database.
	 * 
	 * @param username da cercare nelle prenotazioni per ottenere la sua lista
	 *                 filtrata
	 * @return lista di prenotazioni tramite un future
	 */
	public CompletableFuture<List<Prenotazione>> getUtenteReservation(String username) {
		CompletableFuture<List<Prenotazione>> future = new CompletableFuture<>();
		// cerco nel nodo prenotazioni i figli che hanno utenteUsername uguale a
		// username
		prenotazioni.orderByChild("utente").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				// creo lista di prenotazioni
				List<Prenotazione> prenotazioni = new ArrayList<>();
				/*
				 * Aggiunge alla lista tutte le prenotazioni il cui utenteUsername corrisponde a
				 * username
				 */
				for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
					Prenotazione p = snapshot.getValue(Prenotazione.class);
					prenotazioni.add(p);
				}

				// finito il for salvo nel future la lista
				future.complete(prenotazioni);

			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// TODO Auto-generated method stub
				System.err.println("Errore nel caricamento colonnine: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());

			}

		});

		// ritorno la lista filtrata delle prenotazioni dell'utente
		return future;
	}

	/**
	 * Permette di ottenere la lista di tutte le prenotazioni presenti nel sistema
	 * Ogni prenotazione presente nel nodo prenotazioni viene aggiunta alla lista
	 * che poi verrà restituita in CompletableFuture in modo asincrono.
	 * 
	 * @return future.complete(prenotazioni) --> restituisce la lista,
	 *         future.complete(eccezione) --> errore
	 */

	public CompletableFuture<List<Prenotazione>> getAllReservation() {
		CompletableFuture<List<Prenotazione>> future = new CompletableFuture<>();
		// cerco nel nodo prenotazioni
		prenotazioni.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				// creo lista di prenotazioni
				List<Prenotazione> prenotazioni = new ArrayList<>();
				for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
					Prenotazione p = snapshot.getValue(Prenotazione.class);
					prenotazioni.add(p);
				}

				// finito il for salvo nel future la lista
				future.complete(prenotazioni);

			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// TODO Auto-generated method stub
				System.err.println("Errore nel caricamento colonnine: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());

			}

		});

		// ritorno la lista delle prenotazioni
		return future;
	}

	public CompletableFuture<List<String>> getColonnineSlot(String ora) {

		// Oggetto che permette al programma di non fermarsi perchè sa che conterrà la
		// lista che sta cercando
		CompletableFuture<List<String>> future = new CompletableFuture<>();

		Query query = prenotazioni.orderByChild("inizio").equalTo(ora);

		query.addListenerForSingleValueEvent(new ValueEventListener() {

			// Se li legge senza problemi
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) { // Istantanea dei dati
				List<String> lista = new ArrayList<>();

				if (dataSnapshot.exists()) {
					for (DataSnapshot snap : dataSnapshot.getChildren()) {
						Prenotazione p = snap.getValue(Prenotazione.class);
						 if (p != null && !lista.contains(p.getNomeColonnina()) && p.getData().equals(LocalDate.now().toString())) {
		                        lista.add(p.getNomeColonnina());
		                    }
					}
				}

				future.complete(lista);
			}

			// Se trova errori
			@Override
			public void onCancelled(DatabaseError databaseError) {
				System.err.println("Errore nel caricamento prenotazioni: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}
		});
		return future;
	}

	/**
	 * Conta il numero delle prenotazioni presenti nel database. Conta i figli del
	 * nodo prenotazioni.
	 * 
	 * @return future.complete(count) --> numero di prenotazioni,
	 *         future.complete(eccezione) --> errore
	 */

	public CompletableFuture<Integer> contaPrenotazioni() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		prenotazioni.addListenerForSingleValueEvent(new ValueEventListener() {

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
	 * Conta il numero delle prenotazioni presenti nel database che hanno timestamp
	 * (inteso come data di creazione prenotazione) ugauale alla data odierna.
	 * 
	 * @return future.complete(count) --> numero di prenotazioni che sono state
	 *         effettuate nella data odierna , future.complete(eccezione) --> errore
	 */

	public CompletableFuture<Integer> contaPrenotazioniNuove() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		prenotazioni.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count = 0;

				if (snapshot.exists()) {
					// Prendo la data di oggi
					LocalDate today = LocalDate.now();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Adatta al formato della
																								// tua stringa
					String todayStr = today.format(formatter);

					for (DataSnapshot prenotazioneSnap : snapshot.getChildren()) {
						String dataPrenotazione = prenotazioneSnap.child("timestamp").getValue(String.class);
						if (todayStr.equals(dataPrenotazione)) {
							count++;
						}
					}
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
