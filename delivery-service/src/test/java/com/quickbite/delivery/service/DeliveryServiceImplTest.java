package com.quickbite.delivery.service;

import com.quickbite.delivery.dto.RegisterAgentRequest;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.feign.OrderServiceClient;
import com.quickbite.delivery.exception.*;
import com.quickbite.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private DeliveryAgent verifiedAgent;
    private DeliveryAgent unverifiedAgent;
    private RegisterAgentRequest registerRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deliveryService, "defaultRadiusKm", 10.0);

        verifiedAgent = new DeliveryAgent();
        verifiedAgent.setAgentId(1L);
        verifiedAgent.setUserId(100L);
        verifiedAgent.setFullName("Ravi Kumar");
        verifiedAgent.setPhone("9876543210");
        verifiedAgent.setVehicleType("BIKE");
        verifiedAgent.setVehicleNumber("MH01AB1234");
        verifiedAgent.setIsAvailable(true);
        verifiedAgent.setIsVerified(true);
        verifiedAgent.setAvgRating(4.5);
        verifiedAgent.setTotalDeliveries(20);
        verifiedAgent.setCurrentOrderId(null);
        verifiedAgent.setCurrentLatitude(18.52);
        verifiedAgent.setCurrentLongitude(73.85);
        verifiedAgent.setCreatedAt(LocalDateTime.now());

        unverifiedAgent = new DeliveryAgent();
        unverifiedAgent.setAgentId(2L);
        unverifiedAgent.setUserId(200L);
        unverifiedAgent.setFullName("Amit Shah");
        unverifiedAgent.setPhone("9123456789");
        unverifiedAgent.setVehicleType("SCOOTER");
        unverifiedAgent.setVehicleNumber("DL02CD5678");
        unverifiedAgent.setIsAvailable(false);
        unverifiedAgent.setIsVerified(false);
        unverifiedAgent.setAvgRating(0.0);
        unverifiedAgent.setTotalDeliveries(0);
        unverifiedAgent.setCreatedAt(LocalDateTime.now());

        registerRequest = new RegisterAgentRequest(
            300L, 4L, "Sanjay Patel", "9000000001", "BIKE", "KA03EF9999");
    }

    // ─── registerAgent ────────────────────────────────────────────────────────

    @Test
    @DisplayName("registerAgent: successfully creates agent with isVerified=false")
    void register_success() {
        when(deliveryRepository.existsByUserId(300L)).thenReturn(false);
        when(deliveryRepository.existsByPhone("9000000001")).thenReturn(false);
        when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(inv -> {
            DeliveryAgent a = inv.getArgument(0);
            a.setAgentId(10L);
            return a;
        });

        DeliveryAgent result = deliveryService.registerAgent(registerRequest);

        assertThat(result.getIsVerified()).isFalse();
        assertThat(result.getIsAvailable()).isFalse();
        assertThat(result.getRestaurantId()).isEqualTo(4L);
        assertThat(result.getVehicleType()).isEqualTo("BIKE");
        assertThat(result.getVehicleNumber()).isEqualTo("KA03EF9999");
    }

    @Test
    @DisplayName("registerAgent: throws AgentAlreadyExistsException for duplicate userId")
    void register_duplicateUserId() {
        when(deliveryRepository.existsByUserId(300L)).thenReturn(true);
        assertThatThrownBy(() -> deliveryService.registerAgent(registerRequest))
                .isInstanceOf(AgentAlreadyExistsException.class)
                .hasMessageContaining("300");
    }

    @Test
    @DisplayName("registerAgent: throws AgentAlreadyExistsException for duplicate phone")
    void register_duplicatePhone() {
        when(deliveryRepository.existsByUserId(300L)).thenReturn(false);
        when(deliveryRepository.existsByPhone("9000000001")).thenReturn(true);
        assertThatThrownBy(() -> deliveryService.registerAgent(registerRequest))
                .isInstanceOf(AgentAlreadyExistsException.class)
                .hasMessageContaining("9000000001");
    }

    @Test
    @DisplayName("registerAgent: throws IllegalArgumentException for invalid vehicleType")
    void register_invalidVehicle() {
        registerRequest.setVehicleType("ROCKET");
        when(deliveryRepository.existsByUserId(300L)).thenReturn(false);
        when(deliveryRepository.existsByPhone("9000000001")).thenReturn(false);
        assertThatThrownBy(() -> deliveryService.registerAgent(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ROCKET");
    }

    // ─── getAgentById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAgentById: returns agent when found")
    void getById_found() {
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        DeliveryAgent result = deliveryService.getAgentById(1L);
        assertThat(result.getAgentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAgentById: throws AgentNotFoundException when not found")
    void getById_notFound() {
        when(deliveryRepository.findByAgentId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> deliveryService.getAgentById(99L))
                .isInstanceOf(AgentNotFoundException.class);
    }

    // ─── updateLocation ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateLocation: updates coordinates for verified agent")
    void updateLocation_success() {
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryAgent result = deliveryService.updateLocation(1L, 18.9, 72.8);

        assertThat(result.getCurrentLatitude()).isEqualTo(18.9);
        assertThat(result.getCurrentLongitude()).isEqualTo(72.8);
        assertThat(result.getLocationUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateLocation: throws AgentNotVerifiedException for unverified agent")
    void updateLocation_notVerified() {
        when(deliveryRepository.findByAgentId(2L)).thenReturn(Optional.of(unverifiedAgent));
        assertThatThrownBy(() -> deliveryService.updateLocation(2L, 18.9, 72.8))
                .isInstanceOf(AgentNotVerifiedException.class);
    }

    @Test
    @DisplayName("updateLocation: throws for invalid coordinates")
    void updateLocation_invalidCoords() {
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        assertThatThrownBy(() -> deliveryService.updateLocation(1L, 200.0, 72.8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");
    }

    // ─── setAvailability ──────────────────────────────────────────────────────

    @Test
    @DisplayName("setAvailability: verified agent can go online")
    void setAvailability_onlineSuccess() {
        verifiedAgent.setIsAvailable(false);
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryAgent result = deliveryService.setAvailability(1L, true);
        assertThat(result.getIsAvailable()).isTrue();
    }

    @Test
    @DisplayName("setAvailability: throws AgentNotVerifiedException when unverified goes online")
    void setAvailability_unverifiedOnline() {
        when(deliveryRepository.findByAgentId(2L)).thenReturn(Optional.of(unverifiedAgent));
        assertThatThrownBy(() -> deliveryService.setAvailability(2L, true))
                .isInstanceOf(AgentNotVerifiedException.class);
    }

    @Test
    @DisplayName("setAvailability: throws AgentUnavailableException when going offline on active delivery")
    void setAvailability_offlineDuringDelivery() {
        verifiedAgent.setCurrentOrderId(55L);
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        assertThatThrownBy(() -> deliveryService.setAvailability(1L, false))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessageContaining("55");
    }

    // ─── verifyAgent ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyAgent: sets isVerified=true")
    void verifyAgent_success() {
        when(deliveryRepository.findByAgentId(2L)).thenReturn(Optional.of(unverifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryAgent result = deliveryService.verifyAgent(2L);
        assertThat(result.getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("verifyAgent: idempotent — already verified agent returns without error")
    void verifyAgent_alreadyVerified() {
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        DeliveryAgent result = deliveryService.verifyAgent(1L);
        assertThat(result.getIsVerified()).isTrue();
        verify(deliveryRepository, never()).save(any());
    }

    // ─── assignOrder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignOrder: assigns order and marks agent busy")
    void assignOrder_success() {
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryAgent result = deliveryService.assignOrder(1L, 77L);

        assertThat(result.getCurrentOrderId()).isEqualTo(77L);
        assertThat(result.getIsAvailable()).isFalse();
    }

    @Test
    @DisplayName("assignOrder: throws AgentNotVerifiedException")
    void assignOrder_notVerified() {
        when(deliveryRepository.findByAgentId(2L)).thenReturn(Optional.of(unverifiedAgent));
        assertThatThrownBy(() -> deliveryService.assignOrder(2L, 77L))
                .isInstanceOf(AgentNotVerifiedException.class);
    }

    @Test
    @DisplayName("assignOrder: throws AgentUnavailableException when offline")
    void assignOrder_offline() {
        verifiedAgent.setIsAvailable(false);
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        assertThatThrownBy(() -> deliveryService.assignOrder(1L, 77L))
                .isInstanceOf(AgentUnavailableException.class);
    }

    @Test
    @DisplayName("assignOrder: throws AgentUnavailableException when already on delivery")
    void assignOrder_alreadyBusy() {
        verifiedAgent.setCurrentOrderId(55L);
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        assertThatThrownBy(() -> deliveryService.assignOrder(1L, 77L))
                .isInstanceOf(AgentUnavailableException.class)
                .hasMessageContaining("55");
    }

    // ─── completeDelivery ─────────────────────────────────────────────────────

    @Test
    @DisplayName("completeDelivery: clears orderId, increments totalDeliveries, sets available=true")
    void completeDelivery_success() {
        verifiedAgent.setCurrentOrderId(77L);
        verifiedAgent.setIsAvailable(false);
        verifiedAgent.setTotalDeliveries(20);

        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(orderServiceClient).markDelivered(eq(77L), anyMap());

        DeliveryAgent result = deliveryService.completeDelivery(1L);

        assertThat(result.getCurrentOrderId()).isNull();
        assertThat(result.getIsAvailable()).isTrue();
        assertThat(result.getTotalDeliveries()).isEqualTo(21);
        verify(orderServiceClient).markDelivered(eq(77L), anyMap());
    }

    @Test
    @DisplayName("completeDelivery: throws when agent has no active delivery")
    void completeDelivery_noActiveDelivery() {
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        assertThatThrownBy(() -> deliveryService.completeDelivery(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no active delivery");
    }

    // ─── updateRating ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateRating: rolling average computed correctly")
    void updateRating_rollingAvg() {
        // avg=4.5, totalDeliveries=20, newRating=5.0
        // newAvg = (4.5 * 20 + 5.0) / 21 = 95.0 / 21 ≈ 4.52
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryAgent result = deliveryService.updateRating(1L, 5.0);
        assertThat(result.getAvgRating()).isEqualTo(4.52);
    }

    @Test
    @DisplayName("updateRating: first rating becomes the average directly")
    void updateRating_firstRating() {
        unverifiedAgent.setTotalDeliveries(0);
        unverifiedAgent.setAvgRating(0.0);
        when(deliveryRepository.findByAgentId(2L)).thenReturn(Optional.of(unverifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryAgent result = deliveryService.updateRating(2L, 4.0);
        assertThat(result.getAvgRating()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("updateRating: throws for out-of-range rating")
    void updateRating_outOfRange() {
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(verifiedAgent));
        assertThatThrownBy(() -> deliveryService.updateRating(1L, 6.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1.0 and 5.0");
    }

    // ─── getActiveDeliveries ──────────────────────────────────────────────────

    @Test
    @DisplayName("getActiveDeliveries: returns agents with non-null currentOrderId")
    void getActiveDeliveries() {
        verifiedAgent.setCurrentOrderId(77L);
        when(deliveryRepository.findByCurrentOrderIdIsNotNull()).thenReturn(List.of(verifiedAgent));

        List<DeliveryAgent> result = deliveryService.getActiveDeliveries();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentOrderId()).isEqualTo(77L);
    }

    // ─── countAvailableAgents ─────────────────────────────────────────────────

    @Test
    @DisplayName("countAvailableAgents: returns correct count")
    void countAvailable() {
        when(deliveryRepository.countByIsAvailable(true)).thenReturn(7L);
        assertThat(deliveryService.countAvailableAgents()).isEqualTo(7L);
    }

    // ─── Haversine helper ─────────────────────────────────────────────────────

    @Test
    @DisplayName("haversineDistanceKm: known distance Mumbai-Pune ≈ 120 km")
    void haversine_mumbaiPune() {
        // Mumbai: 19.0760, 72.8777 — Pune: 18.5204, 73.8567
        double dist = DeliveryServiceImpl.haversineDistanceKm(19.0760, 72.8777, 18.5204, 73.8567);
        assertThat(dist).isBetween(115.0, 135.0);
    }

    @Test
    @DisplayName("haversineDistanceKm: same point returns 0.0")
    void haversine_samePoint() {
        double dist = DeliveryServiceImpl.haversineDistanceKm(18.52, 73.85, 18.52, 73.85);
        assertThat(dist).isEqualTo(0.0);
    }
}
