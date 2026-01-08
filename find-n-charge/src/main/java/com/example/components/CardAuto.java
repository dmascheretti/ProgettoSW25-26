/**
 * Questa classe serve a costruire una KPI (Key Performance Indicator) card.
 * 
 * @author Maistrello Tommaso
 */
package com.example.components;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@CssImport("./styles/CSS.css")
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
		
		addClassName("card");
		
		modelloAuto.getStyle().set("margin-bottom", "0");

		
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
		carica.setClassName("charge-status charge-low");
		carica.setText("Carica residua: " + percentualeArrotondata + "%  ⚠ Mettere in carica");
	} else if (nuovoStato <= 55) {
		carica.setClassName("charge-status charge-medium");
		carica.setText("Carica residua: " + percentualeArrotondata + "%");
	} else {
		carica.setClassName("charge-status charge-high");
		carica.setText("Carica residua: " + percentualeArrotondata + "%");
	}
	}
	
}