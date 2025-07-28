@ApplicationModule(allowedDependencies = {
        "common :: response",
//        "user :: model", "user :: enums",
        "auth :: request", "auth :: response", "auth :: exception", "auth"
})
package com.ntd.exchange_crypto.security;

import org.springframework.modulith.ApplicationModule;