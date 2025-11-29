package com.example.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase; // Importante
import org.springframework.context.annotation.Bean;     // Importante
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfiguration {

    // NOTA: Ho cambiato da 'void' a 'FirebaseDatabase' e aggiunto @Bean
    @Bean 
    public FirebaseDatabase firebaseDatabase() throws IOException {
        
        // 1. Controlliamo se Firebase è già attivo
        if (FirebaseApp.getApps().isEmpty()) {
            // Caricamento del file
            InputStream serviceAccount = new ClassPathResource("chiave.json").getInputStream();

            // Configurazione
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://database-sw-9750e-default-rtdb.europe-west1.firebasedatabase.app/")
                    .build();

            // Avvio
            FirebaseApp.initializeApp(options);
            System.out.println("Firebase inizializzato correttamente!");
        }

        // 2. IMPORTANTE: Restituiamo l'istanza del database.
        // Spring prenderà questo oggetto e lo passerà ai tuoi Service.
        return FirebaseDatabase.getInstance();
    }
}