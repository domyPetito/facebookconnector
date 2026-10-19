package com.group.social.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "messaggio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Messaggio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_app_messaggistica", nullable = false)
    private AppMessaggistica appMessaggistica;

    @Column(nullable = false, length = 10)
    private String direzione; // "in" o "out"

    @Column(name = "mittente_esterno")
    private String mittenteEsterno;

    @Column(name = "message_id_esterno")
    private String messageIdEsterno;

    @Column(columnDefinition = "TEXT")
    private String testo;

    @Column(name = "image_url") // Nuova colonna aggiunta per l'immagine
    private String imageUrl;

    @Column(name = "dati_custom", columnDefinition = "jsonb")
    private String datiCustom;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
