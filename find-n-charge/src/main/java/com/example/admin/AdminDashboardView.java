/**
 * Pagina della dashboard dell'amministratore. Permette all'amministratore di avere tutte le informazioni sotto controllo in modo semplice e intuitivo.
 *  
 * @author Maistrello Tommaso
 */
package com.example.admin;

import com.example.AdminLayout;
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
import com.example.database.FirebaseService;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Find&Charge | Dashboard")
@Route(value = "dashboard", layout = AdminLayout.class) // Carica la pagina nel layout dell'admin
@RouteAlias(value = "admin", layout = AdminLayout.class)
@RolesAllowed("ADMIN")

public class AdminDashboardView extends VerticalLayout {
	private FirebaseService fb;

	public AdminDashboardView(FirebaseService fb) {
		this.fb = fb;

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

		add(upperBar, createKpiLayout());

	}

	/**
	 * Metodo per creare un layout verticale con le KpiCard
	 */
	private VerticalLayout createKpiLayout() {

		H2 utenti = new H2("Gestione utenti");
		H2 colonnine = new H2("Gestione colonnine");
		H2 prenotazioni = new H2("Gestione prenotazioni");
		H2 segnalazioni = new H2("Gestione segnalazioni");

		VerticalLayout utentiLayout = new VerticalLayout(utenti, kpiUtenti());
		VerticalLayout colonnineLayout = new VerticalLayout(colonnine, kpiColonnine());

		VerticalLayout prenotazioniLayout = new VerticalLayout(prenotazioni, kpiPrenotazioni());
		VerticalLayout segnalazioniLayout = new VerticalLayout(segnalazioni, kpiSegnalazioni());

		HorizontalLayout utentiAndColonnineLayout = new HorizontalLayout(utentiLayout, colonnineLayout);
		HorizontalLayout prenotazioniAndSegnalazioniLayout = new HorizontalLayout(prenotazioniLayout,
				segnalazioniLayout);
		utentiAndColonnineLayout.setWidthFull();
		prenotazioniAndSegnalazioniLayout.setWidthFull();
		utentiAndColonnineLayout.expand(utentiLayout, colonnineLayout);
		prenotazioniAndSegnalazioniLayout.expand(prenotazioniLayout, segnalazioniLayout);

		VerticalLayout kpiLayout = new VerticalLayout(utentiAndColonnineLayout, prenotazioniAndSegnalazioniLayout);
		kpiLayout.setWidthFull();
		kpiLayout.setPadding(false);
		kpiLayout.setSpacing(true);

		return kpiLayout;
	}

	private HorizontalLayout kpiUtenti() {

		// Creazione delle card
		KpiCard utentiCard = new KpiCard("Utenti totali", "");
		KpiCard utentiNuoviCard = new KpiCard("Nuovi utenti", "");
		KpiCard utentiAttiviCard = new KpiCard("Utenti attivi", "");

		fb.contaUtenti().thenAccept(num -> {

			String utentiTotali = num.toString();

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					utentiCard.setNumber(utentiTotali);
				});
			});
		}).exceptionally(ex -> {

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					utentiCard.setNumber("Errore");
				});
			});
			return null;
		});

		/*
		 * fb.contaUtentiNuovi().thenAccept(num -> {
		 * 
		 * String utentiNuovi = num.toString();
		 * 
		 * getUI().ifPresent(ui -> { ui.access(() -> {
		 * utentiNuoviCard.setNumber(utentiNuovi); }); }); }).exceptionally(ex -> {
		 * 
		 * getUI().ifPresent(ui -> { ui.access(() -> {
		 * utentiNuoviCard.setNumber("Errore"); }); }); return null; });
		 * 
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

		// Creazione delle card

		utentiAttiviCard.getStyle().set("background-color", "var(--lumo-success-color-10pct)");
		utentiAttiviCard.getStyle().set("border-color", "var(--lumo-success-color)");

		HorizontalLayout kpiUtentiLayout = new HorizontalLayout(utentiCard, utentiNuoviCard, utentiAttiviCard);
		kpiUtentiLayout.setWidthFull();
		kpiUtentiLayout.setSpacing(true);

		// Fa in modo che le card si espandano per riempire lo spazio
		kpiUtentiLayout.expand(utentiCard, utentiNuoviCard, utentiAttiviCard);
		return kpiUtentiLayout;
	}

	private HorizontalLayout kpiColonnine() {

		// Creazione delle card
		KpiCard colonnineTotaliCard = new KpiCard("Colonnine totali", "");
		KpiCard colonnineLibereCard = new KpiCard("Colonnine libere", "");
		KpiCard colonnineGuasteCard = new KpiCard("Colonnine guaste", "");

		fb.contaColonnine().thenAccept(num -> {

			String colonnineTotali = num.toString();

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					colonnineTotaliCard.setNumber(colonnineTotali);
				});
			});
		}).exceptionally(ex -> {

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					colonnineTotaliCard.setNumber("Errore");
				});
			});
			return null;
		});

		fb.contaColonnineLibere().thenAccept(num -> {

			String colonnineLibere = num.toString();

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					colonnineLibereCard.setNumber(colonnineLibere);
				});
			});
		}).exceptionally(ex -> {

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					colonnineLibereCard.setNumber("Errore");
				});
			});
			return null;
		});

		/*
		 * fb.contaColonnineGuaste().thenAccept(num -> {
		 * 
		 * String colonnineGuaste = num.toString();
		 * 
		 * getUI().ifPresent(ui -> { ui.access(() -> {
		 * colonnineGuasteCard.setNumber(colonnineGuaste); }); }); }).exceptionally(ex
		 * -> {
		 * 
		 * getUI().ifPresent(ui -> { ui.access(() -> {
		 * colonnineGuasteCard.setNumber("Errore"); }); }); return null; });
		 */

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

	private HorizontalLayout kpiPrenotazioni() {

		// Creazione delle card
		KpiCard prenotazioniTotaliCard = new KpiCard("Prenotazioni totali", "");
		KpiCard prenotazioniNuoveCard = new KpiCard("Nuove prenotazioni", "");

		fb.contaPrenotazioni().thenAccept(num -> {

			String prenotazioniTotali = num.toString();

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					prenotazioniTotaliCard.setNumber(prenotazioniTotali);
				});
			});
		}).exceptionally(ex -> {

			getUI().ifPresent(ui -> {
				ui.access(() -> {
					prenotazioniTotaliCard.setNumber("Errore");
				});
			});
			return null;
		});

		// String prenotazioniNuove = contaPrenotazioniNuove();

		HorizontalLayout kpiColonnineLayout = new HorizontalLayout(prenotazioniTotaliCard, prenotazioniNuoveCard);
		kpiColonnineLayout.setWidthFull();
		kpiColonnineLayout.setSpacing(true);
		// Fa in modo che le card si espandano per riempire lo spazio
		kpiColonnineLayout.expand(prenotazioniTotaliCard, prenotazioniNuoveCard);
		return kpiColonnineLayout;
	}

	private HorizontalLayout kpiSegnalazioni() {

		// Assegnazione dei dati
		// String segnalazioniTotali = FirebaseService.contaSegnalazioniTotali();
		// String segnalazioniNuove = FirebaseService.contaSegnalazioniNuove();
		// String segnalazioniVecchie = FirebaseService.contaSegnalazioniPassate();

		// Creazione delle card
		KpiCard segnalazioniTotaliCard = new KpiCard("Segnalazioni totali", "");
		KpiCard segnalazioniNuoveCard = new KpiCard("Nuove segnalazioni", "");
		KpiCard segnalazioniVecchieCard = new KpiCard("Segnalazioni vecchie", "");

		segnalazioniNuoveCard.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
		segnalazioniNuoveCard.getStyle().set("border-color", "var(--lumo-error-color)");

		HorizontalLayout kpiUtentiLayout = new HorizontalLayout(segnalazioniTotaliCard, segnalazioniNuoveCard,
				segnalazioniVecchieCard);
		kpiUtentiLayout.setWidthFull();
		kpiUtentiLayout.setSpacing(true);
		// Fa in modo che le card si espandano per riempire lo spazio
		kpiUtentiLayout.expand(segnalazioniTotaliCard, segnalazioniNuoveCard, segnalazioniVecchieCard);
		return kpiUtentiLayout;
	}
}