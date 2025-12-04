package com.example.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.models.Colonnina;

import com.example.modelsInterface.ColonnineInterface;

@Service
public class ColonnineService {

	private ColonnineInterface colonnineInterface;

	@Autowired
	public ColonnineService(ColonnineInterface colonnineInterface) {
		this.colonnineInterface = colonnineInterface;
	}

	public String getSlotCorrenteTimestamp() {
		LocalDateTime now = LocalDateTime.now();
		int minuteSlot = (now.getMinute() / 30) * 30;

		LocalDateTime slot = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(),
				minuteSlot);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		return slot.format(formatter);
	}

	public CompletableFuture<Void> aggiornaStato(String msg) {

		CompletableFuture<Void> future = colonnineInterface.getColonnineSlot(getSlotCorrenteTimestamp())
				.thenCompose(lista -> {
					// lista dei CompletableFuture per aggiornare lo stato
					List<CompletableFuture<Void>> listaCF = new ArrayList<>();

					for (String c : lista) {

						listaCF.add(colonnineInterface.cambiaStatoColonnina(c, msg));
					}

					// attendo che tutti terminino
					return CompletableFuture.allOf(listaCF.toArray(new CompletableFuture[0]));
				});

		return future;
	}

	public CompletableFuture<Void> aggiornaStatoCarica(String msg) {

		CompletableFuture<Void> future = colonnineInterface.getColonnineInCarica().thenCompose(lista -> {
			// lista dei CompletableFuture per aggiornare lo stato
			List<CompletableFuture<Void>> listaCF = new ArrayList<>();

			for (String c : lista) {

				listaCF.add(colonnineInterface.cambiaStatoColonnina(c, msg));
			}

			// attendo che tutti terminino
			return CompletableFuture.allOf(listaCF.toArray(new CompletableFuture[0]));
		});

		return future;
	}

	public CompletableFuture<Void> inizializza(String msg) {

		CompletableFuture<Void> future = colonnineInterface.getAllColonnine().thenCompose(lista -> {
			// lista dei CompletableFuture per aggiornare lo stato
			List<CompletableFuture<Void>> listaCF = new ArrayList<>();

			for (Colonnina c : lista) {
				if (!c.getStato().equals("Manutenzione"))
					listaCF.add(colonnineInterface.cambiaStatoColonnina(c.getId(), msg));
			}

			// attendo che tutti terminino
			return CompletableFuture.allOf(listaCF.toArray(new CompletableFuture[0]));
		});

		return future;
	}

	public CompletableFuture<List<Colonnina>> cercaColonnine(String query) {

		return colonnineInterface.cercaColonnine(query);
	}

	public CompletableFuture<List<String>> getColonnineInCarica() {

		CompletableFuture<List<String>> future = new CompletableFuture<>();

		colonnineInterface.getColonnineInCarica().thenAccept(lista -> {
			future.complete(lista);
		}).exceptionally(e -> {
			future.completeExceptionally(e);
			return null;
		});

		return future;
	}

}
