package com.pharos.auth.groupware.service.common;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EncryptionController {
    private final StringEncryptor encryptor;

    public EncryptionController(StringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    /**
     * 테스트용 암호화 API
     * 예) GET /encrypt?plain=MySecretPassword
     * returns ENC(암호화된문자열)
     */
    @GetMapping("/encrypt")
    public String encrypt(@RequestParam("plain") String plain) {
        String cipher = encryptor.encrypt(plain);
        return "ENC(" + cipher + ")";
    }
}
