/**
 * Classe invio mail
 */
package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async 
    public void inviaEmailBenvenuto(String emailDestinatario, String nomeUtente) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("findandcharge.service@gmail.com");
            message.setTo(emailDestinatario);
            message.setSubject("Benvenuto in Find&Charge!");
            
            message.setText("Ciao " + nomeUtente + ",\n\n" +
                    "Grazie per esserti registrato alla nostra piattaforma!\n" +
                    "Il tuo account è attivo e puoi già iniziare a prenotare le colonnine.\n\n" +
                    "A presto,\n" +
                    "Il team di Find&Charge");

            mailSender.send(message);
            System.out.println("Email inviata a " + emailDestinatario);
        } catch (Exception e) {
            System.err.println("Errore invio email: " + e.getMessage());
        }
    }
}