/**
 * Classe MapView usata per visualizzare la mappa Leaflet (presa online) nella web app.
 * 
 * @author Tommaso Maistrello
 */

package com.example.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.example.MainLayout;

@PageTitle("Find&Charge - Mappa")
@Route(value = "map", layout = MainLayout.class) // Carica la pagina nel layout della home page
@RouteAlias(value = "main", layout = MainLayout.class) // URL alternativo

public class MapView extends Div {

	private Div mapDiv;
	private String mapId = "map-container-" + System.currentTimeMillis(); // Id univoco -> evita conflitti

	public MapView() {
		setSizeFull();
		getStyle().set("height", "100vh"); // Altrimenti la mappa collassa a 0px

		// Carica i file CSS e JS di Leaflet
		UI.getCurrent().getPage().addStyleSheet("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css");
		UI.getCurrent().getPage().addJavaScript("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js");

		// Configurazione del contenitore Div
		mapDiv = new Div();
		mapDiv.setSizeFull();
		mapDiv.setId(mapId);
		add(mapDiv);
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

		String jsCode =
				// Questa parte di codice deve aspettare che il file .js di Leaflet sia stato
				// scaricato, quindi lo controlliamo ciclicamente ogni 100ms
				"var checkLeaflet = setInterval(function() {" +
				// Controlla se il file è stato scaricato
						"  if (typeof L !== 'undefined') {" +
						// Se esiste, ferma il controllo
						"    clearInterval(checkLeaflet);" +
						// Crea la mappa dentro al Div corretto (grazie all'Id) e posizionala alle
						// coordinate di Dalmine con zoom pari a 15
						"    var map = L.map('" + mapId + "').setView([45.6493, 9.6021], 15);" +
						// Leaflet si appoggia a OpenStreetMAp.org per disegnare la mappa
						"    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {"
						+ "      attribution: '© OpenStreetMap contributors'" + "    }).addTo(map);" +

						"  }" + "}, 100);"; // Intervallo di 100ms

		// Esegue lo script di js appena scritto
		UI.getCurrent().getPage().executeJs(jsCode);
	}
}