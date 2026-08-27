package com.clinic.dental.security;

import com.clinic.dental.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * 轻量、无状态的 HMAC 签名 Token 服务。
 * Token 格式：Base64URL(payload).Base64URL(HMAC-SHA-256(payload))。
 */
@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] signingKey;
    private final long expirationSeconds;

    public TokenService(@Value("${security.token.secret}") String secret,
                        @Value("${security.token.expiration-seconds:28800}") long expirationSeconds) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("security.token.secret 至少需要 32 个字符");
        }
        this.signingKey = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generate(SysUser user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;
        String payload = String.join("|",
                String.valueOf(user.getId()),
                encode(user.getUsername()),
                encode(user.getRole()),
                user.getDoctorId() == null ? "" : String.valueOf(user.getDoctorId()),
                String.valueOf(issuedAt),
                String.valueOf(expiresAt));
        String encodedPayload = encode(payload);
        return encodedPayload + "." + encode(sign(encodedPayload));
    }

    public Optional<CurrentUser> parse(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.", -1);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                return Optional.empty();
            }
            byte[] expectedSignature = sign(parts[0]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                return Optional.empty();
            }

            String[] values = decode(parts[0]).split("\\|", -1);
            if (values.length != 6 || Instant.now().getEpochSecond() >= Long.parseLong(values[5])) {
                return Optional.empty();
            }
            Long doctorId = values[3].isBlank() ? null : Long.valueOf(values[3]);
            return Optional.of(new CurrentUser(Long.valueOf(values[0]), decode(values[1]), decode(values[2]), doctorId));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private byte[] sign(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成 Token 签名", exception);
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
