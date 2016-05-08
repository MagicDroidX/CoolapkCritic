package io.github.coolapkcritic.mail;

import io.github.coolapkcritic.Main;
import io.github.coolapkcritic.Util;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class TenMinuteMailProvider extends MailProvider {

    public static volatile long timestamp;
    public static volatile boolean processing;

    public String sessionId;

    public static Pattern patternMail = Pattern.compile("(<input type=\"text\" id=\"fe_text\" class=\"mailtext\" value=\")(.*)(\" />)");
    public static Pattern patternVerify = Pattern.compile("(send.coolapk.com</td><td><a href=\")(.*)(\">酷安)");
    public static Pattern patternLink = Pattern.compile("(<a href=\"https://account.coolapk.com/auth/validate)(.*)(from=email\")");

    public TenMinuteMailProvider(SSLContext sslContext) {
        super(sslContext);
    }

    @Override
    public boolean apply() throws IOException {
        while ((System.currentTimeMillis() - TenMinuteMailProvider.timestamp) <= 2000 || TenMinuteMailProvider.processing) {
        }
        TenMinuteMailProvider.processing = true;

        URL url = new URL("https://10minutemail.org/");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        connection.addRequestProperty("User-Agent", Main.USER_AGENT);
        connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

        int code = connection.getResponseCode();

        TenMinuteMailProvider.processing = false;
        TenMinuteMailProvider.timestamp = System.currentTimeMillis();

        if (code == HttpsURLConnection.HTTP_OK) {

            Map<String, List<String>> headers = connection.getHeaderFields();
            List<String> list = new ArrayList<String>();
            if (headers.containsKey("Set-Cookie")) {
                list = headers.get("Set-Cookie");
            }
            for (String str : list) {
                if (str.startsWith("PHPSESSID=")) {
                    this.sessionId = str.substring(0, str.indexOf(";"));

                    Matcher matcher = patternMail.matcher(Util.getContent(connection.getInputStream()));

                    if (matcher.find()) {
                        this.address = matcher.group(2);
                        this.time = System.currentTimeMillis();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean verify() throws IOException {
        while ((System.currentTimeMillis() - TenMinuteMailProvider.timestamp) <= 2000 || TenMinuteMailProvider.processing) {
        }
        TenMinuteMailProvider.processing = true;

        URL url = new URL("https://10minutemail.org/mailbox.ajax.php?_=" + System.currentTimeMillis());
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        connection.addRequestProperty("User-Agent", Main.USER_AGENT);
        connection.addRequestProperty("Cookie", this.sessionId);
        connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

        int code = connection.getResponseCode();

        TenMinuteMailProvider.processing = false;
        TenMinuteMailProvider.timestamp = System.currentTimeMillis();

        if (code == HttpsURLConnection.HTTP_OK) {
            Matcher matcher = patternVerify.matcher(Util.getContent(connection.getInputStream()));

            if (matcher.find()) {
                String link = "https://10minutemail.org/" + matcher.group(2);

                url = new URL(link);
                connection = (HttpsURLConnection) url.openConnection();

                connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                connection.addRequestProperty("Cookie", this.sessionId);
                connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

                code = connection.getResponseCode();

                if (code == HttpsURLConnection.HTTP_OK) {
                    matcher = patternLink.matcher(Util.getContent(connection.getInputStream()));

                    if (matcher.find()) {
                        link = "https://account.coolapk.com/auth/validate" + matcher.group(2) + "from=email";

                        url = new URL(link);
                        connection = (HttpsURLConnection) url.openConnection();
                        connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                        connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

                        code = connection.getResponseCode();

                        if (code == HttpsURLConnection.HTTP_OK) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isValid() {
        return this.address != null && System.currentTimeMillis() - this.time <= 7 * 60 * 1000; //去掉收邮件可能需要的3分钟，保证可用性
    }
}
