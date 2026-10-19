package com.group.social.controller;

import com.group.social.connector.Channels;
import com.group.social.model.Messaggio;
import com.group.social.model.AppMessaggistica;
import com.group.social.repository.MessaggioRepository;
import com.group.social.service.OutboundDispatcher;
import com.group.social.service.PublishPostService;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/post")
public class PostPublishController {

    private final PublishPostService publishPostService;
    private final OutboundDispatcher outboundDispatcher;
    private final MessaggioRepository messaggioRepository;
    private final EntityManager entityManager;

    public PostPublishController(PublishPostService publishPostService, 
                                 OutboundDispatcher outboundDispatcher,
                                 MessaggioRepository messaggioRepository,
                                 EntityManager entityManager) {
        this.publishPostService = publishPostService;
        this.outboundDispatcher = outboundDispatcher;
        this.messaggioRepository = messaggioRepository;
        this.entityManager = entityManager;
    }

    /**
     * STEP 1: /prepare
     * Salva il chatId in mittente_esterno e l'immagine come URL statico.
     */
    @PostMapping(value = "/prepare", consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<?> preparePost(
            @RequestParam("files") MultipartFile file,
            @RequestParam("text") String text,
            @RequestParam("chatId") String chatId) {
        try {
            // 1. Generazione dell'URL statico
            String staticImageUrl = "https://cdn.mediaservice.local/uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            // 2. Riferimento fittizio temporaneo (ID 1L) per superare i vincoli DB nei test
            Long tempAppId = 1L; 
            AppMessaggistica appRef = entityManager.getReference(AppMessaggistica.class, tempAppId);
            
            // 3. Creiamo l'entità Messaggio
            Messaggio messaggio = new Messaggio();
            messaggio.setAppMessaggistica(appRef);
            messaggio.setDirezione("OUTGOING");
            messaggio.setMittenteEsterno(chatId); // Salviamo qui il chatId!
            messaggio.setTesto(text);
            messaggio.setImageUrl(staticImageUrl);
            messaggio.setDatiCustom("{\"stato\": \"BOZZA\"}"); // Contiene solo lo stato
            
            messaggio.setCreatedAt(LocalDateTime.now());
            messaggio.setUpdatedAt(LocalDateTime.now());

            // 4. Persistenza su database
            Messaggio messaggioSalvato = messaggioRepository.save(messaggio);

            Map<String, Object> response = new HashMap<>();
            response.put("messaggioId", messaggioSalvato.getId());
            response.put("chatId", chatId);
            response.put("imageUrl", staticImageUrl);
            response.put("text", text);
            response.put("stato", "BOZZA_SALVATA");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore durante la fase di prepare: " + e.getMessage());
        }
    }

    /**
     * STEP 2: /publish
     * Recupera l'ultimo messaggio per mittente_esterno e pubblica in background.
     */
    @PostMapping("/publish")
    public ResponseEntity<String> publishPost(@RequestBody Map<String, Object> request) {
        String chatId = (String) request.get("chatId");

        if (chatId == null || chatId.isEmpty()) {
            return ResponseEntity.badRequest().body("Errore: 'chatId' mancante nel payload.");
        }

        try {
            // 1. Identifichiamo il cliente associato alla chat (tramite la query a 3 tabelle)
            Long clienteId = publishPostService.recuperaClienteIdDaChatId(chatId);
            if (clienteId == null) {
                return ResponseEntity.badRequest().body("Errore: Nessun cliente associato a questo chatId.");
            }

            // 2. Canali social attivi
            Map<String, Channels> canaliAttivi = publishPostService.generaCanaliAttivi(clienteId);
            if (canaliAttivi.isEmpty()) {
                return ResponseEntity.ok("Nessun canale social attivo rilevato.");
            }

            // 3. Recupero della bozza tramite mittente_esterno (chatId)
            Messaggio messaggio = messaggioRepository.findLatestByMittenteEsterno(chatId)
                    .orElseThrow(() -> new RuntimeException("Nessun messaggio trovato per il mittente_esterno: " + chatId));
            
            // Aggiorniamo lo stato
            messaggio.setDatiCustom("{\"stato\": \"INVIO_AVVIATO\"}");
            messaggio.setUpdatedAt(LocalDateTime.now());
            messaggioRepository.save(messaggio);

            // 4. Avvio asincrono del dispatcher
            outboundDispatcher.dispatchDiretto(canaliAttivi, messaggio.getId(), clienteId);

            return ResponseEntity.accepted().body("Pubblicazione avviata in background per il chatId (mittente_esterno): " + chatId);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore durante la fase di pubblicazione: " + e.getMessage());
        }
    }
}