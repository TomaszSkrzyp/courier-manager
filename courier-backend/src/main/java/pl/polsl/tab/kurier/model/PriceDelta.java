package pl.polsl.tab.kurier.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_deltas")
@Getter
@Setter
@NoArgsConstructor
public class PriceDelta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "delta_id")
    private Integer deltaId;

    @NotNull
    @Column(name = "height_delta", nullable = false)
    private BigDecimal heightDelta;

    @NotNull
    @Column(name = "length_delta", nullable = false)
    private BigDecimal lengthDelta;

    @NotNull
    @Column(name = "width_delta", nullable = false)
    private BigDecimal widthDelta;

    @NotNull
    @Column(name = "weight_delta", nullable = false)
    private BigDecimal weightDelta;

    @NotNull
    @Column(name = "mode_delta", nullable = false)
    private BigDecimal modeDelta;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
