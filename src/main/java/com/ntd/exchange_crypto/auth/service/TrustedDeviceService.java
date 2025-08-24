package com.ntd.exchange_crypto.auth.service;

import com.ntd.exchange_crypto.auth.TrustedDeviceExternalAPI;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrustedDeviceService implements TrustedDeviceExternalAPI {

}
