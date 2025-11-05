/**
 * Classe FirebaseService svolge tutte le funzioni di controllo utenti, controllo prenotazioni e salvataggio
 * dati all'interno del database
 * @author Davide Mascheretti
 */

package com.example.database;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.example.Utente;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
@Service
public class FirebaseService {
	private final DatabaseReference utenti;
	
	
	/*
     * inizializzazione del nodo del database contentente gli utenti
     */
	
	public FirebaseService() {
		this.utenti=FirebaseDatabase.getInstance().getReference("utenti");
	}
	
	/*
     * Salva utente all'interno del database (con nodo username/infoutente) ,
     * CompletableFuture permette di lavorare 
     * in maniera asincrona in background con la UI standard dell'app
     * se errore invia eccezione
     * @param utente
     */
	
    public CompletableFuture<Void> salvaUtente(Utente utente) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        utenti.child(utente.getUsername()).setValue(utente, (databaseError, ref) -> {
            if (databaseError != null) {
                future.completeExceptionally(new RuntimeException(databaseError.getMessage()));
            } else {
                future.complete(null);
            }
        });
        return future;
    }
	
	
	
}
