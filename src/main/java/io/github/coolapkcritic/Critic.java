package io.github.coolapkcritic;


import io.github.coolapkcritic.mail.MailApplier;
import io.github.coolapkcritic.mail.MailInfo;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class Critic extends Thread {

    public int id;
    public Step step = Step.INIT;

    public String sessionId;
    public String phpSessionId;

    public static long mailTimestamp;
    public static boolean applying = false;

    public Logger logger;

    public Main instance;
    public MailApplier applier;
    public SSLContext sslContext;

    public static Pattern patternMail = Pattern.compile("(<input type=\"text\" id=\"fe_text\" class=\"mailtext\" value=\")(.*)(\" />)");
    public static Pattern patternMailVerify = Pattern.compile("(send.coolapk.com</td><td><a href=\")(.*)(\">酷安)");
    public static Pattern patternRequestHash = Pattern.compile("(<input type=\"hidden\" name=\"requestHash\" value=\")(.*)(\"/>)");
    public static Pattern patternTid = Pattern.compile("(<input type=\"hidden\" name=\"tid\" value=\")(.*)(\"/>)");
    public static Pattern patternLink = Pattern.compile("(<a href=\"https://account.coolapk.com/auth/validate)(.*)(from=email\")");


    public String mail;
    public String requestHash;
    public String username = newRandomString(8, 12);
    public String password = newRandomString(8, 12);
    public String auth;
    public String uid;

    public Critic(Main instance, MailApplier applier, SSLContext sslContext) {
        this.instance = instance;
        this.id = instance.nextId();

        this.logger = Logger.getLogger("Critic#" + id);
        this.logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            @Override
            public String format(LogRecord record) {
                return String.format("[%s %s #%d]: %s", dateFormat.format(record.getMillis()), record.getLevel(), id, record.getMessage()) + "\n";
            }
        });

        if (Main.debug) {
            this.logger.setLevel(Level.ALL);
        }

        this.logger.addHandler(handler);

        this.sslContext = sslContext;
        this.applier = applier;
    }

    @Override
    public void run() {
        while (true) {
            try {
                switch (this.step) {
                    case INIT: {
                        this.logger.info("正在初始化");

                        URL url = new URL("http://coolapk.com/account/register");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                        connection.addRequestProperty("User-Agent", Main.USER_AGENT);

                        int code = connection.getResponseCode();
                        boolean result = false;

                        if (code == HttpURLConnection.HTTP_OK) {
                            Map<String, List<String>> headers = connection.getHeaderFields();
                            for (String str : headers.getOrDefault("Set-Cookie", new ArrayList<String>())) {
                                if (str.startsWith("SESSID=")) {
                                    this.sessionId = str.substring(0, str.indexOf(";"));

                                    Matcher matcher = patternRequestHash.matcher(getContent(connection.getInputStream()));

                                    if (matcher.find()) {
                                        this.requestHash = matcher.group(2);
                                        this.step = Step.MAIL_APPLY;
                                        result = true;
                                        break;
                                    }
                                }
                            }

                        } else {
                            this.logger.warning("请求失败：" + code);
                        }

                        if (!result) {
                            this.logger.info("初始化失败");
                        }
                        break;
                    }

                    case MAIL_APPLY: {

                            MailInfo info = this.applier.getNext();

                            if (info!=null){
                                this.mail = info.name;
                                this.phpSessionId = info.sessionId;

                                this.logger.info("使用邮箱 " + this.mail);
                                this.step = Step.COOLAPK_REGISTER;
                            }
                        break;
                    }
                        /*if ((System.currentTimeMillis() - mailTimestamp) <= 10000 || applying) {
                            break;
                        }

                        applying = true;

                        URL url = new URL("https://10minutemail.org/");
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                        connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                        connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

                        int code = connection.getResponseCode();
                        boolean result = false;

                        if (code == HttpsURLConnection.HTTP_OK) {
                            Map<String, List<String>> headers = connection.getHeaderFields();
                            for (String str : headers.getOrDefault("Set-Cookie", new ArrayList<String>())) {
                                if (str.startsWith("PHPSESSID=")) {
                                    this.phpSessionId = str.substring(0, str.indexOf(";"));

                                    Matcher matcher = patternMail.matcher(getContent(connection.getInputStream()));

                                    if (matcher.find()) {
                                        this.mail = matcher.group(2);
                                        this.logger.info("获取邮箱： " + this.mail);
                                        result = true;
                                        mailTimestamp = System.currentTimeMillis();
                                        this.step = Step.COOLAPK_REGISTER;
                                        break;
                                    }
                                }
                            }
                        } else {
                            this.logger.warning("请求失败：" + code);
                        }

                        if (!result) {
                            this.logger.info("邮箱获取失败");
                        }

                        applying = false;
                        break;*/

                case COOLAPK_REGISTER: {
                    URL url = new URL("http://coolapk.com/do?c=account&m=register&ajaxRequest=1&" + System.currentTimeMillis());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");

                    connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                    connection.addRequestProperty("Cookie", this.sessionId);

                    connection.setDoOutput(true);

                    OutputStream stream = connection.getOutputStream();
                    stream.write(
                            String.format("postSubmit=1&requestHash=%s&openId_type=&openId_auth=&forward=%s&email=%s&username=%s&password=%s&password2=%s&readme=on",
                                    this.requestHash,
                                    URLEncoder.encode("http://coolapk.com", "UTF-8"),
                                    URLEncoder.encode(this.mail, "UTF-8"),
                                    this.username,
                                    this.password,
                                    this.password
                            ).getBytes());
                    stream.flush();

                    int code = connection.getResponseCode();

                    if (code == HttpURLConnection.HTTP_OK) {
                        Map<String, List<String>> headers = connection.getHeaderFields();
                        for (String str : headers.getOrDefault("Set-Cookie", new ArrayList<String>())) {
                            if (str.startsWith("auth=") && !str.startsWith("auth=deleted")) {
                                this.auth = str.substring(0, str.indexOf(";"));
                            }

                            if (str.startsWith("uid=")) {
                                this.uid = str.substring(0, str.indexOf(";"));
                            }
                        }

                        if (this.uid != null) {
                            this.logger.info(String.format("%s 注册成功，等待激活邮件", this.mail));

                            this.step = Step.MAIL_VERIFY;
                            break;
                        }

                    } else {
                        this.logger.warning("请求失败：" + code);
                    }

                    this.logger.info("注册失败");

                    break;
                }

                case MAIL_VERIFY: {
                    sleep(5000);
                    URL url = new URL("https://10minutemail.org/mailbox.ajax.php?_=" + System.currentTimeMillis());
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                    connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                    connection.addRequestProperty("Cookie", this.phpSessionId);
                    connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

                    int code = connection.getResponseCode();

                    if (code == HttpsURLConnection.HTTP_OK) {
                        Matcher matcher = patternMailVerify.matcher(getContent(connection.getInputStream()));

                        if (matcher.find()) {
                            String link = "https://10minutemail.org/" + matcher.group(2);

                            url = new URL(link);
                            connection = (HttpsURLConnection) url.openConnection();

                            connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                            connection.addRequestProperty("Cookie", this.phpSessionId);
                            connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

                            code = connection.getResponseCode();

                            if (code == HttpsURLConnection.HTTP_OK) {
                                matcher = patternLink.matcher(getContent(connection.getInputStream()));

                                if (matcher.find()) {
                                    link = "https://account.coolapk.com/auth/validate" + matcher.group(2) + "from=email";

                                    url = new URL(link);
                                    connection = (HttpsURLConnection) url.openConnection();
                                    connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                                    connection.addRequestProperty("Cookie", this.sessionId);
                                    connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

                                    code = connection.getResponseCode();

                                    if (code == HttpsURLConnection.HTTP_OK) {
                                        this.logger.info(String.format("%s 激活成功，开始批判一番", this.mail));
                                        this.step = Step.RATING;
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        this.logger.warning("请求失败：" + code);
                    }

                    this.logger.info("未获取到邮件，等待5秒后刷新");
                    break;
                }
                case RATING: {
                    for (String name : Main.list) {
                        URL url = new URL("http://coolapk.com/apk/com.baidu." + name);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                        connection.addRequestProperty("Cookie", String.format("%s; %s; username=%s", this.auth, this.uid, this.username));

                        int code = connection.getResponseCode();

                        if (code == HttpsURLConnection.HTTP_OK) {
                            Map<String, List<String>> headers = connection.getHeaderFields();
                            for (String str : headers.getOrDefault("Set-Cookie", new ArrayList<String>())) {
                                if (str.startsWith("SESSID=")) {
                                    this.sessionId = str.substring(0, str.indexOf(";"));
                                }
                            }

                            String content = getContent(connection.getInputStream());
                            String requestHash = null, tid = null;

                            Matcher matcher;

                            matcher = patternRequestHash.matcher(content);
                            if (matcher.find()) {
                                requestHash = matcher.group(2);
                            }

                            matcher = patternTid.matcher(content);
                            if (matcher.find()) {
                                tid = matcher.group(2);
                            }

                            if (requestHash != null && tid != null) {

                                url = new URL("http://coolapk.com/do?c=apk&m=rating&id=" + tid + "&value=1&ajaxRequest=1&" + System.currentTimeMillis());
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");

                                connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                                connection.addRequestProperty("Cookie", String.format("%s; %s; %s; username=%s", this.auth, this.sessionId, this.uid, this.username));
                                connection.addRequestProperty("Referer", "http://coolapk.com/apk/com.baidu." + name);
                                connection.addRequestProperty("X-Requested-With", "XMLHttpRequest");
                                connection.addRequestProperty("Origin", "http://coolapk.com");

                                connection.setDoOutput(true);

                                OutputStream stream = connection.getOutputStream();
                                stream.write(("submit=1&requestHash=" + requestHash).getBytes());
                                stream.flush();

                                code = connection.getResponseCode();

                                if (code == HttpURLConnection.HTTP_OK) {
                                    this.logger.info("com.baidu." + name + " 投票成功");
                                } else {
                                    this.logger.warning("请求失败：" + code);
                                }
                            }
                        } else {
                            this.logger.warning("请求失败：" + code);
                        }
                    }

                    this.step = Step.END;
                }

                case END: {
                    this.logger.info("结束");

                    this.instance.remove(this);
                    this.instance.add(new Critic(this.instance, this.applier, this.sslContext));
                    return;
                }
            }
        }catch(Throwable e){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

}

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
