package com.jemurai;

import static com.jemurai.Totp.generateInstance;

public class Main {
    private static void generateTotpInstance() {
        String seed = "06FAD58938EEAD700E67F26A43B6164C66B5465675732F6398CB74B4DCA28FC074D91B87D571AE1583865D94D051102ABCC79DC8159433FCAEC18053A5090839";
        System.out.println(generateInstance(seed));
    }

    public static void main(String[] args) {
        // QrCode.generate("hiwtotp", "Jemurai", "./hiwtotp.png");
        generateTotpInstance();
    }
}
