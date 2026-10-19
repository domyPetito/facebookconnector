package com.group.social.repository;

import com.group.social.model.ClienteSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteSocialRepository extends JpaRepository<ClienteSocial, Long> {

    // Query a 3 tabelle: identifica il Cliente partendo dal mittente_esterno (chatId) presente nel messaggio
    @Query(value = "SELECT c.id " +
                   "FROM cliente c " +
                   "JOIN app_messaggistica am ON am.id_cliente = c.id " +
                   "JOIN messaggio m ON m.id_app_messaggistica = am.id " +
                   "WHERE m.mittente_esterno = :chatId " +
                   "LIMIT 1", nativeQuery = true)
    Long findClienteIdByChatId(@Param("chatId") String chatId);

    @Query(value = "SELECT ts.codice " +
                   "FROM cliente_social cs " +
                   "JOIN tipologia_social ts ON cs.id_tipologia_social = ts.id " +
                   "WHERE cs.id_cliente = :clienteId AND cs.abilitato = true", nativeQuery = true)
    List<String> findCodiciSocialAbilitatiByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT cs FROM ClienteSocial cs " +
           "JOIN cs.tipologiaSocial ts " +
           "WHERE cs.cliente.id = :clienteId AND ts.codice = :codiceSocial")
    Optional<ClienteSocial> findByClienteIdAndCodiceSocial(
            @Param("clienteId") Long clienteId, 
            @Param("codiceSocial") String codiceSocial
    );
}