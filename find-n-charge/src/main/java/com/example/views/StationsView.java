/**
 * La classe StationView gestisce la pagina della lista delle colonnine disponibili
 * 
 * @author Francesco Valenari
 */
package com.example.views;

import com.example.MainLayout;
import com.example.database.FirebaseService;
import com.example.models.Colonnina;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Find&Charge - Colonnine")
@Route(value = "colonnine", layout = MainLayout.class)

public class StationsView extends VerticalLayout {
private Grid<Colonnina> colonGrid = new Grid<>(Colonnina.class);
private FirebaseService firebaseService = new FirebaseService(); // ✅ istanza

	// Classe per l'elenco delle colonnine (senza vederle dalla mappa)
	public StationsView() {
		setSpacing(true);
        setPadding(true);
		Text t = new Text("Questa è la pagina delle colonnine");
		
		TextField searchField = new TextField("Cerca");
        searchField.setPlaceholder("Nome o indirizzo...");
        searchField.setWidth("300px");

        Button searchButton = new Button("Filtra");

        // EVENTO DI RICERCA → chiama cercaColonnine()
        searchButton.addClickListener(e -> {
            String query = searchField.getValue();
            aggiornaGridConFiltro(query);
        });

        // Enter per cercare
        searchField.addKeyPressListener(Key.ENTER, e -> {
            String query = searchField.getValue();
            aggiornaGridConFiltro(query);
        });

        colonGrid.setColumns("nome", "tipo", "stato", "indirizzo", "comune");

        // Caricamento iniziale senza filtri
        aggiornaGridConFiltro("");

		add(t,searchField, searchButton, colonGrid);
    }

    private void aggiornaGridConFiltro(String query) {
        firebaseService.cercaColonnine(query).thenAccept(lista -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                colonGrid.setItems(lista);
            }));
        });
    }
}
