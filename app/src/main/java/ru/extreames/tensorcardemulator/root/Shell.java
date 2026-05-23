package ru.extreames.tensorcardemulator.root;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Base64;

public class Shell {
    public static boolean hasRoot() {
        try {
            Runtime.getRuntime().exec(new String[] {"su"});
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    public static String[] getNFCProcesses() {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {
                    "su", "-c", "ps -A -o NAME | grep -i 'hardware.nfc'"
            });
            String output = new String(process.getInputStream().readAllBytes());
            return Arrays.stream(output.split("\n"))
                    .filter(line -> !line.contains("grep") && !line.trim().isEmpty())
                    .map(String::trim)
                    .toArray(String[]::new);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean killProcess(String name) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {
                    "su", "-c", "killall " + name + " && sleep 1"
            });
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean fileExists(String filePath) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {
                    "su", "-c", "test -f '" + filePath + "'"
            });
            return process.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String readFile(String filePath) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[] {
                "su", "-c", "cat '" + filePath + "'"
        });
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0)
                    output.append("\n");
                output.append(line);
            }
            if (process.waitFor() != 0)
                throw new IOException("Failed to read file via root");
            return output.toString();
        }
    }

    public static void writeFile(String filePath, String content) throws Exception {
        String base64Content = Base64.getEncoder().encodeToString(content.getBytes());
        String command = String.format(
                "echo '%s' | base64 -d > '%s' && chmod 644 '%s'",
                base64Content, filePath, filePath);
        Process process = Runtime.getRuntime().exec(new String[] {
                "su", "-c", command
        });
        if (process.waitFor() != 0)
            throw new IOException("Failed to write file via root");
    }

    public static void copyFile(String src, String dst) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[] {
                "su", "-c", "cp -f '" + src + "' '" + dst + "'"
        });
        if (process.waitFor() != 0)
            throw new IOException("Failed to copy file");
    }
}