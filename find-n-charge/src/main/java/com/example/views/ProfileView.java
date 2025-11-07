package com.example.views;

import com.example.MainLayout;
import com.example.models.Utente;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "profilo", layout = MainLayout.class)
@PageTitle("Find&Charge - Profilo")

public class ProfileView extends Div {

	// Classe per le informazioni utente e area personale
	public ProfileView() {
		setText("Questa è la pagina di profilo di ");
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");

	    setText(utente.getNome()+" "+utente.getCognome()+". Mail: "+utente.getEmail());
	}

}
