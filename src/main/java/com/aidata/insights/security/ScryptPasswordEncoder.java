package com.aidata.insights.security;

import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * scrypt 密码编码器，与既有 Node 服务保持完全一致：
 *
 * <pre>
 * Node: `${salt}` 为 16 字节随机数的 hex 字符串
 *       hash = scryptSync(password, salt, 64).toString('hex')
 *       存储格式 scrypt$salt$hash
 * </pre>
 *
 * <p>注意：Node 的 scryptSync 把 salt 当作字符串参与计算（UTF-8 字节），
 * 而不是把 hex 解码成字节，这里必须保持一致，否则老账号无法登录。</p>
 */
public class ScryptPasswordEncoder implements PasswordEncoder {

    private static final String SCHEME = "scrypt";
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH = 64;
    /** Node.js crypto.scryptSync 的默认参数 */
    private static final int N = 16384;
    private static final int R = 8;
    private static final int P = 1;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    @Override
    public String encode(CharSequence rawPassword) {
        byte[] saltBytes = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        String salt = HEX.formatHex(saltBytes);
        return SCHEME + "$" + salt + "$" + hash(rawPassword, salt);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 3 || !SCHEME.equals(parts[0])) {
            return false;
        }
        String salt = parts[1];
        String expected = parts[2];
        if (salt.isEmpty() || expected.isEmpty()) {
            return false;
        }

        String actual = hash(rawPassword, salt);
        // 定长比较，避免时序攻击
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII),
                expected.getBytes(StandardCharsets.US_ASCII));
    }

    private String hash(CharSequence rawPassword, String salt) {
        byte[] passwordBytes = rawPassword.toString().getBytes(StandardCharsets.UTF_8);
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        byte[] derived = SCrypt.generate(passwordBytes, saltBytes, N, R, P, KEY_LENGTH);
        return HEX.formatHex(derived);
    }
}
