/**
 * Classe di configurazione firebase
 * 
 */
package com.example.database;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

@Configuration
public class FirebaseConfiguration {

    @Bean 
    public FirebaseDatabase firebaseDatabase() throws IOException {
        
        // Controlliamo se Firebase è già attivo
        if (FirebaseApp.getApps().isEmpty()) {
            // Caricamento del file
            InputStream is = new ClassPathResource("chiave.json").getInputStream();

            // Check if the key is a template
            byte[] bytes = is.readAllBytes();
            String content = new String(bytes, StandardCharsets.UTF_8);

            if (content.contains("REPLACE_WITH_YOUR")) {
                System.err.println("------------------------------------------------------------------");
                System.err.println("CONFIGURATION ERROR: 'chiave.json' is still a placeholder!");
                System.err.println("Please paste your actual Firebase service account key in:");
                System.err.println("src/main/resources/chiave.json");
                System.err.println("------------------------------------------------------------------");
                return null; 
            }

            // Re-create the stream for GoogleCredentials
            InputStream serviceAccount = new ByteArrayInputStream(bytes);
            
            // Configurazione
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://database-sw-9750e-default-rtdb.europe-west1.firebasedatabase.app/")
                    .build();

            // Avvio
            FirebaseApp.initializeApp(options);
            System.out.println("Firebase inizializzato correttamente!");
        }

        // Spring prenderà questo oggetto e lo passerà ai tuoi Service.
        return FirebaseDatabase.getInstance();
    }
}