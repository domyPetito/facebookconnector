package com.group.social.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tipologia_social")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipologiaSocial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String codice;

    @Column(nullable = false, length = 100)
    private String nome;

    private String descrizione;

    @Column(name = "icona_url", length = 500)
    private String iconaUrl;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}