package com.group.social.service;

import com.group.social.connector.Channels;
import com.group.social.model.Messaggio;
import com.group.social.repository.MessaggioRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OutboundDispatcher {

    private final MessaggioRepository messaggioRepository;

    public OutboundDispatcher(MessaggioRepository messaggioRepository) {
        this.messaggioRepository = messaggioRepository;
    }

    @Async
    public void dispatchDiretto(Map<String, Channels> canali, Long messaggioId, Long clienteId) {
        System.out.println("Dispatcher: Avvio pubblicazione asincrona per il messaggio ID: " + messaggioId);

        Messaggio messaggio = messaggioRepository.findById(messaggioId)
                .orElseThrow(() -> new RuntimeException("Messaggio non trovato per ID: " + messaggioId));

        String imageUrl = messaggio.getImageUrl();
        String caption = messaggio.getTesto();
        
        // Estraiamo il chatId per non perderlo nell'aggiornamento del JSON
        // Nel tuo test, puoi semplicemente riutilizzare il chatId noto o impostarlo
        String chatId = messaggio.getMittenteEsterno(); // Supponendo che mittenteEsterno contenga il chatId, altrimenti adattare di conseguenza

        for (Map.Entry<String, Channels> entry : canali.entrySet()) {
            Channels socialConnector = entry.getValue();
            try {
                socialConnector.post(imageUrl, caption, clienteId);
                
                messaggio.setDatiCustom("{\"stato\": \"PUBBLICATO\", \"chat_id\": \"" + chatId + "\"}");
                messaggio.setUpdatedAt(LocalDateTime.now());
                messaggioRepository.save(messaggio);
                
            } catch (Exception e) {
                System.err.println("Errore sul canale " + entry.getKey() + ": " + e.getMessage());
                
                messaggio.setDatiCustom("{\"stato\": \"ERRORE_INVIO\", \"chat_id\": \"" + chatId + "\", \"error\": \"" + e.getMessage() + "\"}");
                messaggio.setUpdatedAt(LocalDateTime.now());
                messaggioRepository.save(messaggio);
            }
        }
    }
}