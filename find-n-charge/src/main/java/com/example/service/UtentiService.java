/**
 * Classe di servizio tra Firebase e le viste
 * @author Davide Mascheretti
 */
package com.example.service;

import com.example.database.FirebaseUtentiService;
import com.example.models.Utente;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.threeten.bp.LocalDate;

import java.util.concurrent.CompletableFuture;

@Service
public class UtentiService {

	private final FirebaseUtentiService firebaseUtentiService;
	private final PasswordEncoder passwordEncoder;

	public UtentiService(FirebaseUtentiService firebaseUtentiService, PasswordEncoder passwordEncoder) {
		this.firebaseUtentiService = firebaseUtentiService;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Gestisce verifica dati inseriti comunicando con il database
	 * 
	 * @param username Username di cui verificare esistenza
	 * @param password Password da verificare con Username
	 * @return
	 */
	public CompletableFuture<Utente> login(String username, String password) {

		CompletableFuture<Utente> future = new CompletableFuture<>();
		// Chiamo funzione di firebase
		firebaseUtentiService.verificaUtente(username).thenAccept(utenteTrovato -> {

			// Se utente non trovato invio eccezione
			if (utenteTrovato == null) {
				future.completeExceptionally(new IllegalArgumentException("Questo username non esiste"));
				return;
			}

			// booleano che mi identifica se la password inserita corrisponde a quella
			// dell'utente (criptata)
			boolean verificaPassword = passwordEncoder.matches(password, utenteTrovato.getPassword());

			// Se password errata invia eccezione con messaggio diverso
			if (!verificaPassword) {
				future.completeExceptionally(new IllegalArgumentException("Password errata"));
				return;
			}

			// completo con utente
			future.complete(utenteTrovato);

			// Gestione eccezioni in firebase
		}).exceptionally(e -> {
			future.completeExceptionally(e);
			return null;
		});

		return future;
	}

	/**
	 * Gestisce registrazione e dati inseriti
	 * 
	 * @param nome     Nome utente
	 * @param cognome  Cognome utente
	 * @param username Username utente
	 * @param email    Email utente
	 * @param password Password utente in chiaro
	 * @return
	 */
	public CompletableFuture<Void> registrati(String nome, String cognome, String username, String email,
			String password) {

		CompletableFuture<Void> future = new CompletableFuture<>();
		firebaseUtentiService.verificaUtente(username).thenAccept(utenteTrovato -> {

			// Utente già trovato con questo username, invio eccezione
			if (utenteTrovato != null) {
				future.completeExceptionally(new IllegalArgumentException("Username già esistente"));
				return;
			}

			// Cripto password da salvare
			String passwordCriptata = passwordEncoder.encode(password);

			// Oggetto utente locale
			Utente u = new Utente(nome, cognome, username, email, passwordCriptata, LocalDate.now().toString(),
					"Utente");

			// Chiamo firebase per salvare utente nel database
			firebaseUtentiService.salvaUtente(u).thenRun(() -> {
				future.complete(null);
				// eccezione di salvaUtente
			}).exceptionally(ex -> {

				future.completeExceptionally(ex);
				return null;
			});

			// eccezione di verificaUtente
		}).exceptionally(ex -> {

			future.completeExceptionally(new RuntimeException("Errore"));
			return null;
		});

		// null se tutto è andato a buon fine
		return future;
	}

}