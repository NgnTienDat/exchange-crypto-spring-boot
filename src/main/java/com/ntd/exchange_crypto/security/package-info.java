@ApplicationModule(allowedDependencies = {
        "common :: response",
        "user :: model", "user :: enums", "user :: service",
        "auth :: request", "auth :: response", "auth :: exception", "auth :: service"
})
package com.ntd.exchange_crypto.security;

import org.springframework.modulith.ApplicationModule;