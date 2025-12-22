package com.example.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoTest {

	@Test
	void testCreazioneAuto() {

		Auto test = new Auto("AB123CD", "Model X", "Suv (100 kWh)", "Mario");

		int kwh = test.getCapacitaBatteria();

		assertEquals(100, kwh);
	}

	@Test
	void testStatoCaricaRange() {
		Auto auto = new Auto("AB123CD", "Panda", "Berlina", "Luigi");

		assertTrue(auto.getStatoCarica() >= 20 && auto.getStatoCarica() <= 100);
	}

	@Test
	void testStatoCaricaRangeFalso() {

		Auto auto = new Auto("AB123CD", "Panda", "Berlina", "Luigi");

		assertFalse(auto.getStatoCarica() < 20 || auto.getStatoCarica() > 100);
	}
}