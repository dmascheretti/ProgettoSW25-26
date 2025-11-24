/**
 * Classe per la modellizzazione dell'oggetto Colonnina: costruttore, getters e setters
 * 
 * @author Tommaso Maistrello
 */

package com.example.models;

public class Colonnina {

	private String id;
	private String nome;
	private String tipo;
	private String stato;
	private double latitudine;
	private double longitudine;
	private String indirizzo;
	private String comune;
	private String linkImmagine;
	private double potenza;

	public Colonnina() {
	}
	
	public Colonnina(String id, String nome, String tipo, double latitudine, double longitudine, String indirizzo, String comune, double potenza) {
		this.id=id;
		this.nome=nome;
		this.tipo=tipo;
		this.stato="Libera";
		this.latitudine=latitudine;
		this.longitudine=longitudine;
		this.indirizzo=indirizzo;
		this.comune=comune;
		this.linkImmagine=linkImmagine;
		this.setPotenza(potenza);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getStato() {
		return stato;
	}

	public void setStato(String stato) {
		this.stato = stato;
	}

	public double getLatitudine() {
		return latitudine;
	}

	public void setLatitudine(double latitudine) {
		this.latitudine = latitudine;
	}

	public double getLongitudine() {
		return longitudine;
	}

	public void setLongitudine(double longitudine) {
		this.longitudine = longitudine;
	}

	public String getIndirizzo() {
		return indirizzo;
	}

	public void setIndirizzo(String indirizzo) {
		this.indirizzo = indirizzo;
	}

	public String getComune() {
		return comune;
	}

	public void setComune(String comune) {
		this.comune = comune;
	}

	public String getLinkImmagine() {
		return linkImmagine;
	}

	public double getPotenza() {
		return potenza;
	}

	public void setPotenza(double potenza) {
		this.potenza = potenza;
	}
}