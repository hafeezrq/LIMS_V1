package com.qdc.lims.ui.backup;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.springframework.stereotype.Service;

/**
 * Stores backup settings for the desktop app.
 *
 * Note: this provides "at-rest" obfuscation/encryption of the configured backup password.
 * For stronger guarantees, integrate an OS keychain/credential store.
 */
@Service
public class BackupSettingsService {

    private static final String PREF_NODE = "com.qdc.lims.ui";
    private static final String KEY_BACKUP_PASSWORD = "backup.password";
    private static final String KEY_LAST_BACKUP_DATE = "backup.lastDate";

    private final Preferences prefs = Preferences.userRoot().node(PREF_NODE);

    public Optional<char[]> getBackupPassword() {
        String enc = prefs.get(KEY_BACKUP_PASSWORD, null);
        if (enc == null || enc.isBlank()) {
            return Optional.empty();
        }
        try {
            String plain = decrypt(enc);
            return Optional.of(plain.toCharArray());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void setBackupPassword(char[] password) {
        if (password == null || password.length == 0) {
            prefs.remove(KEY_BACKUP_PASSWORD);
            return;
        }
        try {
            prefs.put(KEY_BACKUP_PASSWORD, encrypt(new String(password)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to store backup password", e);
        }
    }

    public Optional<LocalDate> getLastBackupDate() {
        String val = prefs.get(KEY_LAST_BACKUP_DATE, null);
        if (val == null || val.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(val));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void setLastBackupDate(LocalDate date) {
        if (date == null) {
            prefs.remove(KEY_LAST_BACKUP_DATE);
            return;
        }
        prefs.put(KEY_LAST_BACKUP_DATE, date.toString());
    }

    private static SecretKey deriveKey() throws Exception {
        // Machine-bound-ish key derivation to avoid storing a raw encryption key.
        // This is not equivalent to OS keychain security.
        String material = System.getProperty("user.name", "") + "|" +
                System.getProperty("os.name", "") + "|" +
                System.getProperty("user.home", "") + "|QDC-LIMS";
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(material.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, 0, 32, "AES");
    }

    private static String encrypt(String plain) throws Exception {
        SecretKey key = deriveKey();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        byte[] out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ct, 0, out, iv.length, ct.length);
        return Base64.getEncoder().encodeToString(out);
    }

    private static String decrypt(String enc) throws Exception {
        SecretKey key = deriveKey();
        byte[] in = Base64.getDecoder().decode(enc);
        if (in.length < 12 + 16) {
            throw new IllegalArgumentException("Invalid encrypted payload");
        }

        byte[] iv = new byte[12];
        byte[] ct = new byte[in.length - 12];
        System.arraycopy(in, 0, iv, 0, 12);
        System.arraycopy(in, 12, ct, 0, ct.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] pt = cipher.doFinal(ct);
        return new String(pt, StandardCharsets.UTF_8);
    }
}
