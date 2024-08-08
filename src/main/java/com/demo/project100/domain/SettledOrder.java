package com.demo.project100.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settled_order")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettledOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @NotNull
    private String ticker;

    @NotNull
    @Column(columnDefinition = "DECIMAL(10,2)")
    private Double price;

    @Enumerated(EnumType.STRING)
    private SellType type;

    @NotNull
    private Integer quantity;

    @NotNull
    private LocalDateTime orderDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime executedDate;

}
