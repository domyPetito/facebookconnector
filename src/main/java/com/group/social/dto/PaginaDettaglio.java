package com.group.social.dto;

public record PaginaDettaglio(
    String pageId,
    String pageName,
    boolean attiva,
    String accessToken
) {}
