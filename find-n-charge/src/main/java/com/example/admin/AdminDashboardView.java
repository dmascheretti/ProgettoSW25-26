/**
 * Pagina della dashboard dell'amministratore. Permette all'amministratore di avere tutte le informazioni sotto controllo in modo semplice e intuitivo.
 * @author Maistrello Tommaso
 */
package com.example.admin;

import com.example.admin.components.KpiCard;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.example.database.FirebaseColonnineService;
import com.example.database.FirebasePrenotazioniService;
import com.example.database.FirebaseUtentiService;
import com.example.layout.AdminLayout;
import com.example.util.ColonnineService;

import jakarta.annotation.security.RolesAllowed;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.builder.*;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.chart.builder.ZoomBuilder;
import com.github.appreciated.apexcharts.helper.Series;
import java.util.concurrent.CompletableFuture;

@PageTitle("Find&Charge | Dashboard")
@Route(value = "dashboard", layout = AdminLayout.class) // Carica la pagina nel layout dell'admin
@RouteAlias(value = "admin", layout = AdminLayout.class)
@RolesAllowed("ADMIN")

// AfterNavigationObserver serve per caricare i dati in automatico non appena viene aperta la pagina
public class AdminDashboardView extends VerticalLayout implements AfterNavigationObserver {

	private ApexCharts bookingsChart;
	private ColonnineService colonnineService;
	private FirebaseUtentiService firebaseUtentiService;
	private FirebaseColonnineService firebaseColonnineService;
	private FirebasePrenotazioniService firebasePrenotazioniService;

	// Utenti
	private KpiCard utentiCard;
	private KpiCard utentiNuoviCard;
	// Colonnine
	private KpiCard colonnineTotaliCard;
	private KpiCard colonnineLibereCard;
	private KpiCard colonnineGuasteCard;
	// Prenotazioni
	private KpiCard prenotazioniTotaliCard;
	private KpiCard prenotazioniNuoveCard;
	// Segnalazioni
	private KpiCard recensioniTotaliCard;
	private KpiCard recensioniNuoveCard;

	public AdminDashboardView(FirebaseUtentiService firebaseUtentiService,FirebaseColonnineService firebaseColonnineService, ColonnineService colonnineService,
								FirebasePrenotazioniService firebasePrenotazioniService) {
		this.firebaseUtentiService = firebaseUtentiService;
		this.firebaseColonnineService=firebaseColonnineService;
		this.firebasePrenotazioniService=firebasePrenotazioniService;
		this.colonnineService = colonnineService;

		setSizeFull(); // Occupa tutto lo spazio
		setSpacing(true);
		setPadding(true);

		H1 titolo = new H1("Dashboard");
		Button refresh = new Button(VaadinIcon.REFRESH.create());
		refresh.addThemeVariants(ButtonVariant.LUMO_LARGE);
		refresh.getElement().getThemeList().add("success");
		HorizontalLayout upperBar = new HorizontalLayout(titolo, refresh);
		upperBar.setWidthFull();
		upperBar.setPadding(true);
		upperBar.setJustifyContentMode(JustifyContentMode.BETWEEN); // Titolo a sx, bottone a dx
		upperBar.setDefaultVerticalComponentAlignment(Alignment.CENTER); // Allinea al centro verticalmente

		H2 chartTitle = new H2("Andamento Settimanale");

		// Creazione kpiCards
		VerticalLayout kpiLayout = createKpiLayout();
		// Creazione grafico
		this.bookingsChart = createBookingsChart();

		add(upperBar, kpiLayout, chartTitle, this.bookingsChart);

		// Quando si schiaccia il tasto di refresh, i dati devono essere aggiornati
		refresh.addClickListener(e -> {
			loadKpiData();
			loadChartData();
		});

	}

	/**
	 * Metodo richiamato da Vaadin in automatico al caricamento della pagina. In
	 * questo modo i dati sono subito aggiornati.
	 */
	@Override
	public void afterNavigation(AfterNavigationEvent event) {

		loadKpiData();
		loadChartData();

	}

	/**
	 * Metodo per creare un layout con le KpiCard. Dividiamo la sezione in quattro
	 * parti, ognuna per ogni gestione.
	 * 
	 * @return il layout descritto.
	 */
	private VerticalLayout createKpiLayout() {

		H2 utenti = new H2("Gestione utenti");
		H2 colonnine = new H2("Gestione colonnine");
		H2 prenotazioni = new H2("Gestione prenotazioni");
		H2 segnalazioni = new H2("Gestione segnalazioni");

		// Layout di gestione utenti e colonnine
		VerticalLayout utentiLayout = new VerticalLayout(utenti, kpiUtenti());
		VerticalLayout colonnineLayout = new VerticalLayout(colonnine, kpiColonnine());
		HorizontalLayout utentiAndColonnineLayout = new HorizontalLayout(utentiLayout, colonnineLayout);
		utentiAndColonnineLayout.setWidthFull();
		utentiAndColonnineLayout.expand(utentiLayout, colonnineLayout);

		// Layout di gestione prenotazioni e segnalazioni
		VerticalLayout prenotazioniLayout = new VerticalLayout(prenotazioni, kpiPrenotazioni());
		VerticalLayout segnalazioniLayout = new VerticalLayout(segnalazioni, kpiSegnalazioni());
		HorizontalLayout prenotazioniAndSegnalazioniLayout = new HorizontalLayout(prenotazioniLayout,
				segnalazioniLayout);
		prenotazioniAndSegnalazioniLayout.setWidthFull();
		prenotazioniAndSegnalazioniLayout.expand(prenotazioniLayout, segnalazioniLayout);

		// Unisce i due layout
		VerticalLayout kpiLayout = new VerticalLayout(utentiAndColonnineLayout, prenotazioniAndSegnalazioniLayout);
		kpiLayout.setWidthFull();
		kpiLayout.setPadding(false);
		kpiLayout.setSpacing(true);

		return kpiLayout;
	}

	/**
	 * Metodo per il caricamento di tutte le KPI.
	 */
	private void loadKpiData() {

		// Utenti
		firebaseUtentiService.contaUtenti().thenAccept(num -> {
			getUI().ifPresent(ui -> ui.access(() -> utentiCard.setNumber(num.toString())));
		}).exceptionally(ex -> {
			getUI().ifPresent(ui -> ui.access(() -> utentiCard.setNumber("Errore")));
			return null;
		});

		firebaseUtentiService.contaUtentiNuovi().thenAccept(num -> {

			String utentiNuovi = num.toString();

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					utentiNuoviCard.setNumber(utentiNuovi);
				});
			});
		}).exceptionally(ex -> {

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					utentiNuoviCard.setNumber("Errore");
				});
			});
			return null;
		});

		/*
		 * fb.contaUtentiAttivi().thenAccept(num -> {
		 * 
		 * String utentiAttivi = num.toString();
		 * 
		 * getUI().ifPresent(ui -> { ui.access(() -> {
		 * utentiAttiviCard.setNumber(utentiAttivi); }); }); }).exceptionally(ex -> {
		 * 
		 * getUI().ifPresent(ui -> { ui.access(() -> {
		 * utentiAttiviCard.setNumber("Errore"); }); }); return null; });
		 */

		// Colonnine
		firebaseColonnineService.contaColonnine().thenAccept(num -> {
			getUI().ifPresent(ui -> ui.access(() -> colonnineTotaliCard.setNumber(num.toString())));
		}).exceptionally(ex -> {
			getUI().ifPresent(ui -> ui.access(() -> colonnineTotaliCard.setNumber("Errore")));
			return null;
		});
		

		colonnineService.inizializza("Libera")
	    .thenCompose(v -> colonnineService.aggiornaStato("Prenotata")) 
	    .thenCompose(v -> colonnineService.aggiornaStatoCarica("In carica")) 
	    .thenRun(() -> {
			firebaseColonnineService.contaColonnineLG("Libera").thenAccept(num -> {
				getUI().ifPresent(ui -> ui.access(() -> colonnineLibereCard.setNumber(num.toString())));
			}).exceptionally(ex -> {
				getUI().ifPresent(ui -> ui.access(() -> colonnineLibereCard.setNumber("Errore")));
				return null;
			});
		});
	
		
		firebaseColonnineService.contaColonnineLG("Manutenzione").thenAccept(num -> {

			String colonnineGuaste = num.toString();

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					colonnineGuasteCard.setNumber(colonnineGuaste);
				});
			});
		}).exceptionally(ex -> {

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					colonnineGuasteCard.setNumber("Errore");
				});
			});
			return null;
		});

		// Prenotazioni
		firebasePrenotazioniService.contaPrenotazioni().thenAccept(num -> {
			getUI().ifPresent(ui -> ui.access(() -> prenotazioniTotaliCard.setNumber(num.toString())));
		}).exceptionally(ex -> {
			getUI().ifPresent(ui -> ui.access(() -> prenotazioniTotaliCard.setNumber("Errore")));
			return null;
		});

		firebasePrenotazioniService.contaPrenotazioniNuove().thenAccept(num -> {
			getUI().ifPresent(ui -> ui.access(() -> prenotazioniNuoveCard.setNumber(num.toString())));
		}).exceptionally(ex -> {
			getUI().ifPresent(ui -> ui.access(() -> prenotazioniNuoveCard.setNumber("Errore")));
			return null;
		});

		// Recensioni
		//String recensioniTotali = FirebaseService.contaRecensioniTotali();
		//String recensioniNuove = FirebaseService.contaRecensioniNuove();

	}

	/**
	 * Metodo per creare le card e inserirle nel layout.
	 * 
	 * @return il layout delle kpi degli utenti.
	 */
	private HorizontalLayout kpiUtenti() {

		// Creazione delle card (inizialmente senza dati)
		utentiCard = new KpiCard("Utenti totali", "...");
		utentiNuoviCard = new KpiCard("Nuovi utenti", "...");

		HorizontalLayout kpiUtentiLayout = new HorizontalLayout(utentiCard, utentiNuoviCard);
		kpiUtentiLayout.setWidthFull();
		kpiUtentiLayout.setSpacing(true);

		// Fa in modo che le card si espandano per riempire lo spazio
		kpiUtentiLayout.expand(utentiCard, utentiNuoviCard);

		return kpiUtentiLayout;
	}

	/**
	 * Metodo per creare le card e inserirle nel layout.
	 * 
	 * @return il layout delle kpi delle colonnine.
	 */
	private HorizontalLayout kpiColonnine() {

		// Creazione delle card (inizialmente senza dati)
		colonnineTotaliCard = new KpiCard("Colonnine totali", "...");
		colonnineLibereCard = new KpiCard("Colonnine libere", "...");
		colonnineGuasteCard = new KpiCard("Colonnine guaste", "...");

		colonnineLibereCard.getStyle().set("background-color", "var(--lumo-success-color-10pct)");
		colonnineLibereCard.getStyle().set("border-color", "var(--lumo-success-color)");
		colonnineGuasteCard.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		colonnineGuasteCard.getStyle().set("border-color", "var(--lumo-error-color)");

		HorizontalLayout kpiColonnineLayout = new HorizontalLayout(colonnineTotaliCard, colonnineLibereCard,
				colonnineGuasteCard);
		kpiColonnineLayout.setWidthFull();
		kpiColonnineLayout.setSpacing(true);

		// Fa in modo che le card si espandano per riempire lo spazio
		kpiColonnineLayout.expand(colonnineTotaliCard, colonnineLibereCard, colonnineGuasteCard);

		return kpiColonnineLayout;
	}

	/**
	 * Metodo per creare le card e inserirle nel layout.
	 * 
	 * @return il layout delle kpi delle prenotazioni.
	 */
	private HorizontalLayout kpiPrenotazioni() {

		// Creazione delle card (inizialmente senza dati)
		prenotazioniTotaliCard = new KpiCard("Prenotazioni totali", "...");
		prenotazioniNuoveCard = new KpiCard("Nuove prenotazioni", "...");

		HorizontalLayout kpiColonnineLayout = new HorizontalLayout(prenotazioniTotaliCard, prenotazioniNuoveCard);
		kpiColonnineLayout.setWidthFull();
		kpiColonnineLayout.setSpacing(true);

		// Fa in modo che le card si espandano per riempire lo spazio
		kpiColonnineLayout.expand(prenotazioniTotaliCard, prenotazioniNuoveCard);

		return kpiColonnineLayout;
	}

	/**
	 * Metodo per creare le card e inserirle nel layout.
	 * 
	 * @return il layout delle kpi delle segnalazioni.
	 */
	private HorizontalLayout kpiSegnalazioni() {

		// Creazione delle card (inizialmente senza dati)
		recensioniTotaliCard = new KpiCard("Recensionini totali", "...");
		recensioniNuoveCard = new KpiCard("Nuove recensioni", "...");

		HorizontalLayout kpiUtentiLayout = new HorizontalLayout(recensioniTotaliCard, recensioniNuoveCard);
		kpiUtentiLayout.setWidthFull();
		kpiUtentiLayout.setSpacing(true);

		// Fa in modo che le card si espandano per riempire lo spazio
		kpiUtentiLayout.expand(recensioniTotaliCard, recensioniNuoveCard);

		return kpiUtentiLayout;
	}

	/**
	 * Crea un grafico a linee inizialmente vuoto che riceverà i dati.
	 * 
	 * @return il grafico.
	 */
	private ApexCharts createBookingsChart() {

		Integer[] emptyData = { 0, 0, 0, 0, 0, 0, 0 }; // Dati di default
		String[] days = { "Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom" }; // Settimana completa precedente

		ApexCharts chart = ApexChartsBuilder.get().withChart(ChartBuilder.get().withType(Type.LINE) // Tipo di grafico
																									// (a linee)
				.withZoom(ZoomBuilder.get().withEnabled(false).build()) // Disabilita lo zoom
				.withHeight("500px") // Altezza
				.build()).withSeries(new Series<>("Prenotazioni", emptyData), new Series<>("Nuovi Utenti", emptyData)) // Dati
				.withXaxis(XAxisBuilder.get().withCategories(days) // Etichette asse X
						.build())
				.withYaxis(YAxisBuilder.get().build()).withLegend(LegendBuilder.get().withShow(true).build()) // Mostra
																												// legenda
				.build();

		return chart;
	}

	/**
	 * Carica i dati per il grafico da Firebase in modo asincrono e aggiorna il
	 * grafico quando sono pronti.
	 */
	private void loadChartData() {
					
		// Avvia entrambe le chiamate in parallelo
		CompletableFuture<Integer[]> futureBookings = firebasePrenotazioniService.contaPrenotazioniGiorni();
		//CompletableFuture<Integer[]> futureUsers = fb.getDatiGraficoUtenti();

		// Quando sono finite entrambe le chiamate:
		//CompletableFuture.allOf(futureBookings, futureUsers).thenAccept(voidResult -> {
		CompletableFuture.allOf(futureBookings).thenAccept(voidResult -> {

			// Ottiene i dati da Firebase
			Integer[] bookingsData = futureBookings.join();
			//Integer[] usersData = futureUsers.join();

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					// Aggiorna il grafico con i nuovi dati
					bookingsChart.updateSeries(new Series<>("Prenotazioni", bookingsData)
							//,new Series<>("Nuovi Utenti", usersData)
							);
				});
			});  
    
		}).exceptionally(ex -> {
			// Gestione errori
			ex.printStackTrace();
			return null;
		});
					
		
	}
}