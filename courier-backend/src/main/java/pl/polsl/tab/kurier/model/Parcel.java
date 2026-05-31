package pl.polsl.tab.kurier.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parcels")
@Getter
@Setter
@NoArgsConstructor
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parcel_id")
    private Integer parcelId;

    @Column(name = "tracking_number", unique = true, length = 24)
    private String trackingNumber;

    @NotBlank
    @Pattern(regexp = "^(\\+?\\d[\\d\\s]{7,14})$", message = "Invalid phone number format")
    @Column(name = "phoneNumber", nullable = false)
    private String phoneNumber;

    @NotNull
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1")
    @Column(nullable = false)
    private BigDecimal weight;

    @DecimalMin(value = "1.0", message = "Height must be at least 1")
    @Column
    private BigDecimal height;

    @DecimalMin(value = "1.0", message = "Width must be at least 1")
    @Column
    private BigDecimal width;

    @DecimalMin(value = "1.0", message = "Length must be at least 1")
    @Column
    private BigDecimal length;

    @Column
    private String fragility;

    @Column
    private BigDecimal price;

    @NotNull
    @Column(name = "expected_time", nullable = false)
    private LocalDateTime expectedTime;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_mode_id", nullable = false)
    private DeliveryMode deliveryMode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_address", nullable = false)
    private Address destinationAddress;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_address", nullable = false)
    private Address senderAddress;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_region_id", nullable = false)
    private Region nextRegion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_region_id", nullable = false)
    private Region currentRegion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    private Status status;

    @NotNull
    @Column(nullable = false)
    private Boolean verified = false;

    @Column
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void generateTrackingNumber() {
        if (this.trackingNumber == null) {
            StringBuilder sb = new StringBuilder(24);
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < 24; i++) {
                sb.append(random.nextInt(10));
            }
            this.trackingNumber = sb.toString();
        }
    }
}