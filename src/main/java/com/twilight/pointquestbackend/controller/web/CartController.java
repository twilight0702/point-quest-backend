package com.twilight.pointquestbackend.controller.web;

import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.AddCartItemRequest;
import com.twilight.pointquestbackend.dto.UpdateCartItemRequest;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.CartService;
import com.twilight.pointquestbackend.vo.CartVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartVO> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(cartService.getCart(principal));
    }

    @PostMapping("/items")
    public ApiResponse<CartVO> addItem(@Valid @RequestBody AddCartItemRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        CartVO cart = cartService.addItem(request, principal);
        return ApiResponse.success("cart_updated", cart);
    }

    @PutMapping("/items/{rewardId}")
    public ApiResponse<CartVO> updateItem(@PathVariable Long rewardId,
                                          @Valid @RequestBody UpdateCartItemRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        CartVO cart = cartService.updateItem(rewardId, request.getQuantity(), principal);
        return ApiResponse.success("cart_updated", cart);
    }

    @DeleteMapping("/items/{rewardId}")
    public ApiResponse<Void> removeItem(@PathVariable Long rewardId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        cartService.removeItem(rewardId, principal);
        return ApiResponse.onlySuccess("cart_item_removed");
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal);
        return ApiResponse.onlySuccess("cart_cleared");
    }
}
