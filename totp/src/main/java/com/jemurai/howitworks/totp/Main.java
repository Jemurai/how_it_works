package com.jemurai.howitworks.totp;

import com.bettercloud.vault.VaultException;
import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Scanner;
import static com.jemurai.howitworks.totp.Totp.generateInstance;
import static com.jemurai.howitworks.totp.Totp.hexToBytes;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static String VAULT_TOKEN = System.getenv("VAULT_TOKEN");

    private Main() { }

    private static String generateSeed() {
        String seed = Totp.generateSeed();
        Base32 base32 = new Base32();
        String secret = base32.encodeAsString(hexToBytes(seed)).replace("=", "");
        QrCode.generate("hiwtotp", "Jemurai", "./hiwtotp.png", secret);

        return seed;
    }

    private static String getSeed(final DatabaseConnection connection, final SeedVault vault) throws VaultException {
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
        if (VAULT_TOKEN == null) {
            log.error("VAULT_TOKEN is not set. Exiting");
            System.exit(1);
        }

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
