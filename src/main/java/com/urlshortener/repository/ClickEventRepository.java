package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    List<ClickEvent> findByUrlIdOrderByClickedAtDesc(Long urlId);

    long countByUrlId(Long urlId);

    @Query("""
            SELECT FUNCTION('DATE', c.clickedAt), COUNT(c)
            FROM ClickEvent c
            WHERE c.url.id = :urlId
            GROUP BY FUNCTION('DATE', c.clickedAt)
            ORDER BY FUNCTION('DATE', c.clickedAt)
            """)
    List<Object[]> countClicksByDay(Long urlId);
}