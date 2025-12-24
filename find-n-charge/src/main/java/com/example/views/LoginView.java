/**
 * Classe LoginView che gestisce la pagina di login con il form per accedere grazie alle proprie credenziali
 * 
 * @author Tommaso Maistrello, Davide Mascheretti
 */

package com.example.views;

import com.example.service.UtentiService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
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
import com.vaadin.flow.server.VaadinSession;

// Questa deve essere la prima pagina aperta dal sito web
@Route("login")
@PageTitle("Accedi")
@CssImport("./styles/CSS.css")
public class LoginView extends VerticalLayout {

	private final UtentiService utentiService;

	public LoginView(UtentiService utentiService) {

		this.utentiService = utentiService;

		addClassName("auth-view-container");

		// Titolo del form
		H1 titolo = new H1("Accedi");

		// Inseriamo i campi del form
		// Campo del nome utente
		TextField usernameField = new TextField("Nome utente", "Inserisci il tuo username" );
		usernameField.addClassNames("auth-input", "login-input-width");

		// Campo della password
		PasswordField passwordField = new PasswordField("Password", "Inserisci la password");
		passwordField.addClassNames("auth-input", "login-input-width");

		// Pulsante di accesso
		Button loginButton = new Button("Accedi");
		loginButton.getElement().getThemeList().add("success");
        loginButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
		loginButton.addClickListener(event -> {
			String user = usernameField.getValue();
			String password = passwordField.getValue();

			/*
			 * Richiama classe di servizio che si interfaccia con il database
			 */
			utentiService.login(user, password).thenAccept(utente -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					VaadinSession.getCurrent().setAttribute("utente", utente);
					Notification.show("Accesso effettuato!", 2000, Notification.Position.TOP_CENTER);

					// Se utente trovato e ha ruolo admin accedi alla pagina admin
					if (utente.getRuolo().equals("Admin")) {

						ui.navigate("admin");

						// Se utente trovato ma non è admin allora accedi alla pagina main
					} else {
						ui.navigate("main");
					}
				}));
				// Chiamata se in utentiService si completa con un eccezione
			}).exceptionally(e -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					// Messaggio varia in base all'eccezione rilevata
					String messaggio = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();

					Notification error = Notification.show("Errore: " + messaggio, 3000,
							Notification.Position.TOP_CENTER);
					error.getElement().getThemeList().add("error");
					passwordField.clear();
				}));
				return null;
			});

		});

		// Box di testo
		Span text = new Span("Non hai un account? ");
		// Creazione link che riporti alla pagina di registrazione
		RouterLink registerLink = new RouterLink("Registrati ora!", RegisterView.class);
		registerLink.addClassName("success-link");

		// Disposizione orizzontale di testo e link
		HorizontalLayout registerLayout = new HorizontalLayout(text, registerLink);
		registerLayout.setSpacing(true);
		registerLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

		// Layout della box
		VerticalLayout loginForm = new VerticalLayout(titolo, usernameField, passwordField, loginButton,
				registerLayout);
		loginForm.addClassNames("auth-form-box", "login-box-width");

		// Aggiunge il form all'interno del layout
		add(loginForm);

	}
}