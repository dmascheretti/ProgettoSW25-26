/**
 * Classe RegisterView che gestisce la pagina di registrazione con il form per creare un nuovo account
 * 
 * @author Tommaso Maistrello, Davide Mascheretti
 */

package com.example.views;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.threeten.bp.LocalDate;
import com.example.database.FirebaseUtentiService;
import com.example.models.Utente;
import com.example.util.DataValidator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
public class RegisterView extends VerticalLayout {

	private FirebaseUtentiService firebaseUtentiService;
	private final UI ui;
	private final PasswordEncoder passwordEncoder;

	public RegisterView(FirebaseUtentiService firebaseUtentiService, PasswordEncoder passwordEncoder) {

		this.firebaseUtentiService=firebaseUtentiService;
		this.ui = UI.getCurrent();
		this.passwordEncoder=passwordEncoder;

		setSizeFull();
		setAlignItems(Alignment.CENTER);
		setJustifyContentMode(JustifyContentMode.CENTER);

		H1 titolo = new H1("Registrati");

		// Campi del form di registrazione
		TextField nomeField = new TextField();
		nomeField.setLabel("Nome");
		nomeField.setPlaceholder("Inserisci il tuo nome");
		nomeField.setWidth("190px");
		nomeField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)"); // Tema verde
		nomeField.getStyle().set("margin-top", "var(--lumo-space-l)"); // Spaziatura sopra al campo

		TextField cognomeField = new TextField();
		cognomeField.setLabel("Cognome");
		cognomeField.setPlaceholder("Inserisci il tuo cognome");
		cognomeField.setWidth("190px");
		cognomeField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		// Allinea orizzontalmente i campi di nome e cognome
		HorizontalLayout fullNameLayout = new HorizontalLayout(nomeField, cognomeField);
		fullNameLayout.setSpacing(true);
		fullNameLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

		TextField usernameField = new TextField();
		usernameField.setLabel("Username");
		usernameField.setPlaceholder("Inserisci il tuo nome utente");
		usernameField.setWidth("400px");
		usernameField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		EmailField emailField = new EmailField();
		emailField.setLabel("Indirizzo Email");
		emailField.setPlaceholder("Inserisci la tua email");
		emailField.setWidth("400px");
		emailField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		PasswordField passwordField = new PasswordField();
		passwordField.setLabel("Scegli una Password");
		passwordField.setPlaceholder("Inserisci la tua password");
		passwordField.setWidth("400px");
		passwordField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		PasswordField confirmPasswordField = new PasswordField();
		confirmPasswordField.setLabel("Conferma Password");
		confirmPasswordField.setPlaceholder("Inserisci di nuovo la tua password");
		confirmPasswordField.setWidth("400px");
		confirmPasswordField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		// Pulsante di registrazione
		Button submitButton = new Button("Completa Registrazione");
		submitButton.getElement().getThemeList().add("success");
		submitButton.getStyle().set("margin-top", "var(--lumo-space-l)");
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
			 * verifico se lo username esiste già in modo asincrono, prima verifico utente, al termine prosegue
			 * con il thenAccept
			 * 
			 * se la funzione resistuisce un utente nullo allora non è stato trovato se la
			 * funzione restituisce un utente non nullo allora esiste già
			 * 
			 * se utente==null salvo nuovo utente con quello username
			 */

			firebaseUtentiService.verificaUtente(username).thenAccept(utente -> {
				getUI().ifPresent(ui -> ui.access(() -> {

					if (utente == null) {
						
						String passwordCriptata = passwordEncoder.encode(password);

						Utente nuovoUtente = new Utente(nome, cognome, username, email, passwordCriptata, LocalDate.now().toString(), "Utente");

						/*
						 * in modo asicrono salvo utente nel database il thenRun() permette di lavorare
						 * in background e non bloccare la UI principale della registerView
						 * 
						 * quando salvaUtente termina procede con la registrazione (o eventualmente
						 * eccezione) se salvaUtente() notifica null, tutto ok --> eseguo thenRun() se
						 * salvaUtente() notifica != null allora thenRun() riceve notifica di eccezione,
						 * non viene eseguito, ed esegue .exceptionally (errore del database)
						 */

						firebaseUtentiService.salvaUtente(nuovoUtente).thenRun(() -> ui.access(() -> {
							Notification.show("Registrazione completata! Benvenuto, " + username + ".", 3000,
									Notification.Position.TOP_CENTER);
							ui.navigate("");
						}))

								// gestione e messaggio di errore

								.exceptionally(ex -> {
									ui.access(() -> {
										Notification
												.show("Errore durante il salvataggio: " + ex.getMessage(), 4000,
														Notification.Position.TOP_CENTER)
												.getElement().getThemeList().add("error");
									});
									return null;
								});

					}

					else {

						Notification.show("Lo username : " + username + " e' già in uso, prova con un altro!", 3000,
								Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
			        	   ;

					}

				}));

			});

		});

		// Box di testo
		Span text = new Span("Hai già un account? ");
		// Creazione link che riporti alla pagina di registrazione
		RouterLink loginLink = new RouterLink();
		loginLink.setText("Accedi ora!");
		loginLink.setRoute(LoginView.class);
		loginLink.getStyle().set("color", "var(--lumo-success-text-color)");

		// Disposizione orizzontale di testo e link
		HorizontalLayout loginLayout = new HorizontalLayout(text, loginLink);
		loginLayout.setSpacing(true);
		loginLayout.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

		// Layout del form
		VerticalLayout registerForm = new VerticalLayout(titolo, fullNameLayout, usernameField, emailField,
				passwordField, confirmPasswordField, submitButton, loginLayout);
		registerForm.setSpacing(true);
		registerForm.getStyle().set("gap", "var(--lumo-space-s)"); // Spazio piccolo
		registerForm.setAlignItems(Alignment.CENTER);

		// Stile della box
		registerForm.setMaxWidth("500px");
		registerForm.getElement().getStyle().set("padding", "35px");
		registerForm.getElement().getStyle().set("box-shadow", "0 8px 16px 0 rgba(0,0,0,0.3)");
		registerForm.getElement().getStyle().set("background-color", "white");
		registerForm.getElement().getStyle().set("border-radius", "8px");

		add(registerForm);

		/*
		 * Quando si schiaccia il tasto "Enter" dalla tastiera si ottiene lo stesso
		 * effetto di cliccare sul tasto di registazione
		 */
		submitButton.addClickShortcut(com.vaadin.flow.component.Key.ENTER);
	}
}
