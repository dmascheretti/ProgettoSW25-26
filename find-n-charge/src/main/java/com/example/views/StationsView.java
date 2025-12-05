/**
 * La classe StationView gestisce la pagina della lista delle colonnine disponibili
 * 
 * @author Francesco Valenari
 */
package com.example.views;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import com.example.components.Sidebar;
import com.example.service.AutoService;
import com.example.service.ColonnineService;
import com.example.layout.MainLayout;
import com.example.models.Colonnina;
import com.example.models.Utente;
import com.example.service.PrenotazioniService;
import com.example.util.DataValidator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
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

public class StationsView extends HorizontalLayout {
	private String tema = "#008000";
	private Grid<Colonnina> colonGrid = new Grid<>(Colonnina.class);

	private PrenotazioniService prenotazioniService;
	private AutoService autoService;
	private ColonnineService colonnineService;

	private Sidebar stationSidebar;
	private Colonnina colonninaSelezionata;

	// Classe per l'elenco delle colonnine (indipendente dalla mappa)
	public StationsView(AutoService autoService, PrenotazioniService prenotazioniService,
			ColonnineService ColonnineService) {
		this.autoService = autoService;
		this.colonnineService = ColonnineService;
		this.prenotazioniService = prenotazioniService;
		setSpacing(true);
		setPadding(true);
		setSizeFull();
		getStyle().set("overflow", "hidden"); // Blocca lo scroll della pagina intera (scrolla solo la sidebar)

		// Titolo
		H3 titolo = new H3("Queste sono le colonnine più vicine a te");
		titolo.getStyle().set("color", tema);
		// Barra di ricerca per nome o indirizzo delle colonnine
		TextField searchField = new TextField("Cerca");
		searchField.getElement().getThemeList().add("success");
		searchField.setPlaceholder("Nome o indirizzo...");
		searchField.setWidth("300px");
		searchField.getStyle().set("color", tema);
		searchField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)"); // Tema verde
		// EVENTO DI RICERCA → chiama cercaColonnine()
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.addValueChangeListener(e -> {
			aggiornaGridConFiltro(e.getValue());
		});

		// Istruzioni per prenotare una colonnina su questa pagina
		Text istruz = new Text(
				"Puoi effettuare una prenotazione direttamente su questa pagina...Ti basterà premere sul nome della colonnina interessata");

		// Aggiunta Colonna di bottoni, targati col nome delle colonnine,che permettono
		// la prenotazione
		colonGrid.removeAllColumns();
		colonGrid.addComponentColumn(col -> {
			Button btn = new Button(col.getNome());
			btn.addClickListener(e -> showSidebar(col));
			btn.getStyle().set("color", tema).set("text-decoration", "underline").set("background", "none")
					.set("border", "none");
			return btn;
		}).setHeader("Nome").setComparator(Colonnina::getNome).setSortable(true);

		// Colonne con altre informazioni delle colonnine
		colonGrid.addColumn(Colonnina::getTipo).setHeader("Tipo").setSortable(true);

		colonGrid.addColumn(Colonnina::getStato).setHeader("Stato").setSortable(true);

		colonGrid.addColumn(Colonnina::getIndirizzo).setHeader("Indirizzo").setSortable(true);

		colonGrid.addColumn(Colonnina::getComune).setHeader("Comune").setSortable(true);

		// Caricamento iniziale senza filtri
		aggiornaGridConFiltro("");

		// Crea la sidebar
		stationSidebar = new Sidebar();
		stationSidebar.setHeightFull(); // Occupa tutta l'altezza disponibile
		stationSidebar.getStyle().set("overflow-y", "auto"); // Abilita lo scroll solo per la sidebar

		configuraGestioneOrari(); // Per gestire gli slot orari
		reservationLogic(); // Per gestire le prenotazioni
		configuraAutoUtente(); // Per gestire le auto dell'utente

		VerticalLayout layout = new VerticalLayout(titolo, searchField, istruz, colonGrid);
		layout.setSizeFull();
		layout.expand(colonGrid);

		add(layout, stationSidebar);
	}

	private void showSidebar(Colonnina col) {
		this.colonninaSelezionata = col;
		stationSidebar.setDati(col);
	}

	private void aggiornaGridConFiltro(String query) {
		colonnineService.cercaColonnine(query).thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {
				colonGrid.setItems(lista);
			}));
		});
	}

	private void reservationLogic() {

		stationSidebar.getPrenotaButton().addClickListener(e -> {
			Utente utenteCorrente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
			LocalDate data = stationSidebar.getDataSelezionata();
			String orario = stationSidebar.getOrarioSelezionato();
			String autoSelezionata = stationSidebar.getAutoSelected();

			if (utenteCorrente == null) {
				Notification.show("Effettua il login per prenotare.", 3000, Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");
				getUI().ifPresent(ui -> ui.navigate("login"));
				return;
			}

			if (colonninaSelezionata == null) {
				Notification.show("Nessuna colonnina selezionata.", 3000, Notification.Position.TOP_CENTER).getElement()
						.getThemeList().add("error");
				return;
			}

			if (autoSelezionata == null) {
				Notification.show("Nessuna auto selezionata.", 3000, Notification.Position.TOP_CENTER).getElement()
						.getThemeList().add("error");
				return;
			}

			if (data == null || orario == null) {
				Notification.show("Seleziona data e orario.", 3000, Notification.Position.TOP_CENTER).getElement()
						.getThemeList().add("error");
				return;
			}

			String errore = DataValidator.verificaPrenotazione(colonninaSelezionata.getId(), data, orario);
			if (errore != null) {
				Notification.show(errore, 3000, Notification.Position.TOP_CENTER).getElement().getThemeList()
						.add("error");
				return;
			}

			String dataString = data.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

			prenotazioniService.prenota(colonninaSelezionata, utenteCorrente, dataString, orario, autoSelezionata)
					.thenAccept(success -> {

						getUI().ifPresent(ui -> ui.access(() -> {

							if (!success) {
								Notification
										.show("Slot occupato o errore durante la prenotazione!", 3000,
												Notification.Position.TOP_CENTER)
										.getElement().getThemeList().add("error");
								return;
							}

							Notification.show("Prenotazione confermata!", 3000, Notification.Position.TOP_CENTER)
									.getElement().getThemeList().add("success");

							stationSidebar.setVisible(false);

						}));

					});
		});
	}

	private void configuraAutoUtente() {
		Utente utenteCorrente = (Utente) VaadinSession.getCurrent().getAttribute("utente");

		if (utenteCorrente == null) {
			return;
		}

		autoService.getTargheUtente(utenteCorrente).thenAccept(autoUtente -> {

			getUI().ifPresent(ui -> ui.access(() -> {
				stationSidebar.setAuto(autoUtente);
			}));
		});

	}

	private void configuraGestioneOrari() {
		stationSidebar.getBookingDatePicker().addValueChangeListener(e -> {

			LocalDate dataScelta = e.getValue();
			if (dataScelta == null)
				return;

			stationSidebar.aggiornaOrari(dataScelta, Collections.emptyList()); // Forza l'aggiornamento anche se la data
																				// non cambia

			if (colonninaSelezionata != null) {

				String dataString = dataScelta.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

				prenotazioniService.getSlotOccupati(colonninaSelezionata.getId(), dataString)
						.thenAccept(listaOccupati -> {
							getUI().ifPresent(ui -> ui.access(() -> {
								stationSidebar.aggiornaOrari(dataScelta, listaOccupati);
							}));
						});
			}
		});
	}

}
