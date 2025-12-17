/**
 * Classe ProfileView gestisce consultazione e modifica delle proprie credenziali e delle proprie auto
 * 
 * @author Claudio Morgera, Francesco Valenari, Tommaso Maistrello
 */
package com.example.views;
import com.example.service.ColonnineService;
import com.example.models.Prenotazione;
import com.example.models.Auto;
import com.example.models.Utente;
import com.example.service.AutoService;
import com.example.service.PrenotazioniService;
import com.example.service.UtentiService;
import com.example.components.CardAuto;
import com.example.enums.TipoAuto;
import com.example.layout.MainLayout;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Route(value = "profilo", layout = MainLayout.class)

@PageTitle("Find&Charge - Profilo")
public class ProfileView extends VerticalLayout implements BeforeEnterObserver {

	private final AutoService autoService;
	private final UtentiService utentiService;
	private final PrenotazioniService prenotazioniService;
	private final ColonnineService colonnineService;
    private VerticalLayout modifica;
    private Paragraph mail;
    private HorizontalLayout datiEmodifica;
    private HorizontalLayout autoLayout;
    
	public ProfileView(AutoService autoService, UtentiService utentiService, PrenotazioniService prenotazioniService, ColonnineService colonnineService) {
		this.autoService = autoService;
		this.utentiService = utentiService;
		this.prenotazioniService = prenotazioniService;
		this.colonnineService=colonnineService;
		setSizeFull();
		setSpacing(true);
		setPadding(true);
		datiEmodifica= new HorizontalLayout();
		modifica = new VerticalLayout();
		modifica.setVisible(false);
        modifica.getStyle().set("padding-left", "20px"); 
        modifica.getStyle().set("border-left", "1px solid #ddd");

		// Prendi utente dalla sessione (controllo null per sicurezza)
		Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
		if (utente == null) {

			add(new Paragraph("Utente non trovato. Effettua il login."));
			return;
		}

		// Header
		add(createSaluto(utente.getUsername().toUpperCase()));
		
		// Profile Card
		//add(createProfileCard(utente));
		
	    autoLayout = new HorizontalLayout();
		autoLayout.setWidthFull();
		autoLayout.setPadding(true);
		autoLayout.setSpacing(true);

		add(autoLayout);
		
		autoService.getAutoUtente(utente).thenAccept(lista -> {
			getUI().ifPresent(ui -> ui.access(() -> {

				autoLayout.removeAll();

				for (Auto a : lista) {

					CardAuto card = new CardAuto(a.getModello(), a.getTarga(), a.getStatoCarica());

					prenotazioniService.inCarica(a).thenAccept(trovata -> {
						ui.access(() -> {
							boolean isInCarica=(trovata!=null);
							Paragraph inCarica = new Paragraph(isInCarica ? "Auto in carica" : "Auto non in carica");
							card.add(inCarica);
							
							if (isInCarica) {

							    Button aggiornaBtn = new Button("Aggiorna stato");
							    aggiornaBtn.getStyle().set("background-color", "#27AE60");
							    aggiornaBtn.getStyle().set("color", "white");
							    aggiornaBtn.getStyle().set("border-radius", "6px");

							    aggiornaBtn.addClickListener(ev -> {
							    	LocalTime orarioInizio = LocalTime.parse(trovata.getInizio());
						    	    LocalTime orarioFineSlot = orarioInizio.plusMinutes(30);
						    	   
						    	    LocalDateTime fineSlotData = LocalDateTime.of(java.time.LocalDate.now(), orarioFineSlot);
						    	    LocalDateTime adesso = java.time.LocalDateTime.now();
						    	    
						    	    LocalDateTime orarioPerCalcolo = adesso.isAfter(fineSlotData) ? fineSlotData : adesso;
							    	
						    	    String idColonnina = trovata.getIDColonnina();
							    	colonnineService.getColonninaById(idColonnina).thenAccept(colonnina -> {
							    		
							    	double potenzaColonnina=22;
							    		if (colonnina != null && colonnina.getTipo() != null) {
							                // Controllo il tipo di colonnina
							                if (colonnina.getTipo().equalsIgnoreCase("Standard")) {
							                	potenzaColonnina = 7.0;
							                } else if (colonnina.getTipo().equalsIgnoreCase("Fast")) {
							                	potenzaColonnina = 22.0;
							                } else {
							                    // Se il tipo è sconosciuto, usa la potenza salvata nell'oggetto o resta 22
							                	potenzaColonnina = colonnina.getPotenza() > 0 ? colonnina.getPotenza() : 22.0;
							                }
							            }
							    		final double potenza= potenzaColonnina;
							        autoService.nuovoStato(a, java.time.LocalDateTime.now(), potenza).thenRun(() -> {
							            ui.access(() -> {

							                
							                card.updateStato(a.getStatoCarica());

							                if (adesso.isAfter(fineSlotData)) {
							                    Notification.show("Ricarica conclusa alle " + orarioFineSlot + ". Stato aggiornato.")
							                        .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
							                    prenotazioniService.aggiornaStato(trovata, com.example.enums.StatoPrenotazione.PASSATA);
							                    
							                    aggiornaBtn.setVisible(false);
							                }
							                else {
							                Notification.show("Stato aggiornato!", 2500, Notification.Position.TOP_CENTER)
							                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);}
							            });
							        });
							    });

							    });
							    card.add(aggiornaBtn);
							}


						});
					});
					Button eliminaBtn = new Button("Elimina");
				    eliminaBtn.getStyle().set("background-color", "#E74C3C");
				    eliminaBtn.getStyle().set("color", "white");
				    eliminaBtn.getStyle().set("border-radius", "6px");

				    eliminaBtn.addClickListener(click -> {
				        ConfirmDialog dialog = new ConfirmDialog(
				            "Conferma eliminazione",
				            "Vuoi davvero eliminare questa auto?",
				            "Elimina",
				            ev -> {
				                autoService.eliminaAuto(a).thenRun(() -> {
				                    ui.access(() -> {
				                        Notification.show("Auto eliminata").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				                        autoLayout.remove(card); 
				                    });
				                });
				            },
				            "Annulla",
				            ev -> {}
				        );
				        dialog.open();
				    });

				    card.add(eliminaBtn);
				    autoLayout.add(card);
				}
			}));
		});

		// Email change controls
		TextField emailField = new TextField("Nuova Email");

		VerticalLayout emailCard = createCard("Modifica Email");
		emailField.setWidthFull();
		Button cambiaMailBtn = createMainButton("Aggiorna Email");
		emailCard.add(emailField, cambiaMailBtn);

		cambiaMailBtn.addClickListener(e -> {
			String nuovaEmail = emailField.getValue();
			if (nuovaEmail.isEmpty()) {
				Notification.show("Inserisci una email valida", 3000, Notification.Position.TOP_CENTER);
				return;
			}

			utentiService.cambiaMail(utente, nuovaEmail).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {
				// aggiorna utente in sessione
				utente.setEmail(nuovaEmail);
				VaadinSession session = ui.getSession();
				session.setAttribute("utente", utente);

				// aggiorna label e mostra notifica
				aggiornaMail(nuovaEmail);

				Notification n = Notification.show("Email aggiornata correttamente!", 3000,
						Notification.Position.TOP_CENTER);
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

				emailField.clear();
			}))).exceptionally(ex -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
					Notification n = Notification.show("Errore: " + msg, 3000, Notification.Position.TOP_CENTER);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				}));
				return null;
			});
		});

		PasswordField passwordField = new PasswordField("Nuova Password");
		VerticalLayout pwdCard = createCard("Modifica Password");
		passwordField.setWidthFull();
		Button cambiaPwdBtn = createMainButton("Aggiorna Password");
		pwdCard.add(passwordField, cambiaPwdBtn);

		cambiaPwdBtn.addClickListener(e -> {
			String nuovaPwd = passwordField.getValue();

			if (nuovaPwd.isEmpty()) {
				Notification.show("Inserisci una password", 3000, Notification.Position.TOP_CENTER);
				return;
			}

			utentiService.cambiaPassword(utente, nuovaPwd).thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {

				utente.setPassword(nuovaPwd);
				VaadinSession.getCurrent().setAttribute("utente", utente);

				Notification n = Notification.show("Password aggiornata correttamente!");
				n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
				n.setPosition(Notification.Position.TOP_CENTER);
				passwordField.clear();

			}))).exceptionally(ex -> {
				getUI().ifPresent(ui -> ui.access(() -> {
					String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
					Notification n = Notification.show("Errore: " + msg, 3000, Notification.Position.TOP_CENTER);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
				}));
				return null;
			});

		});

		// --- Aggiungi Auto ---
		TextField targaField = new TextField("Targa");
		TextField modelloField = new TextField("Modello");

		ComboBox<TipoAuto> tipoField = new ComboBox<>("Tipo di auto");
		tipoField.setItems(TipoAuto.values());
		tipoField.setItemLabelGenerator(TipoAuto::getLabel);
		tipoField.setPlaceholder("Seleziona il tipo");
		tipoField.setAllowCustomValue(false);

		VerticalLayout autoCard = createCard("Aggiungi Auto");
		targaField.setWidthFull();
		modelloField.setWidthFull();
		tipoField.setWidthFull();
		Button aggiungiAutoBtn = createMainButton("Aggiungi Auto");
		
		autoCard.add(targaField, modelloField, tipoField, aggiungiAutoBtn);

		aggiungiAutoBtn.addClickListener(e -> {
			// .trim serve per controllare che non sia vuoto es " " -> non va bene
			String targa = targaField.getValue() == null ? "" : targaField.getValue().trim();
			String modello = modelloField.getValue() == null ? "" : modelloField.getValue().trim();
			TipoAuto tipo = tipoField.getValue();
			if (targa.isEmpty() || modello.isEmpty() || tipo == null) {
				Notification.show("Compila tutti i campi dell’auto");
				return;
			}

			if (!targa.matches("^[A-Z]{2}[0-9]{3}[A-Z]{2}$")) {
		        Notification.show("Targa non valida! Usa il formato: AA123BB", 4000, Notification.Position.TOP_CENTER)
		            .addThemeVariants(NotificationVariant.LUMO_ERROR);
		        return;
		        }
			
			String tipoStringa=tipo.getLabel();
			autoService.aggiungiAuto(targa, modello, tipoStringa, utente)
					.thenRun(() -> getUI().ifPresent(ui -> ui.access(() -> {

						Notification n = Notification.show("Auto registrata!", 3000, Notification.Position.TOP_CENTER);
						n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

						targaField.clear();
						modelloField.clear();
						tipoField.clear();

					}))).exceptionally(ex -> {

						getUI().ifPresent(ui -> ui.access(() -> {
							String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
							Notification n = Notification.show("Errore: " + msg, 4000,
									Notification.Position.TOP_CENTER);
							n.addThemeVariants(NotificationVariant.LUMO_ERROR);
						}));
						return null;
					});
		});
		// Layout e aggiunta componenti
		HorizontalLayout confAuto = new HorizontalLayout(targaField, modelloField, tipoField);
		confAuto.setSpacing(true);

		modifica.add(emailField, cambiaMailBtn, passwordField, cambiaPwdBtn, confAuto, aggiungiAutoBtn);
		datiEmodifica.add(createProfileCard(utente), modifica);
		add(datiEmodifica, autoLayout);
		datiEmodifica.setFlexGrow(1, modifica); 
		add(datiEmodifica, autoLayout);

	}
	
	private void aggiornaMail(String nuovaEmail) {

		mail.setText("Mail: " + nuovaEmail);		
	}

	private VerticalLayout createProfileCard(Utente utente) {
		VerticalLayout layout = new VerticalLayout();
		
		
		Paragraph nome = new Paragraph(utente.getNome());
		Paragraph cognome = new Paragraph(utente.getCognome());
		mail = new Paragraph(utente.getEmail());

		VerticalLayout profileCard = new VerticalLayout();
		profileCard.setPadding(true);
		profileCard.setSpacing(true);
		profileCard.setWidth("500px");
		profileCard.setAlignItems(Alignment.CENTER);
		profileCard.getStyle().set("background-color", "white");
		profileCard.getStyle().set("border-radius", "16px");
		profileCard.getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.10)");
		profileCard.getStyle().set("padding", "20px");
		profileCard.getStyle().set("margin-top", "20px");

		Span avatar = new Span("👤");
		avatar.getStyle().set("font-size", "50px");

		Span title = new Span("Profilo Utente");
		title.getStyle().set("font-size", "26px");
		title.getStyle().set("font-weight", "bold");
		title.getStyle().set("margin-bottom", "10px");

		VerticalLayout infoBlock = new VerticalLayout();
        infoBlock.setPadding(false);
        infoBlock.setSpacing(false); // Riduciamo lo spazio tra le righe
        infoBlock.setWidth("100%");
        
        // Creiamo righe orizzontali per ogni dato
        infoBlock.add(createRow("Nome:", nome.getText()));
        infoBlock.add(createRow("Cognome:", cognome.getText()));
        
        // Per la mail, salviamo il riferimento al valore per poterlo aggiornare dopo
        Span labelMail = new Span("Email:");
        labelMail.getStyle().set("font-weight", "bold").set("width", "80px");
        Span valueMail = new Span(mail.getText());
        valueMail.getStyle().set("color", "#333");
		HorizontalLayout rowMail = new HorizontalLayout(labelMail, valueMail);
        rowMail.setAlignItems(Alignment.BASELINE);
        infoBlock.add(rowMail);
		
		Button toggleModificaBtn = new Button("Modifica Dati");
        toggleModificaBtn.getStyle().set("background-color", "#3498DB"); // Blu
        toggleModificaBtn.getStyle().set("color", "white");
        toggleModificaBtn.getStyle().set("margin-top", "15px");

        toggleModificaBtn.addClickListener(e -> {
            boolean isVisible = modifica.isVisible();
            modifica.setVisible(!isVisible); // Inverte la visibilità
            
            // Cambia il testo del bottone per feedback visivo
            if (!isVisible) {
                toggleModificaBtn.setText("Chiudi Modifica");
                toggleModificaBtn.getStyle().set("background-color", "#95A5A6"); // Grigio
            } else {
                toggleModificaBtn.setText("Modifica Dati");
                toggleModificaBtn.getStyle().set("background-color", "#3498DB"); // Blu
            }
        });
        
        layout.add(avatar, title, infoBlock, toggleModificaBtn);
		profileCard.getStyle().set("flex-shrink", "0"); 
		return layout;
	}

	private HorizontalLayout createSaluto(String user) {
		String saluto;
		int ora = LocalTime.now().getHour();

		if (ora >= 6 && ora < 12) {
			saluto = "Buongiorno ";
		} else if (ora >= 12 && ora < 18) {
			saluto = "Buon pomeriggio ";
		} else {
			saluto = "Buonasera ";
		}

		HorizontalLayout titolo = new HorizontalLayout();
		Span userSpan = new Span(user);
        userSpan.getStyle().set("font-weight", "bold");
        H3 title = new H3(new Text(saluto), userSpan, new Text("! Ecco la tua pagina di profilo"));
        title.getStyle().set("color", "#008000");
        userSpan.getStyle().set("color", "#2E7D32");
        
		titolo.add(title);

		return titolo;
	}

	private HorizontalLayout createRow(String labelText, String valueText) {
        Span label = new Span(labelText);
        label.getStyle().set("font-weight", "bold");
        label.getStyle().set("width", "80px"); // Larghezza fissa per allineare i valori verticalmente

        Span value = new Span(valueText);
        value.getStyle().set("color", "#333");

        HorizontalLayout row = new HorizontalLayout(label, value);
        row.setAlignItems(Alignment.BASELINE); // Allinea il testo sulla stessa linea di base
        row.setSpacing(true);
        return row;
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
		}
	}

	private VerticalLayout createCard(String titleText) {
		VerticalLayout card = new VerticalLayout();
		card.setPadding(true);
		card.setSpacing(true);
		card.setWidth("100%");
		card.setMaxWidth("500px");
		card.getStyle().set("background-color", "white");
		card.getStyle().set("border-radius", "12px");
		card.getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.08)");
		card.getStyle().set("margin-top", "20px");

		Span title = new Span(titleText);
		title.getStyle().set("font-size", "18px");
		title.getStyle().set("font-weight", "bold");
		title.getStyle().set("color", "#2ECC71");

		card.add(title);
		return card;
	}

	private Button createMainButton(String label) {
		Button btn = new Button(label);
		btn.setWidthFull();
		btn.getStyle().set("background-color", "#2ECC71");
		btn.getStyle().set("color", "white");
		btn.getStyle().set("font-weight", "bold");
		btn.getStyle().set("border-radius", "8px");
		btn.getStyle().set("padding", "10px");
		return btn;
	}

}
