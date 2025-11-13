/**
 * Classe ReservationView che gestisce la pagina di consulto delle proprie prenotazioni attive o passate
 * 
 * @author Francesco Valenari, Claudio Morgera, Davide Mascheretti
 */
package com.example.views;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.example.MainLayout;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.google.api.services.storage.model.Notification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vaadin.flow.component.Text;
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

    /**
     *Costruttore che genera la griglia contenente tutte le prenotazioni dell'utente
     * @param fb Database per accedere ai dati prenotazione
     */
    public ReservationView(FirebaseService fb) {
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
            try {
                // Parsing delle stringhe data/ora
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

                LocalDate dataPren = LocalDate.parse(prenotazione.getData(), dateFormatter);
                LocalTime oraPren = LocalTime.parse(prenotazione.getInizio(), timeFormatter);

                LocalDateTime prenotazioneDateTime = LocalDateTime.of(dataPren, oraPren);
                LocalDateTime now = LocalDateTime.now();

                if (prenotazioneDateTime.isBefore(now.minusMinutes(30))) { // se già iniziata da più di 30 min
                    return "Passata";
                } else if (prenotazioneDateTime.isBefore(now)) {// se già iniziata da meno di 30 min
                    return "Attiva";
                } else {
                    return "Futura";
                }
            } catch (Exception e) {
                return "—"; // in caso di errore parsing
            }
        }).setHeader("Stato")
        .setSortable(true);
        
        
        // bottone per la cancellazione 
        reservationGrid.addComponentColumn(col -> {
		    Button btn = new Button("Cancella");
		    btn.addClickListener(e -> cancellaPrenot());
		    btn.getStyle().set("color", "red")
		                  .set("text-decoration", "underline")
		                  .set("background", "none")
		                  .set("border", "none");
		    return btn;
		});
        add(titolo, reservationGrid);
         
         /*
          * Chiamo funzione da firebaseService che restituisce la lista delle prenotazioni
          * filtrate per l'utente, la lista ottenuta va in lista
          * L'utilizzo di thenAccept permette di lavorare in maniera asincrona ed è necessaria per 
          * utilizzare il CompletableFuture in getAllReservation
          */
        prenotazioniRef.getAllReservation(utente.getUsername()).thenAccept(lista -> {
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

    //Prototipo di funzione di cancellazione (TODO)
	private void cancellaPrenot() {
	}
  


    
}