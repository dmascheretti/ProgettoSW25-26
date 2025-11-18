/**
 * Classe AdminUtentiView che permette all'amministratore di gestire ogni utente
 * 
 * @author Tommaso Maistrello
 */
package com.example.admin;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.example.AdminLayout;
import com.example.models.Utente;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.example.database.FirebaseService;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Route(value = "gestioneUtenti", layout = AdminLayout.class)
@PageTitle("Find&Charge | Gestione utenti")

public class AdminUtentiView extends VerticalLayout {

	private Grid<Utente> utentiGrid = new Grid<>(Utente.class);
	private final FirebaseService utentiRef;
	private final UI ui;

	/**
	 * Costruttore che genera la griglia contenente tutti gli utenti
	 * 
	 * @param fb Firebase per accedere ai dati utente
	 */
	public AdminUtentiView(FirebaseService fb) {

		this.ui = UI.getCurrent();
		this.utentiRef = fb;
		setSpacing(true);
		setPadding(true);

		H3 titolo = new H3("Ecco la lista di tutti gli utenti ");
		titolo.getStyle().set("color", "#008000");

		//Imposta le colonne
		utentiGrid.removeAllColumns();
		utentiGrid.addColumn(Utente::getUsername).setHeader("Username").setSortable(true);
		// utentiGrid.addColumn(Utente::getDataCreazione).setHeader("DataCreazione").setSortable(true);
		utentiGrid.addColumn(Utente::getEmail).setHeader("Email").setSortable(true);

		// Pulsante per la cancellazione
		utentiGrid.addComponentColumn(p -> {
			Button btn = new Button("Ban");
			btn.addClickListener(e -> banUtente(p));
			btn.getStyle().set("color", "red").set("text-decoration", "underline").set("background", "none")
					.set("border", "none");
			return btn;
		});
		add(titolo, utentiGrid);

		// Ottiene la lista di utenti da Firebase
		utentiRef.getAllUtenti().thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {

				// Aggiunge la lista alla griglia
				utentiGrid.setItems(lista);

			}));

			// Gestione errori
		}).exceptionally(ex -> {
			ex.printStackTrace();
			return null;
		});

	}
	

	private void banUtente(Utente u) {

		utentiRef.cancellaUtente(u).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {

			Notification
					.show("Utente " + u.getUsername() + " bandito dal sistema.", 3000, Notification.Position.TOP_CENTER)
					.getElement().getThemeList().add("success");
			;

			getUI().ifPresent(ui1 -> ui1.getPage().reload());
		})))

				// Gestione errori
				.exceptionally(ex -> {
					getUI().ifPresent(ui -> ui.access(() -> {
						Notification.show("Errore durante la cancellazione: " + ex.getMessage(), 4000,
								Notification.Position.TOP_CENTER).getElement().getThemeList().add("error");
					}));
					return null;
				});
	}
	

		
}