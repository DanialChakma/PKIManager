package com.pki.repository;

import com.pki.model.IntermediateCA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IntermediateCARepository extends JpaRepository<IntermediateCA, Long> {
    Optional<IntermediateCA> findByStatus(String status);

    @Query("SELECT i FROM IntermediateCA i " +
            "WHERE i.status IN :statuses " +
            "AND i.expiresAt > :recentThreshold " + // include recently expired
            "AND i.id <> :newId")
    List<IntermediateCA> findRelevantForCrossSign(@Param("statuses") List<String> statuses,
                                                  @Param("recentThreshold") LocalDateTime recentThreshold,
                                                  @Param("newId") Long newId);
}
