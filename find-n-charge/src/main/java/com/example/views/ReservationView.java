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
import com.vaadin.flow.component.Text;

import java.util.ArrayList;
import java.util.List;

@Route(value = "reservations", layout = MainLayout.class)
@PageTitle("Find&Charge - Prenotazioni")
public class ReservationView extends VerticalLayout {

    private Grid<Prenotazione> reservationGrid = new Grid<>(Prenotazione.class);

    public ReservationView() {
        setSpacing(true);
        setPadding(true);
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        add(new Text("Ciao "+utente.getUsername() +"! Ecco le tue prenotazioni...") ,reservationGrid);

        // Configurazione griglia (visualizzazione dei campi di ogni prenotazione)
        reservationGrid.setColumns("nomeColonnina", "data", "inizio");
        reservationGrid.setItems(loadMockReservationsForUser(utente.getUsername()));
    }
    

    //Mock di prenotazioni (solo per esempio)
    private List<Prenotazione> loadMockReservationsForUser(String user) {
        List<Prenotazione> list = new ArrayList<>();
        list.add(new Prenotazione("Colonnina A", user, "2025-03-01", "09:00"));
        list.add(new Prenotazione("Colonnina B", user, "2025-03-05", "14:30"));
        return list;
    }
}