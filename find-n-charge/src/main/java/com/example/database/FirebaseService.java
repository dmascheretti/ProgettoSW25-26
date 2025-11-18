/**
 * Classe FirebaseService svolge tutte le funzioni di controllo utenti, controllo prenotazioni e salvataggio
 * dati all'interno del database
 * Tutte le funzioni lavorano in maniera asincrona usando CompletableFuture
 * @author Davide Mascheretti, Tommaso Maistrello, Francesco Valenari
 */

package com.example.database;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.storage.model.Notification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.vaadin.flow.component.Text;

@Service
public class FirebaseService {
	private final DatabaseReference utenti;
	private final DatabaseReference colonnine;
	private final DatabaseReference prenotazioni;

	/**
	 * Inizializzazione del riferimento al nodo "utenti", "colonnine" e
	 * "prenotazioni" del database
	 */

	public FirebaseService() {
		this.utenti = FirebaseDatabase.getInstance().getReference("utenti");
		this.colonnine = FirebaseDatabase.getInstance().getReference("colonnine");
		this.prenotazioni = FirebaseDatabase.getInstance().getReference("prenotazioni");

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
		 *Cerco in "utenti" se esiste un figlio con chiave username
		 */
		DatabaseReference userRef = utenti.child(username);
		userRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override

			public void onDataChange(DataSnapshot dataSnapshot) {
				/*
				 *Se esiste quel nodo entra nell'if, altrimenti restituisce
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
		DatabaseReference userRef = prenotazioni.child(c.getNome() + " " + data + " " + ora);
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
	public CompletableFuture<List<Prenotazione>> getAllReservation(String username) {
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
	 * Metodo per recuperare in modo asincrono l'elenco di tutti le colonnine del
	 * database. Per ogni colonnina letta, la chiave del nodo viene impostato come
	 * identificativo dell'oggetto.
	 *
	 * @return se tutto avviene correttamente ritorna l'elenco di tutte le
	 *         colonnine. Se il nodo "colonnine" è vuoto o non esiste, la lista sarà
	 *         vuota. Se avviene un errore durante la lettura del db, partirà
	 *         un'eccezione.
	 */
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

	public CompletableFuture<List<Colonnina>> cercaColonnine(String query) {
		String filtro = query == null ? "" : query.toLowerCase();

		return getAllColonnine().thenApply(lista -> lista.stream().filter(c -> {
			String nome = c.getNome() != null ? c.getNome().toLowerCase() : "";
			String indirizzo = c.getIndirizzo() != null ? c.getIndirizzo().toLowerCase() : "";
			String comune = c.getComune() != null ? c.getComune().toLowerCase() : "";
			return nome.contains(filtro) || indirizzo.contains(filtro) || comune.contains(filtro);
		}).collect(Collectors.toList()));
	}

	public CompletableFuture<Integer> contaUtenti() {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		
		utenti.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count=0;
				
				if(snapshot.exists()) {
					count=(int) snapshot.getChildrenCount();
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
	
	public CompletableFuture<Integer> contaColonnine() {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		
		colonnine.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count=0;
				
				if(snapshot.exists()) {
					count=(int) snapshot.getChildrenCount();
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
	
	public CompletableFuture<Integer> contaColonnineLG(String msg) {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		
		Query colonnineCount = colonnine.orderByChild("stato").equalTo(msg);
		colonnineCount.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count=0;
				
				if(snapshot.exists()) {
					count=(int) snapshot.getChildrenCount();
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
	
	
	public CompletableFuture<Integer> contaPrenotazioni() {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		
		prenotazioni.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count=0;
				
				if(snapshot.exists()) {
					count=(int) snapshot.getChildrenCount();
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

	public CompletableFuture<Integer> contaPrenotazioniNuove() {
			CompletableFuture<Integer> future = new CompletableFuture<>();
			
			prenotazioni.addListenerForSingleValueEvent(new ValueEventListener() {

				@Override
				public void onDataChange(DataSnapshot snapshot) {
					// TODO Auto-generated method stub
					int count=0;
					
					if (snapshot.exists()) {
		                // Prendo la data di oggi
		                LocalDate today = LocalDate.now();
		                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Adatta al formato della tua stringa
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
	
	
	public CompletableFuture<Integer> contaUtentiNuovi() {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		
		utenti.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {
				// TODO Auto-generated method stub
				int count=0;
				
				if (snapshot.exists()) {
	                // Prendo la data di oggi
	                LocalDate today = LocalDate.now();
	                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Adatta al formato della tua stringa
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
						if (u != null) {
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
	
	public CompletableFuture<Void> cancellaUtente(Utente u) {

		CompletableFuture<Void> futureUtente = new CompletableFuture<>();
		
		utenti.child(u.getUsername()).setValue(null,
				(databaseError, ref) -> {
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
	

	
	
}