package com.demo.project100.repo;

import java.time.LocalDateTime;

import com.demo.project100.domain.OpenOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenOrderRepository extends JpaRepository<OpenOrder, Long> {
    Page<OpenOrder> findAllByOrderDateBetween(LocalDateTime startDt, LocalDateTime endDt, Pageable pageable);
}
