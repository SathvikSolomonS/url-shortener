 package com.urlshortener.repository;

import com.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Url> findByUserId(Long userId);

    // Atomically increments click_count directly in the database,
    // avoiding a read-then-write race condition under concurrent clicks
    @Modifying
    @Transactional
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :urlId")
    void incrementClickCount(@Param("urlId") Long urlId);

    @Modifying
    @Transactional
    @Query("UPDATE Url u SET u.category = :category WHERE u.id = :urlId")
    void updateCategory(@Param("urlId") Long urlId, @Param("category") String category);
}