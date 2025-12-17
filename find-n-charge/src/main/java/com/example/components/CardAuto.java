/**
 * Questa classe serve a costruire una KPI (Key Performance Indicator) card.
 * 
 * @author Maistrello Tommaso
 */
package com.example.components;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class CardAuto extends VerticalLayout {

	private H3 modelloAuto = new H3();
	private Span targaAuto = new Span();
	private Span carica = new Span();
	private Double caricaAuto; 

	/**
	 * Costruttore per la KpiCard.
	 *
	 * @param title  Etichetta
	 * @param number Valore
	 */
	public CardAuto(String modello, String targa, double statoCarica) {

		this.caricaAuto=statoCarica;
		modelloAuto.setText(modello);
		targaAuto.setText("Targa: " + targa);
		updateStato(statoCarica);
		
		
		
		modelloAuto.getStyle().set("margin-bottom", "0");

		setAlignItems(Alignment.CENTER); // Centra il contenuto
		setPadding(true);
		setSpacing(false);
		getStyle().set("text-align", "center");
		getStyle().set("border", "1px solid #e0e0e0");
		getStyle().set("border-radius", "12px");
		getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.10)");
		getStyle().set("background-color", "white");
		
		//setMinWidth("150px");
		
		add(modelloAuto, targaAuto, carica);
	}

	/**
	 * Metodo pubblico per aggiornare il titolo dinamicamente.
	 *
	 * @param modello Il nuovo titolo da mostrare
	 */
	public void setModelloAuto(String modello) {
		modelloAuto.setText(modello);
	}
	
	/**
	 * Metodo pubblico per aggiornare la targa dinamicamente dopo che la card è
	 * stata creata.
	 *
	 * @param targa Il nuovo valore da mostrare
	 */
	public void setTarga(String targa) {
		targaAuto.setText(targa);
	}

	/**
	 * Metodo pubblico per aggiornare la carica
	 *
	 * @param carica Il nuovo valore da mostrare
	 */
	public void setCarica(Double caricaNuova) {
		caricaAuto = caricaNuova;
	}
	public void updateStato(double nuovoStato) {
		
		int percentualeArrotondata = (int) Math.ceil(nuovoStato);
		
		if (percentualeArrotondata > 100) {
	        percentualeArrotondata = 100;
	      }
		
		this.caricaAuto = (double) percentualeArrotondata;
		
	if (nuovoStato <= 30) {
		carica.getStyle().set("color", "#FF3B30").set("font-weight", "bold");
		carica.setText("Carica residua: " + percentualeArrotondata + "%  ⚠ Mettere in carica");
	} else if (nuovoStato <= 55) {
		carica.getStyle().set("color", "#F7DC6F").set("font-weight", "bold");
		carica.setText("Carica residua: " + percentualeArrotondata + "%");
	} else {
		carica.getStyle().set("color", "#27AE60").set("font-weight", "bold");
		carica.setText("Carica residua: " + percentualeArrotondata + "%");
	}
	}
	
}