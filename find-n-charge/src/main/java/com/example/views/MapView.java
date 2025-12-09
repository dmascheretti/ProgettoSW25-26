/**
 * Classe MapView usata per visualizzare la mappa Leaflet (presa online) nella web app.
 * 
 * @author Tommaso Maistrello
 */

package com.example.views;

import com.example.components.Sidebar;
import com.example.models.Colonnina;
import com.example.models.StatoColonnina;
import com.example.models.Utente;
import com.example.service.ColonnineService;
import com.example.service.PrenotazioniService;
import com.example.util.DataValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.database.FirebaseAutoService;
import com.example.database.FirebaseColonnineService;
import com.example.database.FirebasePrenotazioniService;
import com.example.layout.MainLayout;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.VaadinSession;

import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Find&Charge - Mappa")
@Route(value = "", layout = MainLayout.class) // Carica la pagina nel layout della home page
@RouteAlias(value = "main", layout = MainLayout.class) // URL alternativo
@RouteAlias(value = "map", layout = MainLayout.class) // URL alternativo

public class MapView extends HorizontalLayout {

	private Div mapDiv;
	private String mapId = "map-container-" + System.currentTimeMillis(); // Id univoco -> evita conflitti

	// Componenti per la Sidebar
	private Sidebar stationSidebar;
	private ColonnineService colonnineService;

	// Serve Firebase con la lista della colonnine del database
	private FirebaseColonnineService firebaseColonnineService;
	private PrenotazioniService prenotazioneService;
	private FirebasePrenotazioniService firebasePrenotazioniService;
	private FirebaseAutoService firebaseAutoService;
	private List<Colonnina> colonnine;
	private Colonnina colonninaSelezionata;
	private ObjectMapper objectMapper = new ObjectMapper(); // Per tradurre gli oggetti da Java a JSON

	public MapView(@Autowired FirebaseColonnineService firebaseColonnineService,FirebaseAutoService firebaseAutoService,
			FirebasePrenotazioniService firebasePrenotazioniService, ColonnineService colonnineService, PrenotazioniService prenotazioneService) {

        this.firebaseColonnineService=firebaseColonnineService;
        this.firebaseAutoService=firebaseAutoService;
        this.firebasePrenotazioniService=firebasePrenotazioniService;
        this.colonnineService = colonnineService;
        this.prenotazioneService=prenotazioneService;

		setSizeFull();
		setPadding(false);
		setSpacing(false);
		getStyle().set("overflow", "hidden"); 	// Blocca lo scroll della pagina intera (scrolla solo la sidebar)

		// Crea la sidebar
		stationSidebar = new Sidebar();
		stationSidebar.setHeightFull();							// Occupa tutta l'altezza disponibile
		stationSidebar.getStyle().set("overflow-y", "auto"); 	// Abilita lo scroll solo per la sidebar
		
		configuraGestioneOrari();	//Per gestire gli slot orari
		reservationLogic();			//Per gestire le prenotazioni
		configuraAutoUtente();		//Per gestire le auto dell'utente
		

		// Crea il contenitore per la mappa
		mapDiv = new Div();
		mapDiv.setSizeFull();
		mapDiv.setId(mapId);

		// Aggiunge mappa e sidebar all'HorizontalLayout
		add(mapDiv, stationSidebar);
		expand(mapDiv); // La mappa occupa tutto lo spazio disponibile

		// Codice JS per rendere l'overlay del bookingDatePicker in primo piano
		// Ora non va più sotto la mappa
		UI.getCurrent().getElement()
				.executeJs("const style = document.createElement('style');"
						+ "style.innerHTML = 'vaadin-date-picker-overlay { z-index: 20000 !important; }';"
						+ "document.head.appendChild(style);");

		// Carica i file CSS e JS di Leaflet
		UI.getCurrent().getPage().addStyleSheet("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css");
		UI.getCurrent().getPage().addJavaScript("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js");

	}


	private void reservationLogic() {

		stationSidebar.getPrenotaButton().addClickListener(e -> {
        
        LocalDate data = stationSidebar.getDataSelezionata();
        String orario = stationSidebar.getOrarioSelezionato();
        String autoSelezionata = stationSidebar.getAutoSelected(); 
        Utente utenteCorrente = (Utente) VaadinSession.getCurrent().getAttribute("utente");

        //Controlla che l'utente sia loggato, altrimenti viene reindirizzato alla pagina di login
        if (utenteCorrente == null) {
            Notification.show("Errore: Utente non loggato.", 3000, Notification.Position.TOP_CENTER)
                    .getElement().getThemeList().add("error");
            UI.getCurrent().navigate("login"); 
            return;
        }

        //Deve essere selezionata una colonnina
        if (colonninaSelezionata == null) {
            Notification.show("Errore: Nessuna colonnina selezionata.", 3000, Notification.Position.TOP_CENTER)
                    .getElement().getThemeList().add("error");
            return;
        }
        
        //Deve essere selezionata una auto
        if (autoSelezionata == null) {
            Notification.show("Errore: Nessuna auto selezionata.", 3000, Notification.Position.TOP_CENTER)
                    .getElement().getThemeList().add("error");
            return;
        }

        // Verifica che non ci sia già una prenotazione per quello slot
        String errore = DataValidator.verificaPrenotazione(colonninaSelezionata.getId(), data, orario);
        if (errore != null) {
            Notification.show(errore, 3000, Notification.Position.TOP_CENTER)
                    .getElement().getThemeList().add("error");
            return;
        }

        //Scrive la prenotazione nel Firebase
        String dataString = data.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        prenotazioneService.prenota(colonninaSelezionata, utenteCorrente, dataString, orario, autoSelezionata)
            .thenAccept(success -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    if (success) {
                        Notification.show("Prenotazione confermata per " + orario + " il " + dataString, 
                                3000, Notification.Position.TOP_CENTER)
                                .getElement().getThemeList().add("success");
                        stationSidebar.setVisible(false);
                    } else {
                        Notification.show("Slot già occupato o errore server.", 4000, Notification.Position.TOP_CENTER)
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
        
		firebaseAutoService.getTargheUtente(utenteCorrente)
		.thenAccept(autoUtente -> {
			
			getUI().ifPresent(ui -> ui.access(() -> {
				stationSidebar.setAuto(autoUtente);
			}));
		});
		
	}
	
	private void configuraGestioneOrari() {
        stationSidebar.getBookingDatePicker().addValueChangeListener(e -> {
            
            LocalDate dataScelta = e.getValue();
            if (dataScelta == null) return;

            stationSidebar.aggiornaOrari(dataScelta, Collections.emptyList());	//Forza l'aggiornamento anche se la data non cambia

            if (colonninaSelezionata != null) {
                
                String dataString = dataScelta.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                firebasePrenotazioniService.getSlotOccupati(colonninaSelezionata.getId(), dataString)
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

						// Funzion che definisce l'icona del marker di default
						"    var IconBase = L.Icon.extend({" + "        options: {"
						+ "            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',"
						+ "            iconSize: [25, 41]," + "            iconAnchor: [12, 41],"
						+ "            popupAnchor: [1, -34]," + "            shadowSize: [41, 41]" + "        }"
						+ "    });" +

						// Genera tre varianti diverse che saranno utilizzate a seconda dello stato
						"    var greenIcon = new IconBase({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png'});"
						+ "    var yellowIcon = new IconBase({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-gold.png'});"
						+ "    var redIcon = new IconBase({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png'});"
						+ "    var greyIcon = new IconBase({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-grey.png'});"
						+ // Colore di default per gestire eventuali problemi di lettura

						// Trasforma la stringa JSON in un array JS
						"    var stations = JSON.parse($1);" + // $1 verrà sostituito da stationsJson
						"    var component = $0;" + // $0 è 'this.getElement()'

						"    stations.forEach(function(station) {" + // Ciclo su ogni elemento dell'array station

						"      var selectedIcon;" + "      var st = station.stato ? station.stato.toLowerCase() : '';" +

						// Controlla lo stato e in base a questo assegna il colore del marker
						"      if (st === 'libera') {" + "          selectedIcon = greenIcon;"
						+ "      } else if (st === 'prenotata') {" + "          selectedIcon = yellowIcon;"
						+ "      } else {" + "          selectedIcon = redIcon;" + // Default
						"      }" +

						// Crea i marker
						"      var marker = L.marker([station.lat, station.lon], {icon: selectedIcon}).addTo(map);" + // Per
																														// ogni
																														// colonnina
																														// crea
																														// il
																														// marker
																														// del
																														// colore
																														// giusto
						"      marker.on('click', function() {" + // Aggiunge il listener di click alla componente
						"      component.$server.onMarkerClick(station.id);" + "      });});}}, 100);"; // Ciclo ogni//
																										// 100ms

		colonnineService.inizializza(StatoColonnina.LIBERA)
	    .thenCompose(v -> colonnineService.aggiornaStato(StatoColonnina.PRENOTATA)) 
	    .thenCompose(v -> colonnineService.aggiornaStatoCarica(StatoColonnina.IN_CARICA)) 
	    .thenRun(() -> {
			
		
			
				
					
				// Se getAllColonnine() ha successo
				firebaseColonnineService.getAllColonnine().thenAccept(stations -> {

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