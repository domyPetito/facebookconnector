package com.group.social.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_social")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenSocial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente_social", nullable = false, unique = true)
    private ClienteSocial clienteSocial;

    @Column(name = "dati_custom", columnDefinition = "jsonb")
    private String datiCustom;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
