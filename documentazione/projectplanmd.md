INTRODUZIONE
Il nostro progetto consiste nello sviluppo di una web app che permette agli utenti di prenotare colonnine di ricarica per veicoli elettrici.
L’app consente agli utenti di bloccare uno slot orario dalla durata di 30 minuti, cumulabili fino ad un massimo di 2 ore, scegliendo direttamente da una mappa integrata.
La ricerca può essere filtrata per tipologia, nome e posizione.
Dopo una prima fase di registrazione (se necessaria) e login, si apre direttamente la mappa stessa con marker che identificano le colonnine presenti nella zona scelta.
La prenotazione può avvenire sia tramite una barra laterale che appare nel momento in cui si clicca su una colonnina, sia tramite una pagina apposita.
A conferma dell’operazione, il sistema genera automaticamente un QR code univoco associato alla colonnina, alla data e all’orario della prenotazione.
Esso viene salvato nella sezione “profilo utente”  dell’app e permette poi tramite la scansione direttamente sulla colonnina di controllare che tutto corrisponda per poi avviare l’erogazione della corrente e la ricarica dell’auto (la gestione di questa fase non rientra nel nostro progetto).
L’idea del QR code permette che solo l’utente che ha prenotato in quello slot può usufruire del servizio, evitando abusi.
L’utente può in ogni momento effettuare il logout dall’applicazione tramite un apposito pulsante presente nel menu.
La nostra applicazione andrebbe a risolvere il problema che, in un futuro con un maggior numero di auto elettriche, una persona che necessita ricarica non rischia di trovare la colonnina occupata, sapendo che nello slot prenotato sarà sicuramente libera.
E’ inoltre molto semplice rendere questa app scalabile anche a grandi città, in cui utenti possono anche collaborare segnalando la presenza di nuove colonnine, guasti e talvolta recensioni e commenti che possono essere inseriti nella scheda della colonnina corrispondente.


MODELLO DI PROCESSO
Il modello utilizzato è un modello di tipo agile. Questo permette di avere piccoli incrementi molto frequenti che ci fanno proseguire con lo sviluppo della nostra applicazione senza avere documenti e fasi rigide da seguire come avverrebbe per altri modelli. In questo modo abbiamo più libertà a favore della collaborazione tra i membri del gruppo, avendo sempre un software funzionante. 
Dunque si pone maggior focus su un design semplice, su meeting frequenti ma brevi e su cambiamenti e integrazione continua.
L’approccio si ispira in particolare al modello SCRUM e sui suoi cicli iterativi così composti.
La suddivisione del tempo a disposizione avviene in tale maniera:
Backlog: 3 giorni di decisione riguardo funzioni da implementare e obiettivi da raggiungere nella fase di Sprint
Sprint: due settimane di  implementazione effettiva delle funzionalità decise in precedenza con meeting in presenza per aggiornamenti su eventuali problematiche 
Review: uno/due meeting di revisione per verificare che tutte le funzionalità dello Sprint siano corrette e funzionanti.


ORGANIZZAZIONE DEL PROCESSO
I ruoli dei membri del gruppo sono stati così suddivisi:
Frontend: sviluppo interfaccia, mappa interattiva, passaggio tra pagine, menù.
Backend+Database: gestione firebase, controlli utente e prenotazioni, salvataggio dati, sicurezza
Testing: 
Documentazione: project plan, diagrammi UML, informazioni generali.


Si possono dividere i ruoli in product owner, scrum master e team di implementazione (di cui fanno parte tutti)
//qui possiamo spiegare cosa fa ognuno e nella parte di implementazione mettiamo front end back end …
Product owner → Davide Mascheretti 
Scrum master → Tommaso Maistrello
Team di implementazione → Tommaso Maistrello, Davide Mascheretti, Claudio Morgera, Francesco Valenari.
Nonostante ciò, nel caso di necessità un membro del gruppo può contribuire alla collaborazione con il membro affidato a quel ruolo. La conoscenza deve essere utilizzata in maniera trasversale.
Ad esempio, se l’addetto al back end riscontra problemi con la gestione del database, al fine di velocizzare i procedimenti, può chiedere supporto aggiuntivo ad un altro membro del gruppo, il quale potrebbe avere le competenze adatte per risolvere il dato problema in minor tempo.
La documentazione chiara e dettagliata è considerata responsabilità condivisa tra tutti i membri del gruppo, essa deve essere il compito di tutti.

Altre importanti caratteristiche di organizzazione sono riportate a seguire.
Brevi meeting principalmente non online per risolvere problemi e per chiarimenti vari.
La comunicazione quotidiana deve avvenire tramite un gruppo Whatsapp apposito, utilizzato per scambio di informazioni e domande inerenti al progetto o a problemi riscontrati.
Decisioni, urgenze e task vengono gestiti dal gruppo stesso durante i meeting settimanali, in cui si decide anche se è necessario un cambio di ruoli.
Utilizzo di GitHub privato condiviso con i membri del gruppo per gestire issue e tenere in memoria le azioni di ogni persona. Utilizzo iniziale di Google Drive per la documentazione e per i riassunti dei meeting con data e descrizione, da caricare poi su GitHub.


STANDARD, LINEE GUIDE E PROCEDURE
Per gestire tutte le procedure e tutti i cambiamenti del nostro progetto si utilizza Github, gestore di configurazione online distribuito che permette ad ognuno di avere una propria local repo. Gli standard di programmazione sono quelli classici di java definite da Oracle. Riguardo la documentazione è necessaria chiarezza sia per le classi aggiunte, che devono essere esplicitamente e dettagliatamente spiegate, sia per tutti i commit che vengono fatti.
Nel titolo del commit ci deve essere in generale le modifiche principali apportate e cosa cambia di importante rispetto alla versione precedente. Nella descrizione è possibile aggiungere tutte le informazioni necessarie che descrivono i cambiamenti passo per passo, le scelte effettuate, le motivazioni se necessarie e tutto ciò che permette di comprendere al meglio da una persona esterna cosa è cambiato in seguito alle modifiche effettuate.
La documentazione è da consegnare necessariamente in seguito al cambiamento del macro argomento su cui stiamo lavorando. Non è necessario fare commenti se i cambiamenti sono ininfluenti sul funzionamento generale dell’applicazione, piuttosto meglio attendere e fare push una volta aggiunto qualcosa di realmente significativo.
Nel caso di modifiche con bug presenti (leggeri non che annullano il funzionamento generale)  è consigliato comunque fare commit, magari aggiungendo la descrizione dell’errore. In questo modo ognuno è aggiornato alla versione corretta ed è possibile risolvere il bug o in parallelo allo sviluppo, oppure alla fine del progetto.
Ci si aspetta che ognuno rispetti le linee guida in maniera autonoma, nessun membro del gruppo verifica che gli altri membri rispettino le linee guida.
Analizziamo ora le scelte tecniche che caratterizzano il nostro progetto.
In relazione al database utilizzato abbiamo deciso di utilizzare regole standard da configurazione, database privato, in modo che solo i membri interni al gruppo possano visualizzare e modificare. Non è gestita parte di password criptate.
Potrebbe essere inoltre utile l'utilizzo di una kanban board per avere una visione oggettiva e in tempo reale dei progressi dell’applicazione.
Per la parte database abbiamo utilizzato firebase - real time database, basato su linguaggio NOSQL. Tramite questo gestiamo tutta la parte di salvataggio, autenticazione, controlli.
Abbiamo tre nodi: uno che rappresenta gli utenti, uno che rappresenta le prenotazioni e uno che rappresenta le prenotazioni per utente. In questo modo è più semplice fare tutte le operazioni di ricerca e confronto che avverranno nel backend della nostra applicazione.
La gestione dei sottonodi verrà decisa in base a come è più comodo, in particolare per il nodo prenotazione (si pensa ad una gestione del tipo prenotazione → date → ora→ info ma non è confermato).
Per la parte grafica utilizziamo Vaadin. Oltre a gestire la parte grafica già integrata, dovrebbe rendere più semplice il collegamento con la parte back end dell’applicazione.
Per la gestione stili e colore di caratteri e in generale della parte web visiva abbiamo integrato Vaadin (che è limitato sotto tale punto di vista) con delle aggiunte css e javascript.
Infine per la mappa abbiamo optato per Leaflet, un software open source che ci permette di usare mappe interattive senza troppi problemi (inizialmente avevamo pensato a google API maps ma la procedura di ottenimento della chiave era molto più complessa).
Queste aggiunte rendono la nostra applicazione più professionale e, in un futuro, realmente utilizzabile.

FORSE PARTE DI COSA ABBIAMO UTILIZZATO MEGLIO METTERLE IN TECNICHE?


ATTIVITÀ DI GESTIONE
La gestione del progetto avviene attraverso incontri ravvicinati soprattutto in ambiente universitario. Durante l‘incontro ogni membro aggiorna il gruppo sulle modifiche apportate al codice, eventuali problemi che ha rilevato, accorgimenti e un ipotetica suddivisione dei compiti che devono essere assegnati agli altri membri nel caso siano necessari per lo sviluppo del progetto.
Naturalmente tutto ciò, oltre ad essere tema di incontro, deve essere dettagliatamente descritto nella documentazione o negli issue tramite GitHub.
In caso di domande o dubbi dopo i vari incontri possono essere chiariti anche tramite gruppo Whatsapp creato per una messaggistica rapida, a cui può seguire una chiamata o una discussione per la ricerca di una possibile risoluzione della problematica. In caso di mancata soluzione al problema, e la conseguente impossibilità di chiusura della issue nel caso sia stata creata, si opterà per un nuovo incontro di gruppo.


POTENZIALI RISCHI
Rischi organizzativi e gestionali.
I principali problemi potrebbero riguardare principalmente ritardi nella stesura nei diversi diagrammi e nella consegna tendenzialmente giornaliera degli aggiornamenti e delle modifiche. 
Potrebbe succedere che per alcuni periodi (di esami) sia più complicato lavorare al progetto, rallentando la sua evoluzione e diminuendo la produttività e il numero di commit effettuati.
Da non sottovalutare sono i rischi tecnici legato alle scelte intraprese per il progetto.
Altri problemi potrebbero ad esempio riguardare il database. 
Utilizzando firebase database (nessun membro del gruppo lo aveva mai utilizzato in precedenza) potrebbero sorgere problematiche di gestione dei dati, creazione database, interrogazioni ecc ecc. 
Importante è anche capire come gestire la chiave privata del service account e tutti i rischi di sicurezza ad essa legata.
Questi fanno parte dei rischi denominato rischi di sicurezza.
Infatti è altamente sconsigliato inserire la chiave direttamente sulla repository online GitHub (se privata è possibile, ma non una buona norma).
Utilizzando inoltre noSQL i problemi potrebbero essere maggiori e più complicati da risolvere nel tempo.
E’ ancora più necessario in questa parte il contributo di tutto il gruppo.
Nello stesso modo vale per Vaadin.
È necessaria una fase inziale di comprensione su come utilizzarlo, cosa permette di fare e se è effettivamente utile e necessario al nostro progetto.
In generale, per avere maggiore realismo e serietà dell’app ci sono innumerevoli rischi e problemi a cui andremo incontro, vista l’assente conoscenza nell’ambito di tutti i membri del gruppo.
Sono sempre poi possibili complessi bug che fanno parte di ogni progetto ed è necessario risolverli prima della consegna dell’elaborato.
Riferendosi invece alla funzionalità dell’app i problemi riguardano principalmente la concorrenza e il fatto che più utenti possono accedere al database in maniera simultanea. Possono essere quindi diversi i rischi legati alle funzionalità dell’app.
Occorre implementare meccanismi di controllo che garantiscano che due utenti non possano prenotare due colonnine nella stessa fascia oraria.
Questo è fondamentale che venga rispettato.
Sono sempre poi presenti potenziali rischi riguardo numerosi aspetti dell’applicazione ma questi li andremo a gestire passo dopo passo, risolvendo o prendendo altre strade per evitarli.


PERSONALE
Il progetto prevede il coinvolgimento costante di quattro membri, studenti del corso di laurea in Ingegneria Informatica. Tutti possiedono competenze nello sviluppo software in Java e nell’utilizzo dell’ambiente di sviluppo Eclipse.
Non sono previsti costi per il personale, poiché si tratta di un progetto universitario a scopo formativo.
Ciascun componente del gruppo partecipa attivamente alla programmazione, con attività sia di Front End sia di Back End, secondo le proprie competenze. Per una migliore organizzazione del lavoro, i ruoli principali sono così assegnati:
[Davide Mascheretti] — Project Manager e Sviluppatore Back End
Coordina le attività del gruppo, pianifica le scadenze. Implementa la logica applicativa e gestisce l’integrazione tra moduli, servizi  e database contenenti i dati del progetto.
[Francesco Valenari] — Progettista Software e Collaboratore Front End
Definisce l’architettura del sistema, la struttura del codice e i diagrammi UML, garantendo coerenza e qualità del design. Aiuta nella realizzazione dell'interfaccia utente.
[Tommaso Maistrello] — Analista dei requisiti e Sviluppatore Front End
Cura la documentazione e raccoglie i requisiti funzionali e non funzionali. Realizza l’interfaccia utente.
[Claudio Morgera] — Collaboratore Back End e Tester
Aiuta nell‘implementazione della logica applicativa e gestisce l’integrazione tra moduli, servizi e database. Si occupa dei test funzionali e unitari e verifica la correttezza complessiva del software.


L’impegno dei membri sarà costante per tutta la durata del progetto, con maggiore intensità nelle fasi di sviluppo e collaudo.


METODI E TECNICHE
Durante lo sviluppo del progetto saranno adottati metodi e tecniche di ingegneria del software che coprono tutte le fasi del ciclo di vita: analisi dei requisiti, progettazione, implementazione e test.
Ingegneria dei requisiti: i requisiti saranno raccolti tramite incontri di gruppo e discussioni guidate dal Project Manager. Saranno documentati in modo strutturato utilizzando diagrammi dei casi d’uso e specifiche testuali, redatte con strumenti di videoscrittura condivisi (ad esempio Google Docs o simili).


Progettazione: sarà impiegato un approccio object-oriented (OO), con l’ausilio di diagrammi UML per rappresentare classi, sequenze e componenti. La progettazione sarà validata in gruppo prima dell’inizio della codifica.


Implementazione: lo sviluppo sarà realizzato in Java, utilizzando l’ambiente Eclipse IDE. Verrà seguito un processo incrementale e iterativo dove ciascun componente sarà sviluppato, testato e integrato progressivamente per tutta la durata del progetto.


Controllo di versione: il codice sarà gestito tramite Git, con repository su GitHub per il versioneamento, la collaborazione e la revisione del codice. Ogni membro effettuerà commit regolari e aprirà pull request per l’integrazione del proprio lavoro.


Documentazione tecnica: sarà prodotta e mantenuta durante tutto il progetto, comprendendo specifiche dei requisiti, manuali tecnici e relazioni di test. I documenti saranno salvati in formato digitale condiviso e versionati insieme al codice.


Ambiente di prova: i test saranno condotti sui computer personali dotati di Java Development Kit (JDK) e Eclipse, replicando l’ambiente di esecuzione previsto.


Integrazione e test: i componenti saranno testati in ordine crescente di complessità, iniziando dai moduli di base fino alla versione integrata. I test di accettazione verranno eseguiti sotto la supervisione del gruppo, verificando la conformità ai requisiti funzionali e prestazionali.


GARANZIE DI QUALITÀ
La garanzia della qualità del software sarà assicurata attraverso un insieme di procedure e controlli mirati a garantire che il prodotto finale soddisfi i requisiti definiti.
Revisione dei requisiti e del design: ogni documento e modello progettuale sarà rivisto in gruppo per verificarne la completezza e la coerenza con gli obiettivi del progetto.


Controllo della qualità del codice: verranno adottate convenzioni di programmazione condivise (stile, nomenclatura, formattazione) e saranno effettuate revisioni periodiche del codice tramite code review su GitHub.


Test di qualità: saranno realizzati test unitari, di integrazione e di sistema per garantire il corretto funzionamento del software. Gli esiti dei test saranno documentati in appositi report.


Tracciabilità: verrà mantenuta una corrispondenza tra requisiti, componenti implementati e casi di test, per assicurare la copertura completa delle funzionalità.


Gestione delle modifiche: eventuali correzioni o miglioramenti saranno tracciati nel sistema di versionamento Git, garantendo il controllo delle revisioni e la riproducibilità del lavoro.


Verifica finale: prima della consegna, il software sarà sottoposto a un test di accettazione finale per confermare la conformità alle specifiche e la stabilità del sistema.


PACCHETTI DI LAVORO

Il progetto è suddiviso nei seguenti macro-blocchi, che verranno poi scomposti tra i diversi membri del gruppo, in relazione al ruolo a loro assegnato.
1- Inizializzazione progetto e configurazione dipendenze
	o	Creazione repository privata GitHub e struttura progetto Vaadin e Spring Boot con dipendenze nel pom
	o	Configurazione Firebase (Realtime Database) e chiavi di accesso nella classe apposita
	o	Creazione prima classe Application per verificare funzionamento

2- Gestione Utente
	o	Implementazione viste di registrazione e login
	o	Creazione collegamento tra la pagina di login e la pagina principale (poi da implementare)
	o	Implementazione del database nelle pagine, salvataggio utenti e controllo username già esistente
	o	Controlli su campi vuoti, mail non valide, password verificate (inserimento doppio della password per verifica)

3- Mappa e Visualizzazione Colonnine:
	o	Implementazione delle dipendenze necessarie per utilizzare Leaflet
	o	Implementazione della pagina principale con mappa e menu laterale
	o	Caricamento dei marker delle colonnine da Firebase sulla mappa Leaflet, con coordinate
	o	Implementazione barra di ricerca e filtri (tipo, nome, posizione) per ricerca colonnine
	o	Visualizzazione sidebar con dettagli colonnina al click sul marker con al suo interno tasto prenota, recensioni e segnalazioni
	o	Aggiunta barra laterale con pagine dedicate (utente, colonnine…) e tasto logout

4- Sistema di Prenotazione:
	o	Sviluppo interfaccia di selezione data e slot orari con menù a tendina nella sidebar che si visualizza dopo il click sulla colonnina
	o	Implementazione logica backend per la validazione della prenotazione, gestione db con salvataggio delle prenotazioni e controlli
	o	Gestione della concorrenza con controllo nel database di un nodo uguale (colonnina/data/orario)
	o	Implementazione in Firebase Service di tutti i nodi necessari per corretta gestione DB

5- Menu laterale e pagine dedicate
	o	Implementazione delle pagine dedicate (utente, colonnine…) nella barra laterale
	o	Implementazione parte grafica in queste pagine
	o	Implementazione collegamento con Database nella pagina utente per visualizzare tutte le prenotazioni

6- Generazione e Gestione QR Code:
	o	Implementazione dipendenza per la generazione del QR code univoco basato sulla stringa assegnata alla prenotazione 
	o	Salvataggio del QR code nel database, utilizzabile poi nella pagina utente

7- Fase di testing
	o	Testing
	o	Aggiunte/modifiche JS e CSS per interfaccia e parte grafica


8- Modifiche e aggiunte finali
	o	Implementazione di funzionalità extra (notifiche, segnalazioni, recensioni…)
	o	Modifiche grafiche per interfaccia più user-friendly


RISORSE

1-	Risorse umane:
	o	4 membri del team di sviluppo

2-	Software (piattaforme): 
	o	IDE: Eclipse IDE for Java Developers - 2025-09
	o	JDK (Java Development Kit) + inserire versione
	o	GitHub (per repository privato).
	o	Firebase Realtime Database (piano gratuito).
	o	Vaadin + inserire versione
	o	Spring Boot + inserire versione
	o	Leaflet (per la mappa)
	o	ZXing per la generazione di QR code
	o	Nessuna licenza a pagamento necessaria, tutti i piano sono gratuiti e limitati

3-	Software (comunicazione e gestione):
	o	WhatsApp (comunicazione quotidiana).
	o	Google Drive (documentazione iniziale aggiornata)
	o	GitHub Desktop per i commit e le modifiche

4-	Hardware:
	o	PC di ogni membro del team già in suo possesso


BUDGET

Trattandosi di un progetto sviluppato in ambito accademico, il budget monetario è pari a zero.
Costi Software: Nulli. 
Tutte le tecnologie e piattaforme utilizzate (Java, Vaadin, Leaflet, Firebase piano base, GitHub Free) sono gratuite per gli scopi del progetto.

Costi Hardware: Nulli. 
Ogni membro utilizza il proprio PC personale già in suo possesso.

Costi di Personale: 
Nulli. Il lavoro fa parte del percorso formativo universitario. Non sono previsti compensi economici.


CAMBIAMENTI
Nel corso del progetto potranno verificarsi modifiche ai requisiti o all’implementazione di grande impatto sul progetto. È quindi necessario gestire tali cambiamenti in modo ordinato e tracciabile, per evitare incoerenze o perdite di qualità del progetto.
Poiché il progetto segue un approccio iterativo e leggero, i cambiamenti corposi saranno gestiti in maniera agile: ogni nuova iterazione di questo tipo introdurrà miglioramenti del software con una documentazione ben dettagliata.
Tuttavia, per garantire il controllo e la coerenza del lavoro svolto, verranno seguite procedure condivise:
Ogni proposta di modifica (ai requisiti, al design o al codice) dovrà essere discussa e approvata dal gruppo, in particolare dal Project Manager.


Una volta approvata, la modifica sarà documentata brevemente indicando la motivazione, l’impatto stimato e i file coinvolti.


Le modifiche al codice saranno gestite tramite Git, creando un nuovo branch o commit dedicato e una pull request per la revisione e l’integrazione.


Le modifiche che comportano aggiornamenti alla documentazione tecnica (diagrammi, manuali o specifiche) saranno riportate anche nei relativi file, per mantenere la coerenza tra codice e documentazione.


Questo approccio permette di mantenere un controllo chiaro sulle versioni del software e di prevenire modifiche non tracciate che potrebbero compromettere la qualità e aumentare i tempi di sviluppo.


CONSEGNA
Per la consegna del progetto è obbligatorio mettere il tutto in un repository github da condividere preventivamente con il professore (account github: garganti) e con l’esercitatrice (account github: silviabonfanti). Mentre per i tempi di consegna possono variare in base alla decisione del gruppo nel voler svolgere l‘esame di Ingegneria del Software, siccome il project plan deve essere pronto un mese circa prima dell’esame. Il progetto deve essere completato 5 giorni prima della sua presentazione.

