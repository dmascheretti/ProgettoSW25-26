/**
 * Classe oggetto utente
 */
package com.example.models;

import com.example.database.FirebaseService;

public class Utente {
	
	private String nome, cognome, username, email, password, timestamp;

	/*
	 * costruttore necessario per firebase
	 */
	public Utente() {
		
	}
	
	public Utente(String nome, String cognome, String username, String email, String password, String timestamp) {
		// TODO Auto-generated constructor stub
		this.setNome(nome);
		this.setCognome(cognome);
		this.setEmail(email);
		this.setPassword(password);
		this.setUsername(username);
		this.setTimestamp(timestamp);
		
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCognome() {
		return cognome;
	}

	public void setCognome(String cognome) {
		this.cognome = cognome;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

}
