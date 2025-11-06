/**
 * Classe FirebaseService svolge tutte le funzioni di controllo utenti, controllo prenotazioni e salvataggio
 * dati all'interno del database
 * Tutte le funzioni lavorano in maniera asincrona usando CompletableFuture
 * @author Davide Mascheretti
 */

package com.example.database;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.Utente;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
@Service
public class FirebaseService {
	private final DatabaseReference utenti;
	
	
	/**
     * Inizializzazione del riferimento al nodo "utenti" del database
     */
	
	public FirebaseService() {
		this.utenti=FirebaseDatabase.getInstance().getReference("utenti");
	}
	
	/**
     * Salva utente all'interno del database (con nodo username/info utente) ,
     * CompletableFuture permette di lavorare 
     * in maniera asincrona in background con la UI standard dell'app
     * 
     * se future.complete(null) --> tutto ok
     * se future.complete(!=null) -->  errore invia eccezione
     * 
     * @param utente utente da salvare nel db
     * @return future funzione terminata, con o senza eccezioni
     */
	
    public CompletableFuture<Void> salvaUtente(Utente utente) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        utenti.child(utente.getUsername()).setValue(utente, (databaseError, ref) -> {
            if (databaseError != null) {
            	//errore --> chiama eccezione anche in RegisterView
                future.completeExceptionally(new RuntimeException(databaseError.getMessage()));
            } else {
                future.complete(null);
            }
        });
        return future; //qui il thenRun() in registrazione capisce che ha finito e prosegue con esecuzione
    }
 
    /**
     * 
     * cerca nodo con nome pari a username, cerca utente con quello username
     * e verifica password e user corrette
     * 
     * restituisce un CompletableFuture che puo essere nullo o non nullo (con utente)
     * 
     * @param username username da cercare
     * @param password password da verificare
     * @return future contiene utente se trovato, altrimenti null
     */
    
    public CompletableFuture<Utente> cercaUtente(String username, String password) {
        CompletableFuture<Utente> future = new CompletableFuture<>();

        /* Cerca direttamente il nodo utente usando l'username come chiave
         * aggiornando la lettura con il listener
         */
        DatabaseReference userRef = utenti.child(username);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override

            public void onDataChange(DataSnapshot dataSnapshot) {
            	/*
            	 * se esiste quel nodo entra nell'if, altrimenti restituisce future.complete(null)
            	 * restituisce future.complete(null) anche se la password non corrisponde all'utente
            	 * inserito
            	 */
                if (dataSnapshot.exists()) {
                    Utente utente = dataSnapshot.getValue(Utente.class);
                    if (utente != null && utente.getPassword() != null && utente.getPassword().equals(password)) {
                        //tutto corretto
                        future.complete(utente);
                    } else {
                        //password errata
                        future.complete(null);
                    }
                } else {
                    //utente non trovato
                    future.complete(null);
                }
            }

            /*
             * gestione errori di sistema e database, vado nella gestione eccezioni in LoginView
             */
            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.err.println("Errore nel database " + databaseError.getMessage());
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }
	
	
}
