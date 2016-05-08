package io.github.coolapkcritic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class Util {

    public static String getContent(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String line;
        StringBuilder builder = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        return builder.toString();
    }

    public static String newRandomString(int minLen, int maxLen) {
        Random random = new Random();
        int length = maxLen - minLen;
        if (length > 0) {
            length = random.nextInt(length + 1);
        }

        length += minLen;

        String string = "";
        for (int i = 0; i < length; i++) {
            char c = (char) (random.nextInt(26) + 65);

            if (random.nextBoolean()) {
                c = Character.toLowerCase(c);
            }

            string += c;
        }

        return string;
    }
}
