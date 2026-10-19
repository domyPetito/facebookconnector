package com.group.social.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group.social.repository.InfoSocialRepository;
import com.group.social.repository.TokenSocialRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Connector implementation for Facebook / Meta Graph API integration.
 * <p>
 * Implements the {@link Channels} interface and registers as a Spring component
 * qualified with the channel key "FACEBOOK" (used by dispatchers/factories).
 * Handles dynamic JSON payload extraction from database records, evaluates active
 * page targets, maps page access tokens, and dispatches photo/text posts to Meta endpoints.
 * </p>
 */
@Component("FACEBOOK")
public class FacebookConnector implements Channels {

    private final InfoSocialRepository infoSocialRepository;
    private final TokenSocialRepository tokenSocialRepository;
    
    // HTTP client used for outbound REST calls to Meta Graph API
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Jackson mapper for parsing JSON trees stored in the database
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor-based dependency injection for data access repositories.
     *
     * @param infoSocialRepository  Repository providing page metadata and active flags.
     * @param tokenSocialRepository Repository providing encrypted/stored page access tokens.
     */
    public FacebookConnector(InfoSocialRepository infoSocialRepository, TokenSocialRepository tokenSocialRepository) {
        this.infoSocialRepository = infoSocialRepository;
        this.tokenSocialRepository = tokenSocialRepository;
    }

    /**
     * Publishes a post to all active Facebook pages associated with the given client ID.
     *
     * @param imageUrl  Optional URL of the image to publish (if present, targets the /photos endpoint).
     * @param caption   Text content or photo caption for the post.
     * @param clienteId Identifier of the tenant/client owning the Facebook configuration.
     */
    @Override
    public void post(String imageUrl, String caption, Long clienteId) {
        try {
            // Retrieve dynamic JSON payloads from repositories filtered by client ID and social platform code
            String infoJson = infoSocialRepository.findDatiCustomByClienteIdAndCodiceSocial(clienteId, "FACEBOOK");
            String tokenJson = tokenSocialRepository.findDatiCustomByClienteIdAndCodiceSocial(clienteId, "FACEBOOK");

            // Guard clause: ensure both page metadata and security token payloads exist
            if (infoJson == null || tokenJson == null) {
                System.err.println("Nessun account Facebook configurato nel database per il cliente: " + clienteId);
                return;
            }

            // Parse specific JSON sub-trees ("pagine" array and "token_pagine" array)
            JsonNode infoNode = objectMapper.readTree(infoJson).get("pagine");
            JsonNode tokenNode = objectMapper.readTree(tokenJson).get("token_pagine");

            // Build a fast lookup map (page_id -> access_token) from the token JSON node
            Map<String, String> tokenMap = new HashMap<>();
            if (tokenNode != null && tokenNode.isArray()) {
                for (JsonNode tNode : tokenNode) {
                    tokenMap.put(tNode.get("page_id").asText(), tNode.get("access_token").asText());
                }
            }

            // Iterate over configured pages and publish only to those flagged as active ("attiva": true)
            if (infoNode != null && infoNode.isArray()) {
                for (JsonNode iNode : infoNode) {
                    if (iNode.get("attiva").asBoolean()) {
                        String pageId = iNode.get("page_id").asText();
                        String pageToken = tokenMap.get(pageId);

                        // Dispatch to Meta only if a matching page access token is present
                        if (pageToken != null) {
                            inviaAMeta(imageUrl, caption, pageId, pageToken);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore interno durante la scomposizione dei JSON Facebook: " + e.getMessage());
        }
    }

    /**
     * Dispatches the post payload directly to Meta Graph API v20.0 via HTTP POST.
     * <p>
     * Automatically branches the target endpoint:
     * <ul>
     *   <li>If an image URL is provided: routes to {@code /{page-id}/photos} with image URL and caption.</li>
     *   <li>If text-only: routes to {@code /{page-id}/feed} with a message body.</li>
     * </ul>
     * </p>
     *
     * @param imageUrl  Publicly accessible image URL, or null/empty for text-only posts.
     * @param caption   Post text message or photo caption.
     * @param pageId    Target Meta Page Identifier.
     * @param pageToken Long-lived Page Access Token providing publishing permissions.
     */
    private void inviaAMeta(String imageUrl, String caption, String pageId, String pageToken) {
        String url;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", pageToken);

        // Branching logic: determine media vs. status update endpoint
        if (imageUrl != null && !imageUrl.isEmpty()) {
            url = String.format("https://graph.facebook.com/v20.0/%s/photos", pageId);
            params.put("url", imageUrl);
            params.put("caption", caption);
        } else {
            url = String.format("https://graph.facebook.com/v20.0/%s/feed", pageId);
            params.put("message", caption);
        }

        // Prepare HTTP headers and attach parameters payload
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            // Execute outbound POST call to Meta Graph API
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Post pubblicato con successo sulla pagina Meta ID: " + pageId);
            }
        } catch (Exception e) {
            System.err.println("Errore durante la chiamata HTTP a Meta per la pagina " + pageId + ": " + e.getMessage());
        }
    }
}