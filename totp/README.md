# How it Works: TOTP Based MFA

## Introduction

Multi-Factor Authentication has become a requirement for any application that values security. In fact, it has become a regulatory requirement in some industries and is being adopted as a requirement in several others. We often discover misconceptions or downright misunderstandings about how MFA works and think this is a topic worth diving into. This particular article will focus on one of the most common second factors, Time based One Time Password, or TOTP. 

TOTP authentication uses a combination of a secret and the current time to derive a predictable multi-digit value. The secret is shared between the issuer and the user in order to compare generated values to determine if the user in fact posses the required secret. You may have heard this incorrectly referred to as "Google Authenticator". While Google had a major part in popularizing this method, it has nothing to do with how TOTP actually works. Any site may create and issue tokens and any mobile application with a correct implementation of TOTP generation may produce a one time value. In this article we will implement server side TOTP token issuing and discuss its security requirements.   

To read more about TOTP token generation, please take a look at [RFC 6238](https://tools.ietf.org/html/rfc6238).

The example code in this article is written in Java. This task can be accomplished in any programming language that supports the underlying cryptographic functions.

## Establishing a Seed

The foundation for the security of a TOTP token begins with the seed. This value is used in conjunction with the current time to derive the instance of the token. Because time can be calculated it is not suitable as the only value for our token. Choosing a seed is incredibly important and should not be left up to the user. Seeds should be randomly generated using a [Cryptographically Secure Pseudo Random Number Generator](https://en.wikipedia.org/wiki/Cryptographically_secure_pseudorandom_number_generator). You can determine the number of bytes you want to use, and in this example we are using 64. We will use the [SecureRandom](https://docs.oracle.com/javase/8/docs/api/java/security/SecureRandom.html) implementation provided by the Java language.  

```java
static String generateSeed() {
    SecureRandom random = new SecureRandom();
    byte[] randomBytes = new byte[SEED_LENGTH_IN_BYTES];
    random.nextBytes(randomBytes);
    return printHexBinary(randomBytes);
}
```

In order to cosume the token we will return the hex representation of the bytes generated. This allows us to pass the value around a bit easier.

## Establishing a counter

The other side of TOTP token generation relies on the current time. We take the current time represented as a long, which is the number of seconds since epoch. This can be derived using `System.currentTimeMillis() / 1000L`. Next, we take the value and divide it by our period, or the number of seconds the token will be valid before rotating. We will use a value of `30` in our example, which is the recommended setting. Finally, we need to put the value into a byte array. There are a few ways to do this, but the following method is on the conservative side accounting for non 64 bit longs and possible endian differences. 

```java
private static byte[] counterToBytes(long time) {
    long counter = time / PERIOD;
    byte[] 
    buffer = new byte[Long.SIZE / Byte.SIZE];
    for (int i = 7; i >= 0; i--) {
        buffer[i] = (byte)(counter & 0xff);
        counter = counter >> 8;
    }
    return buffer;
}
```

Once we have this value we can execute our hmac operation to produce the long form of our OTP value.

## Generating a Value

Using the seed as a key and the counter as a message, we will derive our long form OTP value. The value returned from our HMac operation will be truncated in order to produce the 6 digit value we will compare against in the end. The following code is an [HMacSHA1](https://en.wikipedia.org/wiki/HMAC) operation using the standard Java encryption libraries. While [RFC 6238](https://tools.ietf.org/html/rfc6238) describes the possible options of HMacSHA256 and HMacSHA512, they are not viable when distributing the secret for use on most mobile authenticator applications.  

```java
private static byte[] hash(final byte[] key, final byte[] message) {
    try {
        Mac hmac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(key, "RAW");
        hmac.init(keySpec);
        return hmac.doFinal(message);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
        log.error(e.getMessage(), e);
        return null;
    }
}
```

TODO: Complete sequence code and description

## Providing the Secret to the User

In order to provide the secret to the user, we need to provide a consistent string in a format that allows the user to generate tokens reliably. There are several ways to do this, including simply providing the secret and issuer to the user directly. The most common method is by providing a QR code that contains the information. For this example we will use the [Zebra Crossing](https://github.com/zxing/zxing) library.  

```java
class QrCode {
    private static final int WIDTH = 350;
    private static final int HEIGHT = 350;

    static void generate(String applicationName, String issuer, String path, String secret) {
        try {
            String qrdata = String.format("otpauth://totp/%s?secret=%s&issuer=%s", applicationName, secret, issuer);
            generateQRCodeImage(qrdata, path);
        } catch (WriterException | IOException e) {
            System.out.println("Could not generate QR Code: " + e.getMessage());
        }
    }

    private static void generateQRCodeImage(String text, String filePath) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, WIDTH, HEIGHT);
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
    }
}
```

Executing this code will save a PNG with the corresponding QR code. In a real world situation, you would render this image directly to the user for import by their application of choice.

## Consuming the Token

In order to test our implementation we will need a program that can accept an `otpauth://` string or a QR Code. This can be done a number of ways. If you want to do this via a mobile device, you can use Google Authenticator or Authy. Both of these programs will scan a QR code. If you want to try locally, 1Password provides a way to import a QR image by adding a label to a login and selecting One-Time Password as the type. You can import the created image using the QR code icon and selecting the path to the generated image.

Once the secret is imported it will start producing values. You can use these as your entry values into the example program.

## Protecting the Seed

It is important to respect the secret for what it is -- a secret. With any secret we must do our part to protect it from misuse. How do we do this? Like any other piece of persisted sensitive information, we encrypt it. Because these secrets are not large in size, we have a number of options at our disposal. The important part is not to manage this step on your own. Take advantage of a system that can encrypt and decrypt for you and just worry about storing the encrypted secret value. There are cloud based tools like [Amazon KMS](https://aws.amazon.com/kms/), [Google Cloud Key Management](https://cloud.google.com/kms/), and [Azure Key Vault](https://azure.microsoft.com/en-us/services/key-vault/) as well as services you can run like [Thycotic Secret Server](https://thycotic.com/products/secret-server/), [CyberArk Conjur](https://www.conjur.org/), and [Hashicorp Vault](https://www.vaultproject.io/). All of these options require some kind of setup, and some are commercial products. 

### Setting up Secret Storage

To keep this example both relevant and free of cost to run, we will use Hashicorp Vault. Vault is an open source project with an optional enterprise offering. It's a wonderful project with capabilities far past this example. There are a number of ways to install Vault, but since it is a single binary, the easiest way is to [download the binary](https://www.vaultproject.io/downloads.html) and run it. Start vault with the development flag:

```sh
λ vault server -dev
``` 

During the boot sequence you will be presented with an unseal key and a root token. Copy the root token into the `VAULT_TOKEN` variable in `Main.java`. The messages will look similar to the following:

```sh
λ vault server -dev
==> Vault server configuration:

             Api Address: http://127.0.0.1:8200
                     Cgo: disabled
         Cluster Address: https://127.0.0.1:8201
              Listener 1: tcp (addr: "127.0.0.1:8200", cluster address: "127.0.0.1:8201", max_request_duration: "1m30s", max_request_size: "33554432", tls: "disabled")
               Log Level: (not set)
                   Mlock: supported: false, enabled: false
                 Storage: inmem
                 Version: Vault v0.11.2
             Version Sha: 2b1a4304374712953ff606c6a925bbe90a4e85dd

WARNING! dev mode is enabled! In this mode, Vault runs entirely in-memory
and starts unsealed with a single unseal key. The root token is already
authenticated to the CLI, so you can immediately begin using Vault.

You may need to set the following environment variable:

    $ set VAULT_ADDR=http://127.0.0.1:8200

The unseal key and root token are displayed below in case you want to
seal/unseal the Vault or re-authenticate.

Unseal Key: mkniY94IlJngQz07gfPZlQnZnvEHMXWQ3/MiFegsfr8=
Root Token: 4uYnD1vVZZcNkbYe03t0cLkh

Development mode should NOT be used in production installations!
```

Once Vault is booted you will need to enable the [Transit Backend](https://www.vaultproject.io/docs/secrets/transit/index.html). This allows us to create an encryption key inside of Vault and seamlessly encrypt and decrypt information.

```sh
λ export VAULT_ADDR=http://127.0.0.1:8200
λ vault secrets enable transit
Success! Enabled the transit secrets engine at: transit/
λ vault write -f transit/keys/how_it_works_totp
Success! Data written to: transit/keys/how_it_works_totp
λ echo "my secret" | base64
Im15IHNlY3JldCIgDQo=
λ vault write transit/encrypt/myapp plaintext=Im15IHNlY3JldCIgDQo=
Key           Value
---           -----
ciphertext    vault:v1:/HeILzBTv+JbxdaYeKLVB9RVH9o/b+Lilrja88VhCuaSSlvUY+IzHp2Uλ vault write transit/encrypt/myapp plaintext=Im15IHNlY3JldCIgDQo=
λ vault write -field=plaintext transit/decrypt/myapp ciphertext=vault:v1:/HeILzBTv+JbxdaYeKLVB9RVH9o/b+Lilrja88VhCuaSSlvUY+IzHp2U | base64 -d
"my secret"
```

We can now encrypt and decrypt our TOTP secrets.

TODO: Write persistence code so this section can be finished.

## Drift

TODO: Write

## Running the Example

Make sure you have read an executed all of the steps above. Once you have properly configured the example code you can execute it. You should have your MFA token generator application open and our test token selected. You can setup and execute the program by running:

```sh
createdb totp
mvn flyway:migrate
mvn compile
mvn exec:java -Dexec.mainClass=com.jemurai.Main
```

You will be prompted to enter your token value. After pressing return the program will echo the value you entered, the expected token value, and if the values match. This is the core logic necessary to confirm a TOTP based MFA authentication sequence. If your token values do not match, make sure to enter your token value with plenty of time to spare on the countdown. Because we have not implemented a solution that accounts for drift, the value must be entered during the same period the server generates the expected value. If this is your first time running the example you will need to import the QR code that was generated before the input prompt. If everything was done correctly you will see output similar to the following:

```sh
MFA Token: 
808973
Entered: 808973 : Generated: 808973 : Match: true
```

At this point you have successfully implemented server side TOTP based MFA and used a client side token generator to validate the implementation.

## Conclusions

TODO: Write