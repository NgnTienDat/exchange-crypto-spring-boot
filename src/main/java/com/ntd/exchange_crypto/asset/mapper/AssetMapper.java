package com.ntd.exchange_crypto.asset.mapper;

import com.ntd.exchange_crypto.asset.dto.response.AssetResponse;
import com.ntd.exchange_crypto.asset.model.Asset;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetResponse toAssetResponse(Asset asset);
}
