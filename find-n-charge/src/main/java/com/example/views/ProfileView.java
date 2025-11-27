package com.example.views;

import com.example.models.Auto;
import com.example.models.Utente;
import com.example.database.FirebaseAutoService;
import com.example.database.FirebasePrenotazioniService;
import com.example.database.FirebaseService;
import com.example.layout.MainLayout;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;

@Route(value = "profilo", layout = MainLayout.class)
@PageTitle("Find&Charge - Profilo")
public class ProfileView extends VerticalLayout {

    private final FirebaseService firebaseService;
    private final FirebasePrenotazioniService fbPrenotazioni;
    private final FirebaseAutoService firebaseAutoService = new FirebaseAutoService();

    public ProfileView(FirebaseService firebaseService, FirebasePrenotazioniService fbPrenotazioni) {
        this.firebaseService = firebaseService;
        this.fbPrenotazioni=fbPrenotazioni;

        setSpacing(true);
        setPadding(true);

        // Prendi utente dalla sessione (controllo null per sicurezza)
        Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        if (utente == null) {
            // Comportamento di fallback (puoi cambiare: redirect, notifica, ecc.)
            add(new Paragraph("Utente non trovato. Effettua il login."));
            return;
        }

        // Header
        H3 titolo = new H3();
        titolo.getStyle().set("color", "#008000");
        Span userSpan = new Span(utente.getUsername());
        userSpan.getStyle()
                .set("color", "#FF5722")
                .set("font-weight", "bold");
        titolo.add(new Text("Ciao "), userSpan, new Text("! Ecco la tua pagina di profilo"));

        Paragraph nome = new Paragraph("Nome: " + utente.getNome());
        Paragraph cognome = new Paragraph("Cognome: " + utente.getCognome());
        Paragraph mail = new Paragraph("Mail: " + utente.getEmail());
        
        HorizontalLayout hl = new HorizontalLayout();
        hl.setWidthFull();
        hl.setPadding(true);
        hl.setSpacing(true);

        firebaseAutoService.listaAutoUtente(utente).thenAccept(lista -> {
            getUI().ifPresent(ui -> ui.access(() -> {

                hl.removeAll();

                for (Auto a : lista) {

                    VerticalLayout card = new VerticalLayout();
                    card.setPadding(true);
                    card.setSpacing(false);
                    card.setWidth("350px");
                    card.getStyle().set("border", "1px solid #e0e0e0");
                    card.getStyle().set("border-radius", "12px");
                    card.getStyle().set("box-shadow", "0 2px 8px rgba(0,0,0,0.10)");
                    card.getStyle().set("background-color", "white");

                    H3 titolo2 = new H3(a.getModello());
                    titolo.getStyle().set("margin-bottom", "0");

                    Paragraph targa = new Paragraph("Targa: " + a.getTarga());
                    Paragraph carica = new Paragraph("Carica residua: " + a.getStatoCarica() + "%");
                    
                    card.add(titolo2, targa, carica);
                    
                   fbPrenotazioni.inCarica(a).thenAccept(trovata->{
                	   ui.access(()->{
                	   Paragraph inCarica;
                	   if(trovata) {
                	   inCarica = new Paragraph("Auto in carica");
                	   }
                	   else {
                		 inCarica = new Paragraph("Auto non in carica");
                	   }
                       card.add(inCarica);
                	   
                	   });
                   });
                    
                    

                    

                    hl.add(card);
                }
            }));
        });


        // Email change controls
        TextField emailField = new TextField("Nuova Email");
        Button cambiaMailBtn = new Button("Aggiorna Email");

        cambiaMailBtn.addClickListener(e -> {
            String nuovaEmail = emailField.getValue();
            if (nuovaEmail == null || nuovaEmail.isEmpty()) {
                Notification notif = Notification.show("Inserisci una email valida");
                notif.setPosition(Notification.Position.TOP_CENTER);
                return;
            }

            getUI().ifPresent(ui -> {
                firebaseService.cambiaMail(utente, nuovaEmail)
                        .thenRun(() -> ui.access(() -> {
                            // aggiorna utente in sessione
                            utente.setEmail(nuovaEmail);
                            VaadinSession session = ui.getSession();
                            session.setAttribute("utente", utente);

                            // aggiorna label e mostra notifica
                            mail.setText("Mail: " + nuovaEmail);
                            Notification notif = Notification.show("Email aggiornata correttamente!");
                            notif.setPosition(Notification.Position.TOP_CENTER);
                            notif.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                            emailField.clear();
                        }))
                        .exceptionally(ex -> {
                            ui.access(() -> {
                                Notification notif = Notification.show("Errore aggiornamento email: " + ex.getMessage());
                                notif.setPosition(Notification.Position.TOP_CENTER);
                                notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
                            });
                            return null;
                        });
            });
        });

        // Password change (locale - se vuoi integrazione con Firebase, sostituire)
        PasswordField passwordField = new PasswordField("Nuova Password");
        Button cambiaPwdBtn = new Button("Aggiorna Password");
        cambiaPwdBtn.addClickListener(e -> {
            String nuovaPwd = passwordField.getValue();
            if (nuovaPwd != null && !nuovaPwd.isEmpty()) {
                utente.setPassword(nuovaPwd);
                Notification.show("Password aggiornata correttamente!");
                passwordField.clear();
            } else {
                Notification.show("Inserisci una password valida");
            }
        });

        // --- Aggiungi Auto ---
        TextField targaField = new TextField("Targa");
        TextField modelloField = new TextField("Modello");

        ComboBox<String> tipoField = new ComboBox<>("Tipo di auto");
        tipoField.setItems("Berlina (65 kWh)", "Suv (100 kWh)", "Sportiva (75 kWh)", "Utilitaria (40 kWh)");
        tipoField.setPlaceholder("Seleziona il tipo");
        tipoField.setAllowCustomValue(false);

        Button aggiungiAutoBtn = new Button("Aggiungi Auto");

        aggiungiAutoBtn.addClickListener(e -> {

            String targa = targaField.getValue() != null ? targaField.getValue().trim() : "";
            String modello = modelloField.getValue() != null ? modelloField.getValue().trim() : "";
            String tipo = tipoField.getValue();

            if (targa.isEmpty() || modello.isEmpty() || tipo == null || tipo.isEmpty()) {
                Notification.show("Compila tutti i campi dell’auto");
                return;
            }

            // Crea l'oggetto auto con l'utente (passa l'oggetto utente se il costruttore lo richiede)
            Auto auto = new Auto(targa, modello, tipo, utente.getUsername());

            // verificaTarga deve restituire CompletableFuture<Auto> (auto esistente) o null se non esiste
            firebaseAutoService.verificaTarga(targa).thenAccept(autoDB -> {

                getUI().ifPresent(ui -> ui.access(() -> {

                    if (autoDB != null) {
                        // AUTO GIÀ ESISTE
                        Notification n = Notification.show(
                                "La targa è già registrata nel sistema!",
                                4000,
                                Notification.Position.TOP_CENTER
                        );
                        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }

                    // AUTO NON ESISTE: SALVA
                    firebaseAutoService.salvaAuto(auto)
                            .thenRun(() -> ui.access(() -> {
                                Notification n = Notification.show(
                                        "Auto registrata correttamente!",
                                        3000,
                                        Notification.Position.TOP_CENTER
                                );
                                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                                // aggiorna modello locale utente
                                if (utente.getAutoList() != null) {
                                    utente.getAutoList().add(auto);
                                }

                                // pulisci campi
                                targaField.clear();
                                modelloField.clear();
                                tipoField.clear();
                            }))
                            .exceptionally(ex -> {
                                ui.access(() -> {
                                    Notification n = Notification.show(
                                            "Errore: " + ex.getMessage(),
                                            4000,
                                            Notification.Position.TOP_CENTER
                                    );
                                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                                });
                                return null;
                            });

                })); // fine ui.access

            }).exceptionally(ex -> {
                // gestione eventuale errore nella verifica targa
                getUI().ifPresent(ui -> ui.access(() -> {
                    Notification n = Notification.show(
                            "Errore durante la verifica targa: " + ex.getMessage(),
                            4000,
                            Notification.Position.TOP_CENTER
                    );
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }));
                return null;
            });
        });

        // Layout e aggiunta componenti (solo qui, una volta)
        HorizontalLayout confAuto = new HorizontalLayout(targaField, modelloField, tipoField);
        confAuto.setSpacing(true);

        add(
                titolo,
                nome,
                cognome,
                mail,
                hl,
                emailField,
                cambiaMailBtn,
                passwordField,
                cambiaPwdBtn,
                confAuto,
                aggiungiAutoBtn
        );

        setDefaultHorizontalComponentAlignment(Alignment.START);
    }
}
