package com.example.views;

import com.example.MainLayout;
import com.example.models.Auto;
import com.example.models.Utente;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.component.combobox.ComboBox;
import com.example.database.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "profilo", layout = MainLayout.class)
@PageTitle("Find&Charge - Profilo")

public class ProfileView extends VerticalLayout {
	
	private final FirebaseService firebaseService;
	// Classe per le informazioni utente e area personale
	public ProfileView(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
		setSpacing(true);
        setPadding(true);
        Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        
        H3 titolo = new H3();
        titolo.getStyle().set("color", "#008000");
        Span userSpan = new Span(utente.getUsername());
        userSpan.getStyle()
                .set("color", "#FF5722")  
                .set("font-weight", "bold");

        titolo.add(new Text("Ciao "), userSpan, new Text("!\n Ecco la tua pagina di profilo"));
		Paragraph nome = new Paragraph("Nome: "+utente.getNome());
		Paragraph cognome = new Paragraph("Cognome: "+utente.getCognome());
		Paragraph mail = new Paragraph("Mail: "+utente.getEmail());
		add(titolo, nome, cognome, mail);
		
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
		            .thenRun(() -> {
		                ui.access(() -> {
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
		                });
		            })
		            .exceptionally(ex -> {
		                ui.access(() -> {
		                    Notification notif = Notification.show("Inserisci una mail valida!");
		                    notif.setPosition(Notification.Position.TOP_CENTER);
		                    notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
		                });
		                return null;
		            });
		    });
		});

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
            String targa = targaField.getValue();
            String modello = modelloField.getValue();
            String tipo = tipoField.getValue();
            if (!targa.isEmpty() && !modello.isEmpty()) {
                if (utente.getAutoList() == null) {
                    utente.setAutoList(new java.util.ArrayList<>());
                }
                utente.getAutoList().add(new Auto(targa, modello, tipo, utente));
                Notification.show("Auto aggiunta correttamente!");
                targaField.clear();
                modelloField.clear();
            } else {
                Notification.show("Compila tutti i campi dell’auto");
            }
        });

        HorizontalLayout confAuto=new HorizontalLayout(targaField, modelloField, tipoField);
        confAuto.setSpacing(true);
        // Layout finale
        add(
            titolo,
            nome,
            cognome,
            mail,
            emailField,
            cambiaMailBtn,
            passwordField,
            cambiaPwdBtn,
            confAuto,
            aggiungiAutoBtn
        );

        setDefaultHorizontalComponentAlignment(Alignment.START);
	}}
    
	//QUI DA IMPLEMENTARE I CAMBIA PASSWORD E CAMBIA MAIL CON THENRUN


