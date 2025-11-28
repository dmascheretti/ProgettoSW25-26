package com.example.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.database.FirebaseService;
import com.example.models.Colonnina;
import com.example.models.Prenotazione;

@Service
public class ColonnineService {
	
	private FirebaseService fb;
	
	@Autowired
	public ColonnineService(FirebaseService fb) {
		this.fb=fb;
	}

	public String getSlotCorrenteTimestamp() {
	    LocalDateTime now = LocalDateTime.now();
	    int minuteSlot = (now.getMinute() / 30) * 30;

	    LocalDateTime slot = LocalDateTime.of(
	            now.getYear(), now.getMonth(), now.getDayOfMonth(),
	            now.getHour(), minuteSlot
	    );

	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
	    return slot.format(formatter);
	}
	
	public CompletableFuture<Void> aggiornaStato(String msg) {

		CompletableFuture<Void> future =fb.getColonnineSlot(getSlotCorrenteTimestamp()).thenCompose(lista -> {
	        // lista dei CompletableFuture per aggiornare lo stato
	        List<CompletableFuture<Void>> listaCF = new ArrayList<>();
	        
	        for (String c : lista) {
	        	
	        	listaCF.add(fb.cambiaStatoColonnina(c, msg));
	        }

	        // attendo che tutti terminino
	        return CompletableFuture.allOf(listaCF.toArray(new CompletableFuture[0]));
	    });
		
		return future;
	}
	
	public CompletableFuture<Void> aggiornaStatoCarica(String msg) {

		CompletableFuture<Void> future =fb.getColonnineInCarica().thenCompose(lista -> {
	        // lista dei CompletableFuture per aggiornare lo stato
	        List<CompletableFuture<Void>> listaCF = new ArrayList<>();
	        
	        for (String c : lista) {
	        	
	        	listaCF.add(fb.cambiaStatoColonnina(c, msg));
	        }

	        // attendo che tutti terminino
	        return CompletableFuture.allOf(listaCF.toArray(new CompletableFuture[0]));
	    });
		
		return future;
	}
	
	public CompletableFuture<Void> inizializza(String msg) {

		CompletableFuture<Void> future =fb.getAllColonnine().thenCompose(lista -> {
	        // lista dei CompletableFuture per aggiornare lo stato
	        List<CompletableFuture<Void>> listaCF = new ArrayList<>();
	        
	        for (Colonnina c : lista) {
	        	if (!c.getStato().equals("Manutenzione"))
	            listaCF.add(fb.cambiaStatoColonnina(c.getId(), msg));
	        }

	        // attendo che tutti terminino
	        return CompletableFuture.allOf(listaCF.toArray(new CompletableFuture[0]));
	    });
		
		return future;
	}

	
	
}
