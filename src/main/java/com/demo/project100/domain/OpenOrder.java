package com.demo.project100.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "open_order")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"settle"})
@ToString(exclude = {"settle"})
public class OpenOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /** Original quantity, retained while quantity tracks the unfilled remainder. */
    @JsonIgnore
    private Integer originalQuantity;

    @NotNull
    private LocalDateTime orderDate;

    @Transient
    private boolean settle;

    @PrePersist
    private void initializeOriginalQuantity() {
        if (originalQuantity == null) {
            originalQuantity = quantity;
        }
    }
}
