package com.ntd.exchange_crypto.order.mapper;


import com.ntd.exchange_crypto.order.OrderDTO;
import com.ntd.exchange_crypto.order.dto.request.OrderCreationRequest;
import com.ntd.exchange_crypto.order.dto.response.OrderResponse;
import com.ntd.exchange_crypto.order.model.Order;
import com.ntd.exchange_crypto.user.dto.request.UserCreationRequest;
import com.ntd.exchange_crypto.user.dto.response.UserResponse;
import com.ntd.exchange_crypto.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    Order toOrder(OrderCreationRequest orderCreationRequest);
    OrderResponse toOrderResponse(OrderDTO order);
//
//    //Bo qua cac truong null khi update
////    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(target = "roles", ignore = true)
//    void updateUser(@MappingTarget User user, UserUpdateRequest userUpdateRequest);
//
//
    OrderResponse toOrderResponse(Order order, String pairId);

}
