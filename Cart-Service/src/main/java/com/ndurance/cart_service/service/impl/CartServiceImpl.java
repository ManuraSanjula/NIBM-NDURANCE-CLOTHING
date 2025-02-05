package com.ndurance.cart_service.service.impl;

import com.ndurance.cart_service.entity.CartEntity;
import com.ndurance.cart_service.exceptions.CartNotFoundServiceException;
import com.ndurance.cart_service.feign_client.OrderClient;
import com.ndurance.cart_service.repository.CartRepository;
import com.ndurance.cart_service.service.CartService;
import com.ndurance.cart_service.shared.Utils;
import com.ndurance.cart_service.shared.dto.CartDTO;
import com.ndurance.cart_service.shared.model.request.CartRequestModel;
import com.ndurance.cart_service.shared.model.request.OrderRequestModelC;
import com.ndurance.cart_service.shared.model.response.ErrorMessages;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;
    private final ModelMapper modelMapper = new ModelMapper();
    @Autowired
    private OrderClient orderClient;
    @Autowired
    private Utils utils;

    @Override
    public void saveCart(CartRequestModel requestModel) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        CartEntity alreadyInCart = cartRepository.findByProductId(requestModel.getProductId());
        if(alreadyInCart != null){
            alreadyInCart.setQuantity( alreadyInCart.getQuantity() + 1);
            cartRepository.save(alreadyInCart);
        }else{
            CartEntity cartEntity = new CartEntity();
            cartEntity.setProductId(requestModel.getProductId());
            cartEntity.setPrice(requestModel.getPrice());
            cartEntity.setQuantity(requestModel.getQuantity());
            cartEntity.setName(requestModel.getName());
            cartEntity.setImages(requestModel.getImages());

            cartEntity.setUser(username);
            cartEntity.setCartId(utils.generateAddressId(20));
            cartRepository.save(cartEntity);
        }
    }

    @Override
    public void saveCart(CartRequestModel requestModel, String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        CartEntity alreadyInCart = cartRepository.findByProductIdAndUser(requestModel.getProductId(), userId);
        if(alreadyInCart != null){
            alreadyInCart.setQuantity( alreadyInCart.getQuantity() + 1);
            cartRepository.save(alreadyInCart);
        }else {
            CartEntity cartEntity = new CartEntity();
            cartEntity.setProductId(requestModel.getProductId());
            cartEntity.setPrice(requestModel.getPrice());
            cartEntity.setQuantity(requestModel.getQuantity());
            cartEntity.setName(requestModel.getName());
            cartEntity.setImages(requestModel.getImages());

            cartEntity.setUser(username);
            cartEntity.setCartId(utils.generateAddressId(20));
            cartRepository.save(cartEntity);
        }
    }

    @Override
    public List<CartDTO> getCart(String userId) {
        List<CartDTO> wishListDTOS = new ArrayList<>();
        cartRepository.findByUser(userId).forEach(i->{
            wishListDTOS.add(modelMapper.map(i, CartDTO.class));
        });
        return wishListDTOS;
    }

    @Override
    public void deleteCart(String id, String userid) {

        List<CartEntity> cartEntityList = cartRepository.findByUser(userid);

        if(cartEntityList == null || cartEntityList.isEmpty())
            throw new CartNotFoundServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());

        cartEntityList.forEach(cartEntity -> {
            if(Objects.equals(cartEntity.getCartId(), id)){
                cartRepository.deleteById(cartEntity.getId());
            }
        });
    }

    @Override
    public void checkout(String userId, String token, boolean addressSame) {
        List<CartEntity> cart = cartRepository.findByUser(userId);
        OrderRequestModelC orderRequestModel = new OrderRequestModelC();
        orderRequestModel.setCart(cart);

        orderClient.checkOut(token, userId, orderRequestModel);
    }

    @Override
    public void increaseAndDecrease(String cartid, boolean in, boolean de) {
        CartEntity alreadyInCart = cartRepository.findByCartId(cartid);
        if (in) {
            int qu = alreadyInCart.getQuantity() + 1;
            alreadyInCart.setQuantity(qu);
        } else if (de) {
            int qu = alreadyInCart.getQuantity() - 1;
            alreadyInCart.setQuantity(qu);
        }
        cartRepository.save(alreadyInCart);
    }
}
