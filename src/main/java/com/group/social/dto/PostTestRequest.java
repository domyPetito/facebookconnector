package com.group.social.dto;

public record PostTestRequest(
    String chatId,    // ID Telegram reale usato per identificare l'utente nel DB
    String imageUrl,  // URL dell'immagine (opzionale)
    String caption    // Testo del post
) {}