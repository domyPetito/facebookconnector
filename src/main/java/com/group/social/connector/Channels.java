package com.group.social.connector;

public interface Channels {
    // Ogni social implementerà questo metodo gestendo internamente i propri token e ID
    void post(String imageUrl, String caption, Long clienteId);
}
