package com.group.social.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_messaggistica")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppMessaggistica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_app", nullable = false)
    private TipoAppMessaggistica tipoApp;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "identificativo_esterno")
    private String identificativoEsterno;

    private Boolean abilitato = true;

    // Mappiamo il JSONB come Stringa semplice per adesso (o usando librerie tipo Hypersistence Optimizer in futuro)
    @Column(name = "dati_custom", columnDefinition = "jsonb")
    private String datiCustom;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}