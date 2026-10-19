package com.group.social.dto;

public record FacebookTokenExchangeRequest(
    Long clienteId,
    String userAccessToken // Token temporaneo dell'utente ottenuto dal Login di Facebook
) {}