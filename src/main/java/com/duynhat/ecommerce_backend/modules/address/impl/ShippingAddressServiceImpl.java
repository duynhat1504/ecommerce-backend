package com.duynhat.ecommerce_backend.modules.address.impl;

import com.duynhat.ecommerce_backend.common.core.exception.BadRequestException;
import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.modules.address.ShippingAddressRepository;
import com.duynhat.ecommerce_backend.modules.address.ShippingAddressService;
import com.duynhat.ecommerce_backend.modules.address.dto.request.CreateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.request.UpdateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.response.ShippingAddressResponse;
import com.duynhat.ecommerce_backend.modules.address.entity.ShippingAddress;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingAddressServiceImpl implements ShippingAddressService {

    private final ShippingAddressRepository addressRepository;

    private final UserRepository userRepository;

    @Override
    @Transactional
    public ShippingAddressResponse create(CreateShippingAddressRequest req) {
        User user = getCurrentUserForUpdate();

        boolean hasAddress = addressRepository.existsByUserId(user.getId());

        boolean shouldBeDefault = !hasAddress || Boolean.TRUE.equals(req.getDefaultAddress());

        if (shouldBeDefault && hasAddress) {
            clearCurrentDefault(user.getId());
        }

        ShippingAddress address = new ShippingAddress();

        address.setUser(user);
        address.setRecipientName(normalize(req.getRecipientName()));
        address.setPhoneNumber(normalize(req.getPhoneNumber()));
        address.setProvince(normalize(req.getProvince()));
        address.setDistrict(normalize(req.getDistrict()));
        address.setWard(normalize(req.getWard()));
        address.setAddressLine(normalize(req.getAddressLine()));
        address.setDefaultAddress(shouldBeDefault);

        ShippingAddress saved = addressRepository.save(address);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingAddressResponse> getMyAddresses() {
        User user = getCurrentUser();

        return addressRepository
                .findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingAddressResponse getMyAddressById(
            UUID addressId
    ) {

        User user = getCurrentUser();

        ShippingAddress address = addressRepository
                .findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        return toResponse(address);
    }

    @Override
    @Transactional
    public ShippingAddressResponse update(
            UUID addressId,
            UpdateShippingAddressRequest req
    ) {
        User user = getCurrentUserForUpdate();

        ShippingAddress address = addressRepository
                .findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        address.setRecipientName(normalize(req.getRecipientName()));
        address.setPhoneNumber(normalize(req.getPhoneNumber()));
        address.setProvince(normalize(req.getProvince()));
        address.setDistrict(normalize(req.getDistrict()));
        address.setWard(normalize(req.getWard()));
        address.setAddressLine(normalize(req.getAddressLine()));

        return toResponse(address);
    }

    @Override
    @Transactional
    public ShippingAddressResponse setDefault(UUID addressId) {
        User user = getCurrentUserForUpdate();

        ShippingAddress address = addressRepository
                .findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        if (Boolean.TRUE.equals(address.getDefaultAddress())) {
            return toResponse(address);
        }

        clearCurrentDefault(user.getId());

        address.setDefaultAddress(true);

        ShippingAddress saved = addressRepository.saveAndFlush(address);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID addressId) {
        User user = getCurrentUserForUpdate();

        ShippingAddress address = addressRepository
                .findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));

        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());

        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            addressRepository
                    .findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                    .ifPresent(
                            nextDefault -> {
                                nextDefault.setDefaultAddress(true);
                                addressRepository.saveAndFlush(nextDefault);
                            }
                    );
        }
    }

    private void clearCurrentDefault(UUID userId) {
        addressRepository
                .findByUserIdAndDefaultAddressTrue(userId)
                .ifPresent(
                        currentDefault -> {
                            currentDefault.setDefaultAddress(false);
                            addressRepository.saveAndFlush(currentDefault);
                        }
                );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        validateAuthentication(authentication);

        return userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User getCurrentUserForUpdate() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        validateAuthentication(authentication);

        return userRepository
                .findByEmailIgnoreCaseForUpdate(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateAuthentication(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal()
        )) {
            throw new BadRequestException("User is not authenticated");
        }
    }

    private ShippingAddressResponse toResponse(ShippingAddress address) {
        return ShippingAddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .addressLine(address.getAddressLine())
                .fullAddress(address.toFullAddress())
                .defaultAddress(address.getDefaultAddress())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    private String normalize(String value) {
        return value.trim();
    }
}
