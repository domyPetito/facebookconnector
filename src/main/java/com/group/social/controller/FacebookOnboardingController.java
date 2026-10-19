package com.group.social.controller;

import com.group.social.dto.FacebookTokenExchangeRequest;
import com.group.social.service.FacebookOnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/social/facebook")
public class FacebookOnboardingController {

    private final FacebookOnboardingService onboardingService;

    public FacebookOnboardingController(FacebookOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/setup")
    public ResponseEntity<String> setupFacebookPages(@RequestBody FacebookTokenExchangeRequest request) {
        if (request.clienteId() == null || request.userAccessToken() == null || request.userAccessToken().isEmpty()) {
            return ResponseEntity.badRequest().body("Errore: Dati utente o token mancanti.");
        }
        
        onboardingService.recuperaESalvaPagineDaMeta(request);
        return ResponseEntity.ok("Fase Onboarding terminata con successo. Tabelle aggiornate.");
    }
}