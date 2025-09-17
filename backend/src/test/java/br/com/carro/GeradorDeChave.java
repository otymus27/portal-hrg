import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;

public class GeradorDeChave {
    public static void main(String[] args) {
        // Generate a 32-byte (256-bit) key
        byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);

        // Encode the key to a URL-safe Base64 string with padding
        String encodedKey = Base64.getUrlEncoder().encodeToString(keyBytes);
        System.out.println(encodedKey);
    }
}
