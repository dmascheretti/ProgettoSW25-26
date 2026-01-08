/**
 * Questa classe serve a costruire una KPI (Key Performance Indicator) card.
 * 
 * @author Maistrello Tommaso
 */
package com.example.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
@CssImport("./styles/CSS.css")
public class KpiCard extends VerticalLayout {

	private H2 numberDisplay = new H2();
	private Span titleDisplay = new Span();

	/**
	 * Costruttore per la KpiCard.
	 *
	 * @param title  Etichetta
	 * @param number Valore
	 */
	public KpiCard(String title, String number) {

		// Imposta i valori
		titleDisplay.setText(title);
		numberDisplay.setText(number);
		addClassName("card");
		
		add(numberDisplay, titleDisplay);
	}

	
	/**
	 * Metodo pubblico per aggiornare il numero dinamicamente dopo che la card è
	 * stata creata.
	 *
	 * @param number Il nuovo valore da mostrare
	 */
	public void setNumber(String number) {
		numberDisplay.setText(number);
	}

	/**
	 * Metodo pubblico per aggiornare il titolo dinamicamente.
	 *
	 * @param title Il nuovo titolo da mostrare
	 */
	public void setTitle(String title) {
		titleDisplay.setText(title);
	}
}