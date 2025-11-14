package com.example.admin.components; // Assicurati che il package sia corretto

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Componente UI personalizzato per mostrare un singolo
 * Key Performance Indicator (KPI) in una "card" (un riquadro).
 */
public class KpiCard extends VerticalLayout {

    private H2 numberDisplay = new H2();
    private Span titleDisplay = new Span();

    /**
     * Costruttore per la KpiCard.
     *
     * @param title  Il testo da mostrare come etichetta (es. "Utenti Totali")
     * @param number Il valore numerico da mostrare in grande (es. "142")
     */
    public KpiCard(String title, String number) {
        
        // Imposta i valori
        titleDisplay.setText(title);
        numberDisplay.setText(number);

        // --- Stile ---
        
        // Stile del numero (grande e senza margini)
        numberDisplay.getStyle().set("margin-top", "0");
        numberDisplay.getStyle().set("margin-bottom", "0");

        // Stile del titolo (più piccolo e grigio)
        titleDisplay.getStyle().set("font-size", "var(--lumo-font-size-s)");
        titleDisplay.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Stile del contenitore (la "card")
        setAlignItems(Alignment.CENTER); // Centra il contenuto
        getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        getStyle().set("padding", "var(--lumo-space-m)");
        
        // Imposta una larghezza minima per un layout omogeneo
        setMinWidth("180px"); 

        // Aggiunge i componenti al layout (numero sopra, titolo sotto)
        add(numberDisplay, titleDisplay);
    }

    /**
     * Metodo pubblico per aggiornare il numero dinamicamente 
     * dopo che la card è stata creata.
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