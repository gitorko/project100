package com.demo.project100.repo;

import java.time.LocalDateTime;

import com.demo.project100.domain.SettledOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettledOrderRepository extends JpaRepository<SettledOrder, Long> {
    Page<SettledOrder> findAllByOrderDateBetween(LocalDateTime startDt, LocalDateTime endDt, Pageable pageable);
}
