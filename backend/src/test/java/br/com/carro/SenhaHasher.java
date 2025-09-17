import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SenhaHasher {
    public static void main(String[] args) {
        // Altere "sua_senha_secreta" para a senha que você deseja usar
        String senhaEmTextoPuro = "123";

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String senhaCriptografada = passwordEncoder.encode(senhaEmTextoPuro);

        System.out.println("Senha para a Migration: " + senhaCriptografada);
    }
}