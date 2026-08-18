package com.duynhat.ecommerce_backend.integration.address;

import com.duynhat.ecommerce_backend.common.core.exception.ResourceNotFoundException;
import com.duynhat.ecommerce_backend.integration.AbstractIntegrationTest;
import com.duynhat.ecommerce_backend.modules.address.ShippingAddressRepository;
import com.duynhat.ecommerce_backend.modules.address.ShippingAddressService;
import com.duynhat.ecommerce_backend.modules.address.dto.request.CreateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.request.UpdateShippingAddressRequest;
import com.duynhat.ecommerce_backend.modules.address.dto.response.ShippingAddressResponse;
import com.duynhat.ecommerce_backend.modules.address.entity.ShippingAddress;
import com.duynhat.ecommerce_backend.modules.user.UserRepository;
import com.duynhat.ecommerce_backend.modules.user.entity.User;
import com.duynhat.ecommerce_backend.modules.user.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingAddressIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_EMAIL =
            "shipping-address-integration@example.com";

    private static final String OTHER_USER_EMAIL =
            "other-shipping-address-user@example.com";

    @Autowired
    private ShippingAddressService addressService;

    @Autowired
    private ShippingAddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        user = createUser(USER_EMAIL, "Shipping Address User");

        authenticate(USER_EMAIL);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        cleanDatabase();
    }

    @Test
    void create_firstAddress_shouldAutomaticallyBecomeDefault() {
        ShippingAddressResponse address =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        assertThat(address.getId()).isNotNull();

        assertThat(address.getDefaultAddress()).isTrue();

        assertThat(addressRepository.countByUserId(user.getId()))
                .isEqualTo(1);

        ShippingAddress persisted = addressRepository
                .findById(address.getId())
                .orElseThrow();

        assertThat(persisted.getDefaultAddress()).isTrue();
    }

    @Test
    void create_secondAddressWithoutDefault_shouldKeepExistingDefault() {
        ShippingAddressResponse first =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        ShippingAddressResponse second =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Dong Da",
                                "Lang Ha",
                                "456 Lang Ha",
                                false
                        )
                );

        assertThat(first.getDefaultAddress()).isTrue();

        assertThat(second.getDefaultAddress()).isFalse();

        List<ShippingAddress> addresses = addressRepository
                .findAllByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(addresses).hasSize(2);

        assertThat(countDefaultAddresses(user.getId()))
                .isEqualTo(1);

        ShippingAddress defaultAddress = getDefaultAddress(user.getId());

        assertThat(defaultAddress.getId()).isEqualTo(first.getId());
    }

    @Test
    void create_newAddressWithDefaultTrue_shouldReplaceOldDefault() {
        ShippingAddressResponse first =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        ShippingAddressResponse second =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van B",
                                "0912345678",
                                "Ha Noi",
                                "Dong Da",
                                "Lang Ha",
                                "456 Lang Ha",
                                true
                        )
                );

        ShippingAddress firstPersisted = addressRepository
                .findById(first.getId())
                .orElseThrow();

        ShippingAddress secondPersisted = addressRepository
                .findById(second.getId())
                .orElseThrow();

        assertThat(firstPersisted.getDefaultAddress()).isFalse();

        assertThat(secondPersisted.getDefaultAddress()).isTrue();

        assertThat(countDefaultAddresses(user.getId())).isEqualTo(1);

        assertThat(getDefaultAddress(user.getId()).getId()).isEqualTo(second.getId());
    }

    @Test
    void setDefault_shouldMoveDefaultToRequestedAddress() {
        ShippingAddressResponse first =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        ShippingAddressResponse second =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Thanh Xuan",
                                "Nhan Chinh",
                                "789 Nguyen Trai",
                                false
                        )
                );

        assertThat(getDefaultAddress(user.getId()).getId()).isEqualTo(first.getId());

        ShippingAddressResponse result = addressService.setDefault(second.getId());

        assertThat(result.getDefaultAddress()).isTrue();

        assertThat(addressRepository
                .findById(first.getId())
                .orElseThrow()
                .getDefaultAddress()
        ).isFalse();

        assertThat(addressRepository
                .findById(second.getId())
                .orElseThrow()
                .getDefaultAddress()
        ).isTrue();

        assertThat(countDefaultAddresses(user.getId())).isEqualTo(1);
    }

    @Test
    void setDefault_whenAlreadyDefault_shouldBeIdempotent() {
        ShippingAddressResponse address =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        ShippingAddressResponse firstCall = addressService.setDefault(address.getId());

        ShippingAddressResponse secondCall = addressService.setDefault(address.getId());

        assertThat(firstCall.getId()).isEqualTo(address.getId());

        assertThat(secondCall.getId()).isEqualTo(address.getId());

        assertThat(firstCall.getDefaultAddress()).isTrue();

        assertThat(secondCall.getDefaultAddress()).isTrue();

        assertThat(addressRepository.countByUserId(user.getId())).isEqualTo(1);

        assertThat(countDefaultAddresses(user.getId())).isEqualTo(1);
    }

    @Test
    void delete_defaultAddress_shouldPromoteAnotherAddressToDefault() {
        ShippingAddressResponse first =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        ShippingAddressResponse second =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van B",
                                "0912345678",
                                "Ha Noi",
                                "Dong Da",
                                "Lang Ha",
                                "456 Lang Ha",
                                false
                        )
                );

        assertThat(getDefaultAddress(user.getId()).getId()).isEqualTo(first.getId());

        addressService.delete(first.getId());

        assertThat(addressRepository.findById(first.getId())).isEmpty();

        ShippingAddress remaining = addressRepository
                .findById(second.getId())
                .orElseThrow();

        assertThat(remaining.getDefaultAddress()).isTrue();

        assertThat(addressRepository.countByUserId(user.getId())).isEqualTo(1);

        assertThat(countDefaultAddresses(user.getId())).isEqualTo(1);
    }

    @Test
    void delete_nonDefaultAddress_shouldKeepCurrentDefault() {
        ShippingAddressResponse first =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        ShippingAddressResponse second =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van B",
                                "0912345678",
                                "Ha Noi",
                                "Dong Da",
                                "Lang Ha",
                                "456 Lang Ha",
                                false
                        )
                );

        addressService.delete(second.getId());

        assertThat(addressRepository.findById(second.getId())).isEmpty();

        ShippingAddress defaultAddress = getDefaultAddress(user.getId());

        assertThat(defaultAddress.getId()).isEqualTo(first.getId());

        assertThat(defaultAddress.getDefaultAddress()).isTrue();

        assertThat(countDefaultAddresses(user.getId())).isEqualTo(1);
    }

    @Test
    void update_shouldModifyAddressButKeepDefaultStatus() {
        ShippingAddressResponse address =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        UpdateShippingAddressRequest request =
                createUpdateRequest(
                        "  Tran Van B  ",
                        "0987654321",
                        "  Ho Chi Minh  ",
                        "  Quan 1  ",
                        "  Ben Nghe  ",
                        "  100 Le Loi  "
                );

        ShippingAddressResponse updated = addressService.update(address.getId(), request);

        assertThat(updated.getRecipientName()).isEqualTo("Tran Van B");

        assertThat(updated.getPhoneNumber()).isEqualTo("0987654321");

        assertThat(updated.getProvince()).isEqualTo("Ho Chi Minh");

        assertThat(updated.getDistrict()).isEqualTo("Quan 1");

        assertThat(updated.getWard()).isEqualTo("Ben Nghe");

        assertThat(updated.getAddressLine()).isEqualTo("100 Le Loi");

        assertThat(updated.getDefaultAddress()).isTrue();

        ShippingAddress persisted = addressRepository
                .findById(address.getId())
                .orElseThrow();

        assertThat(persisted.getRecipientName()).isEqualTo("Tran Van B");

        assertThat(persisted.getDefaultAddress()).isTrue();
    }

    @Test
    void getMyAddressById_forAnotherUsersAddress_shouldReturnNotFound() {
        ShippingAddressResponse address =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        createUser(OTHER_USER_EMAIL, "Other User");

        authenticate(OTHER_USER_EMAIL);

        assertThatThrownBy(
                () -> addressService.getMyAddressById(address.getId())
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shipping address not found");
    }

    @Test
    void update_forAnotherUsersAddress_shouldReturnNotFound() {
        ShippingAddressResponse address =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        User otherUser = createUser(OTHER_USER_EMAIL, "Other User");

        authenticate(otherUser.getEmail());

        UpdateShippingAddressRequest request =
                createUpdateRequest(
                        "Hacker",
                        "0987654321",
                        "Ha Noi",
                        "Ba Dinh",
                        "Lieu Giai",
                        "Unauthorized address"
                );

        assertThatThrownBy(
                () -> addressService.update(
                        address.getId(),
                        request
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shipping address not found");

        ShippingAddress persisted = addressRepository
                .findById(address.getId())
                .orElseThrow();

        assertThat(persisted.getRecipientName()).isEqualTo("Nguyen Van A");
    }

    @Test
    void delete_forAnotherUsersAddress_shouldReturnNotFound() {
        ShippingAddressResponse address =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        createUser(OTHER_USER_EMAIL, "Other User");

        authenticate(OTHER_USER_EMAIL);

        assertThatThrownBy(
                () -> addressService.delete(
                        address.getId()
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shipping address not found");

        assertThat(addressRepository.findById(address.getId())).isPresent();
    }

    @Test
    void setDefault_forAnotherUsersAddress_shouldReturnNotFound() {
        ShippingAddressResponse address =
                addressService.create(
                        createAddressRequest(
                                "Nguyen Van A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "123 Tran Thai Tong",
                                false
                        )
                );

        createUser(OTHER_USER_EMAIL, "Other User");

        authenticate(OTHER_USER_EMAIL);

        assertThatThrownBy(
                () -> addressService.setDefault(
                        address.getId()
                )
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Shipping address not found");

        assertThat(addressRepository
                .findById(address.getId())
                .orElseThrow()
                .getDefaultAddress()
        ).isTrue();
    }

    @Test
    void setDefault_concurrently_shouldKeepExactlyOneDefaultAddress() throws Exception {
        ShippingAddressResponse first =
                addressService.create(
                        createAddressRequest(
                                "Address A",
                                "0901234567",
                                "Ha Noi",
                                "Cau Giay",
                                "Dich Vong",
                                "Address A",
                                false
                        )
                );

        ShippingAddressResponse second =
                addressService.create(
                        createAddressRequest(
                                "Address B",
                                "0912345678",
                                "Ha Noi",
                                "Dong Da",
                                "Lang Ha",
                                "Address B",
                                false
                        )
                );

        ShippingAddressResponse third =
                addressService.create(
                        createAddressRequest(
                                "Address C",
                                "0923456789",
                                "Ha Noi",
                                "Thanh Xuan",
                                "Nhan Chinh",
                                "Address C",
                                false
                        )
                );

        assertThat(getDefaultAddress(user.getId()).getId()).isEqualTo(first.getId());

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            Future<ShippingAddressResponse> secondFuture =
                    executor.submit(
                            concurrentSetDefaultTask(
                                    second.getId(),
                                    ready,
                                    start
                            )
                    );

            Future<ShippingAddressResponse> thirdFuture =
                    executor.submit(
                            concurrentSetDefaultTask(
                                    third.getId(),
                                    ready,
                                    start
                            )
                    );

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            ShippingAddressResponse secondResult = secondFuture.get(10, TimeUnit.SECONDS);

            ShippingAddressResponse thirdResult = thirdFuture.get(10, TimeUnit.SECONDS);

            assertThat(secondResult).isNotNull();

            assertThat(thirdResult).isNotNull();

            assertThat(
                    countDefaultAddresses(
                            user.getId()
                    )
            ).isEqualTo(1);

            List<ShippingAddress> addresses = addressRepository
                    .findAllByUserIdOrderByCreatedAtDesc(user.getId());

            assertThat(addresses).hasSize(3);

            List<ShippingAddress> defaultAddresses = addresses.stream()
                    .filter(address -> Boolean.TRUE.equals(address.getDefaultAddress()))
                    .toList();

            assertThat(defaultAddresses).hasSize(1);

            UUID finalDefaultId = defaultAddresses.getFirst().getId();

            assertThat(finalDefaultId).isIn(second.getId(), third.getId());

        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<ShippingAddressResponse> concurrentSetDefaultTask(
            UUID addressId,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            authenticate(USER_EMAIL);

            try {
                ready.countDown();

                boolean started = start.await(5, TimeUnit.SECONDS);

                if (!started) {
                    throw new IllegalStateException("Timed out waiting to start setDefault");
                }

                return addressService.setDefault(addressId);

            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private CreateShippingAddressRequest createAddressRequest(
            String recipientName,
            String phoneNumber,
            String province,
            String district,
            String ward,
            String addressLine,
            Boolean defaultAddress
    ) {
        CreateShippingAddressRequest request = new CreateShippingAddressRequest();

        request.setRecipientName(recipientName);
        request.setPhoneNumber(phoneNumber);
        request.setProvince(province);
        request.setDistrict(district);
        request.setWard(ward);
        request.setAddressLine(addressLine);
        request.setDefaultAddress(defaultAddress);

        return request;
    }

    private UpdateShippingAddressRequest createUpdateRequest(
            String recipientName,
            String phoneNumber,
            String province,
            String district,
            String ward,
            String addressLine
    ) {
        UpdateShippingAddressRequest request = new UpdateShippingAddressRequest();

        request.setRecipientName(recipientName);
        request.setPhoneNumber(phoneNumber);
        request.setProvince(province);
        request.setDistrict(district);
        request.setWard(ward);
        request.setAddressLine(addressLine);

        return request;
    }

    private User createUser(String email, String fullName) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .password("test-password")
                .fullName(fullName)
                .role(Role.USER)
                .active(true)
                .build()
        );
    }

    private ShippingAddress getDefaultAddress(UUID userId) {
        return addressRepository
                .findByUserIdAndDefaultAddressTrue(userId)
                .orElseThrow();
    }

    private long countDefaultAddresses(UUID userId) {
        return addressRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(address ->
                        Boolean.TRUE.equals(address.getDefaultAddress())
                )
                .count();
    }

    private void authenticate(String email) {
        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of()
                        )
                );
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    shipping_addresses,
                    users
                CASCADE
                """);
    }
}