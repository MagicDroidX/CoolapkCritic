package io.github.coolapkcritic.mail;

import io.github.coolapkcritic.Main;
import io.github.coolapkcritic.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class ChacuoMailProvider extends MailProvider {

    public static volatile long timestamp;
    public static volatile boolean processing;

    public String sessionId;
    public String mainCK;
    public String cfduid;

    public String name;

    public static Pattern patternMail = Pattern.compile("(<input  id=\"converts\" name=\"converts\" type=\"text\"  value=\")(.*)(\"  valid)");
    public static Pattern patternLink = Pattern.compile("(<a href=\"https://account.coolapk.com/auth/validate)(.*)(from=email\")");

    public ChacuoMailProvider(SSLContext sslContext) {
        super(sslContext);
    }

    @Override
    public boolean apply() throws IOException {
        while ((System.currentTimeMillis() - ChacuoMailProvider.timestamp) <= 2000 || ChacuoMailProvider.processing) {
        }
        ChacuoMailProvider.processing = true;

        URL url = new URL("http://24mail.chacuo.net/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.addRequestProperty("User-Agent", Main.USER_AGENT);

        int code = connection.getResponseCode();

        ChacuoMailProvider.processing = false;
        ChacuoMailProvider.timestamp = System.currentTimeMillis();

        if (code == HttpURLConnection.HTTP_OK) {

            Map<String, List<String>> headers = connection.getHeaderFields();
            List<String> list = new ArrayList<String>();
            if (headers.containsKey("Set-Cookie")) {
                list = headers.get("Set-Cookie");
            }

            for (String str : list) {
                if (str.startsWith("sid=")) {
                    this.sessionId = str;
                }

                if (str.startsWith("mail_ck=")) {
                    this.mainCK = str.substring(0, str.indexOf(";"));
                }

                if (str.startsWith("__cfduid=")) {
                    this.cfduid = str.substring(0, str.indexOf(";"));
                }
            }

            if (this.sessionId == null || this.mainCK == null || this.cfduid == null) {
                return false;
            }

            Matcher matcher = patternMail.matcher(Util.getContent(connection.getInputStream()));

            if (matcher.find()) {
                this.name = matcher.group(2);
                this.address = this.name + "@gcd.moe";
                this.time = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean verify() throws IOException {
        while ((System.currentTimeMillis() - ChacuoMailProvider.timestamp) <= 2000 || ChacuoMailProvider.processing) {
        }
        ChacuoMailProvider.processing = true;

        URL url = new URL("http://24mail.chacuo.net/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");

        connection.addRequestProperty("User-Agent", Main.USER_AGENT);
        connection.addRequestProperty("Cookie", this.cfduid + "; " + this.mainCK + "; " + this.sessionId);
        connection.addRequestProperty("X-Requested-With", "XMLHttpRequest");
        connection.addRequestProperty("Referer", "http://24mail.chacuo.net/");
        connection.addRequestProperty("Origin", "http://24mail.chacuo.net/");

        connection.setDoOutput(true);

        OutputStream stream = connection.getOutputStream();
        stream.write(("data=" + this.name + "&type=refresh&arg=").getBytes());
        stream.flush();

        int code = connection.getResponseCode();

        ChacuoMailProvider.processing = false;
        ChacuoMailProvider.timestamp = System.currentTimeMillis();

        if (code == HttpURLConnection.HTTP_OK) {
            int mid;
            try {
                JSONObject json = new JSONObject(Util.getContent(connection.getInputStream()));
                JSONObject data = json.getJSONArray("data").getJSONObject(0);
                JSONArray list = data.getJSONArray("list");
                JSONObject object = list.getJSONObject(0);
                String from = object.getString("FROM");

                if (from.contains("coolapk")) {
                    mid = object.getInt("MID");
                } else {
                    return false;
                }
            } catch (Throwable e) {
                return false;
            }

            url = new URL("http://24mail.chacuo.net/");
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            connection.addRequestProperty("User-Agent", Main.USER_AGENT);
            connection.addRequestProperty("Cookie", this.mainCK + "; " + this.sessionId);
            connection.addRequestProperty("X-Requested-With", "XMLHttpRequest");

            connection.setDoOutput(true);

            stream = connection.getOutputStream();
            stream.write(("data=" + this.name + "&type=mailinfo&arg=" + URLEncoder.encode("f=" + mid, "UTF-8")).getBytes());
            stream.flush();

            code = connection.getResponseCode();

            if (code == HttpsURLConnection.HTTP_OK) {

                try {
                    JSONObject json = new JSONObject(Util.getContent(connection.getInputStream()));

                    Matcher matcher = patternLink.matcher(
                            json.getJSONArray("data")
                                    .getJSONArray(0)
                                    .getJSONArray(1)
                                    .getJSONObject(0)
                                    .getJSONArray("DATA")
                                    .getString(0)
                    );

                    if (matcher.find()) {
                        String link = "https://account.coolapk.com/auth/validate" + matcher.group(2) + "from=email";

                        url = new URL(link);
                        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                        conn.addRequestProperty("User-Agent", Main.USER_AGENT);
                        conn.setSSLSocketFactory(this.sslContext.getSocketFactory());

                        code = conn.getResponseCode();

                        if (code == HttpsURLConnection.HTTP_OK) {
                            return true;
                        }
                    }

                } catch (Throwable e) {
                    return false;
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
