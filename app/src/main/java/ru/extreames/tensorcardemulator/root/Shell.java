package ru.extreames.tensorcardemulator.root;

import java.util.Base64;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class Shell {

    public static boolean hasRoot() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                return process.waitFor() == 0 && output != null && output.contains("uid=0");
            } finally {
                process.destroy();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean fileExists(String filePath) {
        if (filePath == null) return false;
        try {
            String[] cmd = {"su", "-c", "[ -f " + escapeShellArg(filePath) + " ]"};
            Process process = Runtime.getRuntime().exec(cmd);
            try {
                return process.waitFor() == 0;
            } finally {
                process.destroy();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static void copyFile(String source, String destination) throws Exception {
        if (source == null || destination == null) throw new IllegalArgumentException("Paths cannot be null");
        String command = "cp " + escapeShellArg(source) + " " + escapeShellArg(destination);
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Failed to copy file via root command execution");
            }
        } finally {
            process.destroy();
        }
    }

    public static String readFile(String filePath) throws Exception {
        if (filePath == null) throw new IllegalArgumentException("File path cannot be null");
        String command = "cat " + escapeShellArg(filePath);
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) output.append("\n");
                output.append(line);
            }
            if (process.waitFor() != 0) {
                throw new IOException("Failed to read file via root shell execution");
            }
            return output.toString();
        } finally {
            process.destroy();
        }
    }

    public static void writeFile(String filePath, String content) throws Exception {
        if (filePath == null || content == null) throw new IllegalArgumentException("Arguments cannot be null");
        
        String command = "base64 -d > " + escapeShellArg(filePath);
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
        
        try {
            try (OutputStream os = process.getOutputStream()) {
                byte[] base64Data = Base64.getEncoder().encode(content.getBytes("UTF-8"));
                os.write(base64Data);
                os.flush();
            }
            
            if (process.waitFor() != 0) {
                throw new IOException("Failed to write content to target path");
            }
        } finally {
            process.destroy();
        }
    }

    public static boolean restartNFC() {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "svc nfc disable"}).waitFor();
            Thread.sleep(500);
            Runtime.getRuntime().exec(new String[]{"su", "-c", "svc nfc enable"}).waitFor();
            Thread.sleep(1000);
            
            Runtime.getRuntime().exec(new String[]{"su", "-c", "killall android.hardware.nfc-service.st"}).waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String escapeShellArg(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }
}