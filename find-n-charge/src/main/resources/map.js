// Questa parte di codice deve aspettare che il file .js di Leaflet sia stato
// scaricato, quindi lo controlliamo ciclicamente ogni 100ms
window.initLeafletMap = function (component, stationsJson, mapId) {
	var checkLeaflet = setInterval(function () {
		if (typeof L !== 'undefined') {						// Controlla se
			// il file è
			// stato
			// scaricato
			clearInterval(checkLeaflet); // Se esiste, ferma il controllo
			var mapElement = document.getElementById(mapId); // Cerca il div che deve
			// contenere la mappa
			if (!mapElement) return; // Se non esiste interrompe
			if (mapElement._leaflet_id) return; // Controlla che non ci sia già una mappa nel div,
			// in caso affermativo interrompe

			// Crea la mappa
			var map = L.map(mapId, {
				// Crea la mappa
				minZoom: 3,                                      // Impedisce zoom troppo lontani
				maxBounds: [[-90, -180], [90, 180]],             // Movimento limitato ai confini del mondo
				maxBoundsViscosity: 0.0                         // Muro elastico
			}).setView([45.6493, 9.6021], 15); 					// Coordinate di default su Dalmine con zoom default pari a 15

			L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
				// Si appoggia a
				// OpenStreetMap per
				// disegnare le
				// mattonelle della
				// mappa
				attribution: '© OpenStreetMap contributors',
				noWrap: true // La mappa non si
				// ripete
			}).addTo(map);

			// Geolocalizzazione
			map.locate({ setView: true, maxZoom: 15, enableHighAccuracy: false }); // Trova la
			// posizione e
			// centra la mappa

			map.on('locationfound', function (e) {
				// Quando la posizione viene trovata:

				var userIcon = L.divIcon({
					// Definisce un'icona per il marker della posizione
					// attuale dell'utente
					className: 'user-location-marker', // Classe CSS personalizzata per la box in cui
					// verrà visualizzata l'icona
					html: '<vaadin-icon icon= "vaadin:bullseye" style="color: #305000; width: 35px; height: 35px; filter: drop-shadow(1px 1px 1px rgba(0,0,0,0.5));"></vaadin-icon>',
					iconSize: [35, 35], // Grandezza dell'icona in pixel
					iconAnchor: [17.5, 17.5], // Centra l'incona
				});

				var userMarker = L.marker(e.latlng, {
					// Creazione marker della posizione utente
					icon: userIcon,
					zIndexOffset: 10000 // Sempre sopra le colonnine
				}).addTo(map);

				userMarker.on('click', function () {
					// Quando viene cliccato il marker utente:
					map.flyTo(e.latlng, 15);  // Viene centrata la mappa sulla posizione corrente
				});
			});

			// Funzione che definisce l'icona del marker di default
			var IconBase = L.Icon.extend({
				options: {
					shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
					iconSize: [25, 41],
					iconAnchor: [12, 41],
					popupAnchor: [1, -34],
					shadowSize: [41, 41]
				}
			});

			// Genera tre varianti diverse che saranno utilizzate a seconda dello stato
			var greenIcon = new IconBase({ iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png' });
			var yellowIcon = new IconBase({ iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-gold.png' });
			var redIcon = new IconBase({ iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png' });
			var greyIcon = new IconBase({ iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-grey.png' });
			// Colore di default per gestire eventuali problemi di lettura

			// Trasforma la stringa JSON in un array JS
			var stations = JSON.parse(stationsJson);
			stations.forEach(function (station) {
				// Ciclo su ogni elemento dell'array station

				var selectedIcon;
				var st = station.stato ? station.stato.toLowerCase() : '';

				// Controlla lo stato e in base a questo assegna il colore del marker
				if (st === 'libera') {
					selectedIcon = greenIcon;
				} else if (st === 'prenotata') {
					selectedIcon = yellowIcon;
				} else if (st === 'guasta') {
					selectedIcon = greyIcon;  
				} else {
					selectedIcon = redIcon;
				}

				// Crea i marker
				var marker = L.marker([station.lat, station.lon], { icon: selectedIcon }).addTo(map); // Per
				// ogni
				// colonnina
				// crea
				// il
				// marker
				// del
				// colore
				// giusto
				marker.on('click', function () {
					// Aggiunge il listener di click alla componente
					component.$server.onMarkerClick(station.id);
				});
			});
		}
	}, 100); // Ciclo ogni//
	// 100ms
};