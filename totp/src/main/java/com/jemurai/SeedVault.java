package com.jemurai;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import java.util.HashMap;
import java.util.Map;

class SeedVault {
    private final Vault client;

    SeedVault(String token) throws VaultException {
        VaultConfig config = new VaultConfig()
                .address("http://127.0.0.1:8200")
                .token(token)
                .build();
        this.client = new Vault(config);
    }

    String encryptSeed(String seed) throws VaultException {
        final Map<String, Object> entry = new HashMap<>();
        entry.put("plaintext", seed);
        final LogicalResponse response = client.logical().write("transit/encrypt/myapp", entry);

        return response.getData().get("ciphertext");
    }

    String decryptSeed(String ciphertext) throws VaultException {
        final Map<String, Object> entry = new HashMap<>();
        entry.put("ciphertext", ciphertext);
        final LogicalResponse response = client.logical().write("transit/decrypt/myapp", entry);

        return response.getData().get("plaintext");
    }
}
