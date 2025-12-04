/**
 * Classe di servizio tra Firebase e le viste
 * @author Davide Mascheretti
 */
package com.example.service;

import com.example.models.Utente;
import com.example.modelsInterface.UtentiInterface;
import com.example.util.DataValidator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.threeten.bp.LocalDate;

import java.util.concurrent.CompletableFuture;

@Service
public class UtentiService {

	private final UtentiInterface utentiInterface;
	private final PasswordEncoder passwordEncoder;

	public UtentiService(UtentiInterface utentiInterface, PasswordEncoder passwordEncoder) {
		this.utentiInterface = utentiInterface;
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
		utentiInterface.verificaUtente(username).thenAccept(utenteTrovato -> {

			// Se utente non trovato invio eccezione
			if (utenteTrovato == null) {
				future.completeExceptionally(new IllegalArgumentException("Utente o password errati."));
				return;
			}

			// booleano che mi identifica se la password inserita corrisponde a quella
			// dell'utente (criptata)
			boolean verificaPassword = passwordEncoder.matches(password, utenteTrovato.getPassword());

			// Se password errata invia eccezione con messaggio diverso
			if (!verificaPassword) {
				future.completeExceptionally(new IllegalArgumentException("Utente o password errati."));
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
		utentiInterface.verificaUtente(username).thenAccept(utenteTrovato -> {

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
			utentiInterface.salvaUtente(u).thenRun(() -> {
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

	public CompletableFuture<Void> cambiaPassword(Utente u, String nuovaPassword) {

		CompletableFuture<Void> future = new CompletableFuture<>();
		if (!DataValidator.controllaPassword(nuovaPassword)) {
		    future.completeExceptionally(new IllegalArgumentException("Password non valida"));
		    return future;
		}
		String passwordCriptata = passwordEncoder.encode(nuovaPassword);

		utentiInterface.cambiaPassword(u, passwordCriptata).thenRun(() -> {
			future.complete(null);
		}).exceptionally(e -> {
			future.completeExceptionally(e);
			return null;
		});

		return future;
	}

	public CompletableFuture<Void> cambiaMail(Utente u, String nuovaMail) {

		CompletableFuture<Void> future = new CompletableFuture<>();

		if (!DataValidator.controllaMail(nuovaMail)) {
		    future.completeExceptionally(new IllegalArgumentException("Email non valida"));
		    return future;
		}

		utentiInterface.cambiaMail(u, nuovaMail).thenRun(() -> {
			future.complete(null);
		}).exceptionally(e -> {
			future.completeExceptionally(e);
			return null;
		});

		return future;
	}

}