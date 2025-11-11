/**
 * La classe StationView gestisce la pagina della lista delle colonnine disponibili
 * 
 * @author Francesco Valenari
 */
package com.example.views;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.example.MainLayout;
import com.example.database.FirebaseService;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.example.util.RegisterValidator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Find&Charge - Colonnine")
@Route(value = "colonnine", layout = MainLayout.class)

public class StationsView extends VerticalLayout {
private String tema = "#008000";
private Grid<Colonnina> colonGrid = new Grid<>(Colonnina.class);
private FirebaseService firebaseService = new FirebaseService(); // è un'istanza di FirebaseSystem
private VerticalLayout stationSidebar;
private H3 sidebarTitle;
private VerticalLayout sidebarDetails;

private DatePicker bookingDatePicker;
private ComboBox<String> bookingTimeSlot;
private Button prenotaButton;

private com.vaadin.flow.shared.Registration prenotaButtonListener;

private Colonnina colonninaSelezionata;


	// Classe per l'elenco delle colonnine (indipendente dalla mappa)
	public StationsView() {
		setSpacing(true);
        setPadding(true);
        
        //Titolo
		H3 titolo = new H3("Queste sono le colonnine più vicine a te");
		titolo.getStyle().set("color", tema);
		//Barra di ricerca per nome o indirizzo delle colonnine
		TextField searchField = new TextField("Cerca");
		searchField.getElement().getThemeList().add("success");
        searchField.setPlaceholder("Nome o indirizzo...");
        searchField.setWidth("300px");
        searchField.getStyle().set("color", tema);
        // EVENTO DI RICERCA → chiama cercaColonnine()
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> {
            aggiornaGridConFiltro(e.getValue());
        });
        
        //Istruzioni per prenotare una colonnina su questa pagina
        Text istruz = new Text("Puoi effettuare una prenotazione direttamente su questa pagina...Ti basterà premere sul nome della colonnina interessata");

        // Colonna cliccabile per "nome"
        colonGrid.removeAllColumns();
        colonGrid.addComponentColumn(col -> {
            com.vaadin.flow.component.button.Button btn = new com.vaadin.flow.component.button.Button(col.getNome());
            btn.addClickListener(e -> showSidebar(col));
            btn.getStyle().set("color", tema).set("text-decoration", "underline").set("background", "none").set("border", "none");
            return btn;
        }).setHeader("Nome");

        // Altre colonne normali
        colonGrid.addColumn(Colonnina::getTipo).setHeader("Tipo");
        colonGrid.addColumn(Colonnina::getStato).setHeader("Stato");
        colonGrid.addColumn(Colonnina::getIndirizzo).setHeader("Indirizzo");
        colonGrid.addColumn(Colonnina::getComune).setHeader("Comune");

        // Caricamento iniziale senza filtri
        aggiornaGridConFiltro("");

        stationSidebar = createSidebar();
        HorizontalLayout layout = new HorizontalLayout(colonGrid, stationSidebar);
        layout.setSizeFull();
        layout.expand(colonGrid);

        add(titolo, searchField, istruz, layout);
    }
	
	
	private VerticalLayout createSidebar() {
	    VerticalLayout sidebar = new VerticalLayout();
	    sidebar.setWidth("350px");
	    sidebar.setHeightFull();
	    sidebar.getStyle().set("background-color", "var(--lumo-base-color)")
	            .set("border-left", "1px solid var(--lumo-contrast-20pct)")
	            .set("padding", "var(--lumo-space-m)");

	    sidebar.setVisible(false);

	    Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> sidebar.setVisible(false));
	    closeButton.getStyle().set("align-self", "flex-end");
	    closeButton.getElement().getThemeList().add("success");

	    sidebarTitle = new H3("Dettagli colonnina");
	    sidebarDetails = new VerticalLayout();
	    sidebarDetails.setSpacing(false);
	    sidebarDetails.setPadding(false);

	    bookingDatePicker = new DatePicker("Giorno");
	    bookingDatePicker.setMin(LocalDate.now());

	    bookingTimeSlot = new ComboBox<>("Orario (slot 30 min)");
	    bookingTimeSlot.setItems(generateTimeSlots());

	    prenotaButton = new Button("Prenota ora");
	    prenotaButton.getElement().getThemeList().add("success");

	    sidebar.add(closeButton, sidebarTitle, sidebarDetails,
	            bookingDatePicker, bookingTimeSlot, prenotaButton);

	    return sidebar;
	}
	
	private void showSidebar(Colonnina col) {
	    this.colonninaSelezionata = col;

	    sidebarTitle.setText(col.getNome());

	    sidebarDetails.removeAll();
	    sidebarDetails.add(
	            new Span("Indirizzo: " + col.getIndirizzo() + ", " + col.getComune()),
	            new Span("Stato: " + col.getStato())
	    );

	    bookingDatePicker.clear();
	    bookingTimeSlot.clear();

	    if (prenotaButtonListener != null) {
	        prenotaButtonListener.remove();
	        prenotaButtonListener = null;
	    }

	    prenotaButtonListener = prenotaButton.addClickListener(e -> effettuaPrenotazione());

	    stationSidebar.setVisible(true);
	}
	
	
	private List<String> generateTimeSlots() {
	    List<String> slots = new ArrayList<>();
	    LocalTime t = LocalTime.MIDNIGHT;
	    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

	    for (int i = 0; i < 48; i++) {
	        slots.add(t.format(fmt));
	        t = t.plusMinutes(30);
	    }
	    return slots;
	}
	
	
    private void aggiornaGridConFiltro(String query) {
        firebaseService.cercaColonnine(query).thenAccept(lista -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                colonGrid.setItems(lista);
            }));
        });
    }
    
    private void effettuaPrenotazione() {

        Utente utenteCorrente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        LocalDate data = bookingDatePicker.getValue();
        String orario = bookingTimeSlot.getValue();

        if (data == null || orario == null) {
            Notification.show("Seleziona data e orario.", 3000, Notification.Position.TOP_CENTER)
                .getElement().getThemeList().add("error");
            return;
        }

        String dataString = data.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        if (utenteCorrente == null) {
            Notification.show("Effettua il login per prenotare.", 3000, Notification.Position.TOP_CENTER)
                    .getElement().getThemeList().add("error");
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }

        String errore = RegisterValidator.verificaPrenotazione(
                colonninaSelezionata.getId(), dataString, orario
        );

        if (errore != null) {
            Notification.show(errore, 3000, Notification.Position.TOP_CENTER)
                    .getElement().getThemeList().add("error");
            return;
        }

        firebaseService.cercaPrenotazione(colonninaSelezionata, dataString, orario)
            .thenAccept(prenotazione -> {
                getUI().ifPresent(ui -> ui.access(() -> {

                    if (prenotazione != null) {
                        Notification.show("Slot già occupato.", 3000, Notification.Position.TOP_CENTER)
                                .getElement().getThemeList().add("error");
                        return;
                    }

                    Prenotazione p = new Prenotazione(
                            colonninaSelezionata.getNome(),
                            utenteCorrente.getUsername(),
                            dataString,
                            orario
                    );

                    firebaseService.salvaPrenotazione(p)
                        .thenRun(() -> {
                            ui.access(() -> {
                                Notification.show("Prenotazione confermata!", 3000,
                                                Notification.Position.TOP_CENTER)
                                        .getElement().getThemeList().add("success");
                                stationSidebar.setVisible(false);
                            });
                        })
                        .exceptionally(ex -> {
                            ui.access(() -> Notification.show("Errore: " + ex.getMessage(), 3000,
                                    Notification.Position.TOP_CENTER)
                                    .getElement().getThemeList().add("error"));
                            return null;
                        });

                }));
            });
    }
}
