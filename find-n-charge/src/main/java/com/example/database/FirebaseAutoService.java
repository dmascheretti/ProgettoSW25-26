package com.example.database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.example.models.Auto;
import com.example.models.Utente;
import com.example.modelsInterface.AutoInterface;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class FirebaseAutoService implements AutoInterface {

	private final DatabaseReference automobile;

	/**
	 * Assegno ad automobile il link al nodo "auto" nel database
	 * 
	 * @param db
	 */
	public FirebaseAutoService(FirebaseDatabase db) {
		this.automobile = db.getReference("auto");
	}

	/**
	 * Verifica se la targa inserita è già esistente nel sistema
	 * 
	 * @param targa Targa da verificare
	 * @return Future che contiene auto se trovata, null se non trovata
	 */
	public CompletableFuture<Auto> verificaTarga(String targa) {
		CompletableFuture<Auto> future = new CompletableFuture<>();

		DatabaseReference targaRef = automobile.child(targa);
		targaRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {

				if (dataSnapshot.exists()) {
					Auto macchina = dataSnapshot.getValue(Auto.class);
					future.complete(macchina);
				} else {
					// utente non trovato
					future.complete(null);
				}
			}

			@Override
			public void onCancelled(DatabaseError error) {
				// Gestione errore standard, come negli altri metodi
				future.completeExceptionally(error.toException());
			}

		});
		return future;
	}

	/**
	 * Salva auto nel database
	 * 
	 * @param auto Da salvare
	 * @return Future di tipo void
	 */
	public CompletableFuture<Void> salvaAuto(Auto auto) {
		CompletableFuture<Void> futureAuto = new CompletableFuture<>();
		automobile.child(auto.getTarga()).setValue(auto, (databaseError, ref) -> {
			if (databaseError != null) {
				// errore --> chiama eccezione anche in RegisterView
				futureAuto.completeExceptionally(new RuntimeException(databaseError.getMessage()));
			} else {
				futureAuto.complete(null);
			}
		});
		return futureAuto; // qui il thenRun() in registrazione capisce che ha finito e prosegue con
		// esecuzione
	}

	public double calcolaNuovoStato(Auto auto, LocalDateTime oraAttuale, double potenzaColonninaKw) {

		if (auto.getInizioRicarica() == null)
			return auto.getStatoCarica();

		Duration durata = Duration.between(auto.getInizioRicarica(), oraAttuale);
		double oreTrascorse = durata.toMinutes() / 60.0;

		// energia caricata in kWh
		double energiaCaricata = potenzaColonninaKw * oreTrascorse;

		// energia iniziale
		double energiaIniziale = auto.getCapacitaBatteria() * (auto.getStatoCarica() / 100.0);

		// nuova energia
		double energiaTotale = energiaIniziale + energiaCaricata;

		if (energiaTotale > auto.getCapacitaBatteria())
			energiaTotale = auto.getCapacitaBatteria();

		// convertitore in %
		double nuovoSoC = (energiaTotale / auto.getCapacitaBatteria()) * 100.0;

		auto.setStatoCarica(nuovoSoC);
		return nuovoSoC;
	}

	/**
	 * Restituisce la lista delle auto dato utente
	 * 
	 * @param u Utente
	 * @return Lista di auto dell'utente passato come parametro
	 */
	public CompletableFuture<List<Auto>> listaAutoUtente(Utente u) {
		CompletableFuture<List<Auto>> future = new CompletableFuture<>();
		ArrayList<Auto> lista = new ArrayList<>();

		Query q = automobile.orderByChild("proprietario").equalTo(u.getUsername());
		q.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {

				lista.clear();

				if (snapshot.exists()) {
					for (DataSnapshot d : snapshot.getChildren()) {
						lista.add(d.getValue(Auto.class));

					}
				}

				future.complete(lista);

			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// Gestione errore standard, come negli altri metodi
				System.err.println("Errore nel contare le colonnine: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}

		});
		return future;

	}

	/**
	 * Restituisce lista targhe utente
	 * 
	 * @param u Utente selezionato
	 * @return Lista di stringhe che contiene tutte le targhe dell'utente
	 */
	public CompletableFuture<List<String>> getTargheUtente(Utente u) {
		CompletableFuture<List<String>> future = new CompletableFuture<>();
		ArrayList<String> lista = new ArrayList<>();

		Query q = automobile.orderByChild("proprietario").equalTo(u.getUsername());
		q.addListenerForSingleValueEvent(new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot snapshot) {

				lista.clear();

				if (snapshot.exists()) {
					for (DataSnapshot d : snapshot.getChildren()) {
						lista.add(d.getValue(Auto.class).getTarga());

					}
				}

				future.complete(lista);

			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				// Gestione errore standard, come negli altri metodi
				System.err.println("Errore: " + databaseError.getMessage());
				future.completeExceptionally(databaseError.toException());
			}

		});
		return future;

	}

}
