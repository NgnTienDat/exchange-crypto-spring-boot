package com.ntd.exchange_crypto.asset.controller;

import com.ntd.exchange_crypto.asset.AssetExternalAPI;
import com.ntd.exchange_crypto.asset.AssetInternalAPI;
import com.ntd.exchange_crypto.asset.dto.request.AssetCreationRequest;
import com.ntd.exchange_crypto.asset.dto.response.AssetResponse;
import com.ntd.exchange_crypto.common.dto.response.APIResponse;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssetController {
    AssetInternalAPI assetInternalAPI;
    AssetExternalAPI assetExternalAPI;
    private <T> APIResponse<T> buildResponse(T result, String message, HttpStatus status) {
        return APIResponse.<T>builder()
                .success(true)
                .code(status.value())
                .message(message)
                .result(result)
                .build();
    }

    @PostMapping("/")
    public ResponseEntity<APIResponse<AssetResponse>> createAsset(@RequestBody @Valid AssetCreationRequest assetCreationRequest) {
        System.out.println(assetCreationRequest);
        AssetResponse assetResponse = assetExternalAPI.createNewAsset(assetCreationRequest.getProductId(), assetCreationRequest.getNewBalance());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildResponse(assetResponse, "Asset created", HttpStatus.CREATED));
    }

    @GetMapping("/my-balance/{productId}")
    public ResponseEntity<APIResponse<BigDecimal>> getAvailableBalance(@PathVariable("productId") String productId) {
        return ResponseEntity.ok(buildResponse(
                assetExternalAPI.getAvailableBalance(productId),
                "Balance available",
                HttpStatus.OK));
    }

    @GetMapping("/my")
    public ResponseEntity<APIResponse<List<AssetResponse>>> getMyAssets() {
        return ResponseEntity.ok(buildResponse(
                assetExternalAPI.getMyAsset(),
                "My assets retrieved",
                HttpStatus.OK));
    }


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<AssetResponse>>> getUserAssets(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(buildResponse(
                assetExternalAPI.getUserAsset(userId),
                "User assets retrieved",
                HttpStatus.OK));
    }



}
