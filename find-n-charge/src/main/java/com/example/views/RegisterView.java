/**
 * Classe RegisterView che gestisce la pagina di registrazione con il form per creare un nuovo account
 * 
 * @author Tommaso Maistrello, Davide Mascheretti
 */

package com.example.views;

import com.example.service.UtentiService;
import com.example.util.DataValidator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;

@Route("register")
@PageTitle("Registrati")
@UIScope
@CssImport("./styles/CSS.css")
public class RegisterView extends VerticalLayout {

	private final UtentiService utentiService;

	public RegisterView(UtentiService utentiService) {

		this.utentiService = utentiService;

		addClassName("auth-view-container");

		H1 titolo = new H1("Registrati");

		// Campi del form di registrazione
		TextField nomeField = new TextField("Nome", "Inserisci il tuo nome");
		nomeField.addClassNames("auth-input", "register-input-2", "register-margin-top");

		TextField cognomeField = new TextField("Cognome", "Inserisci il tuo cognome");
		cognomeField.addClassNames("auth-input", "register-input-2");

		// Allinea orizzontalmente i campi di nome e cognome
		HorizontalLayout fullNameLayout = new HorizontalLayout(nomeField, cognomeField);
		fullNameLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

		TextField usernameField = new TextField("Username", "Inserisci il tuo nome utente");
		usernameField.addClassNames("auth-input", "register-input-width");
		
		EmailField emailField = new EmailField("Indirizzo Email", "Inserisci la tua email");
		emailField.addClassNames("auth-input", "register-input-width");
		
		PasswordField passwordField = new PasswordField("Scegli una Password", "Inserisci la tua password");
		passwordField.addClassNames("auth-input", "register-input-width");
		
		PasswordField confirmPasswordField = new PasswordField("Conferma Password", "Inserisci di nuovo la tua password");
		confirmPasswordField.addClassNames("auth-input", "register-input-width");
		
		// Pulsante di registrazione
		Button submitButton = new Button("Completa Registrazione");
		submitButton.getElement().getThemeList().add("success");
		submitButton.addClassName("register-margin-top");
		/*
		 * Quando si schiaccia il tasto "Enter" dalla tastiera si ottiene lo stesso
		 * effetto di cliccare sul tasto di registazione
		 */
		submitButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
		submitButton.addClickListener(e -> {

			String nome = nomeField.getValue();
			String cognome = cognomeField.getValue();
			String email = emailField.getValue();
			String username = usernameField.getValue();
			String password = passwordField.getValue();
			String conferma = confirmPasswordField.getValue();

			/*
			 * controlli base
			 */

			String errore = DataValidator.verificaDati(nome, cognome, username, email, password, conferma);
			if (errore != null) {
				Notification.show(errore, 3000, Notification.Position.TOP_CENTER).getElement().getThemeList()
						.add("error");
				return;
			}

			/*
			 * Chiamo funzione da UtentiService che si interfaccerà con il database
			 */

			utentiService.registrati(nome, cognome, username, email, password).thenRun(() -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					//Tutto ok, thenRun senza eccezioni
					Notification.show("Registrazione completata! Benvenuto, " + username + ".", 3000,
							Notification.Position.TOP_CENTER);
					ui.navigate("login");
				}));
				//Eccezione rilevata nel thenRun
			}).exceptionally(ex -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					//Messaggio che identifica l'eccezione
					String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();

					Notification.show("Errore: " + msg, 4000, Notification.Position.TOP_CENTER).getElement()
							.getThemeList().add("error");
				}));
				return null;
			});
		});

		// Box di testo
		Span text = new Span("Hai già un account? ");
		// Creazione link che riporti alla pagina di registrazione
		RouterLink loginLink = new RouterLink("Accedi ora!", LoginView.class);
		loginLink.addClassName("success-link");
		
		// Disposizione orizzontale di testo e link
		HorizontalLayout loginLayout = new HorizontalLayout(text, loginLink);
		loginLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

		// Layout del form
		VerticalLayout registerForm = new VerticalLayout(titolo, fullNameLayout, usernameField, emailField,
				passwordField, confirmPasswordField, submitButton, loginLayout);
		registerForm.addClassNames("auth-form-box", "register-box-width");

		add(registerForm);

	}
}
