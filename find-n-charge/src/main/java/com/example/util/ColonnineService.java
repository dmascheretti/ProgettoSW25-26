package com.example.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

	public long getSlotCorrenteTimestamp() {
	    LocalDateTime now = LocalDateTime.now();
	    int minuteSlot = (now.getMinute() / 30) * 30;

	    LocalDateTime slot = LocalDateTime.of(
	            now.getYear(), now.getMonth(), now.getDayOfMonth(),
	            now.getHour(), minuteSlot
	    );

	    return slot.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
	
	public CompletableFuture<Void> aggiornaStato() {

		CompletableFuture<Void> future =fb.getPrenotazioniSlot(getSlotCorrenteTimestamp()).thenCompose(lista -> {
	        // lista dei CompletableFuture per aggiornare lo stato
	        List<CompletableFuture<Void>> updates = new ArrayList<>();
	        
	        for (Prenotazione p : lista) {
	            updates.add(fb.cambiaStatoColonnina(p.getNomeColonnina(), "Prenotata"));
	        }

	        // attendo che tutti terminino
	        return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0]));
	    });
		
		return future;
	}

	
	
}
