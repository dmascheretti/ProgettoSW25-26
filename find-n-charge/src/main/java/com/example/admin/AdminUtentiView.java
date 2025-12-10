/**
 * Classe AdminUtentiView che permette all'amministratore di gestire ogni utente
 * 
 * @author Tommaso Maistrello
 */
package com.example.admin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.example.models.Utente;
import com.example.service.UtentiService;
import com.example.util.DataValidator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.example.layout.AdminLayout;
import org.threeten.bp.LocalDate;

@Route(value = "gestioneUtenti", layout = AdminLayout.class)
@PageTitle("Find&Charge | Gestione utenti")

public class AdminUtentiView extends VerticalLayout {

	private Grid<Utente> utentiGrid = new Grid<>(Utente.class);
	private final UtentiService utentiService;

	/**
	 * Costruttore che genera la griglia contenente tutti gli utenti
	 * 
	 * @param fb Firebase per accedere ai dati utente
	 */
	public AdminUtentiView(UtentiService utentiService) {

		this.utentiService = utentiService;
		setSpacing(true);
		setPadding(true);

		H3 titolo = new H3("Ecco la lista di tutti gli utenti ");
		titolo.getStyle().set("color", "#008000");

		// Imposta le colonne
		utentiGrid.removeAllColumns();
		utentiGrid.addColumn(Utente::getUsername).setHeader("Username").setSortable(true);
		// utentiGrid.addColumn(Utente::getDataCreazione).setHeader("DataCreazione").setSortable(true);
		utentiGrid.addColumn(Utente::getEmail).setHeader("Email").setSortable(true);

		// Pulsante per la cancellazione
		utentiGrid.addComponentColumn(p -> {
			Button btn = new Button("Ban");
			btn.addClickListener(e -> banUtente(p));
			btn.getStyle().set("color", "red").set("text-decoration", "underline").set("background", "none")
					.set("border", "none");
			return btn;
		});

		Button nuovoUtenteBtn = new Button("Nuovo Utente");
		nuovoUtenteBtn.getElement().getThemeList().add("success");

		nuovoUtenteBtn.addClickListener(e -> newUtente());

		add(titolo, nuovoUtenteBtn, utentiGrid);

		// Ottiene la lista di utenti da Firebase
		utentiService.getAllUtenti().thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {

				// Aggiunge la lista alla griglia
				utentiGrid.setItems(lista);

			}));

			// Gestione errori
		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});

	}

	private void banUtente(Utente u) {

		utentiService.cancellaUtente(u).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {

			Notification
					.show("Utente " + u.getUsername() + " bandito dal sistema.", 3000, Notification.Position.TOP_CENTER)
					.getElement().getThemeList().add("success");
			;

			getUI().ifPresent(ui1 -> ui1.getPage().reload());
		})))

				// Gestione errori
				.exceptionally(ex -> {
					getUI().ifPresent(ui -> ui.access(() -> {
						Notification.show("Errore durante la cancellazione: " + ex.getMessage(), 4000,
								Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
					}));
					return null;
				});
	}

	private void newUtente() {

		Dialog dialog = new Dialog();

		VerticalLayout formLayout = new VerticalLayout();
		formLayout.setPadding(true);
		formLayout.setSpacing(true);

		H3 titolo = new H3("Crea un nuovo utente");
		titolo.getStyle().set("color", "#008000");

		TextField nome = new TextField("Nome");
		TextField cognome = new TextField("Cognome");
		TextField username = new TextField("Username");
		EmailField email = new EmailField("Email");
		PasswordField password = new PasswordField("Password");

		Button salva = new Button("Salva Utente");
		salva.getElement().getThemeList().add("success");

		Button annulla = new Button("Annulla", e -> dialog.close());

		salva.addClickListener(e -> {

			if (nome.isEmpty() || cognome.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {

				Notification.show("Compila tutti i campi!", 3000, Notification.Position.TOP_CENTER).getElement()
						.getThemeList().add("error");
				return;
			}

			String n = nome.getValue();
			String c = cognome.getValue();
			String u = username.getValue();
			String em = email.getValue();
			String p = password.getValue();

			String errore = DataValidator.verificaDati(n, c, u, em, p, p);
			if (errore != null) {
				Notification.show(errore, 3000, Notification.Position.TOP_CENTER).getElement().getThemeList()
						.add("error");
				return;
			}

			/*
			 * verifico se lo username esiste già in modo asincrono
			 * 
			 * se la funzione resistuisce un utente nullo allora non è stato trovato se la
			 * funzione restituisce un utente non nullo allora esiste già
			 * 
			 * se utente==null salvo nuovo utente con quello username
			 */

			utentiService.verificaUtente(u).thenAccept(utente -> {
				getUI().ifPresent(ui -> ui.access(() -> {

					if (utente == null) {

						Utente nuovoUtente = new Utente(n, c, u, em, p, LocalDate.now().toString(), "Utente");

						/*
						 * in modo asicrono salvo utente nel database il thenRun() permette di lavorare
						 * in background e non bloccare la UI principale della registerView
						 * 
						 * quando salvaUtente termina procede con la registrazione (o eventualmente
						 * eccezione) se salvaUtente() notifica null, tutto ok --> eseguo thenRun() se
						 * salvaUtente() notifica != null allora thenRun() riceve notifica di eccezione,
						 * non viene eseguito, ed esegue .exceptionally (errore del database)
						 */

						utentiService.salvaUtente(nuovoUtente).thenRun(() -> ui.access(() -> {
							Notification.show("Registrazione completata! Benvenuto, " + u + ".", 3000,
									Notification.Position.TOP_CENTER);
							getUI().ifPresent(ui1 -> ui1.getPage().reload());
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

						Notification.show("Lo username : " + u + " e' già in uso, prova con un altro!", 3000,
								Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
						;

					}

				}));

			});

		});

		HorizontalLayout datiPersonaliLayout = new HorizontalLayout(nome, cognome);
		datiPersonaliLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		HorizontalLayout credenzialiLayout = new HorizontalLayout(username, password);
		credenzialiLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		HorizontalLayout pulsantiLayout = new HorizontalLayout(salva, annulla);
		pulsantiLayout.setAlignItems(FlexComponent.Alignment.CENTER);

		formLayout.add(titolo, datiPersonaliLayout, email, credenzialiLayout, pulsantiLayout);

		formLayout.expand(titolo, datiPersonaliLayout, email, credenzialiLayout, pulsantiLayout);

		// Mostra il form in una nuova finestra/dialog
		dialog.add(formLayout);
		dialog.setModal(true);
		// dialog.setWidth("600px");
		dialog.open();
	}

}