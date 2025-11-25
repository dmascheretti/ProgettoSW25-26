/**Classe che modella la sidebar, una scheda che si apre quando viene cliccata una colonnina sulla mappa o dalla lista.
 * Nella sidebar appaiono tutti i dettagli della colonnina 
 * 
 * @author Maistrello Tommaso
 */

package com.example.components;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.example.database.FirebaseService;
import com.example.models.Colonnina;
import com.example.models.Utente;
import com.example.util.DataValidator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.server.VaadinSession;
import com.example.util.PrenotazioneService;

public class Sidebar extends VerticalLayout {

	private H3 title;
	private VerticalLayout details;
	private DatePicker bookingDatePicker; // Calendario interattivo
	private ComboBox<String> bookingTimeSlot; // Menù a tendina con gli slot orari
	private FirebaseService firebaseService;
	private PrenotazioneService prenotazioneService;
	private Button prenotaButton;
	private com.vaadin.flow.shared.Registration prenotaButtonListener; // Per avere un solo linstener attivo

	public Sidebar() {

		this.firebaseService = firebaseService;
		this.prenotazioneService = new PrenotazioneService(firebaseService);

		setWidth("35%");
		setHeightFull();
		getStyle().set("background-color", "var(--lumo-base-color)")
				.set("border-left", "1px solid var(--lumo-contrast-20pct)").set("padding", "var(--lumo-space-m)");

		// Di default deve essere nascosta
		setVisible(false);

		Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> setVisible(false));
		closeButton.getStyle().set("align-self", "flex-end");
		closeButton.getElement().getThemeList().add("success");

		title = new H3("Dettagli");
		details = new VerticalLayout();
		details.setSpacing(false); // Rimuove spazio extra tra le righe
		details.setPadding(false); // Rimuove padding

		bookingDatePicker = new DatePicker("Giorno");
		bookingDatePicker.setMin(LocalDate.now()); // Non si può prenotare nel passato
		bookingDatePicker.setValue(LocalDate.now()); // Valore di default
		bookingDatePicker.getStyle().set("width", "100%");
		bookingDatePicker.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		bookingTimeSlot = new ComboBox<>("Orario (slot 30 min)");
		bookingTimeSlot.setEnabled(false);

		bookingDatePicker.addValueChangeListener(event -> {

			LocalDate dataSelezionata = event.getValue();

			// Se la modifica è stata fatta da Java (nel metodo onMarkerClick()), ignorala
			// ed esci
			if (!event.isFromClient()) {
				return;
			}

			// Se l'utente ha tolto la data, non genero slot
			if (dataSelezionata == null) {
				bookingTimeSlot.clear(); // Pulisce gli slot vecchi
				bookingTimeSlot.setEnabled(false); // Disabilita la tendina degli orari
				return; // Esce dal listener
			}

			// La data è stata inserita, quindi abilita la tendina
			bookingTimeSlot.setEnabled(true);

			// Genera i time slots
			bookingTimeSlot.setItems(generateTimeSlots(dataSelezionata));

		});

		bookingTimeSlot.getStyle().set("width", "100%");
		bookingTimeSlot.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		prenotaButton = new Button("Prenota ora");
		prenotaButton.getElement().getThemeList().add("success");
		prenotaButton.getStyle().set("margin-top", "var(--lumo-space-l)");

		add(closeButton, title, details, bookingDatePicker, bookingTimeSlot, prenotaButton);

	}	

	public void setDati(Colonnina colonnina) {
		// Mette i dati della colonnina nella sidebar
        this.title.setText(colonnina.getNome());
		// Pulisci i dettagli vecchi
        this.details.removeAll();
        this.details.setAlignItems(Alignment.CENTER);

        //Aggiunge l'immagine (se esiste)
        if (colonnina.getLinkImmagine() != null && !colonnina.getLinkImmagine().isEmpty()) {
            Image image = new Image(colonnina.getLinkImmagine(), "IMMAGINE COLONNINA");
            image.setWidth("250px");
            image.getStyle().set("margin-top", "10px").set("margin-bottom", "10px");
            this.details.add(image);
        }

		this.details.add( // Span serve per poter mandare a capo le righe
				new Span("Indirizzo: " + colonnina.getIndirizzo() + ", " + colonnina.getComune()),
				new Span("Stato: " + colonnina.getStato()));

		// Pulisce i campi di prenotazione precedenti
		bookingDatePicker.setValue(null);
		bookingTimeSlot.setValue(null);
		bookingTimeSlot.setEnabled(false);
		
        setVisible(true);
        
	}
	
	/**
	 * Metodo per generare la lista degli slot orari. Se la data è oggi, parte
	 * dall'orario attuale arrotondato alla mezz'ora successiva. Se la data è
	 * futura, parte da mezzanotte.
	 * 
	 * @param date La data per cui generare gli slot.
	 * @return Lista di stringhe formato "HH:mm".
	 */
	private List<String> generateTimeSlots(LocalDate date) {
		List<String> slots = new ArrayList<>();

		// Orario di partenza
		LocalTime time;

		// Se la data selezionata è oggi, calcola la prossima mezz'ora, altrimenti parte
		// da mezzanotte
		if (date.equals(LocalDate.now())) {
			LocalTime now = LocalTime.now();
			if (now.getMinute() < 30) {
				time = now.withMinute(30).withSecond(0).withNano(0);
			} else {
				time = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
			}

			if (time.equals(LocalTime.MIDNIGHT)) { // Se siamo nell'ultima mezz'ora del giorno (dopo le 23.40), allora
													// la lista di slot è vuota per oggi
				return slots;
			}
		} else {
			time = LocalTime.MIDNIGHT;
		}

		// Generazione slots fino al giorno successivo in formato "HH:mm"
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

		while (true) {
			slots.add(time.format(formatter));
			LocalTime nextTime = time.plusMinutes(30);
			if (nextTime.equals(LocalTime.MIDNIGHT))
				break;
			time = nextTime;
		}

		return slots;
	}
	public LocalDate getDataSelezionata() {
        return bookingDatePicker.getValue();
    }

    public String getOrarioSelezionato() {
        return bookingTimeSlot.getValue();
    }

    public Button getPrenotaButton() {
        return prenotaButton;
    }
}
