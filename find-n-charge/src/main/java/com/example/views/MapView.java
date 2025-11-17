/**
 * Classe MapView usata per visualizzare la mappa Leaflet (presa online) nella web app.
 * 
 * @author Tommaso Maistrello
 */

package com.example.views;

import com.example.MainLayout;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;
import com.example.models.Utente;
import com.example.util.DataValidator;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.database.FirebaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Find&Charge - Mappa")
@Route(value = "map", layout = MainLayout.class) // Carica la pagina nel layout della home page
@RouteAlias(value = "main", layout = MainLayout.class) // URL alternativo

public class MapView extends HorizontalLayout {

	private Div mapDiv;
	private String mapId = "map-container-" + System.currentTimeMillis(); // Id univoco -> evita conflitti

	// Componenti per la Sidebar
	private VerticalLayout stationSidebar;
	private H3 sidebarTitle;
	private VerticalLayout sidebarDetails;
	private DatePicker bookingDatePicker; // Calendario interattivo
	private ComboBox<String> bookingTimeSlot; // Menù a tendina con gli slot orari
	private Button prenotaButton;
	private com.vaadin.flow.shared.Registration prenotaButtonListener; // Per avere un solo linstener attivo

	// Serve Firebase con la lista della colonnine del database
	private FirebaseService firebaseService;
	private List<Colonnina> colonnine;
	private Colonnina colonninaSelezionata;
	private ObjectMapper objectMapper = new ObjectMapper(); // Per tradurre gli oggetti da Java a JSON

	public MapView(@Autowired FirebaseService firebaseService) {

		this.firebaseService = firebaseService;

		setSizeFull();
		setPadding(false);
		setSpacing(false);
		getStyle().set("height", "100vh"); // Altrimenti la mappa collassa a 0px

		// Crea la sidebar
		stationSidebar = createSidebar();

		// Crea il contenitore per la mappa
		mapDiv = new Div();
		mapDiv.setSizeFull();
		mapDiv.setId(mapId);
		add(mapDiv);

		// Aggiunge mappa e sidebar all'HorizontalLayout
		add(mapDiv, stationSidebar);
		expand(mapDiv); // La mappa occupa tutto lo spazio disponibile

		// Carica i file CSS e JS di Leaflet
		UI.getCurrent().getPage().addStyleSheet("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css");
		UI.getCurrent().getPage().addJavaScript("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js");

	}

	/**
	 * Crea la scheda colonnina
	 */
	private VerticalLayout createSidebar() {

		VerticalLayout sidebar = new VerticalLayout();
		sidebar.setWidth("350px");
		sidebar.setHeightFull();
		sidebar.getStyle().set("background-color", "var(--lumo-base-color)")
				.set("border-left", "1px solid var(--lumo-contrast-20pct)").set("padding", "var(--lumo-space-m)");

		// Di default deve essere nascosta
		sidebar.setVisible(false);

		Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> sidebar.setVisible(false));
		closeButton.getStyle().set("align-self", "flex-end");
		closeButton.getElement().getThemeList().add("success");

		sidebarTitle = new H3("Dettagli");
		sidebarDetails = new VerticalLayout();
		sidebarDetails.setSpacing(false); // Rimuove spazio extra tra le righe
		sidebarDetails.setPadding(false); // Rimuove padding

		bookingDatePicker = new DatePicker("Giorno");
		bookingDatePicker.setMin(LocalDate.now()); // Non si può prenotare nel passato
		bookingDatePicker.getStyle().set("width", "100%");
		bookingDatePicker.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		bookingTimeSlot = new ComboBox<>("Orario (slot 30 min)");
		bookingTimeSlot.setItems(generateTimeSlots()); // Carica gli slot
		bookingTimeSlot.getStyle().set("width", "100%");
		bookingTimeSlot.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)");

		prenotaButton = new Button("Prenota ora");
		prenotaButton.getElement().getThemeList().add("success");
		prenotaButton.getStyle().set("margin-top", "var(--lumo-space-l)");

		sidebar.add(closeButton, sidebarTitle, sidebarDetails, bookingDatePicker, bookingTimeSlot, prenotaButton);

		return sidebar;
	}

	/**
	 * Metodo per generare la lista degli slot orari
	 * 
	 * @return Lista di stringhe con formato: "HH:mm"
	 */
	private List<String> generateTimeSlots() {

		List<String> slots = new ArrayList<>();

		LocalTime time = LocalTime.MIDNIGHT;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

		// Uno slot ogni 30 minuti -> 48 slot in 24 ore
		for (int i = 0; i < 48; i++) {
			slots.add(time.format(formatter));
			time = time.plusMinutes(30);
		}
		return slots;
	}

	/**
	 * Sovrascrive il metodo standard di Vaadin che viene chiamato automaticamente
	 * quando il componente (MapView) viene aggiunto al DOM (Document Object Model,
	 * rappresentazione ad albero che il browser crea del documento web).
	 * 
	 * A questo punto questo metodo esegue codice JS per interagire con gli elementi
	 * della pagina: ogni 100ms controlla che la libreria esterna Leaflet.js sia
	 * stata caricata e sia disponibile; quando è pronta, inizializza la mappa
	 * all'interno dello specifico Div (mapId); imposta le coordinate di default su
	 * Dalmine e aggiunge il layer con le grafiche;
	 *
	 * @param attachEvent Evento che segnala l'aggiunta del componente al DOM.
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);

		// Per l'asincronicità
		UI ui = attachEvent.getUI();

		String jsCode =
				// Questa parte di codice deve aspettare che il file .js di Leaflet sia stato
				// scaricato, quindi lo controlliamo ciclicamente ogni 100ms
				"var checkLeaflet = setInterval(function() {" + "    if (typeof L !== 'undefined') {" + // Controlla se
																										// il file è
																										// stato
																										// scaricato
						"    clearInterval(checkLeaflet);" + // Se esiste, ferma il controllo
						"    var mapElement = document.getElementById('" + mapId + "');" + // Cerca il div che deve
																							// contenere la mappa
						"    if (!mapElement) return;" + // Se non esiste interrompe
						"    if (mapElement._leaflet_id) return;" + // Controlla che non ci sia già una mappa nel div,
																	// in caso affermativo interrompe

						// Crea la mappa
						"    var map = L.map('" + mapId + "').setView([45.6493, 9.6021], 15);" + // Crea la mappa con
																									// coordinate
																									// default su
																									// Dalmine e zoom
																									// pari a 15
						"    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" + // Si appoggia a
																									// OpenStreetMap per
																									// disegnare le
																									// mattonelle della
																									// mappa
						"      attribution: '© OpenStreetMap contributors'" + "    }).addTo(map);" +

						// Trasforma la stringa JSON in un array JS
						"    var stations = JSON.parse($1);" + // $1 verrà sostituito da stationsJson
						"    var component = $0;" + // $0 è 'this.getElement()'

						// Crea i marker
						"    stations.forEach(function(station) {" + // Ciclo su ogni elemento dell'array station
						"      var marker = L.marker([station.lat, station.lon]).addTo(map);" + // Per ogni colonnina
																								// crea il marker
						"      marker.on('click', function() {" + // Aggiunge il listener di click alla componente
						"      component.$server.onMarkerClick(station.id);" + "      });});}}, 100);"; // Ciclo ogni
																										// 100ms

		// Se getAllColonnine() ha successo
		firebaseService.getAllColonnine().thenAccept(stations -> {

			// Memorizza la lista delle colonnine
			this.colonnine = stations;

			String stationsJson;
			try {
				// Prende i dati che servono per disegnare il marker
				List<Map<String, Object>> markerData = stations.stream().map(c -> { // Trasforma la lista
																					// List<Colonnina> in una
																					// List<Map<String, Object>>
					// Usa un HashMap esplicito per evitare problemi di inferenza dei tipi
					Map<String, Object> map = new java.util.HashMap<>();
					map.put("id", c.getId());
					map.put("lat", c.getLatitudine());
					map.put("lon", c.getLongitudine());
					return map; // Restituisce l'HashMap
				}).collect(Collectors.toList());

				stationsJson = objectMapper.writeValueAsString(markerData); // Converte la lista Java in una lista JSON

			} catch (JsonProcessingException e) {
				e.printStackTrace();
				stationsJson = "[]"; // Array vuoto in caso di errore
			}

			// Esegue il JS in modo sicuro sul thread della UI
			// Dobbiamo usare ui.access() perché siamo in un thread asincrono
			String finalStationsJson = stationsJson;
			ui.access(() -> { // Questo thread deve essere eseguito sulla UI perchè deve aggiornarla
				getElement().executeJs(jsCode, getElement(), finalStationsJson); // Viene eseguito il codice JS
			});

		}).exceptionally(ex -> { // In caso fallisca getAllColonnine()
			ex.printStackTrace();
			ui.access(() -> {
				Notification.show("Errore nel caricamento delle colonnine: " + ex.getMessage(), 3000,
						Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
			});
			return null;
		});

	}

	/**
	 * Metodo richiamato da JS quando un marker viene cliccato e attiva la sidebar.
	 *
	 * @param stationId L'ID (chiave univoca di Firebase) della colonnina cliccata.
	 */
	@ClientCallable // Rende il codice Java chiamabile da codice JS
	public void onMarkerClick(String stationId) {

		// Esegue sul thread UI principale
		getUI().ifPresent(ui -> ui.access(() -> {

			// Trova la colonnina nella lista e la salva nella variabile d'istanza
			this.colonninaSelezionata = colonnine.stream().filter(c -> stationId.equals(c.getId())).findFirst()
					.orElse(null);

			// Controlla la variabile d'istanza
			if (colonninaSelezionata != null) {

				// Mette i dati della colonnina nella sidebar
				sidebarTitle.setText(colonninaSelezionata.getNome());
				// Pulisci i dettagli vecchi
				sidebarDetails.removeAll();
				sidebarDetails.add( // Span serve per poter mandare a capo le righe
						new Span("Indirizzo: " + colonninaSelezionata.getIndirizzo() + ", "
								+ colonninaSelezionata.getComune()),
						new Span("Stato: " + colonninaSelezionata.getStato()));

				// Pulisce i campi di prenotazione precedenti
				bookingDatePicker.setValue(null);
				bookingTimeSlot.setValue(null);

				// Deve esistere solo un listener alla volta per leggere la colonnina corretta
				if (prenotaButtonListener != null) {
					prenotaButtonListener.remove();
					prenotaButtonListener = null; // Pulisce la variabile così sappiamo che è attivo un solo listener al
													// massimo
				}

				// Logica di salvataggio prenotazione
				prenotaButtonListener = prenotaButton.addClickListener(e -> {
					/*
					 * Salvo utente, data e orario al click
					 */
					Utente utenteCorrente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
					LocalDate dataSelezionata = bookingDatePicker.getValue();
					String orarioSelezionato = bookingTimeSlot.getValue();

					// se utente è null torno al login

					if (utenteCorrente == null) {
						Notification.show("Errore: Utente non loggato. Effettua il login per prenotare.", 3000,
								Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");

						// In caso reindirizziamo alla pagina di login
						getUI().ifPresent(ui2 -> ui.navigate(""));
						return;
					}

					/*
					 * Verifico con apposita classe errori riguardo colonnina, data e orario nulli.
					 * Se non presenti non permette prenotazione.
					 */

					String errore = DataValidator.verificaPrenotazione(colonninaSelezionata.getId(), dataSelezionata,
							orarioSelezionato);

					if (errore != null) {
						Notification.show(errore, 3000, Notification.Position.TOP_CENTER).getElement().getThemeList()
								.add("error");
						return;
					}

					// La data viene salvata qui per evitare eccezioni dati da LocalDate

					String dataString = dataSelezionata.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

					/*
					 * Chiamo funzione di firebase per verificare se la prenotazione è possibile
					 */
					firebaseService.cercaPrenotazione(colonninaSelezionata, dataString, orarioSelezionato)
							.thenAccept(prenotazione -> {
								// Non posso usare la ui precedente, uso ui1
								getUI().ifPresent(ui1 -> ui.access(() -> {

									/*
									 * Se il nodo non esiste la prenotazione è possibile
									 */

									if (prenotazione == null) {
										// Salvo oggetto prenotazione e chiamo la funzione di firebase
										Prenotazione p = new Prenotazione(colonninaSelezionata.getNome(),
												utenteCorrente.getUsername(), dataString, orarioSelezionato, java.time.LocalDate.now().toString());
										firebaseService.salvaPrenotazione(p).thenRun(() -> ui.access(() -> {

											Notification
													.show("Prenotazione confermata per " + orarioSelezionato + " il "
															+ dataString, 3000, Notification.Position.TOP_CENTER)
													.getElement().getThemeList().add("success");
											stationSidebar.setVisible(false); // Nasconde la sidebar

										}))
												/*
												 * Eccezioni del databse (es non trovato o problemi durante caricamento
												 * dati)
												 */
												 
												.exceptionally(ex -> {
													ui.access(() -> {
														Notification
																.show("Errore durante il salvataggio: "
																		+ ex.getMessage(), 4000,
																		Notification.Position.TOP_CENTER)
																.getElement().getThemeList().add("error");
													});
													return null;
												});

									}
									// Caso in cui lo slot scelto non è libero
									else {
										Notification
												.show("Impossibile effettuare la prenotazione, lo slot e' gia occupato",
														4000, Notification.Position.TOP_CENTER)
												.getElement().getThemeList().add("error");

									}

								}));

							});

				});
				// Mostra la sidebar
				stationSidebar.setVisible(true);

			}

			// Caso dati colonnina non trovati
			else {
				Notification.show("Errore: Dati colonnina non trovati.", 2000, Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");

			}

		}));

	}

}