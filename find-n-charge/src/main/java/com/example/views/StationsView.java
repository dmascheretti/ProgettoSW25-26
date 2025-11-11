/**
 * La classe StationView gestisce la pagina della lista delle colonnine disponibili
 * 
 * @author Francesco Valenari
 */
package com.example.views;

import com.example.MainLayout;
import com.example.database.FirebaseService;
import com.example.models.Colonnina;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Find&Charge - Colonnine")
@Route(value = "colonnine", layout = MainLayout.class)

public class StationsView extends VerticalLayout {
private Grid<Colonnina> colonGrid = new Grid<>(Colonnina.class);
private FirebaseService firebaseService = new FirebaseService(); // è un'istanza di FirebaseSystem

	// Classe per l'elenco delle colonnine (indipendente dalla mappa)
	public StationsView() {
		setSpacing(true);
        setPadding(true);
        
        //Titolo
		H3 titolo = new H3("Queste sono le colonnine più vicine a te");
		titolo.getStyle().set("color", "#013220");
		//Barra di ricerca per nome o indirizzo delle colonnine
		TextField searchField = new TextField("Cerca");
		searchField.getElement().getThemeList().add("success");
        searchField.setPlaceholder("Nome o indirizzo...");
        searchField.setWidth("300px");
        // EVENTO DI RICERCA → chiama cercaColonnine()
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            aggiornaGridConFiltro(e.getValue());
        });
        
        //Istruzioni per prenotare una colonnina su questa pagina
        Text istruz = new Text("Puoi effettuare una prenotazione direttamente su questa pagina...Ti basterà premere sul nome della colonnina interessata");

        colonGrid.setColumns("nome", "tipo", "stato", "indirizzo", "comune");

        // Caricamento iniziale senza filtri
        aggiornaGridConFiltro("");

		add(titolo, searchField, istruz, colonGrid);
    }

    private void aggiornaGridConFiltro(String query) {
        firebaseService.cercaColonnine(query).thenAccept(lista -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                colonGrid.setItems(lista);
            }));
        });
    }
}
