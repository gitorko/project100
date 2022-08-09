package com.demo.project100.repo;

import java.util.List;

import com.demo.project100.domain.SettlementSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementSummaryRepository extends JpaRepository<SettlementSummary, Long> {

    List<SettlementSummary> findByBuyOrderIdOrderBySellOrderId(Long buyOrderId);

    List<SettlementSummary> findBySellOrderIdOrderByBuyOrderId(Long sellOrderId);
}
