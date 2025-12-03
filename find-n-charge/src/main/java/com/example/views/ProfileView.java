package com.example.views;

import com.example.models.Auto;
import com.example.models.Utente;
import com.example.service.AutoService;
import com.example.service.PrenotazioniService;
import com.example.service.UtentiService;

import com.example.layout.MainLayout;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import java.time.LocalTime;

@Route(value = "profilo", layout = MainLayout.class)

@PageTitle("Find&Charge - Profilo")
public class ProfileView extends VerticalLayout implements BeforeEnterObserver {

	private final AutoService autoService;
	private final UtentiService utentiService;
	private final PrenotazioniService prenotazioniService;

	public ProfileView(AutoService autoService, UtentiService utentiService, PrenotazioniService prenotazioniService) {
		this.autoService = autoService;
		this.utentiService = utentiService;
		this.prenotazioniService = prenotazioniService;
		setSizeFull();
		setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		setSpacing(true);
		setPadding(true);

		// Prendi utente dalla sessione (controllo null per sicurezza)
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
		if (utente == null) {

			add(new Paragraph("Utente non trovato. Effettua il login."));
			return;
		}

		// Header
		String saluto;
		int ora = LocalTime.now().getHour();

		if (ora >= 6 && ora < 12) {
			saluto = "Buongiorno";
		} else if (ora >= 12 && ora < 18) {
			saluto = "Buon pomeriggio";
		} else {
			saluto = "Buonasera";
		}

		H3 titolo = new H3();
		titolo.getStyle().set("color", "#2ECC71");
		Span userSpan = new Span(utente.getUsername());
		userSpan.getStyle().set("color", "blue").set("font-weight", "bold");

		titolo.add(new Text(saluto + " "), userSpan, new Text("! \nEcco la tua pagina di profilo"));

		Paragraph nome = new Paragraph(utente.getNome());
		Paragraph cognome = new Paragraph(utente.getCognome());
		Paragraph mail = new Paragraph(utente.getEmail());

		VerticalLayout profileCard = new VerticalLayout();
		profileCard.setPadding(true);
		profileCard.setSpacing(true);
		profileCard.setWidth("100%");
		profileCard.setMaxWidth("500px");
		profileCard.setAlignItems(Alignment.CENTER);
		profileCard.getStyle().set("background-color", "white");
		profileCard.getStyle().set("border-radius", "16px");
		profileCard.getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.10)");
		profileCard.getStyle().set("padding", "30px");
		profileCard.getStyle().set("margin-top", "20px");

		Span avatar = new Span("👤");
		avatar.getStyle().set("font-size", "70px");

		Span title = new Span("Profilo Utente");
		title.getStyle().set("font-size", "26px");
		title.getStyle().set("font-weight", "bold");
		title.getStyle().set("margin-bottom", "10px");

		VerticalLayout infoBlock = new VerticalLayout();
		infoBlock.setAlignItems(Alignment.START);
		infoBlock.setPadding(false);
		infoBlock.setSpacing(true);
		infoBlock.setWidth("100%");
		infoBlock.setMaxWidth("350px");

		Span labelNome = new Span("Nome:");
		labelNome.getStyle().set("font-weight", "bold");
		Span valueNome = new Span(nome.getText());
		valueNome.getStyle().set("color", "#333");

		Span labelCognome = new Span("Cognome:");
		labelCognome.getStyle().set("font-weight", "bold");
		Span valueCognome = new Span(cognome.getText());
		valueCognome.getStyle().set("color", "#333");

		Span labelMail = new Span("Email:");
		labelMail.getStyle().set("font-weight", "bold");
		Span valueMail = new Span(mail.getText());
		valueMail.getStyle().set("color", "#333");

		infoBlock.add(labelNome, valueNome, labelCognome, valueCognome, labelMail, valueMail);

		profileCard.add(avatar, title, infoBlock);

		HorizontalLayout hl = new HorizontalLayout();
		hl.setWidthFull();
		hl.setPadding(true);
		hl.setSpacing(true);
		this.getStyle().set("background-color", "white");

		autoService.getAutoUtente(utente).thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {

				hl.removeAll();

				for (Auto a : lista) {

					VerticalLayout card = new VerticalLayout();
					card.setPadding(true);
					card.setSpacing(false);
					card.setWidth("350px");
					card.getStyle().set("border", "1px solid #e0e0e0");
					card.getStyle().set("border-radius", "12px");
					card.getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.10)");
					card.getStyle().set("background-color", "white");

					H3 titolo2 = new H3(a.getModello());
					titolo.getStyle().set("margin-bottom", "0");

					Paragraph targa = new Paragraph("Targa: " + a.getTarga());
					Paragraph carica = new Paragraph("Carica residua: " + a.getStatoCarica() + "%");

					if (a.getStatoCarica() <= 30) {
						carica.getStyle().set("color", "#FF3B30").set("font-weight", "bold");
						carica.setText("Carica residua: " + a.getStatoCarica() + "%  ⚠ Mettere in carica");
					} else if (a.getStatoCarica() <= 55) {
						carica.getStyle().set("color", "#F7DC6F").set("font-weight", "bold");
					} else {
						carica.getStyle().set("color", "#27AE60").set("font-weight", "bold");
					}

					card.add(titolo2, targa, carica);

					prenotazioniService.inCarica(a).thenAccept(trovata -> {
						ui.access(() -> {
							Paragraph inCarica = new Paragraph(trovata ? "Auto in carica" : "Auto non in carica");
							card.add(inCarica);
						});
					});
					hl.add(card);
				}
			}));
		});

		// Email change controls
		TextField emailField = new TextField("Nuova Email");

		VerticalLayout emailCard = createCard("Modifica Email");
		emailField.setWidthFull();
		Button cambiaMailBtn = createMainButton("Aggiorna Email");
		emailCard.add(emailField, cambiaMailBtn);

		cambiaMailBtn.addClickListener(e -> {
			String nuovaEmail = emailField.getValue();
			if (nuovaEmail.isEmpty()) {
				Notification.show("Inserisci una email valida", 3000, Notification.Position.TOP_CENTER);
				return;
			}

			utentiService.cambiaMail(utente, nuovaEmail).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
				// aggiorna utente in sessione
				utente.setEmail(nuovaEmail);
				VaadinSession session = ui.getSession();
				session.setAttribute("utente", utente);

				// aggiorna label e mostra notifica
				mail.setText("Mail: " + nuovaEmail);

				Notification n = Notification.show("Email aggiornata correttamente!", 3000,
						Notification.Position.TOP_CENTER);
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

				emailField.clear();
			}))).exceptionally(ex -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
					Notification n = Notification.show("Errore: " + msg, 3000, Notification.Position.TOP_CENTER);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				}));
				return null;
			});
		});

		PasswordField passwordField = new PasswordField("Nuova Password");
		VerticalLayout pwdCard = createCard("Modifica Password");
		passwordField.setWidthFull();
		Button cambiaPwdBtn = createMainButton("Aggiorna Password");
		pwdCard.add(passwordField, cambiaPwdBtn);

		cambiaPwdBtn.addClickListener(e -> {
			String nuovaPwd = passwordField.getValue();

			if (nuovaPwd.isEmpty()) {
				Notification.show("Inserisci una password", 3000, Notification.Position.TOP_CENTER);
				return;
			}

			utentiService.cambiaPassword(utente, nuovaPwd).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {

				utente.setPassword(nuovaPwd);
				VaadinSession.getCurrent().setAttribute("utente", utente);

				Notification n = Notification.show("Password aggiornata correttamente!");
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				n.setPosition(Notification.Position.TOP_CENTER);
				passwordField.clear();

			}))).exceptionally(ex -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
					Notification n = Notification.show("Errore: " + msg, 3000, Notification.Position.TOP_CENTER);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				}));
				return null;
			});

		});

		// --- Aggiungi Auto ---
		TextField targaField = new TextField("Targa");
		TextField modelloField = new TextField("Modello");

		ComboBox<String> tipoField = new ComboBox<>("Tipo di auto");
		tipoField.setItems("Berlina (65 kWh)", "Suv (100 kWh)", "Sportiva (75 kWh)", "Utilitaria (40 kWh)");
		tipoField.setPlaceholder("Seleziona il tipo");
		tipoField.setAllowCustomValue(false);

		VerticalLayout autoCard = createCard("Aggiungi Auto");
		targaField.setWidthFull();
		modelloField.setWidthFull();
		tipoField.setWidthFull();
		Button aggiungiAutoBtn = createMainButton("Aggiungi Auto");
		autoCard.add(targaField, modelloField, tipoField, aggiungiAutoBtn);

		aggiungiAutoBtn.addClickListener(e -> {
			// .trim serve per controllare che non sia vuoto es " " -> non va bene
			String targa = targaField.getValue() == null ? "" : targaField.getValue().trim();
			String modello = modelloField.getValue() == null ? "" : modelloField.getValue().trim();
			String tipo = tipoField.getValue();
			if (targa.isEmpty() || modello.isEmpty() || tipo == null || tipo.isEmpty()) {
				Notification.show("Compila tutti i campi dell’auto");
				return;
			}

			autoService.aggiungiAuto(targa, modello, tipo, utente)
					.thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {

						Notification n = Notification.show("Auto registrata!", 3000, Notification.Position.TOP_CENTER);
						n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

						targaField.clear();
						modelloField.clear();
						tipoField.clear();

					}))).exceptionally(ex -> {

						getUI().ifPresent(ui -> ui.access(() -> {
							String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
							Notification n = Notification.show("Errore: " + msg, 4000,
									Notification.Position.TOP_CENTER);
							n.addThemeVariants(NotificationVariant.LUMO_ERROR);
						}));
						return null;
					});
		});
		// Layout e aggiunta componenti
		HorizontalLayout confAuto = new HorizontalLayout(targaField, modelloField, tipoField);
		confAuto.setSpacing(true);

		add(titolo, profileCard, hl, emailField, cambiaMailBtn, passwordField, cambiaPwdBtn, confAuto, aggiungiAutoBtn);

		setDefaultHorizontalComponentAlignment(Alignment.START);
	}

	/**
	 * Se l'utente prova ad accedere direttamente a questa pagina senza aver
	 * effettuato l'accesso, lo si reindirizza alla pagina di login mostrando una
	 * notifica di errore. beforeEnter viene eseguito un attimo prima che la pagina
	 * venga mostrata all'utente.
	 */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");

		if (utente == null) {
			event.forwardTo("login"); // Reindirizza alla pagina di login
			Registration[] registrationWrapper = new Registration[1]; // Array per registrare l'aggiunta del listener
			// Dopo che ha cambiato pagina, mostra la notifica
			registrationWrapper[0] = UI.getCurrent().addAfterNavigationListener(navEvent -> {
				Notification.show("Utente non trovato. Effettua il login.", 3000, Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");

				// Rimuove il listener, altrimenti scatterebbe ogni volta
				if (registrationWrapper[0] != null) {
					registrationWrapper[0].remove();
				}
			});
		}
	}

	private VerticalLayout createCard(String titleText) {
		VerticalLayout card = new VerticalLayout();
		card.setPadding(true);
		card.setSpacing(true);
		card.setWidth("100%");
		card.setMaxWidth("500px");
		card.getStyle().set("background-color", "white");
		card.getStyle().set("border-radius", "12px");
		card.getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.08)");
		card.getStyle().set("margin-top", "20px");

		Span title = new Span(titleText);
		title.getStyle().set("font-size", "18px");
		title.getStyle().set("font-weight", "bold");
		title.getStyle().set("color", "#2ECC71");

		card.add(title);
		return card;
	}

	private Button createMainButton(String label) {
		Button btn = new Button(label);
		btn.setWidthFull();
		btn.getStyle().set("background-color", "#2ECC71");
		btn.getStyle().set("color", "white");
		btn.getStyle().set("font-weight", "bold");
		btn.getStyle().set("border-radius", "8px");
		btn.getStyle().set("padding", "10px");
		return btn;
	}

}
