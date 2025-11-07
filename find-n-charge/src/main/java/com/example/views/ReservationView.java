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
import com.example.Prenotazione;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

import java.util.ArrayList;
import java.util.List;

@Route("reservations")
@PageTitle("Prenotazioni")
public class ReservationView extends VerticalLayout {

    private TextField userField = new TextField("Utente");
    private Button searchButton = new Button("Cerca prenotazioni");
    private Grid<Prenotazione> reservationGrid = new Grid<>(Prenotazione.class);

    public ReservationView() {
        setSpacing(true);
        setPadding(true);

        add(userField, searchButton, reservationGrid);

        // Configurazione griglia (visualizzazione dei campi di ogni prenotazione)
        reservationGrid.setColumns("nomeColonnina", "data", "inizio");

        searchButton.getElement().getThemeList().add("success"); // Tema verde
        searchButton.addClickListener(e -> {
            String user = userField.getValue();
            if (user == null || user.isEmpty()) {
                Notification.show("Inserisci un nome utente");
                return;
            }
            // Dati fittizi: solo per mostrare come apparirebbe la UI
            reservationGrid.setItems(loadMockReservationsForUser(user));
        });
    }

    //Mock di prenotazioni (solo per esempio)
    private List<Prenotazione> loadMockReservationsForUser(String user) {
        List<Prenotazione> list = new ArrayList<>();
        list.add(new Prenotazione("Colonnina A", user, "2025-03-01", "09:00"));
        list.add(new Prenotazione("Colonnina B", user, "2025-03-05", "14:30"));
        return list;
    }
}