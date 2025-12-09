<div align="center">

# ⚡ Find&Charge
### Progetto Ingegneria del Software 2025-2026

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Vaadin](https://img.shields.io/badge/Vaadin-24-00B4F0?style=for-the-badge&logo=vaadin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Realtime_DB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)

<br>

**Find&Charge** è un'applicazione web moderna progettata per la prenotazione e la gestione di colonnine di ricarica per veicoli elettrici.

</div>

---

## 🏗️ Architettura del Software

Il progetto segue un'architettura divisa a **Layer** per garantire il disaccoppiamento tra le classi e un alto livello di manutenibilità.

* **Presentation Layer (Views):** Gestione dell'interfaccia utente (Vaadin).
* **Business Layer (Service):** Logica applicativa e validazione.
* **Data Access Layer (Interface + FirebaseService):** Gestione dati.

---

## ✨ Funzionalità Principali

### 🚗 Per l'Utente
*  **Mappa Interattiva:** Navigazione sulla mappa con marker dinamici.
*  **Prenotazione Smart:** Prenotazione diretta dalla mappa con controllo disponibilità slot in tempo reale.
*  **Profilo & Garage:** Spazio unico per gestire dati personali e veicoli.
*  **QR Code Dinamico:** Sblocco della prenotazione con un click tramite QR Code univoco.
*  **Feedback:** Sistema di valutazione e recensione post-ricarica.

### 🛡️ Per l'Admin
*  **Dashboard KPI:** Visualizzazione grafici interattivi e dati in tempo reale.
*  **Gestione Completa:** Pannelli dedicati per colonnine, prenotazioni e utenti.
*  **Monitoraggio:** Controllo stato colonnine e segnalazione guasti.
*  **Sicurezza:** Gestione accessi e ban degli utenti indesiderati.

---

## 🛠️ Tecnologie Utilizzate

| Ambito | Tecnologia | Dettagli |
| :--- | :--- | :--- |
| **Backend** | ![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white) ![Spring](https://img.shields.io/badge/Spring_Boot-6DB33F?logo=spring&logoColor=white) | Java 17, Spring Boot |
| **Frontend** | ![Vaadin](https://img.shields.io/badge/Vaadin-00B4F0?logo=vaadin&logoColor=white) | Framework UI |
| **Database** | ![Firebase](https://img.shields.io/badge/Firebase-FFCA28?logo=firebase&logoColor=black) | Realtime NoSQL Database |
| **Mappe** | **Leaflet.js** | Rendering mappe e marker custom |
| **Utility** | **ZXing** | Generazione QR Code |
| **Sicurezza** | **BCrypt** | Sicurezza password |
| **Analytics** | **ApexCharts** | Grafici e statistiche|

---

## 🚀 Istruzioni per Installazione e Utilizzo

### Prerequisiti
* **JDK 17** o superiore installato.
* **Maven** installato.
* Connessione internet stabile attiva.

### Avvio Rapido
1.  **Clona la repository:**
   
2.  **Compila ed esegui:**
    Da terminale o dal tuo IDE:
    > `Run As` -> `Java Application` -> `Application.java`
    
3.  **Attendi l'avvio:**
    L'applicazione si avvierà in maniera automatica sul browser predefinito su una porta libera del tuo PC.

---

## 🧪 Qualità del Codice e Testing



---

## 👥 Team di Sviluppo

Progetto realizzato per il corso di **Ingegneria del Software**, Università degli Studi di Bergamo (A.A. 2025/2026).

* **Tommaso Maistrello** - *Scrum Master & Frontend Developer*
* **Davide Mascheretti** - *Product Owner & Backend Developer*
* **Claudio Morgera** - *Backend & Testing*
* * **Francesco Valenari** - *Frontend & Software Architect*
  
