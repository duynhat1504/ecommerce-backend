package com.duynhat.ecommerce_backend.modules.address.entity;

import com.duynhat.ecommerce_backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipping_addresses")
@Setter
@Getter
@NoArgsConstructor
public class ShippingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String ward;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(name = "is_default", nullable = false)
    private Boolean defaultAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (defaultAddress == null) {
            defaultAddress = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String toFullAddress() {
        return String.join(
                ", ",
                addressLine,
                ward,
                district,
                province
        );
    }
}
