/**
 * Questa classe serve a costruire una KPI (Key Performance Indicator) card.
 * 
 * @author Maistrello Tommaso
 */
package com.example.components;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

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

		numberDisplay.getStyle().set("margin-top", "0");
		numberDisplay.getStyle().set("margin-bottom", "0");

		titleDisplay.getStyle().set("font-size", "var(--lumo-font-size-s)");
		titleDisplay.getStyle().set("color", "var(--lumo-secondary-text-color)");

		setAlignItems(Alignment.CENTER); // Centra il contenuto
		setPadding(true);
		setSpacing(false);
		getStyle().set("border", "1px solid #e0e0e0");
		getStyle().set("border-radius", "12px");
		getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.10)");
		getStyle().set("background-color", "white");
		
		// Imposta una larghezza minima per un layout omogeneo
		setMinWidth("180px");

		
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