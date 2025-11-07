/**
 * CLasse RegisterValidator utilizzata da RegisterView per vericare la correttezza dei campi inseriti
 * @author Davide Mascheretti
 */
package com.example;

public class RegisterValidator {

	/*
	 * metodo statico (della classe) per controllare la velidità dei campi inseiriti
	 * 1-i campi non possono essere vuoti
	 * 2-le password devono coincidere
	 * 3-la password deve essere lunga almeno 6 caratteri
	 * 4-la mail deve essere del tipo @ e dominio 
	 */
   
    public static String verificaDati(String nome, String cognome, String username,
                                   String email, String password, String password_conferma) {

        if (nome.isEmpty() || cognome.isEmpty() || username.isEmpty() || email.isEmpty()) {
            return "Tutti i campi sono obbligatori!";
        }

        if (!password.equals(password_conferma)) {
            return "Le password non corrispondono!";
        }

        if (password.length() < 6) {
            return "La password deve contenere almeno 6 caratteri!";
        }
        
        //mail del tipo lettere/numeri+@+dominio
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            return "Inserisci un indirizzo email valido. Assicurati che contenga @ e un dominio valido!";
        }

        return null; // Nessun errore
    }
}
