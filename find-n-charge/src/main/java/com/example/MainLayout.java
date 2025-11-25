/**
 * Classe MainLayout: è la pagina principale della web app; attraverso questa pagina è possibile accedere a tutte le altre pagine grazie al menù
 * 
 * @author Tommaso Maistrello
 */

package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.example.views.ReservationView;
import com.example.views.StationsView;
import com.example.models.Utente;
import com.example.views.MapView;
import com.example.views.ProfileView;

//Non ha Route perchè è solo un contenitore
@PageTitle("Find&Charge")

public class MainLayout extends AppLayout {

	public MainLayout() {

		// Tasto dell'hamburger menu
		DrawerToggle toggle = new DrawerToggle();

		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");

		H1 title = new H1("FIND&CHARGE");
		if (utente != null) {
			title.add(" | ");
			title.add(utente.getUsername());
		}
		title.getStyle().set("font-size", "var(--lumo-font-size-l)");

		// Barra superiore
		addToNavbar(toggle, title);

		// Menu laterale
		VerticalLayout drawerMenu = new VerticalLayout();
		drawerMenu.setPadding(true);
		drawerMenu.setHeightFull();
		setDrawerOpened(false); // Inizialmente non visibile

		// Pulsante di logout
		Button logoutButton = new Button("Esci", event -> {
			VaadinSession.getCurrent().close(); // Chiude la sessione
			UI.getCurrent().getPage().setLocation("/"); // Torna al login
		});
		logoutButton.getElement().getThemeList().add("success"); // Tema verde

		// Spaziatore per posizionare in basso il tasto di logout
		Div spacer = new Div();

		// Voci del menu
		drawerMenu.add(createMenuLink(MapView.class, "Mappa", VaadinIcon.GLOBE),
				createMenuLink(ProfileView.class, "Profilo", VaadinIcon.USER),
				createMenuLink(ReservationView.class, "Prenotazioni", VaadinIcon.LIST),
				createMenuLink(StationsView.class, "Colonnine", VaadinIcon.MAP_MARKER), spacer, logoutButton);

		// Espande lo spacer in modo da occupare tutto lo spazio extra disponibile
		// all'interno di draweMenu (non utilizzato)
		drawerMenu.expand(spacer);

		// Aggiunge il menu al drawer
		addToDrawer(drawerMenu);
	}

	/**
	 * Questo metodo crea e dà lo stile ai link di navigazione (RouterLink) da
	 * inserire nel menù laterale.
	 *
	 * I link sono composti da un'icona e un testo e diretti alla route specificata.
	 *
	 * @param <T>       Il tipo generico della vista.
	 * @param viewClass La classe della vista Vaadin a cui il link deve puntare.
	 * @param text      Il testo da visualizzare nel link.
	 * @param icon      L'icona da aggiungere al testo.
	 * @return Un componente formato da icona e testo cliccabile che reinderizza
	 *         alla pagina desiderata.
	 */
	private <T extends com.vaadin.flow.component.Component> RouterLink createMenuLink(Class<T> viewClass, String text,
			VaadinIcon icon) {

		// Crea il link vuoto
		RouterLink link = new RouterLink();

		// Imposta la destinazione
		link.setRoute(viewClass);

		// Crea l'icona e il testo
		Icon linkIcon = icon.create();
		Span linkText = new Span(text);

		// Allinea icona e testo
		link.getStyle().set("display", "flex").set("align-items", "center").set("gap", "var(--lumo-space-s)")
				.set("color", "var(--lumo-success-text-color)"); // Testo verde

		// Aggiunge l'icona e il testo dentro al link
		link.add(linkIcon, linkText);

		return link;
	}
}