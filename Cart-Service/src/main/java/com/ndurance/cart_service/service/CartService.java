package com.ndurance.cart_service.service;

import com.ndurance.cart_service.shared.dto.CartDTO;
import com.ndurance.cart_service.shared.model.request.CartRequestModel;

import java.util.List;

public interface CartService {
    void saveCart(CartRequestModel requestModel);
    void saveCart(CartRequestModel requestModel, String userId);
    List<CartDTO> getCart(String userId);
    void deleteCart(String id, String userid);
    void checkout(String userId, String token, boolean addressSame);
    void increaseAndDecrease(String cartid, boolean in, boolean de);
}
