package pharos.groupware.service;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = AuthGroupwareServiceApplication.class)
public class EncryptionTest {

    @Autowired
    StringEncryptor encryptor; // ✅ EncryptionController와 동일한 Bean 주입 방식

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void testPasswordEncoding() {
        String raw = "1234";
        String encoded = passwordEncoder.encode(raw);
        System.out.println("🔐 BCrypt encoded: " + encoded);
        assertThat(passwordEncoder.matches(raw, encoded)).isTrue();
    }


    @Test
    void testEncryptDecrypt_sameAsController() {
        // given
        String plain = "mypassword";

        // when
        String encrypted = encryptor.encrypt(plain);
        String decrypted = encryptor.decrypt(encrypted);
//        System.out.println("decrypted: " + decrypted);
        // then
        System.out.println("🔐 ENC(" + encrypted + ")");
        assertThat(decrypted).isEqualTo(plain);
    }

}
