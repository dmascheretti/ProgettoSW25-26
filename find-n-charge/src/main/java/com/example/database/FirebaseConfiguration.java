/**
 * Classe FirebaseConfiguration, crea la connessione tra l'applicazione e il database online se presente.
 * In caso contrario rileva un errore.
 * 
 * @author Davide Mascheretti
 */

package com.example.database;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfiguration {

	/*
	 * Inizializzazione del database e creazione connessione con l'app
	 */
    @PostConstruct
    public void initialize() {
        try {
            //Controlla se l'app è già stata inizializzata per evitare errori 
            if (FirebaseApp.getApps().isEmpty()) {
                //Riferimento al file json contenente la chiave in src/main/resources
                InputStream serviceAccount = new ClassPathResource("chiave.json").getInputStream();

                //Configurazione del Firebase con url preso da console online
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl("https://database-sw-9750e-default-rtdb.europe-west1.firebasedatabase.app/")
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase si è inizializzato correttamente!");
            }
        } catch (IOException e) {
            //Gestione dell'errore in caso il file non venga trovato
            e.printStackTrace();
            throw new RuntimeException("Errore durante l'inizializzazione di Firebase: " + e.getMessage());
        }
    }
}