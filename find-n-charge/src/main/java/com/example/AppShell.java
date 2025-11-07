/**
 * Classe AppShell che permette a Vaadin di aggiornare la UI anche in caso di operazioni asincrone,
 * tenendo aperto sempre un canale di comunicazione tra il server e il client
 * @author Davide Mascheretti, Tommaso Maistrello
 */
package com.example;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;

@Push
public class AppShell implements AppShellConfigurator {
    
}