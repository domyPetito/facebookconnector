package com.group.social.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cliente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ragione_sociale", nullable = false)
    private String ragioneSociale;

    @Column(name = "partita_iva", length = 16)
    private String partitaIva;

    @Column(name = "codice_fiscale", length = 16)
    private String codiceFiscale;

    @Column(nullable = false)
    private String email;

    private String telefono;
    private String indirizzo;
    
    // Invertiti come richiesto!
    private String citta;
    private String cap;
    
    private String nazione;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean attivo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
