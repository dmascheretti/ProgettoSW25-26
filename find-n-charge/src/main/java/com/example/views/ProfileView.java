package com.example.views;

import com.example.MainLayout;
import com.example.models.Utente;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(value = "profilo", layout = MainLayout.class)
@PageTitle("Find&Charge - Profilo")

public class ProfileView extends VerticalLayout {

	// Classe per le informazioni utente e area personale
	public ProfileView() {
		setSpacing(true);
        setPadding(true);
        Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        
        H3 titolo= new H3("Ciao "+utente.getUsername()+"! Ecco la tua pagina di profilo");
        titolo.getStyle().set("color", "#008000");
		Paragraph nome = new Paragraph("Nome: "+utente.getNome());
		Paragraph cognome = new Paragraph("Cognome: "+utente.getCognome());
		Paragraph mail = new Paragraph("Mail: "+utente.getEmail());
		add(titolo, nome, cognome, mail);
	}
	
	//QUI DA IMPLEMENTARE I CAMBIA PASSWORD E CAMBIA MAIL CON THENRUN

}
