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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.example.util.QRCode;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.example.database.FirebaseColonnineService;
import com.example.database.FirebasePrenotazioniService;
import com.example.layout.MainLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Route(value = "prenotazioni", layout = MainLayout.class)
@PageTitle("Find&Charge - Prenotazioni")

public class ReservationView extends VerticalLayout implements BeforeEnterObserver {

	private Grid<Prenotazione> newPrenoGrid = new Grid<>(Prenotazione.class, false);
	private Grid<Prenotazione> oldPrenoGrid = new Grid<>(Prenotazione.class, false);
    private List<String> idColonnineOccupate = new ArrayList<>();
	private CompletableFuture<List<Prenotazione>> listaPreno;
	private FirebasePrenotazioniService firebasePrenotazioniService;
	private FirebaseColonnineService firebaseColonnineService;
	private final UI ui;
	private QRCode qr;
	
	/**
	 * Costruttore che genera la griglia contenente tutte le prenotazioni
	 * dell'utente
	 * 
	 * @param fb Database per accedere ai dati prenotazione
	 */
	
	public ReservationView(FirebasePrenotazioniService firebasePrenotazioniService, FirebaseColonnineService firebaseColonnineService) {
		this.firebasePrenotazioniService=firebasePrenotazioniService;
		this.firebaseColonnineService=firebaseColonnineService;
		this.ui = UI.getCurrent();
		
		// salvo utente che è nell'applicazione
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
		if (utente == null) {
			return;
		}
		
		setSpacing(true);
		setPadding(true);
		H3 titolo = new H3("Ciao " + utente.getUsername() + "! Ecco le tue prenotazioni...");
		titolo.getStyle().set("color", "#008000");

		// Configurazione griglia (visualizzazione dei campi di ogni prenotazione)
		
		// Prenotazioni attive o future
		newPrenoGrid.removeAllColumns();
		newPrenoGrid.addColumn(Prenotazione::getIDColonnina).setHeader("Colonnina").setSortable(true);
		newPrenoGrid.addColumn(Prenotazione::getData).setHeader("Data").setSortable(true);
		newPrenoGrid.addColumn(Prenotazione::getInizio).setHeader("Inizio").setSortable(true);
		newPrenoGrid.addColumn(prenotazione -> {
			String stato = calcolaStato(prenotazione);
			return stato;
		}).setHeader("Stato").setSortable(true);
		
		//Prenotazioni passate
		oldPrenoGrid.removeAllColumns();
		oldPrenoGrid.addColumn(Prenotazione::getIDColonnina).setHeader("Colonnina").setSortable(true);
		oldPrenoGrid.addColumn(Prenotazione::getData).setHeader("Data").setSortable(true);
		oldPrenoGrid.addColumn(Prenotazione::getInizio).setHeader("Inizio").setSortable(true);

		firebaseColonnineService.getColonnineInCarica().thenAccept(listaInCarica -> {
		    
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
		
		// Aggiunge la colonna per la visualizzazione del QR Code attivabile 5 minuti prima dello slot orario prenotato
		newPrenoGrid.addComponentColumn(p -> {
			
			try {
		        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		        
		        // Trasforma le stringhe in LocalDate e LocalTime formattati
		        LocalDate dataPrenotazione = LocalDate.parse(p.getData(), dateFormatter);
		        LocalTime oraInizio = LocalTime.parse(p.getInizio(), timeFormatter);
		        
		        LocalDateTime inizioPrenotazione = LocalDateTime.of(dataPrenotazione, oraInizio);	//Inizio slot prenotazione
		        LocalDateTime adesso = LocalDateTime.now();
		        LocalDateTime orarioMinimo = inizioPrenotazione.minusMinutes(5);
		        
		        // Se è troppo presto il bottone non compare
		        if (adesso.isBefore(orarioMinimo)) {
		            Span text = new Span("Disponibile 5 min prima.");
		            text.getStyle().set("font-size", "12px").set("color", "gray");
		            return text; 
		        }
		        //se la colonnina è occupata da un altro utente, il bottone non compare
		        if (this.idColonnineOccupate.contains(p.getIDColonnina()) && !p.getStato().equals("In carica")) {
		            Span text = new Span("Colonnina occupata.");
		            text.getStyle().set("font-size", "12px").set("color", "red");
		            return text;
		        }     
		        
		    } catch (Exception e) {
		        e.printStackTrace();
		        return null;
		    }
			
			Button btn = new Button("Visualizza QR");
			btn.getStyle().set("color", "green").set("text-decoration", "underline").set("background", "none")
					.set("border", "none");

			btn.addClickListener(event -> {
				String id = p.getId();
				
					firebasePrenotazioniService.aggiornaStato(p, "In carica")
			    .exceptionally(ex -> {
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
				closeButton.getStyle().set("align-self", "flex-end");
				closeButton.getElement().getThemeList().add("success");
				closeButton.getStyle().set("margin-left", "auto"); // Spinge il pulsante tutto a destra

				dialog.getHeader().add(header, closeButton);
				dialog.open();
			});

			return btn;
		
		}).setHeader("");

		// bottone per la cancellazione
		newPrenoGrid.addComponentColumn(p -> {
			String stato = calcolaStato(p);
			if (stato.equals("Passata")) {
				return null;
			} else {
				Button btn = new Button("Cancella");
				btn.addClickListener(e -> cancellaPrenot(p));
				btn.getStyle().set("color", "red").set("text-decoration", "underline").set("background", "none")
						.set("border", "none");
				return btn;
			}
		});
		
		H4 newP = new H4("Prenotazioni Attive o Future");
		newP.getStyle().set("color", "#00E000");
		H4 oldP = new H4("Prenotazioni Passate");
		oldP.getStyle().set("color", "#00E000");
		
		add(titolo, newP, newPrenoGrid, oldP, oldPrenoGrid);

		firebaseColonnineService.getColonnineInCarica().thenAccept(occupate -> {

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
			firebasePrenotazioniService.getUtenteReservation(utente.getUsername()).thenAccept(lista -> {
				getUI().ifPresent(ui -> ui.access(() -> {
	
					// aggiungo la lista alla griglia
					
					List<Prenotazione> filtrata = lista.stream()
				            .filter(p -> !calcolaStato(p).equals("Passata"))
				            .toList();
					newPrenoGrid.setItems(filtrata);
					
					List<Prenotazione> rimanenti = lista.stream()
				            .filter(p -> calcolaStato(p).equals("Passata"))
				            .toList();
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

	// funzione di cancellazione, DA METTERE IN UNA CLASSE A PARTE!
	private void cancellaPrenot(Prenotazione p) {

		firebasePrenotazioniService.cancellaPrenotazione(p).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
			Notification.show("Prenotazione eliminata con successo", 3000, Notification.Position.TOP_CENTER);

			getUI().ifPresent(ui1 -> ui1.getPage().reload());
		})))

				// gestione e messaggio di errore

				.exceptionally(ex -> {
					getUI().ifPresent(ui -> ui.access(() -> {
						Notification.show("Errore durante il salvataggio: " + ex.getMessage(), 4000,
								Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
					}));
					return null;
				});
	}

	/**
     * Se l'utente prova ad accedere direttamente a questa pagina senza aver effettuato l'accesso,
     * lo si reindirizza alla pagina di login mostrando una notifica di errore.
     * beforeEnter viene eseguito un attimo prima che la pagina venga mostrata all'utente.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        
        if (utente == null) {
            event.forwardTo("");	//Reindirizza alla pagina di login
            Registration[] registrationWrapper = new Registration[1];		//Array per registrare l'aggiunta del listener
            //Dopo che ha cambiato pagina, mostra la notifica
            registrationWrapper[0] = UI.getCurrent().addAfterNavigationListener(navEvent -> {
            	Notification.show("Utente non trovato. Effettua il login.", 3000, Notification.Position.TOP_CENTER)
                .getElement().getThemeList().add("error");

                //Rimuove il listener, altrimenti scatterebbe ogni volta
                if (registrationWrapper[0] != null) {
                    registrationWrapper[0].remove();
                }
            });
        }
    }

}