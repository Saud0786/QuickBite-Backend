package com.cg.order.orderservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.order.dto.CartResponse;
import com.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderItem;
import com.quickbite.order.exception.*;
import com.quickbite.order.feign.CartServiceClient;
import com.quickbite.order.repository.OrderRepository;
import com.quickbite.order.service.OrderServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;
    private PlaceOrderRequest placeRequest;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order();
        sampleOrder.setOrderId(1L);
        sampleOrder.setCustomerId("user-100");
        sampleOrder.setRestaurantId(10L);
        sampleOrder.setOrderStatus("PLACED");
        sampleOrder.setOrderDate(LocalDateTime.now());
        sampleOrder.setTotalAmount(500.0);
        sampleOrder.setFinalAmount(500.0);
        sampleOrder.setDiscount(0.0);
        sampleOrder.setModeOfPayment("COD");
        sampleOrder.setDeliveryAddress("123 Main St");
        sampleOrder.setOrderItems(new ArrayList<>());

        placeRequest = new PlaceOrderRequest("user-100", "123 Main St", "COD", null);
    }

    // ─── placeOrder ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("placeOrder: successfully creates order from cart")
    void placeOrder_success() {
        CartResponse cartResp = buildCartResponse("user-100", 10L, List.of(
                new CartResponse.CartItemData(1L, 201L, "Burger", 199.0, 2, null, 398.0),
                new CartResponse.CartItemData(2L, 202L, "Fries",  99.0, 1, null,  99.0)
        ));
        when(cartServiceClient.getCart("user-100")).thenReturn(cartResp);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(1L);
            return o;
        });

        Order result = orderService.placeOrder(placeRequest);

        assertThat(result.getCustomerId()).isEqualTo("user-100");
        assertThat(result.getRestaurantId()).isEqualTo(10L);
        assertThat(result.getOrderStatus()).isEqualTo("PLACED");
        assertThat(result.getTotalAmount()).isEqualTo(497.0); // 199*2 + 99*1
        assertThat(result.getOrderItems()).hasSize(2);
        verify(cartServiceClient).clearCart("user-100");
    }

    @Test
    @DisplayName("placeOrder: throws EmptyCartException when cart is empty")
    void placeOrder_emptyCart() {
        CartResponse empty = new CartResponse("success", "ok",
            new CartResponse.CartData(1L, "user-100", 10L, 0.0, List.of(), 0));
        when(cartServiceClient.getCart("user-100")).thenReturn(empty);

        assertThatThrownBy(() -> orderService.placeOrder(placeRequest))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    @DisplayName("placeOrder: throws IllegalArgumentException for invalid payment mode")
    void placeOrder_invalidPaymentMode() {
        PlaceOrderRequest bad = new PlaceOrderRequest("user-100", "123 St", "BITCOIN", null);
        assertThatThrownBy(() -> orderService.placeOrder(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BITCOIN");
    }

    // ─── getOrderById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderById: returns order when found")
    void getOrderById_found() {
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));
        Order result = orderService.getOrderById(1L);
        assertThat(result.getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getOrderById: throws OrderNotFoundException when not found")
    void getOrderById_notFound() {
        when(orderRepository.findByOrderId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: PLACED → CONFIRMED succeeds")
    void updateStatus_validTransition() {
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.updateStatus(1L, "CONFIRMED");
        assertThat(result.getOrderStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("updateStatus: PLACED → DELIVERED throws InvalidStatusTransitionException")
    void updateStatus_invalidTransition() {
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.updateStatus(1L, "DELIVERED"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("updateStatus: DELIVERED → CONFIRMED throws InvalidStatusTransitionException")
    void updateStatus_terminalState() {
        sampleOrder.setOrderStatus("DELIVERED");
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.updateStatus(1L, "CONFIRMED"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ─── assignDeliveryAgent ──────────────────────────────────────────────────

    @Test
    @DisplayName("assignDeliveryAgent: succeeds when status is CONFIRMED")
    void assignAgent_success() {
        sampleOrder.setOrderStatus("CONFIRMED");
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.assignDeliveryAgent(1L, 55L);
        assertThat(result.getDeliveryAgentId()).isEqualTo(55L);
    }

    @Test
    @DisplayName("assignDeliveryAgent: throws when status is PLACED")
    void assignAgent_wrongStatus() {
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder)); // PLACED

        assertThatThrownBy(() -> orderService.assignDeliveryAgent(1L, 55L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ─── cancelOrder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelOrder: succeeds when status is PLACED")
    void cancelOrder_placed() {
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(1L);
        assertThat(sampleOrder.getOrderStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("cancelOrder: throws OrderCancellationException when status is PICKED_UP")
    void cancelOrder_pickedUp() {
        sampleOrder.setOrderStatus("PICKED_UP");
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(OrderCancellationException.class);
    }

    // ─── reorderFromHistory ───────────────────────────────────────────────────

    @Test
    @DisplayName("reorderFromHistory: creates new order with same items")
    void reorder_success() {
        OrderItem item = new OrderItem(201L, "Burger", 199.0, 2, null);
        sampleOrder.addOrderItem(item);
        sampleOrder.setOrderStatus("DELIVERED");

        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(2L);
            return o;
        });

        Order result = orderService.reorderFromHistory(1L, placeRequest);

        assertThat(result.getOrderId()).isEqualTo(2L);
        assertThat(result.getOrderStatus()).isEqualTo("PLACED");
        assertThat(result.getOrderItems()).hasSize(1);
        assertThat(result.getTotalAmount()).isEqualTo(398.0); // 199 * 2
    }

    @Test
    @DisplayName("reorderFromHistory: throws when customer doesn't own the order")
    void reorder_wrongCustomer() {
        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleOrder));

        PlaceOrderRequest wrongCustomer = new PlaceOrderRequest("user-999", "addr", "COD", null);
        assertThatThrownBy(() -> orderService.reorderFromHistory(1L, wrongCustomer))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── getOrderCount ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOrderCount: returns correct platform-wide count")
    void getOrderCount() {
        when(orderRepository.countAllOrders()).thenReturn(42L);
        assertThat(orderService.getOrderCount()).isEqualTo(42);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private CartResponse buildCartResponse(String customerId, Long restaurantId,
                                            List<CartResponse.CartItemData> items) {
        double total = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        CartResponse.CartData data = new CartResponse.CartData(
                1L, customerId, restaurantId, total, items, items.size());
        return new CartResponse("success", "ok", data);
    }
}
