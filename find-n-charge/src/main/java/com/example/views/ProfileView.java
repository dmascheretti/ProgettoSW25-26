package com.example.views;

import com.example.MainLayout;
import com.example.models.Auto;
import com.example.models.Utente;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.component.combobox.ComboBox;


@Route(value = "profilo", layout = MainLayout.class)
@PageTitle("Find&Charge - Profilo")

public class ProfileView extends VerticalLayout {

	// Classe per le informazioni utente e area personale
	public ProfileView() {
		setSpacing(true);
        setPadding(true);
        Utente utente = (Utente) VaadinSession.getCurrent().getAttribute("utente");
        
        H3 titolo= new H3("Ciao "+utente.getUsername()+"! Ecco la tua pagina di profilo");
        titolo.getStyle().set("color", "#008000");
		Paragraph nome = new Paragraph("Nome: "+utente.getNome());
		Paragraph cognome = new Paragraph("Cognome: "+utente.getCognome());
		Paragraph mail = new Paragraph("Mail: "+utente.getEmail());
		add(titolo, nome, cognome, mail);
		
		TextField emailField = new TextField("Nuova Email");
        Button cambiaMailBtn = new Button("Aggiorna Email");
        cambiaMailBtn.addClickListener(e -> {
            String nuovaEmail = emailField.getValue();
            if (nuovaEmail != null && !nuovaEmail.isEmpty()) {
                utente.setEmail(nuovaEmail);
                VaadinSession.getCurrent().setAttribute("utente", utente); // aggiorna sessione
                mail.setText("Mail: " + utente.getEmail());
                Notification.show("Email aggiornata correttamente!");
                emailField.clear();
            } else {
                Notification.show("Inserisci una email valida");
            }
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
    }
	}
	
	//QUI DA IMPLEMENTARE I CAMBIA PASSWORD E CAMBIA MAIL CON THENRUN


