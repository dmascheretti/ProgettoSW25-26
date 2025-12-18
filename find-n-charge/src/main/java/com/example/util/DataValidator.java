/**
 * CLasse RegisterValidator utilizzata per vericare la correttezza dei campi inseriti
 * @author Davide Mascheretti
 */
package com.example.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DataValidator {

	/**
	 * Metodo statico (della classe) per controllare la velidità dei campi inseiriti
	 * 1-i campi non possono essere vuoti 2-le password devono coincidere 3-la
	 * password deve essere lunga almeno 6 caratteri 4-la mail deve essere del
	 * tipo @ e dominio
	 * 
	 * @param nome
	 * @param cognome
	 * @param username
	 * @param email
	 * @param password
	 * @param password_conferma
	 * @return messaggio di errore / null
	 */
	
	private static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,}$";
	
	public static String verificaDati(String nome, String cognome, String username, String email, String password,
			String password_conferma) {

		if (nome.isEmpty() || cognome.isEmpty() || username.isEmpty() || email.isEmpty()) {
			return "Tutti i campi sono obbligatori!";
		}

		if (!password.equals(password_conferma)) {
			return "Le password non corrispondono!";
		}
		
		if (!password.matches(PASSWORD_REGEX)) {
            return "La password deve avere almeno 8 caratteri, una maiuscola, una minuscola, un numero e un carattere speciale (@ # $ % ^ & + = ! . _ -)";
        }


		// mail del tipo lettere/numeri+@+dominio
		if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
			return "Inserisci un indirizzo email valido. Assicurati che contenga @ e un dominio valido!";
		}

		return null; // Nessun errore
	}

	/**
	 * 
	 * Metodo statico per verificare i dati inseriti nel campo della prenotazione
	 * (non devono essere nulli o vuoti)
	 * 
	 * @param c colonnina
	 * @param d data
	 * @param t orario
	 * @return
	 */

	public static String verificaPrenotazione(String c, LocalDate d, String t) {

		if (d == null) {

			return "Seleziona una data prima di prenotare.";
		}

		if (t == null || t.isEmpty()) {

			return "Seleziona un orario.";
		}
		if (c == null) {

			return "Errore: Nessuna colonnina selezionata.";
		}

		return null;
	}
	
	public static String getSlotCorrenteTimestamp() {
	    LocalDateTime now = LocalDateTime.now();
	    int minuteSlot = (now.getMinute() / 30) * 30;

	    LocalDateTime slot = LocalDateTime.of(
	            now.getYear(), now.getMonth(), now.getDayOfMonth(),
	            now.getHour(), minuteSlot
	    );

	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
	    return slot.format(formatter);
	}
	
	public static boolean controllaPassword(String password) {
		return password != null && password.matches(PASSWORD_REGEX);
	}
																					
	public static boolean controllaMail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
