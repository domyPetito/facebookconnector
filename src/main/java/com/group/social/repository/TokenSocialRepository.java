package com.group.social.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.group.social.model.ClienteSocial;
import com.group.social.model.InfoSocial;
import com.group.social.model.TokenSocial;

@Repository
public interface TokenSocialRepository extends JpaRepository<TokenSocial, Long> {

    @Query(value = "SELECT tks.dati_custom FROM token_social tks " +
                   "JOIN cliente_social cs ON tks.id_cliente_social = cs.id " +
                   "JOIN tipologia_social ts ON cs.id_tipologia_social = ts.id " +
                   "WHERE cs.id_cliente = :clienteId AND ts.codice = :codiceSocial", nativeQuery = true)
    String findDatiCustomByClienteIdAndCodiceSocial(@Param("clienteId") Long clienteId, @Param("codiceSocial") String codiceSocial);

    // @Modifying
    // @Transactional
    // @Query(value = "UPDATE token_social tks " +
    //                "SET tks.dati_custom = :jsonString " +
    //                "WHERE tks.id_cliente_social = ( " +
    //                "  SELECT cs.id FROM cliente_social cs " +
    //                "  JOIN tipologia_social ts ON cs.id_tipologia_social = ts.id " +
    //                "  WHERE cs.id_cliente = :clienteId AND ts.codice = :codiceSocial " +
    //                ")", nativeQuery = true)
    // void updateDatiCustomByClienteIdAndCodiceSocial(@Param("clienteId") Long clienteId, @Param("codiceSocial") String codiceSocial, @Param("jsonString") String jsonString);

    Optional<TokenSocial> findByClienteSocial(ClienteSocial clienteSocial);
}