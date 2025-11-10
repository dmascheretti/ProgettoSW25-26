/**
 * Classe FirebaseService svolge tutte le funzioni di controllo utenti, controllo prenotazioni e salvataggio
 * dati all'interno del database
 * Tutte le funzioni lavorano in maniera asincrona usando CompletableFuture
 * @author Davide Mascheretti, Tommaso Maistrello
 */

package com.example.database;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import java.util.ArrayList;
import java.util.List;

import com.google.api.services.storage.model.Notification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vaadin.flow.component.Text;

@Service
public class FirebaseService {
	private final DatabaseReference utenti;
	private final DatabaseReference colonnine;
	private final DatabaseReference prenotazioni;

	/**
	 * Inizializzazione del riferimento al nodo "utenti" del database
	 */

	public FirebaseService() {
		this.utenti = FirebaseDatabase.getInstance().getReference("utenti");
		this.colonnine = FirebaseDatabase.getInstance().getReference("colonnine");
		this.prenotazioni= FirebaseDatabase.getInstance().getReference("prenotazioni");
		
	}

	/**
	 * Salva utente all'interno del database (con nodo username/info utente) ,
	 * CompletableFuture permette di lavorare in maniera asincrona in background con
	 * la UI standard dell'app
	 * 
	 * se future.complete(null) --> tutto ok se future.complete(!=null) --> errore
	 * invia eccezione
	 * 
	 * @param utente Utente da salvare nel db
	 * @return future Funzione terminata, con o senza eccezioni
	 */

	public CompletableFuture<Void> salvaUtente(Utente utente) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		utenti.child(utente.getUsername()).setValue(utente, (databaseError, ref) -> {
			if (databaseError != null) {
				// errore --> chiama eccezione anche in RegisterView
				future.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				future.complete(null);
			}
		});
		return future; // qui il thenRun() in registrazione capisce che ha finito e prosegue con
						// esecuzione
	}

	/**
	 * 
	 * cerca nodo con nome pari a username, cerca utente con quello username e
	 * verifica password e user corrette
	 * 
	 * restituisce un CompletableFuture che puo essere nullo o non nullo (con
	 * utente)
	 * 
	 * @param username username da cercare
	 * @param password password da verificare
	 * @return future contiene utente se trovato, altrimenti null
	 */

	public CompletableFuture<Utente> cercaUtente(String username, String password) {
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
	 * Verifica se esiste già un utente con lo stesso username, cercando nel database
	 * utenti/username Se esiste restituisce quell'utente, se non esiste restituisce
	 * null e si può completare la registrazione
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
	
	public CompletableFuture<List<Prenotazione>> getAllReservation(String username){
		CompletableFuture<List<Prenotazione>> future = new CompletableFuture<>();
        prenotazioni.orderByChild("utenteUsername").equalTo(username)
        .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Prenotazione> prenotazioni = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Prenotazione p = snapshot.getValue(Prenotazione.class);
                    prenotazioni.add(p);
                }
               
                future.complete(prenotazioni);
                
            }

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// TODO Auto-generated method stub
				System.err.println("Errore nel caricamento colonnine: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
				
			}

			
			
        });
        return future;
        }

}
