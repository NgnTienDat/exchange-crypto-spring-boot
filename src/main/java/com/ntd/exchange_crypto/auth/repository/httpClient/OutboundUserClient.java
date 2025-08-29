package com.ntd.exchange_crypto.auth.repository.httpClient;


import com.ntd.exchange_crypto.auth.dto.request.ExchangeTokenRequest;
import com.ntd.exchange_crypto.auth.dto.response.ExchangeTokenResponse;
import com.ntd.exchange_crypto.auth.dto.response.OutboundUserResponse;
import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outbound-user", url = "${external.api.identity.user-url}")
public interface OutboundUserClient {
    @GetMapping(value = "/oauth2/v1/userinfo", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    OutboundUserResponse getUserInfo(
            @RequestParam("alt") String alt,
            @RequestParam("access_token") String accessToken
    );
}
