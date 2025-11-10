/**
 * Classe ReservationView che gestisce la pagina di consulto delle proprie prenotazioni attive o passate
 * 
 * @author Francesco Valenari
 */
package com.example.views;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.example.MainLayout;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.google.api.services.storage.model.Notification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vaadin.flow.component.Text;
import com.example.database.FirebaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Route(value = "prenotazioni", layout = MainLayout.class)
@PageTitle("Find&Charge - Prenotazioni")
public class ReservationView extends VerticalLayout {

    private Grid<Prenotazione> reservationGrid = new Grid<>(Prenotazione.class);
    private CompletableFuture<List<Prenotazione>> listaPreno;
    private final FirebaseService prenotazioniRef;

    public ReservationView(FirebaseService fb) {
    	this.prenotazioniRef=fb;
        setSpacing(true);
        setPadding(true);
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        add(new Text("Ciao "+utente.getUsername() +"! Ecco le tue prenotazioni...") ,reservationGrid);
        listaPreno = prenotazioniRef.getAllReservation(utente.getUsername());
        
        
     // Configurazione griglia (visualizzazione dei campi di ogni prenotazione)
        reservationGrid.setColumns("nomeColonnina", "data", "inizio");
         add(reservationGrid);
        prenotazioniRef.getAllReservation(utente.getUsername()).thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {
				
				reservationGrid.setItems(lista);
				
			}));
			
        });
			
			
 }
  


    
}