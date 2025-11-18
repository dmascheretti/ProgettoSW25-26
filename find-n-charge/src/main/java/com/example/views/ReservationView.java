/**
 * Classe ReservationView che gestisce la pagina di consulto delle proprie prenotazioni attive o passate
 * 
 * @author Francesco Valenari, Claudio Morgera, Davide Mascheretti
 */
package com.example.views;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.VaadinSession;
import com.example.MainLayout;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.example.database.FirebaseService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Route(value = "prenotazioni", layout = MainLayout.class)
@PageTitle("Find&Charge - Prenotazioni")

public class ReservationView extends VerticalLayout {
	
	

    private Grid<Prenotazione> reservationGrid = new Grid<>(Prenotazione.class);
    private CompletableFuture<List<Prenotazione>> listaPreno;
    private final FirebaseService prenotazioniRef;
    private final UI ui;

    /**
     *Costruttore che genera la griglia contenente tutte le prenotazioni dell'utente
     * @param fb Database per accedere ai dati prenotazione
     */
    public ReservationView(FirebaseService fb) {
    	this.ui = UI.getCurrent();
    	this.prenotazioniRef=fb;
        setSpacing(true);
        setPadding(true);
        //salvo utente che è nell'applicazione
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
		H3 titolo = new H3("Ciao "+utente.getUsername() +"! Ecco le tue prenotazioni...");  
		titolo.getStyle().set("color", "#008000");
        
		// Configurazione griglia (visualizzazione dei campi di ogni prenotazione)
        reservationGrid.setColumns("nomeColonnina", "data", "inizio");
        
        reservationGrid.addColumn(prenotazione -> {
        	String stato = calcolaStato(prenotazione);
            return stato;
        }).setHeader("Stato")
        .setSortable(true);
        
        
        // bottone per la cancellazione 
        reservationGrid.addComponentColumn(p -> {
        	String stato = calcolaStato(p);
            if (stato.equals("Passata")) {
                return null;
            } else {
            	Button btn = new Button("Cancella");
    		    btn.addClickListener(e -> cancellaPrenot(p));
    		    btn.getStyle().set("color", "red")
    		                  .set("text-decoration", "underline")
    		                  .set("background", "none")
    		                  .set("border", "none");
    		    return btn;
            }
		});
        add(titolo, reservationGrid);
         
         /*
          * Chiamo funzione da firebaseService che restituisce la lista delle prenotazioni
          * filtrate per l'utente, la lista ottenuta va in lista
          * L'utilizzo di thenAccept permette di lavorare in maniera asincrona ed è necessaria per 
          * utilizzare il CompletableFuture in getAllReservation
          */
        prenotazioniRef.getUtenteReservation(utente.getUsername()).thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {
				
				//aggiungo la lista alla griglia 
				reservationGrid.setItems(lista);
				
			}));
			
			//gestione errori 
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
    
    
    //funzione di cancellazione
	private void cancellaPrenot(Prenotazione p) {
		
		prenotazioniRef.cancellaPrenotazione(p).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
			Notification.show("Prenotazione eliminata con successo", 3000,
					Notification.Position.TOP_CENTER);
			
			getUI().ifPresent(ui1 -> ui1.getPage().reload());
		})))

				// gestione e messaggio di errore

		.exceptionally(ex -> {
			getUI().ifPresent(ui -> ui.access(() -> { // <-- CORREZIONE 2: getUI() e (ui -> ui.access(...))
				Notification
						.show("Errore durante il salvataggio: " + ex.getMessage(), 4000,
								Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");
			}));
			return null;
				});
	}
  


    
}