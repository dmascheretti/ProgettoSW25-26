package com.example.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

class DataValidatorTest {

	/*
	 * Dati ok
	 */
	@Test
	void testVerificaDati_Successo() {

		String result = DataValidator.verificaDati("Mario", "Rossi", "mariorossi", "mario@example.com", "Password123!",
				"Password123!");
		assertNull(result);
	}

	/*
	 * Dati no ok
	 */
	@Test
	void testVerificaDati_CampiErrati() {
		String result = DataValidator.verificaDati("", "Rossi", "user", "a@b.c", "P1!", "P1!");
		assertEquals("Tutti i campi sono obbligatori!", result);

		result = DataValidator.verificaDati("Mario", "", "user", "a@b.c", "P1!", "P1!");
		assertEquals("Tutti i campi sono obbligatori!", result);
		
		result = DataValidator.verificaDati("Mario", "Rossi", "mario.rossi", "a@b.c", "P1!", "P1!");
		assertEquals("L'username non è valido! Non utilizzare simboli speciali: sono possibili solo trattini (-) e underscore (_)! ", result);
	}

	@Test
	void testVerificaDati_PasswordNonCoincidenti() {
		String result = DataValidator.verificaDati("Mario", "Rossi", "user", "mail@test.com", "Password1!",
				"Password1");
		assertEquals("Le password non corrispondono!", result);
	}

	@Test
	void testVerificaDati_PasswordDebole() {
		String atteso = "La password deve avere almeno 8 caratteri, una maiuscola, una minuscola, un numero e un carattere speciale (@ # $ % ^ & + = ! . _ -)";

		String result = DataValidator.verificaDati("M", "R", "u", "a@b.c", "P1!a", "P1!a");
		assertEquals(atteso, result);

		result = DataValidator.verificaDati("M", "R", "u", "a@b.c", "password123!", "password123!");
		assertEquals(atteso, result);

		result = DataValidator.verificaDati("M", "R", "u", "a@b.c", "Password!", "Password!");
		assertEquals(atteso, result);

		result = DataValidator.verificaDati("M", "R", "u", "a@b.c", "Password123", "Password123");
		assertEquals(atteso, result);
	}

	/*
	 * Prenotazione ok
	 */
	void testVerificaPrenotazione_Successo() {

		String risultato = DataValidator.verificaPrenotazione("Colonnina", LocalDate.now(), "16:30");
		assertNull(risultato);

	}
	
	/*
	 * Prenotazione no ok
	 */
	@Test
	void testVerificaPrenotazione_Errore() {

		String result = DataValidator.verificaPrenotazione("Colonnina", null, "13:30");
		assertEquals("Seleziona una data prima di prenotare.", result);

		result = DataValidator.verificaPrenotazione("Colonnina", LocalDate.now(), "");
		assertEquals("Seleziona un orario.", result);

		result = DataValidator.verificaPrenotazione("Colonnina", LocalDate.now(), null);
		assertEquals("Seleziona un orario.", result);

		result = DataValidator.verificaPrenotazione(null, LocalDate.now(), "13:30");
		assertEquals("Errore: Nessuna colonnina selezionata.", result);
	}
	/*
	 * Mail ok
	 */
	@Test
	void testVerificaMail_Successo() {

		boolean risultato = DataValidator.controllaMail("scrum.master@gmail.com");
		assertEquals(true, risultato);

	}
	
	/*
	 * Errori insrimento mail
	 */
	@Test
	void testVerificaMail_Errore() {

		
		boolean risultato = DataValidator.controllaMail("scrum.mastergmail.com");
		assertEquals(false, risultato);
		
		risultato = DataValidator.controllaMail(null);
		assertEquals(false, risultato);

	}

}