/**
 * Classe LoginView che gestisce la pagina di login con il form per accedere grazie alle proprie credenziali
 * 
 * @author Tommaso Maistrello
 */

package com.example.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

// Questa deve essere la prima pagina aperta dal sito web
@Route("")
@PageTitle("Accedi")

public class LoginView extends VerticalLayout {

	public LoginView() {

		// Dimensione massima per occupare tutta la finestra
		setSizeFull();
		// Centra orizzontalmente e verticalmente il form nella pagina
		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.CENTER);

		// Titolo del form
		H1 titolo = new H1("Accedi");

		// Inseriamo i campi del form
		// Campo del nome utente
		TextField usernameField = new TextField();
		usernameField.setLabel("Nome utente o indirizzo email");
		usernameField.setPlaceholder("Inserisci la tua email");
		usernameField.setWidth("300px");
		usernameField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)"); // Tema verde

		// Campo della password
		PasswordField passwordField = new PasswordField();
		passwordField.setLabel("Password");
		passwordField.setPlaceholder("Inserisci la password");
		passwordField.setWidth("300px");
		passwordField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)"); // Tema verde

		// Pulsante di accesso
		Button loginButton = new Button("Accedi");
		loginButton.getElement().getThemeList().add("success"); // Tema verde
		loginButton.addClickListener(event -> {
			Notification.show("Accesso in corso", 1000, Notification.Position.TOP_CENTER);
			UI.getCurrent().navigate("map");
		});

		// Box di testo
		Span text = new Span("Non hai un account? ");
		// Creazione link che riporti alla pagina di registrazione
		RouterLink registerLink = new RouterLink();
		registerLink.setText("Registrati ora!");
		registerLink.setRoute(RegisterView.class);
		registerLink.getStyle().set("color", "var(--lumo-success-text-color)");

		// Disposizione orizzontale di testo e link
		HorizontalLayout registerLayout = new HorizontalLayout(text, registerLink);
		registerLayout.setSpacing(true);
		registerLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

		// Layout della box
		VerticalLayout loginForm = new VerticalLayout(titolo, usernameField, passwordField, loginButton,
				registerLayout);
		loginForm.setAlignItems(Alignment.CENTER);

		// Stile della box
		loginForm.setMaxWidth("400px");
		loginForm.getElement().getStyle().set("padding", "40px");
		loginForm.getElement().getStyle().set("box-shadow", "0 8px 16px 0 rgba(0,0,0,0.3)");
		loginForm.getElement().getStyle().set("background-color", "white");
		loginForm.getElement().getStyle().set("border-radius", "8px");

		// Aggiunge il form all'interno del layout
		add(loginForm);

		/*
		 * Quando si schiaccia il tasto "Enter" dalla tastiera si ottiene lo stesso
		 * effetto di cliccare sul tasto di accesso
		 */
		loginButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);

	}
}