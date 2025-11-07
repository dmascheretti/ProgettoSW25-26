package com.example.views;

import com.example.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Find&Charge - Colonnine")
@Route(value = "colonnine", layout = MainLayout.class)

public class StationsView extends Div {

	// Classe per l'elenco delle colonnine (senza vederle dalla mappa)
	public StationsView() {
		setText("Questa è la pagina delle colonnine");
	}

}
