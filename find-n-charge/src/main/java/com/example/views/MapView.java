/**
 * Classe MapView usata per visualizzare la mappa Leaflet (presa online) nella web app.
 * 
 * @author Tommaso Maistrello
 */

package com.example.views;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.components.Sidebar;
import com.example.enums.StatoColonnina;
import com.example.layout.MainLayout;
import com.example.models.Colonnina;
import com.example.models.Utente;
import com.example.service.AutoService;
import com.example.service.ColonnineService;
import com.example.service.PrenotazioniService;
import com.example.service.RecensioniService;
import com.example.util.DataValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Find&Charge - Mappa")
@Route(value = "", layout = MainLayout.class) // Carica la pagina nel layout della home page
@RouteAlias(value = "main", layout = MainLayout.class) // URL alternativo
@RouteAlias(value = "map", layout = MainLayout.class) // URL alternativo

@JavaScript("./map.js")
@CssImport("./styles/CSS.css")
//Per la mappa Leaflet
@StyleSheet("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css")
@JavaScript("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js")
public class MapView extends HorizontalLayout {

	private Div mapDiv;
	private String mapId = "map-container-" + System.currentTimeMillis(); // Id univoco -> evita conflitti

	// Componenti per la Sidebar
	private Sidebar stationSidebar;
	private final ColonnineService colonnineService;
	private final PrenotazioniService prenotazioniService;
	private final AutoService autoService;
	private final RecensioniService recensioniService;

	private List<Colonnina> colonnine;
	private Colonnina colonninaSelezionata;

	private ObjectMapper objectMapper = new ObjectMapper(); // Per tradurre gli oggetti da Java a JSON

	public MapView(AutoService autoService, ColonnineService colonnineService, PrenotazioniService prenotazioniService,
			RecensioniService recensioniService) {

		this.colonnineService = colonnineService;
		this.prenotazioniService = prenotazioniService;
		this.recensioniService = recensioniService;
		this.autoService=autoService;

		setSizeFull();
		setPadding(false);
		setSpacing(false);
		addClassName("map-view");

		// Crea la sidebar
		stationSidebar = new Sidebar(recensioniService);
		stationSidebar.setHeightFull(); // Occupa tutta l'altezza disponibile
		stationSidebar.addClassName("sidebar-scrollable");

		configuraGestioneOrari(); // Per gestire gli slot orari
		reservationLogic(); // Per gestire le prenotazioni
		configuraAutoUtente(); // Per gestire le auto dell'utente

		// Crea il contenitore per la mappa
		mapDiv = new Div();
		mapDiv.setSizeFull();
		mapDiv.addClassName("map-canvas");
		mapDiv.setId(mapId);

		// Aggiunge mappa e sidebar all'HorizontalLayout
		add(mapDiv, stationSidebar);
		expand(mapDiv); // La mappa occupa tutto lo spazio disponibile
		
	}

	private void reservationLogic() {

		stationSidebar.getPrenotaButton().addClickListener(e -> {

			LocalDate data = stationSidebar.getDataSelezionata();
			String orario = stationSidebar.getOrarioSelezionato();
			String autoSelezionata = stationSidebar.getAutoSelected();
			Utente utenteCorrente = (Utente) VaadinSession.getCurrent().getAttribute("utente");

			// Controlla che l'utente sia loggato, altrimenti viene reindirizzato alla
			// pagina di login
			if (utenteCorrente == null) {
				Notification.show("Errore: Utente non loggato.", 3000, Notification.Position.TOP_CENTER).getElement()
						.getThemeList().add("error");
				UI.getCurrent().navigate("login");
				return;
			}

			// Deve essere selezionata una colonnina
			if (colonninaSelezionata == null) {
				Notification.show("Errore: Nessuna colonnina selezionata.", 3000, Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");
				return;
			}

			// Deve essere selezionata una auto
			if (autoSelezionata == null) {
				Notification.show("Errore: Nessuna auto selezionata.", 3000, Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");
				return;
			}

			// Verifica che non ci sia già una prenotazione per quello slot
			String errore = DataValidator.verificaPrenotazione(colonninaSelezionata.getId(), data, orario);
			if (errore != null) {
				Notification.show(errore, 3000, Notification.Position.TOP_CENTER).getElement().getThemeList()
						.add("error");
				return;
			}

			// Scrive la prenotazione nel Firebase
			String dataString = data.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

			prenotazioniService.prenota(colonninaSelezionata, utenteCorrente, dataString, orario, autoSelezionata)
					.thenAccept(success -> {
						getUI().ifPresent(ui -> ui.access(() -> {
							if (success) {
								Notification
										.show("Prenotazione confermata per " + orario + " il " + dataString, 3000,
												Notification.Position.TOP_CENTER)
										.getElement().getThemeList().add("success");
								stationSidebar.setVisible(false);
							} else {
								Notification
										.show("Slot già occupato o errore server.", 4000,
												Notification.Position.TOP_CENTER)
										.getElement().getThemeList().add("error");
							}
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
		

		//Prima inizalizza a libere tutte le colonnine, poi controlla quelle prenotate ed infine quelle in carica
		//In questo modo tutte sono verdi, solo alcune solo gialle e solo alcune di quelle gialle diventano rosse
		colonnineService.inizializza(StatoColonnina.LIBERA)
				.thenCompose(v -> colonnineService.aggiornaStato(StatoColonnina.PRENOTATA))
				.thenCompose(v -> colonnineService.aggiornaStatoCarica(StatoColonnina.IN_CARICA)).thenRun(() -> {

					// Se getAllColonnine() ha successo
					colonnineService.getAllColonnine().thenAccept(stations -> {

						// Memorizza la lista delle colonnine
						this.colonnine = stations;

						String stationsJson;
						try {
							// Prende i dati che servono per disegnare il marker
							List<Map<String, Object>> markerData = stations.stream().map(c -> { // Trasforma la lista
																								// List<Colonnina> in
																								// una
																								// List<Map<String,
																								// Object>>
								// Usa un HashMap esplicito per evitare problemi di inferenza dei tipi
								Map<String, Object> map = new java.util.HashMap<>();
								map.put("id", c.getId());
								map.put("lat", c.getLatitudine());
								map.put("lon", c.getLongitudine());
								map.put("stato", c.getStato());
								return map; // Restituisce l'HashMap
							}).collect(Collectors.toList());

							stationsJson = objectMapper.writeValueAsString(markerData); // Converte la lista Java in una
																						// lista JSON

						} catch (JsonProcessingException e) {
							e.printStackTrace();
							stationsJson = "[]"; // Array vuoto in caso di errore
						}

						// Esegue il JS in modo sicuro sul thread della UI
						// Dobbiamo usare ui.access() perché siamo in un thread asincrono
						String finalStationsJson = stationsJson;
						ui.access(() -> { // Questo thread deve essere eseguito sulla UI perchè deve aggiornarla
							getElement().executeJs("window.initLeafletMap($0, $1, $2)", getElement(), finalStationsJson, mapId); // Viene eseguito il codice
																								// JS
						});
					}).exceptionally(ex -> { // In caso fallisca getAllColonnine()
						ex.printStackTrace();
						ui.access(() -> {
							Notification.show("Errore nel caricamento delle colonnine: " + ex.getMessage(), 3000,
									Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
						});
						return null;
					});
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
				stationSidebar.setDati(colonninaSelezionata);

			} else {
				Notification.show("Errore: Dati colonnina non trovati.", 2000, Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");
			}

		}));
	}

}