/**
 * Classe AdminColonnineView che permette all'amministratore di consultare e gestire tutte le colonnine
 * 
 * @author Francesco Valenari
 */
package com.example.admin;

import com.example.components.Sidebar;
import com.example.enums.StatoColonnina;
import com.example.layout.AdminLayout;
import com.example.models.Colonnina;
import com.example.models.Utente;
import com.example.service.ColonnineService;
import com.example.service.RecensioniService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

@Route(value = "gestioneColonnine", layout = AdminLayout.class)
@PageTitle("Find&Charge | Gestione colonnine")

@CssImport("./styles/CSS.css")
public class AdminColonnineView extends HorizontalLayout implements BeforeEnterObserver {
	private String tema = "#008000";
	private Grid<Colonnina> colonGrid = new Grid<>(Colonnina.class);
	private final ColonnineService colonnineService;
	private Sidebar stationSidebar;
	private Dialog nuovaColonninaLayout;
	private final RecensioniService recensioniService;

	private TextField idField, nomeField, tipoField, latField, lonField, indirizzoField, comuneField, linkField;
	private NumberField potenzaField;

	private Button salvaNuovaButton;
	private Button annullaNuovaButton;

	// Classe per l'elenco delle colonnine (indipendente dalla mappa)
	public AdminColonnineView(ColonnineService colonnineService, RecensioniService recensioniService) {
		this.colonnineService = colonnineService;
		this.recensioniService = recensioniService;

		setSpacing(true);
		setPadding(true);
		setSizeFull();
		addClassName("map-view");	// Per bloccare lo scroll usiamo lo stile di MapView
		
		// Titolo
		H3 titolo = new H3("Lista universale delle colonnine di ricarica");
		titolo.addClassName("text-green-primary");

		// Bottone per l'inizializzazione inserimento dati di una nuova colonnina
		Button nuovaColonninaButton = new Button("Nuova colonnina");
		nuovaColonninaButton.getElement().getThemeList().add("success");
		nuovaColonninaButton.addClickListener(e -> mostraDialog());

		// Barra di ricerca per nome o indirizzo delle colonnine
		TextField searchField = new TextField("Cerca");
		searchField.setPlaceholder("Nome o indirizzo...");
		searchField.addClassName("search-field");
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
			btn.addClassName("grid-btn-link");
			return btn;
		}).setHeader("Nome").setComparator(Colonnina::getNome).setSortable(true);

		// Colonne con altre informazioni delle colonnine
		colonGrid.addColumn(Colonnina::getTipo).setHeader("Tipo").setSortable(true);

		colonGrid.addColumn(Colonnina::getStato).setHeader("Stato").setSortable(true);

		colonGrid.addColumn(Colonnina::getIndirizzo).setHeader("Indirizzo").setSortable(true);

		colonGrid.addColumn(Colonnina::getComune).setHeader("Comune").setSortable(true);

		colonGrid.addComponentColumn(colonnina -> {
			Button guasta = new Button("Segnala Guasta");

			if (colonnina.getStato().equals(StatoColonnina.GUASTA.toString())) {
				guasta.setText("Ripristina");
				guasta.addClickListener(e -> {
					colonnineService.cambiaStatoColonnina(colonnina.getId(), StatoColonnina.LIBERA).thenRun(() -> {
						getUI().ifPresent(ui -> ui.access(() -> {
							Notification.show("Colonnina ripristinata!");
							aggiornaGridConFiltro("");
						}));
					});
				});
			} else {

				guasta.addThemeVariants(ButtonVariant.LUMO_ERROR);
				guasta.addClickListener(e -> {
					colonnineService.cambiaStatoColonnina(colonnina.getId(), StatoColonnina.GUASTA).thenRun(() -> {
						getUI().ifPresent(ui -> ui.access(() -> {
							Notification.show("Colonnina segnalata!");
							aggiornaGridConFiltro(""); 
						}));
					});
				});
			}
			return guasta;
		}).setHeader("Gestione Guasti");
		
		
		colonGrid.addComponentColumn(colonnina -> {
		    Button elimina = new Button("Elimina");
		    elimina.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		    
		    elimina.addClickListener(e -> {
		        Dialog confermaDialog= new Dialog();
		        confermaDialog.add(new H3("Sei sicuro di voler eliminare la colonnina " + colonnina.getNome() + "?"));
		        
		        Button conferma = new Button("Conferma Eliminazione", evt -> {
		            colonnineService.eliminaColonnina(colonnina.getId()).thenRun(() -> {
		                 getUI().ifPresent(ui -> ui.access(() -> {
		                     aggiornaGridConFiltro("");
		                     Notification.show("Eliminata!");
		                     confermaDialog.close();
		                 }));
		            })
		            .exceptionally(ex -> {
	                    getUI().ifPresent(ui -> ui.access(() -> {
	                        Notification.show("Errore: " + ex.getMessage())
	                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
	                        confermaDialog.close();
	                    }));
	                    return null;
	                });
	        });
		        conferma.addThemeVariants(ButtonVariant.LUMO_ERROR);
		        
		        Button cancel = new Button("Annulla", evt -> confermaDialog.close());
		        
		        confermaDialog.add(new HorizontalLayout(conferma, cancel));
		        confermaDialog.open();
		    });
		    
		    return elimina;
		}).setHeader("Elimina");

		// Caricamento iniziale senza filtri
		aggiornaGridConFiltro("");

		stationSidebar = new Sidebar(recensioniService);
		stationSidebar.setHeightFull();
		stationSidebar.addClassNames("sidebar", "sidebar-scrollable");
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
		linkField = new TextField("Link per l'immagine");
		potenzaField = new NumberField("Potenza");

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
		HorizontalLayout idLinkLayout = new HorizontalLayout(idField, linkField);
		coordinateLayout.setAlignItems(FlexComponent.Alignment.CENTER);

		dialogLayout.add(new H3("Nuova colonnina"), idLinkLayout, potenzaField, infoLayout, coordinateLayout,
				indirizzoLayout, new HorizontalLayout(salvaNuovaButton, annullaNuovaButton));

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
					indirizzoField.getValue(), comuneField.getValue(), potenzaField.getValue(), linkField.getValue());

			colonnineService.salvaColonnina(nuova).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
				Notification.show("Colonnina salvata!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				nascondiDialog();
				aggiornaGridConFiltro("");
			}))).exceptionally(ex -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					Notification.show("Errore salvataggio: " + ex.getMessage())
							.addThemeVariants(NotificationVariant.LUMO_ERROR);
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

	/**
	 * Se l'utente prova ad accedere direttamente a questa pagina senza aver
	 * effettuato l'accesso, lo si reindirizza alla pagina di login mostrando una
	 * notifica di errore. beforeEnter viene eseguito un attimo prima che la pagina
	 * venga mostrata all'utente.
	 */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");

		if (utente == null) {
			event.forwardTo("login"); // Reindirizza alla pagina di login
			Registration[] registrationWrapper = new Registration[1]; // Array per registrare l'aggiunta del listener
			// Dopo che ha cambiato pagina, mostra la notifica
			registrationWrapper[0] = UI.getCurrent().addAfterNavigationListener(navEvent -> {
				Notification.show("Utente non trovato. Effettua il login.", 3000, Notification.Position.TOP_CENTER)
						.getElement().getThemeList().add("error");

				// Rimuove il listener, altrimenti scatterebbe ogni volta
				if (registrationWrapper[0] != null) {
					registrationWrapper[0].remove();
				}
			});
		} else if (utente.getRuolo().equals("Utente")) {
			event.forwardTo("");
		}
	}
}
