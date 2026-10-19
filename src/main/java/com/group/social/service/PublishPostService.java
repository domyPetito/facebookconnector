package com.group.social.service;

import com.group.social.connector.Channels;
import com.group.social.repository.ClienteSocialRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for resolving client identities and filtering active social channels.
 * <p>
 * Leverages Spring's automatic map-based collection injection to implement the Strategy Pattern,
 * mapping social platform identifiers (e.g., "FACEBOOK") directly to their concrete {@link Channels} implementations.
 * </p>
 */
@Service
public class PublishPostService {

    /**
     * Map containing all registered {@link Channels} beans available in the Spring context,
     * keyed by their respective bean names (e.g., "FACEBOOK").
     */
    private final Map<String, Channels> tuttiIConnectors;

    /**
     * Data access repository for client-social channel relationships and chat integrations.
     */
    private final ClienteSocialRepository clienteSocialRepository;

    /**
     * Constructor injection for the service collaborators.
     *
     * @param tuttiIConnectors        Spring-managed map collecting all implementations of {@link Channels}.
     * @param clienteSocialRepository Persistence repository handling client social memberships and chat lookups.
     */
    public PublishPostService(Map<String, Channels> tuttiIConnectors, ClienteSocialRepository clienteSocialRepository) {
        this.tuttiIConnectors = tuttiIConnectors;
        this.clienteSocialRepository = clienteSocialRepository;
    }

    /**
     * Resolves the primary client ID associated with an external messaging conversation (e.g., Telegram/WhatsApp chat ID).
     *
     * @param chatId Unique identifier of the incoming messaging session.
     * @return The associated client ID, or {@code null} if the input is blank or unmapped.
     */
    public Long recuperaClienteIdDaChatId(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return null;
        }
        return clienteSocialRepository.findClienteIdByChatId(chatId.trim());
    }

    /**
     * Filters and returns only the active, enabled social channel connectors for a given client.
     * <p>
     * Queries the database for the client's enabled platform codes, matches them against the injected
     * connector implementations, and produces an executable subset ready for dispatching.
     * </p>
     *
     * @param clienteId Database identifier of the client.
     * @return Map of enabled platform codes to their corresponding {@link Channels} connectors.
     */
    public Map<String, Channels> generaCanaliAttivi(Long clienteId) {
        Map<String, Channels> canaliPronti = new HashMap<>();
        if (clienteId == null) {
            return canaliPronti;
        }

        // Retrieve the list of active social codes (e.g., ["FACEBOOK", "INSTAGRAM"]) enabled for this tenant
        List<String> codiciSocialAbilitati = clienteSocialRepository.findCodiciSocialAbilitatiByClienteId(clienteId);

        for (String codice : codiciSocialAbilitati) {
            if (codice != null) {
                String codeUpper = codice.toUpperCase().trim();
                
                // Strategy Pattern resolution: verify if a corresponding connector bean is registered
                if (tuttiIConnectors.containsKey(codeUpper)) {
                    canaliPronti.put(codeUpper, tuttiIConnectors.get(codeUpper));
                }
            }
        }
        return canaliPronti;
    }
}