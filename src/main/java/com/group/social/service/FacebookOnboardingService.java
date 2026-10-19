package com.group.social.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.group.social.dto.FacebookTokenExchangeRequest;
import com.group.social.model.ClienteSocial;
import com.group.social.model.InfoSocial;
import com.group.social.model.TokenSocial;
import com.group.social.repository.ClienteSocialRepository;
import com.group.social.repository.InfoSocialRepository;
import com.group.social.repository.TokenSocialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class FacebookOnboardingService {

    private final InfoSocialRepository infoSocialRepository;
    private final TokenSocialRepository tokenSocialRepository;
    private final ClienteSocialRepository clienteSocialRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${facebook.app.id}")
    private String appId;

    @Value("${facebook.app.secret}")
    private String appSecret;

    public FacebookOnboardingService(InfoSocialRepository infoSocialRepository, 
                                     TokenSocialRepository tokenSocialRepository,
                                     ClienteSocialRepository clienteSocialRepository) {
        this.infoSocialRepository = infoSocialRepository;
        this.tokenSocialRepository = tokenSocialRepository;
        this.clienteSocialRepository = clienteSocialRepository;
    }

    @Transactional
    public void recuperaESalvaPagineDaMeta(FacebookTokenExchangeRequest request) {
        try {
            // =========================================================================
            // STEP 1: Scambio del token utente (da short-lived a long-lived di 60 giorni)
            // =========================================================================
            /*
            String urlScambioToken = String.format(
                "https://graph.facebook.com/v20.0/oauth/access_token" +
                "?grant_type=fb_exchange_token" +
                "&client_id=%s" +
                "&client_secret=%s" +
                "&fb_exchange_token=%s",
                this.appId, this.appSecret, request.userAccessToken()
            );

            System.out.println("Avvio scambio token con Meta per il cliente: " + request.clienteId());
            ResponseEntity<String> tokenResponse = restTemplate.getForEntity(urlScambioToken, String.class);
            
            if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                System.err.println("Errore durante lo scambio del token a lungo termine.");
                return;
            }

            JsonNode tokenNode = objectMapper.readTree(tokenResponse.getBody());
            String longLivedUserToken = tokenNode.get("access_token").asText();
            */

            String longLivedUserToken = request.userAccessToken();
            System.out.println("Test rapido: utilizzo diretto del token fornito per il cliente: " + request.clienteId());

            // =========================================================================
            // STEP 2: Richiesta delle pagine Facebook gestite da Meta
            // =========================================================================
            String urlMeAccounts = String.format(
                "https://graph.facebook.com/v20.0/me/accounts?access_token=%s", 
                longLivedUserToken
            );

            ResponseEntity<String> response = restTemplate.getForEntity(urlMeAccounts, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.err.println("Errore di comunicazione con le API di Meta per il recupero delle pagine.");
                return;
            }

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode dataArray = rootNode.get("data");

            if (dataArray == null || !dataArray.isArray() || dataArray.isEmpty()) {
                System.out.println("Nessuna pagina Facebook associata a questo profilo.");
                return;
            }

            // =========================================================================
            // STEP 3: Recupero della relazione ponte ClienteSocial obbligatoria
            // =========================================================================
            ClienteSocial clienteSocial = clienteSocialRepository.findByClienteIdAndCodiceSocial(request.clienteId(), "FACEBOOK")
                    .orElseThrow(() -> new RuntimeException("Associazione cliente_social non trovata alla registrazione per il cliente: " + request.clienteId()));

            // =========================================================================
            // STEP 4: Check-in ed estrazione alberi JSON preesistenti o inizializzazione
            // =========================================================================
            InfoSocial infoSocial = infoSocialRepository.findByClienteSocial(clienteSocial).orElse(null);
            TokenSocial tokenSocial = tokenSocialRepository.findByClienteSocial(clienteSocial).orElse(null);

            ObjectNode infoRoot;
            ArrayNode pagineArray;
            ObjectNode tokenRoot;
            ArrayNode tokenArray;
            Set<String> pageIdSalvati = new HashSet<>();

            if (infoSocial == null || tokenSocial == null) {
                // PRIMO LOGIN: Inizializziamo record ex novo
                System.out.println("Primo login assoluto: creazione nuove strutture per il cliente " + request.clienteId());
                
                if (infoSocial == null) {
                    infoSocial = new InfoSocial();
                    infoSocial.setClienteSocial(clienteSocial);
                }

                if (tokenSocial == null) {
                    tokenSocial = new TokenSocial();
                    tokenSocial.setClienteSocial(clienteSocial);
                }

                infoRoot = objectMapper.createObjectNode();
                pagineArray = infoRoot.putArray("pagine");

                tokenRoot = objectMapper.createObjectNode();
                tokenArray = tokenRoot.putArray("token_pagine");
            } else {
                // LOGIN SUCCESSIVI: Estraiamo i dati attuali per il check-in
                infoRoot = (ObjectNode) objectMapper.readTree(infoSocial.getDatiCustom());
                pagineArray = (ArrayNode) infoRoot.get("pagine");

                tokenRoot = (ObjectNode) objectMapper.readTree(tokenSocial.getDatiCustom());
                tokenArray = (ArrayNode) tokenRoot.get("token_pagine");

                if (pagineArray != null) {
                    for (JsonNode pag : pagineArray) {
                        pageIdSalvati.add(pag.get("page_id").asText());
                    }
                }
            }

            boolean nuovePagineRilevate = false;

            // =========================================================================
            // STEP 5: Ciclo di sdoppiamento e inserimento incrementale in coda
            // =========================================================================
            for (JsonNode metaPage : dataArray) {
                String pageId = metaPage.get("id").asText();
                String pageName = metaPage.get("name").asText();
                String pageAccessToken = metaPage.get("access_token").asText();

                // Se la pagina non è presente nel set delle pagine salvate, la aggiungiamo in coda
                if (!pageIdSalvati.contains(pageId)) {
                    nuovePagineRilevate = true;
                    System.out.println("Check-in: Rilevata nuova pagina da accodare -> " + pageName + " (ID: " + pageId + ")");

                    // Struttura incrementale per info_social (attiva false di default)
                    ObjectNode infoPage = objectMapper.createObjectNode();
                    infoPage.put("page_id", pageId);
                    infoPage.put("page_name", pageName);
                    infoPage.put("attiva", false);
                    pagineArray.add(infoPage);

                    // Struttura incrementale protetta per token_social
                    ObjectNode tokenPage = objectMapper.createObjectNode();
                    tokenPage.put("page_id", pageId);
                    tokenPage.put("access_token", pageAccessToken);
                    tokenArray.add(tokenPage);
                }
            }

            // SE ABBIAMO GIÀ DELLE PAGINE MA NESSUNA DI QUELLe INVIATE DA META È NUOVA, CI FERMIAMO
            if (pageIdSalvati.size() > 0 && !nuovePagineRilevate) {
                System.out.println("Check-in completato: Tutte le pagine sono già registrate per il cliente: " + request.clienteId() + ". Operazione interrotta.");
                return;
            }

            // Conversione degli alberi JSON Object aggiornati in stringhe pronte per il DB
            String infoJsonString = objectMapper.writeValueAsString(infoRoot);
            String tokenJsonString = objectMapper.writeValueAsString(tokenRoot);

            // =========================================================================
            // STEP 6: Persistenza idempotente tramite .save() nativo JPA
            // =========================================================================
            infoSocial.setDatiCustom(infoJsonString);
            infoSocial.setUpdatedAt(LocalDateTime.now());
            infoSocialRepository.save(infoSocial);

            tokenSocial.setDatiCustom(tokenJsonString);
            tokenSocial.setUpdatedAt(LocalDateTime.now());
            tokenSocialRepository.save(tokenSocial);

            System.out.println("Onboarding completato con successo per il cliente: " + request.clienteId());

        } catch (Exception e) {
            System.err.println("Errore nel processo di onboarding di Facebook: " + e.getMessage());
        }
    }
}