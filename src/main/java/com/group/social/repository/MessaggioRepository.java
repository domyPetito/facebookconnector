package com.group.social.repository;

import com.group.social.model.Messaggio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MessaggioRepository extends JpaRepository<Messaggio, Long> {

    // Recupera l'ultimo messaggio creato in uscita ("out") filtrando sul mittente_esterno
    @Query(value = "SELECT * FROM messaggio " +
                   "WHERE direzione = 'out' " +
                   "AND mittente_esterno = :chatId " +
                   "ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Messaggio> findLatestByMittenteEsterno(@Param("chatId") String chatId);
}