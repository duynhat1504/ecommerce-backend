package com.duynhat.ecommerce_backend.modules.address;

import com.duynhat.ecommerce_backend.modules.address.entity.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, UUID> {

    List<ShippingAddress> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<ShippingAddress> findByIdAndUserId(UUID id, UUID userId);
    Optional<ShippingAddress> findByUserIdAndDefaultAddressTrue(UUID userId);
    Optional<ShippingAddress> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    boolean existsByUserId(UUID userId);
    long countByUserId(UUID userId);
}
