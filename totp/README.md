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
    byte[] buffer = new byte[Long.SIZE / Byte.SIZE];
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

## Protecting the Seed

## Drift

## Running the Example

## Conclusions