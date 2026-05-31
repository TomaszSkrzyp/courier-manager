package pl.polsl.tab.kurier.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = true)
    private Region region;

    @NotBlank
    @Column(nullable = false)
    private String street;

    @NotBlank
    @Column(name = "building_number", nullable = false)
    private String buildingNumber;

    @NotBlank
    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
