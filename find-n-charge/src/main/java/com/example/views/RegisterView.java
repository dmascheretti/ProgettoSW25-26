/**
 * Classe RegisterView che gestisce la pagina di registrazione con il form per creare un nuovo account
 * 
 * @author Tommaso Maistrello
 */

package com.example.views;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("register")
@PageTitle("Registrati")
public class RegisterView extends VerticalLayout {

	public RegisterView() {
		super();
	}

}
