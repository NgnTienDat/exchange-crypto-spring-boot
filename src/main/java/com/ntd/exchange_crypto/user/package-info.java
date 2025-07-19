@ApplicationModule(allowedDependencies = {
        "common :: exception", "common :: response",
        "auth :: tfa",
})
package com.ntd.exchange_crypto.user;

import org.springframework.modulith.ApplicationModule;