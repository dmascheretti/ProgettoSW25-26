/**
 * Classe AdminColonnineView che permette all'amministratore di consultare e gestire tutte le colonnine
 * 
 * @author Francesco Valenari
 */
package com.example.admin;

import com.example.components.Sidebar;
import com.example.layout.AdminLayout;
import com.example.models.Colonnina;
import com.example.service.ColonnineService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "gestioneColonnine", layout = AdminLayout.class)
@PageTitle("Find&Charge | Gestione colonnine")

public class AdminColonnineView extends HorizontalLayout {
	private String tema = "#008000";
	private Grid<Colonnina> colonGrid = new Grid<>(Colonnina.class);
	private final ColonnineService colonnineService;
	private Sidebar stationSidebar;
	private Dialog nuovaColonninaLayout;

	private TextField idField, nomeField, tipoField, latField, lonField, indirizzoField, comuneField;

	private Button salvaNuovaButton;
	private Button annullaNuovaButton;

	// Classe per l'elenco delle colonnine (indipendente dalla mappa)
	public AdminColonnineView(ColonnineService colonnineService) {
		this.colonnineService = colonnineService;
		setSpacing(true);
		setPadding(true);

		// Titolo
		H3 titolo = new H3("Lista universale delle colonnine di ricarica");
		titolo.getStyle().set("color", tema);

		// Bottone per l'inizializzazione inserimento dati di una nuova colonnina
		Button nuovaColonninaButton = new Button("Nuova colonnina");
		nuovaColonninaButton.getElement().getThemeList().add("success");
		nuovaColonninaButton.addClickListener(e -> mostraDialog());

		// Barra di ricerca per nome o indirizzo delle colonnine
		TextField searchField = new TextField("Cerca");
		searchField.getElement().getThemeList().add("success");
		searchField.setPlaceholder("Nome o indirizzo...");
		searchField.setWidth("300px");
		searchField.getStyle().set("color", tema);
		searchField.getStyle().set("--lumo-primary-text-color", "var(--lumo-success-text-color)"); // Tema verde
		// EVENTO DI RICERCA → chiama cercaColonnine()
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.addValueChangeListener(e -> {
			aggiornaGridConFiltro(e.getValue());
		});

		// Aggiunta Colonna di bottoni, targati col nome delle colonnine,che permettono
		// la consultazione delle colonnine
		colonGrid.removeAllColumns();

		colonGrid.addColumn(Colonnina::getId).setHeader("ID").setSortable(true);

		colonGrid.addComponentColumn(col -> {
			Button btn = new Button(col.getNome());
			btn.addClickListener(e -> showSidebar(col));
			btn.getStyle().set("color", tema).set("text-decoration", "underline").set("background", "none")
					.set("border", "none");
			return btn;
		}).setHeader("Nome").setComparator(Colonnina::getNome).setSortable(true);

		// Colonne con altre informazioni delle colonnine
		colonGrid.addColumn(Colonnina::getTipo).setHeader("Tipo").setSortable(true);

		colonGrid.addColumn(Colonnina::getStato).setHeader("Stato").setSortable(true);

		colonGrid.addColumn(Colonnina::getIndirizzo).setHeader("Indirizzo").setSortable(true);

		colonGrid.addColumn(Colonnina::getComune).setHeader("Comune").setSortable(true);

		// Caricamento iniziale senza filtri
		aggiornaGridConFiltro("");

		stationSidebar = new Sidebar();
		stationSidebar.setHeightFull();
		stationSidebar.setWidth("400px");
		stationSidebar.getStyle().set("background-color", "white");
		stationSidebar.getStyle().set("box-shadow", "5px 0 15px rgba(0,0,0,0.1)");
		stationSidebar.setVisible(false);

		nuovaColonninaLayout = createNuovaColonninaLayout();
		VerticalLayout layout = new VerticalLayout(titolo, nuovaColonninaButton, nuovaColonninaLayout, searchField,
				colonGrid);
		layout.setSizeFull();
		layout.expand(colonGrid);

		add(layout, stationSidebar);
	}

	private Dialog createNuovaColonninaLayout() {

		Dialog dialog = new Dialog();
		VerticalLayout dialogLayout = new VerticalLayout();
		dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);

		idField = new TextField("ID");
		nomeField = new TextField("Nome");
		tipoField = new TextField("Tipo");
		latField = new TextField("Latitudine");
		lonField = new TextField("Longitudine");
		indirizzoField = new TextField("Indirizzo");
		comuneField = new TextField("Comune");

		salvaNuovaButton = new Button("Salva");
		salvaNuovaButton.getElement().getThemeList().add("success");
		salvaNuovaButton.addClickListener(e -> salvaNuovaColonnina());
		annullaNuovaButton = new Button("Annulla", e -> dialog.close());

		HorizontalLayout infoLayout = new HorizontalLayout(nomeField, tipoField);
		infoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		HorizontalLayout indirizzoLayout = new HorizontalLayout(indirizzoField, comuneField);
		indirizzoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		HorizontalLayout coordinateLayout = new HorizontalLayout(latField, lonField);
		coordinateLayout.setAlignItems(FlexComponent.Alignment.CENTER);

		dialogLayout.add(new H3("Nuova colonnina"), idField, infoLayout, coordinateLayout, indirizzoLayout,
				new HorizontalLayout(salvaNuovaButton, annullaNuovaButton));

		dialog.add(dialogLayout);
		return dialog;
	}

	private void mostraDialog() {
		nuovaColonninaLayout.open();
	}

	private void nascondiDialog() {
		nuovaColonninaLayout.close();
	}

	private void salvaNuovaColonnina() {
		try {
			Colonnina nuova = new Colonnina(idField.getValue(), nomeField.getValue(), tipoField.getValue(),
					Double.parseDouble(latField.getValue()), Double.parseDouble(lonField.getValue()),
					indirizzoField.getValue(), comuneField.getValue(), 0);

			colonnineService.salvaColonnina(nuova)
            .thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
                Notification.show("Colonnina salvata!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                nascondiDialog();
                aggiornaGridConFiltro("");
            })))
            .exceptionally(ex -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    Notification.show("Errore salvataggio: " + ex.getMessage()).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }));
                return null;
            });

		} catch (Exception e) {
			System.out.println("Errore inserimento nuova colonnina: " + e.getMessage());
		}
		
	}

	private void showSidebar(Colonnina col) {

		
		stationSidebar.setDati(col);
		stationSidebar.getPrenotaButton().setVisible(false);

		stationSidebar.setVisible(true);
	}

	private void aggiornaGridConFiltro(String query) {
		colonnineService.cercaColonnine(query).thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {
				colonGrid.setItems(lista);
			}));
		});
	}
}
