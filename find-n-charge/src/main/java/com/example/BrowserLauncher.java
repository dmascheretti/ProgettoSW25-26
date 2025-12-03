/**
 * Classe BrowserLauncher che automatizza l'apertura dell'app sul browser
 * 
 * @author Francesco Valenari
 */
package com.example;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.awt.Desktop;
import java.net.URI;

@Component
public class BrowserLauncher {

    //Inietta il contesto del server web
    private final ServletWebServerApplicationContext webServerAppCtxt;

    public BrowserLauncher(ServletWebServerApplicationContext webServerAppCtxt) {
        this.webServerAppCtxt = webServerAppCtxt;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        // Imposta headless a false (necessario per aprire finestre grafiche/browser)
        System.setProperty("java.awt.headless", "false");

        // Recupera la porta dinamica
        int port = webServerAppCtxt.getWebServer().getPort();
        
        // Costruisce l'URL
        String url = "http://localhost:" + port;


        System.out.println(">>> AUTOMATION: Apertura browser su " + url);

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                }
            }
        } catch (Exception e) {
            // Logga l'errore senza rompere l'app
            System.err.println("Non sono riuscito ad aprire il browser automaticamente.");
            e.printStackTrace();
        }
    }
}