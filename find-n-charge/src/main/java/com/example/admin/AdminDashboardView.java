package com.example.admin;

import com.example.AdminLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import jakarta.annotation.security.RolesAllowed;

@PageTitle("Find&Charge | Dashboard")
@Route(value = "dashboard", layout = AdminLayout.class) // Carica la pagina nel layout dell'admin
@RouteAlias(value = "admin", layout = AdminLayout.class) 

public class AdminDashboardView extends VerticalLayout {

    public AdminDashboardView() {
       
    }
}