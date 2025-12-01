package com.example.database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

@Service
public class FirebaseUtentiService {
	private final DatabaseReference utenti;
	private final DatabaseReference prenotazioni;
	private FirebasePrenotazioniService firebasePrenotazioniService;

	/**
	 * Inizializzazione del riferimento al nodo "utenti", "colonnine" e
	 * "prenotazioni" del database
	 */

	public FirebaseUtentiService(FirebasePrenotazioniService firebasePrenotazioniService) {
		this.utenti = FirebaseDatabase.getInstance().getReference("utenti");
		this.prenotazioni = FirebaseDatabase.getInstance().getReference("prenotazioni");
		this.firebasePrenotazioniService=firebasePrenotazioniService;

	}

	/**
	 * Salva utente all'interno del database (con nodo username/info utente) ,
	 * CompletableFuture permette di lavorare in maniera asincrona in background con
	 * la UI standard dell'app
	 * 
	 * se futureUtente.complete(null) --> tutto ok se futureUtente.complete(!=null)
	 * --> errore invia eccezione
	 * 
	 * @param utente Utente da salvare nel db
	 * @return futureUtente Funzione terminata, con o senza eccezioni
	 */

	public CompletableFuture<Void> salvaUtente(Utente utente) {
		CompletableFuture<Void> futureUtente = new CompletableFuture<>();
		utenti.child(utente.getUsername()).setValue(utente, (databaseError, ref) -> {
			if (databaseError != null) {
				// errore --> chiama eccezione anche in RegisterView
				futureUtente.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				futureUtente.complete(null);
			}
		});
		return futureUtente; // qui il thenRun() in registrazione capisce che ha finito e prosegue con
		// esecuzione
	}

	/**
	 * Permette all'utente di cambiare la password
	 * 
	 * @param u        Utente loggato
	 * @param password
	 * @return
	 */

	public CompletableFuture<Void> cambiaPassword(Utente u, String nuovaPassword) {
		CompletableFuture<Void> cambio = new CompletableFuture<>();

		if (nuovaPassword == null || nuovaPassword.length() < 6) {
			cambio.completeExceptionally(
					new IllegalArgumentException("La password deve essere lunga almeno 6 caratteri"));
			return cambio;
		}

		utenti.child(u.getUsername()).child("password").setValue(nuovaPassword, (databaseError, ref) -> {

			if (databaseError != null) {
				cambio.completeExceptionally(new RuntimeException(databaseError.getMessage()));

			}

			else {
				cambio.complete(null);
			}
		});
		return cambio;
	}

	/**
	 * Permette all'utente di cambiare la mail
	 * 
	 * @param u        Utente loggato
	 * @param password
	 * @return
	 */

	public CompletableFuture<Void> cambiaMail(Utente u, String nuovaMail) {

		CompletableFuture<Void> cambio = new CompletableFuture<>();

		if (!nuovaMail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
			cambio.completeExceptionally(new IllegalArgumentException("La mail non è valida"));
			return cambio;
		}

		utenti.child(u.getUsername()).child("mail").setValue(nuovaMail, (databaseError, ref) -> {

			if (databaseError != null) {
				// errore --> chiama eccezione anche in RegisterView
				cambio.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				cambio.complete(null);
			}
		});
		return cambio;
	}

	/**
	 * 
	 * Cerca sottonodo di "utenti" con nome uguale a username passato nella funzione
	 * 
	 * Richiama utente con quello username e verifica che la passowrd inserita sia
	 * corretta
	 * 
	 * Restituisce un CompletableFuture che puo essere nullo o non nullo (con
	 * utente)
	 * 
	 * @param username username da cercare
	 * @param password password da verificare
	 * @return future contiene utente se trovato, altrimenti null
	 */

	public CompletableFuture<Utente> cercaUtente(String username, String password) {
		CompletableFuture<Utente> future = new CompletableFuture<>();

		/*
		 * Cerco in "utenti" se esiste un figlio con chiave username
		 */
		DatabaseReference userRef = utenti.child(username);
		userRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override

			public void onDataChange(DataSnapshot dataSnapshot) {
				/*
				 * Se esiste quel nodo entra nell'if, altrimenti restituisce
				 * future.complete(null) restituisce future.complete(null) anche se la password
				 * non corrisponde all'utente inserito
				 */
				if (dataSnapshot.exists()) {
					Utente utente = dataSnapshot.getValue(Utente.class);
					if (utente != null && utente.getPassword() != null && utente.getPassword().equals(password)) {
						// tutto corretto
						future.complete(utente);
					} else {
						// password errata
						future.complete(null);
					}
				} else {
					// utente non trovato
					future.complete(null);
				}
			}

			/*
			 * gestione errori di sistema e database, vado nella gestione eccezioni in
			 * LoginView
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
	 * Verifica se esiste già un utente con lo stesso username, cercando nel
	 * database utenti/username. Se esiste restituisce quell'utente, se non esiste
	 * restituisce null e si può completare la registrazione
	 * 
	 * @param username da cercare nel db
	 * @return future che può contenere o non contenere un utente
	 */

	public CompletableFuture<Utente> verificaUtente(String username) {
		CompletableFuture<Utente> future = new CompletableFuture<>();

		/*
		 * Cerca direttamente il nodo utente usando l'username come chiave aggiornando
		 * la lettura con il listener
		 */
		DatabaseReference userRef = utenti.child(username);
		userRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override

			public void onDataChange(DataSnapshot dataSnapshot) {
				/*
				 * se esiste quel nodo entra nell'if, altrimenti restituisce
				 * future.complete(null) utente trovato --> future.complete(utente) -->
				 * registrazione non possibile utente non trovato --> future.complete(null) -->
				 * registrazione possibile
				 */
				if (dataSnapshot.exists()) {
					Utente utente = dataSnapshot.getValue(Utente.class);
					future.complete(utente);
				} else {
					// utente non trovato
					future.complete(null);
				}
			}

			/*
			 * gestione errori di sistema e database, vado nella gestione eccezioni in
			 * LoginView
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
	 * Cancella un utente dal database e insieme anche tutte le sue prenotazioni.
	 * @param u Utente da eliminare dal sistema
	 * @return future di tipo void
	 */
	public CompletableFuture<Void> cancellaUtente(Utente u) {

	    CompletableFuture<Void> futureUtente = new CompletableFuture<>();

	    // Cancella l'utente
	    utenti.child(u.getUsername()).setValue(null, (databaseError, ref) -> {

	        if (databaseError != null) {
	            futureUtente.completeExceptionally(
	                    new RuntimeException(databaseError.getMessage()));
	            return;
	        }

	        //ottengo tutte le prenotaazioni dell'utente eliminato
	        getUtenteReservation(u.getUsername()).thenAccept(lista -> {

	            List<CompletableFuture<Void>> prenotazioniUtente = new ArrayList<>();
	            //per ogni prenotazione presente in lista
	            for (Prenotazione p : lista) {
	                
	            	//cancella la prenotazione e aggiungi la funzione alla lista
	                CompletableFuture<Void> pren = firebasePrenotazioniService.cancellaPrenotazione(p)
	                        .thenRun(()->{});
	                prenotazioniUtente.add(pren);
	            }

	            // futureUtente termina solo quando sono tutte le funzioni di cancellazione sono state completate
	            CompletableFuture.allOf(prenotazioniUtente.toArray(new CompletableFuture[0]))
	                    .thenRun(() -> futureUtente.complete(null))
	                    .exceptionally(ex -> {
	                        futureUtente.completeExceptionally(ex);
	                        return null;
	                    });

	        }).exceptionally(ex -> {
	            futureUtente.completeExceptionally(ex);
	            return null;
	        });
	    });

	    return futureUtente;
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
	 * Conta il numero degli utenti presenti nel database. Conta i figli del nodo
	 * utenti.
	 * 
	 * @return future.complete(count) --> numero di utenti,
	 *         future.complete(eccezione) --> errore
	 */

	public CompletableFuture<Integer> contaUtenti() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		utenti.addListenerForSingleValueEvent(new ValueEventListener() {

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
				System.err.println("Errore nel contare gli utenti: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}

		});
		return future;

	}

	/**
	 * Conta il numero degli utenti presenti nel database che hanno data di
	 * iscrizione uguale alla data odierna
	 * 
	 * @return future.complete(count) --> numero di utenti che si sono registrati
	 *         nella data odierna , future.complete(eccezione) --> errore
	 */

	public CompletableFuture<Integer> contaUtentiNuovi() {
		CompletableFuture<Integer> future = new CompletableFuture<>();

		utenti.addListenerForSingleValueEvent(new ValueEventListener() {

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

					// Itero su tutte le prenotazioni
					for (DataSnapshot utenteSnap : snapshot.getChildren()) {
						String data = utenteSnap.child("timestamp").getValue(String.class);
						if (todayStr.equals(data)) {
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

	/**
	 * Permette di ottenere la lista di tutti gli utenti presenti nel sistema
	 * 
	 * @return future.complete(listaUtenti) --> restituisce la lista,
	 *         future.complete(eccezione) --> errore
	 */

	public CompletableFuture<List<Utente>> getAllUtenti() {

		// Oggetto che permette al programma di non fermarsi perchè sa che conterrà la
		// lista che sta cercando
		CompletableFuture<List<Utente>> future = new CompletableFuture<>();

		// Legge il nodo utenti
		utenti.addListenerForSingleValueEvent(new ValueEventListener() {

			// Se li legge senza problemi
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				List<Utente> listaUtenti = new ArrayList<>();
				if (dataSnapshot.exists()) {
					for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
						Utente u = snapshot.getValue(Utente.class); // Acquisisce tutti i dati degli
																	// utenti
						if (u != null && u.getRuolo()!=null && u.getRuolo().equals("Utente")) {
							listaUtenti.add(u);
						}
					}
				}
				future.complete(listaUtenti); // Restituisce la lista (piena o vuota)
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

}
