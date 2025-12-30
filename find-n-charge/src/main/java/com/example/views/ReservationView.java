/**
 * Classe ReservationView che gestisce la pagina di consulto delle proprie prenotazioni attive o passate
 * 
 * @author Francesco Valenari, Claudio Morgera, Davide Mascheretti
 */
package com.example.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.example.service.ColonnineService;
import com.example.service.PrenotazioniService;
import com.example.service.RecensioniService;
import com.example.util.QRCode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.example.enums.StatoPrenotazione;
import com.example.layout.MainLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "prenotazioni", layout = MainLayout.class)
@PageTitle("Find&Charge - Prenotazioni")

@CssImport("./styles/CSS.css")
public class ReservationView extends VerticalLayout implements BeforeEnterObserver {

	private Grid<Prenotazione> newPrenoGrid = new Grid<>(Prenotazione.class, false);
	private Grid<Prenotazione> oldPrenoGrid = new Grid<>(Prenotazione.class, false);
	private List<String> idColonnineOccupate = new ArrayList<>();
	private final PrenotazioniService prenotazioniService;
	private final ColonnineService colonnineService;
	private final RecensioniService recensioniService;

	/**
	 * Costruttore che genera la griglia contenente tutte le prenotazioni
	 * dell'utente
	 * 
	 * @param fb Database per accedere ai dati prenotazione
	 */

	public ReservationView(PrenotazioniService prenotazioniService, ColonnineService colonnineService,
			RecensioniService recensioniService) {
		this.prenotazioniService = prenotazioniService;
		this.colonnineService = colonnineService;
		this.recensioniService = recensioniService;
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
		if (utente == null) {
			return;
		}

		setSpacing(true);
		setPadding(true);
		H3 titolo = new H3("Ciao " + utente.getUsername().toUpperCase() + "! Ecco le tue prenotazioni...");
		titolo.addClassName("text-green-primary");
		
		// Configurazione griglia (visualizzazione dei campi di ogni prenotazione)

		// Prenotazioni attive o future
		newPrenoGrid.removeAllColumns();
		newPrenoGrid.addColumn(Prenotazione::getNomeColonnina).setHeader("Colonnina").setSortable(true);
		newPrenoGrid.addColumn(Prenotazione::getData).setHeader("Data").setSortable(true);
		newPrenoGrid.addColumn(Prenotazione::getInizio).setHeader("Inizio").setSortable(true);
		newPrenoGrid.addColumn(prenotazione -> {
			StatoPrenotazione stato = calcolaStato(prenotazione);
			return stato.toString();
		}).setHeader("Stato").setSortable(true);

		// Prenotazioni passate
		oldPrenoGrid.removeAllColumns();
		oldPrenoGrid.addColumn(Prenotazione::getNomeColonnina).setHeader("Colonnina").setSortable(true);
		oldPrenoGrid.addColumn(Prenotazione::getData).setHeader("Data").setSortable(true);
		oldPrenoGrid.addColumn(Prenotazione::getInizio).setHeader("Inizio").setSortable(true);
		// --- AGGIUNTA NUOVA COLONNA VOTO ---
		oldPrenoGrid.addComponentColumn(prenotazione -> {
			return creaComponenteStelle(prenotazione);
		}).setHeader("Dai un Voto al Servizio");
		// ------------------------------------

		colonnineService.getColonnineInCarica().thenAccept(listaInCarica -> {

			getUI().ifPresent(ui -> ui.access(() -> {

				if (listaInCarica != null) {
					idColonnineOccupate.addAll(listaInCarica);
				}

				ui.push();
			}));

		}).exceptionally(ex -> {
			// Gestione errori
			ex.printStackTrace();
			return null;
		});

		// Aggiunge la colonna per la visualizzazione del QR Code attivabile 5 minuti
		// prima dello slot orario prenotato
		newPrenoGrid.addComponentColumn(p -> {

			try {
				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

				// Trasforma le stringhe in LocalDate e LocalTime formattati
				LocalDate dataPrenotazione = LocalDate.parse(p.getData(), dateFormatter);
				LocalTime oraInizio = LocalTime.parse(p.getInizio(), timeFormatter);

				LocalDateTime inizioPrenotazione = LocalDateTime.of(dataPrenotazione, oraInizio); // Inizio slot
																									// prenotazione
				LocalDateTime adesso = LocalDateTime.now();
				LocalDateTime orarioMinimo = inizioPrenotazione.minusMinutes(5);

				// Se è troppo presto il bottone non compare
				if (adesso.isBefore(orarioMinimo)) {
					Span text = new Span("Disponibile 5 min prima.");
					text.addClassName("qrcode-msg");
					text.addClassName("wait");
					return text;
				}
				// se la colonnina è occupata da un altro utente, il bottone non compare
				if (this.idColonnineOccupate.contains(p.getIDColonnina())
						&& !p.getStato().equals(StatoPrenotazione.IN_CARICA.toString())) {
					Span text = new Span("Colonnina occupata.");
					text.addClassName("qrcode-msg");
					text.addClassName("error");
					return text;
				}

			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			Button btn = new Button("Visualizza QR");
			btn.addClassName("grid-btn-link");

			btn.addClickListener(event -> {
				String id = p.getId();

				prenotazioniService.aggiornaStato(p, StatoPrenotazione.IN_CARICA).exceptionally(ex -> {
					ex.printStackTrace();
					return null;
				});

				BufferedImage tempQr = null; // Crea variabile dell'immagine QR Code
				try {
					tempQr = QRCode.generaQR(id); // Assegna il valore del QR Code
				} catch (Exception e) {
					e.printStackTrace();
					Notification.show("Errore generazione QR", 3000, Notification.Position.TOP_CENTER).getElement()
							.getThemeList().add("error");
					return;
				}

				final BufferedImage finalQrCode = tempQr;

				Dialog dialog = new Dialog();

				// Da BufferedImage (volatile) a Image (componente Vaadin)
				StreamResource resource = new StreamResource("qr.png", () -> {
					try {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						ImageIO.write(finalQrCode, "png", bos);
						return new ByteArrayInputStream(bos.toByteArray());
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				});

				Image image = new Image(resource, "QR Code");
				image.setWidth("300px");

				VerticalLayout dialogLayout = new VerticalLayout(image);
				dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
				dialog.add(dialogLayout);

				H3 header = new H3(" QR Code Prenotazione");

				// Pulsante di chiusura dialog
				Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> dialog.close());
				closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
				closeButton.addClassName("dialog-close-btn");

				dialog.getHeader().add(header, closeButton);
				dialog.open();
			});

			return btn;

		}).setHeader("QR");

		// bottone per la cancellazione
		newPrenoGrid.addComponentColumn(p -> {
			StatoPrenotazione stato = calcolaStato(p);
			if (stato.equals(StatoPrenotazione.PASSATA.toString())) {
				return null;
			} else {
				Button btn = new Button("Cancella");
				btn.addClickListener(e -> prenotazioniService.cancellaPrenotazione(p));
				btn.addClassName("grid-btn-link");
				btn.addClassName("danger");
				return btn;
			}
		});

		H4 newP = new H4("Prenotazioni Attive o Future");
		newP.addClassName("text-green-dark");
		H4 oldP = new H4("Prenotazioni Passate");
		oldP.addClassName("text-green-dark");

		add(titolo, newP, newPrenoGrid, oldP, oldPrenoGrid);

		colonnineService.getColonnineInCarica().thenAccept(occupate -> {

			// Salvo la lista nella variabile della classe
			if (occupate != null) {
				this.idColonnineOccupate = occupate;
			} else {
				this.idColonnineOccupate = new ArrayList<>();
			}
			/*
			 * Chiamo funzione da firebaseService che restituisce la lista delle
			 * prenotazioni filtrate per l'utente, la lista ottenuta va in lista L'utilizzo
			 * di thenAccept permette di lavorare in maniera asincrona ed è necessaria per
			 * utilizzare il CompletableFuture in getAllReservation
			 */
			prenotazioniService.getUtenteReservation(utente.getUsername()).thenAccept(lista -> {
				getUI().ifPresent(ui -> ui.access(() -> {

					// aggiungo la lista alla griglia

					List<Prenotazione> filtrata = lista.stream()
							.filter(p -> !(calcolaStato(p) == StatoPrenotazione.PASSATA)).toList();
					newPrenoGrid.setItems(filtrata);

					List<Prenotazione> rimanenti = lista.stream()
							.filter(p -> calcolaStato(p) == StatoPrenotazione.PASSATA).toList();
					oldPrenoGrid.setItems(rimanenti);

				}));

				// gestione errori
			}).exceptionally(ex -> {
				ex.printStackTrace();
				return null;
			});

		}).exceptionally(ex -> {
			ex.printStackTrace(); // Errore nel caricamento colonnine occupate
			return null;
		});

	}

	private StatoPrenotazione calcolaStato(Prenotazione prenotazione) {
		try {
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

			LocalDate dataPren = LocalDate.parse(prenotazione.getData(), dateFormatter);
			LocalTime oraPren = LocalTime.parse(prenotazione.getInizio(), timeFormatter);
			LocalDateTime prenDateTime = LocalDateTime.of(dataPren, oraPren);
			LocalDateTime now = LocalDateTime.now();

			if (prenDateTime.isBefore(now.minusMinutes(30))) {
				return StatoPrenotazione.PASSATA;
			} else if (prenDateTime.isBefore(now)) {
				return StatoPrenotazione.ATTIVA;
			} else {
				return StatoPrenotazione.FUTURA;
			}
		} catch (Exception e) {
			return null;
		}
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

	/**
	 * Crea un layout orizzontale con 5 stelline cliccabili. Gestisce la logica
	 * visiva e il salvataggio del voto.
	 */
	private Component creaComponenteStelle(Prenotazione p) {
		HorizontalLayout starLayout = new HorizontalLayout();
		starLayout.setSpacing(false); // Tiene le stelle vicine

		// Lista per tenere traccia delle icone e poterle aggiornare visivamente
		List<Icon> starIcons = new ArrayList<>();

		int votoAttuale = 0; // Voto parte da 0, Npn è salvato finche l'utente non clicca sulle stelline

		for (int i = 1; i <= 5; i++) {
			final int starValue = i;

			// Decide se la stella è piena o vuota in base al voto attuale
			VaadinIcon iconType = (votoAttuale >= i) ? VaadinIcon.STAR : VaadinIcon.STAR_O;
			Icon star = iconType.create();

			// Stile: Colore oro e cursore "mano" al passaggio del mouse
			star.setColor(votoAttuale >= i ? "#FFD700" : "gray");
			star.addClassName("star-icon");
			
			// Gestore del click
			star.addClickListener(event -> {

				aggiornaStelle(starIcons, starValue);

				recensioniService.aggiungiRecensione(p.getUtente(), p.getIDColonnina(), starValue, p).thenRun(() -> {
					getUI().ifPresent(ui -> ui.access(() -> {
						Notification.show("Voto salvato: " + starValue + "/5", 3000, Notification.Position.TOP_CENTER)
								.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

						disabilitaStelle(starIcons);
					}));
				}).exceptionally(ex -> {
					getUI().ifPresent(ui -> ui.access(() -> {
						String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
						Notification.show("Errore: " + msg, 3000, Notification.Position.TOP_CENTER)
								.addThemeVariants(NotificationVariant.LUMO_ERROR);

					}));
					return null;
				});
			});

			starIcons.add(star);
			starLayout.add(star);
		}

		recensioniService.getRecensionePrenotazione(p).thenAccept(recensione -> {
			if (recensione != null) {
				getUI().ifPresent(ui -> ui.access(() -> {
					int votoSalvato = recensione.getStelle();

					aggiornaStelle(starIcons, votoSalvato);

					disabilitaStelle(starIcons);

				}));

			}
		});

		return starLayout;
	}

	/**
	 * Aggiorna stelle prendendo valore dal db se la valutazione è già esistente
	 * 
	 * @param icons
	 * @param valore
	 */
	private void aggiornaStelle(List<Icon> icons, int valore) {
		for (int j = 0; j < icons.size(); j++) {
			com.vaadin.flow.component.icon.Icon s = icons.get(j);
			if (j < valore) {
				s.getElement().setAttribute("icon", "vaadin:star"); // Stella piena
				s.setColor("#FFD700"); // Oro
			} else {
				s.getElement().setAttribute("icon", "vaadin:star-o"); // Stella vuota
				s.setColor("gray");
			}
		}
	}

	/**
	 * Disabilita stelle se il voto è già stato assegnato
	 * 
	 * @param icons
	 */
	private void disabilitaStelle(List<Icon> icons) {
		for (Icon s : icons) {
			s.removeClassName("star-icon"); // Rimuove il puntatore
			s.addClassName("disabled");
		}
	}

}