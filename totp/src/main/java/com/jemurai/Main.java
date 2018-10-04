package com.jemurai;

import com.bettercloud.vault.VaultException;

import java.util.Scanner;

import static com.jemurai.Totp.generateInstance;

public class Main {
    static String ciphtertext = "vault:v1:QavAgAi7ij6flGazjFw25nXzC7l2aPC23k4H7peIRUmpbP2Vxvz4ddPXc/coW5g2U8W1bye1Kgy480qvlsZ1jNGRSDPT+0JSx8T0mPUey6SziCZ6/AaK7d27SjtURS9V7NSWzioL+cjd64YpAOQVt3AJRSgb1h0sEV+/tw==";
    static String generatedSeed = "06FAD58938EEAD700E67F26A43B6164C66B5465675732F6398CB74B4DCA28FC074D91B87D571AE1583865D94D051102ABCC79DC8159433FCAEC18053A5090839";

    public static void main(String[] args) throws VaultException {
        // QrCode.generate("hiwtotp", "Jemurai", "./hiwtotp.png");

        Scanner reader = new Scanner(System.in);
        System.out.println("MFA Token: ");
        String token = reader.next();

        SeedVault vault = new SeedVault();
        String seed = vault.decryptSeed(ciphtertext);
        String instance = generateInstance(seed);

        System.out.println("Entered: " + token + " : Generated: " + instance + " : Match: " + instance.equals(token));
    }
}