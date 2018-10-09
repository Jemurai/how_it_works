package com.jemurai;

import com.bettercloud.vault.VaultException;
import org.apache.commons.codec.binary.Base32;

import java.sql.SQLException;
import java.util.Scanner;

import static com.jemurai.Totp.generateInstance;
import static com.jemurai.Totp.hexToBytes;

public class Main {
    static String VAULT_TOKEN = "vEJtbJXidkbF02eO4Rk46I9G";

    private static String generateSeed() {
        String seed = Totp.generateSeed();
        Base32 base32 = new Base32();
        String secret = base32.encodeAsString(hexToBytes(seed)).replace("=", "");
        QrCode.generate("hiwtotp", "Jemurai", "./hiwtotp.png", secret);

        return seed;
    }

    private static String getSeed(DatabaseConnection connection, SeedVault vault) throws VaultException {
        String secret = connection.getSecret();

        if (secret == null) {
            System.out.println("Could not find secret, generating new value");
            String seed = generateSeed();
            secret = vault.encryptSeed(seed);
            connection.writeSecret(secret);
            return secret;
        }

        return secret;
    }

    public static void main(String[] args) throws VaultException, SQLException {
        DatabaseConnection connection = new DatabaseConnection();
        SeedVault vault = new SeedVault(VAULT_TOKEN);

        String secret = getSeed(connection, vault);
        String seed = vault.decryptSeed(secret);

        Scanner reader = new Scanner(System.in);
        System.out.println("MFA Token: ");
        String token = reader.next();
        String instance = generateInstance(seed);

        System.out.println("Entered: " + token + " : Generated: " + instance + " : Match: " + instance.equals(token));
    }
}