package com.example.admin;

import com.example.AdminLayout;
import com.example.admin.components.KpiCard;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
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
		this.fb=fb;

		setSizeFull(); // Occupa tutto lo spazio
		setSpacing(true);
		setPadding(true);

		H1 titolo = new H1("Dashboard");

		add(titolo, createKpiLayout());

	}

	/**
	 * Metodo per creare un layout orizzontale con le KpiCard
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

		kpiLayout.setSpacing(true);

		return kpiLayout;
	}

	private HorizontalLayout kpiUtenti() {
		// Assegnazione dei dati
		
		//DA IMPLEMENTARE
		fb.contaUtenti().thenAccept(num -> {
			String utentiTotali=num.toString();
		}).exceptionally(null);
		
		
		String utentiNuovi = FirebaseService.contaUtentiNuovi();
		
		String utentiAttivi= FirebaseService.contaUtentiAttivi();
		

		// Creazione delle card
		KpiCard utentiCard = new KpiCard("Utenti totali", utentiTotali);
		KpiCard utentiNuoviCard = new KpiCard("Nuovi utenti", utentiNuovi);
		KpiCard utentiAttiviCard = new KpiCard("Utenti attivi", utentiAttivi);

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

		// Assegnazione dei dati
		fb.contaColonnine().thenAccept(ris->{
			String colonnineTotali=ris.toString();
		});
		
		String colonnineLibere = FirebaseService.contaColonnineLibere();
		String colonnineGuaste = FirebaseService.contaColonnineGuaste();

		// Creazione delle card
		KpiCard colonnineTotaliCard = new KpiCard("Colonnine totali", colonnineTotali);
		KpiCard colonnineLibereCard = new KpiCard("Colonnine libere", colonnineLibere);
		KpiCard colonnineGuasteCard = new KpiCard("Colonnine guaste", colonnineGuaste);

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

		// Assegnazione dei dati
		
		fb.contaPrenotazioni().thenAccept(ris->{
			String prenotazioniTotali=ris.toString();
		});
		
		String prenotazioniNuove = contaPrenotazioniNuove();

		// Creazione delle card
		KpiCard prenotazioniTotaliCard = new KpiCard("Prenotazioni totali", prenotazioniTotali);
		KpiCard prenotazioniNuoveCard = new KpiCard("Nuove prenotazioni", prenotazioniNuove);

		HorizontalLayout kpiColonnineLayout = new HorizontalLayout(prenotazioniTotaliCard, prenotazioniNuoveCard);
		kpiColonnineLayout.setWidthFull();
		kpiColonnineLayout.setSpacing(true);
		// Fa in modo che le card si espandano per riempire lo spazio
		kpiColonnineLayout.expand(prenotazioniTotaliCard, prenotazioniNuoveCard);
		return kpiColonnineLayout;
	}

	private HorizontalLayout kpiSegnalazioni() {

		// Assegnazione dei dati
		String segnalazioniTotali = FirebaseService.contaSegnalazioniTotali();
		String segnalazioniNuove = FirebaseService.contaSegnalazioniNuove();
		String segnalazioniVecchie = FirebaseService.contaSegnalazioniPassate();

		// Creazione delle card
		KpiCard segnalazioniTotaliCard = new KpiCard("Segnalazioni totali", segnalazioniTotali);
		KpiCard segnalazioniNuoveCard = new KpiCard("Nuove segnalazioni", segnalazioniNuove);
		KpiCard segnalazioniVecchieCard = new KpiCard("Segnalazioni vecchie", segnalazioniVecchie);

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