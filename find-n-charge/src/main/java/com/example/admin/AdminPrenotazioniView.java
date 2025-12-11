/**
 * Classe AdminPrenotazioniView che permette all'amministratore di consultare e gestire tutte le prenotazioni
 * 
 * @author Francesco Valenari
 */
package com.example.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import com.example.layout.AdminLayout;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.example.service.PrenotazioniService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

@Route(value = "gestionePrenotazioni", layout = AdminLayout.class)
@PageTitle("Find&Charge | Gestione prenotazioni")

public class AdminPrenotazioniView extends VerticalLayout implements BeforeEnterObserver{

	private Grid<Prenotazione> prenoGrid = new Grid<>(Prenotazione.class);
	private final PrenotazioniService prenotazioniService;

	public AdminPrenotazioniView(PrenotazioniService prenotazioniService) {
		this.prenotazioniService = prenotazioniService;
		setSpacing(true);
		setPadding(true);
		H3 titolo = new H3("Lista universale delle prenotazioni...");
		titolo.getStyle().set("color", "#008000");

		prenoGrid.setColumns("IDColonnina", "utente", "data", "inizio");

		prenoGrid.addColumn(prenotazione -> {
			String stato = calcolaStato(prenotazione);
			return stato;
		}).setHeader("Stato").setSortable(true);

		// bottone per la cancellazione
		prenoGrid.addComponentColumn(p -> {
			Button btn = new Button("Cancella");
			btn.addClickListener(e -> cancellaPrenot(p));
			btn.getStyle().set("color", "red").set("text-decoration", "underline").set("background", "none")
					.set("border", "none");
			return btn;
		});
		add(titolo, prenoGrid);
		/*
		 * Chiamo funzione da firebaseService che restituisce la lista delle
		 * prenotazioni la lista ottenuta va in lista L'utilizzo di thenAccept permette
		 * di lavorare in maniera asincrona ed è necessaria per utilizzare il
		 * CompletableFuture in getAllReservation
		 */
		prenotazioniService.getAllReservation().thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {

				// aggiungo la lista alla griglia
				prenoGrid.setItems(lista);

			}));

			// gestione errori
		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});
	}

	private String calcolaStato(Prenotazione prenotazione) {
		try {
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

			LocalDate dataPren = LocalDate.parse(prenotazione.getData(), dateFormatter);
			LocalTime oraPren = LocalTime.parse(prenotazione.getInizio(), timeFormatter);
			LocalDateTime prenDateTime = LocalDateTime.of(dataPren, oraPren);
			LocalDateTime now = LocalDateTime.now();

			if (prenDateTime.isBefore(now.minusMinutes(30))) {
				return "Passata";
			} else if (prenDateTime.isBefore(now)) {
				return "Attiva";
			} else {
				return "Futura";
			}
		} catch (Exception e) {
			return "";
		}
	}

	private void cancellaPrenot(Prenotazione p) {

		prenotazioniService.cancellaPrenotazione(p).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
			Notification.show("Prenotazione eliminata con successo", 3000, Notification.Position.TOP_CENTER);

			getUI().ifPresent(ui1 -> ui1.getPage().reload());
		})))

				// gestione e messaggio di errore

				.exceptionally(ex -> {
					getUI().ifPresent(ui -> ui.access(() -> { // <-- CORREZIONE 2: getUI() e (ui -> ui.access(...))
						Notification.show("Errore durante il salvataggio: " + ex.getMessage(), 4000,
								Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
					}));
					return null;
				});
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
		} else if(utente.getRuolo().equals("Utente")) {
			event.forwardTo("");
		} 
	}


}
