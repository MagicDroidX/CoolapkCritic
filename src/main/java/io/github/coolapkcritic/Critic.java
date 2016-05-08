package io.github.coolapkcritic;


import io.github.coolapkcritic.mail.ChacuoMailProvider;
import io.github.coolapkcritic.mail.MailApplier;
import io.github.coolapkcritic.mail.MailProvider;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class Critic extends Thread {

    public int id;
    public Step step = Step.MAIL_APPLY;

    public String sessionId;

    public Logger logger;

    public Main instance;
    public MailApplier applier;
    public SSLContext sslContext;

    public static Pattern patternRequestHash = Pattern.compile("(<input type=\"hidden\" name=\"requestHash\" value=\")(.*)(\"/>)");
    public static Pattern patternTid = Pattern.compile("(<input type=\"hidden\" name=\"tid\" value=\")(.*)(\"/>)");

    public MailProvider mail;

    public String requestHash;
    public String username = Util.newRandomString(8, 12);
    public String password = Util.newRandomString(8, 12);
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
                    case MAIL_APPLY: {
                        //MailProvider mail = new TenMinuteMailProvider(this.sslContext);
                        MailProvider mail = new ChacuoMailProvider(this.sslContext);

                        while (!mail.apply()) {
                        }

                        this.mail = mail;
                        this.logger.info("使用邮箱 " + this.mail.address);

                        this.step = Step.COOLAPK_REGISTER;

                        break;
                    }

                    case COOLAPK_REGISTER: {
                        URL url = new URL("http://coolapk.com/account/register");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                        connection.addRequestProperty("User-Agent", Main.USER_AGENT);

                        int code = connection.getResponseCode();
                        boolean result = false;

                        if (code == HttpURLConnection.HTTP_OK) {
                            Map<String, List<String>> headers = connection.getHeaderFields();
                            List<String> list = new ArrayList<String>();
                            if (headers.containsKey("Set-Cookie")) {
                                list = headers.get("Set-Cookie");
                            }
                            for (String str : list) {
                                if (str.startsWith("SESSID=")) {
                                    this.sessionId = str.substring(0, str.indexOf(";"));

                                    Matcher matcher = patternRequestHash.matcher(Util.getContent(connection.getInputStream()));

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
                            break;
                        }

                        url = new URL("http://coolapk.com/do?c=account&m=register&ajaxRequest=1&" + System.currentTimeMillis());
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");

                        connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                        connection.addRequestProperty("Cookie", this.sessionId);

                        connection.setDoOutput(true);

                        OutputStream stream = connection.getOutputStream();
                        stream.write(
                                String.format("postSubmit=1&requestHash=%s&openId_type=&openId_auth=&forward=%s&email=%s&username=%s&password=%s&password2=%s&readme=on",
                                        this.requestHash,
                                        URLEncoder.encode("http://coolapk.com", "UTF-8"),
                                        URLEncoder.encode(this.mail.address, "UTF-8"),
                                        this.username,
                                        this.password,
                                        this.password
                                ).getBytes());
                        stream.flush();

                        code = connection.getResponseCode();

                        if (code == HttpURLConnection.HTTP_OK) {
                            Map<String, List<String>> headers = connection.getHeaderFields();
                            List<String> list = new ArrayList<String>();
                            if (headers.containsKey("Set-Cookie")) {
                                list = headers.get("Set-Cookie");
                            }
                            for (String str : list) {
                                if (str.startsWith("auth=") && !str.startsWith("auth=deleted")) {
                                    this.auth = str.substring(0, str.indexOf(";"));
                                }

                                if (str.startsWith("uid=")) {
                                    this.uid = str.substring(0, str.indexOf(";"));
                                }
                            }

                            if (this.uid != null) {
                                this.logger.info(String.format("%s 注册成功，等待激活邮件", this.mail.address));

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
                        while (!this.mail.verify()) {
                        }

                        this.logger.info(String.format("%s 激活成功，开始批判一番", this.mail.address));
                        this.step = Step.RATING;

                        break;
                    }

                    case RATING: {
                        for (String name : Main.list) {
                            URL url = new URL("http://coolapk.com/apk/" + name);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                            connection.addRequestProperty("Cookie", String.format("%s; %s; username=%s", this.auth, this.uid, this.username));

                            int code = connection.getResponseCode();

                            if (code == HttpsURLConnection.HTTP_OK) {
                                Map<String, List<String>> headers = connection.getHeaderFields();
                                List<String> list = new ArrayList<String>();
                                if (headers.containsKey("Set-Cookie")) {
                                    list = headers.get("Set-Cookie");
                                }
                                for (String str : list) {
                                    if (str.startsWith("SESSID=")) {
                                        this.sessionId = str.substring(0, str.indexOf(";"));
                                    }
                                }

                                String content = Util.getContent(connection.getInputStream());
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
                                    connection.addRequestProperty("Referer", "http://coolapk.com/apk/" + name);
                                    connection.addRequestProperty("X-Requested-With", "XMLHttpRequest");
                                    connection.addRequestProperty("Origin", "http://coolapk.com");

                                    connection.setDoOutput(true);

                                    OutputStream stream = connection.getOutputStream();
                                    stream.write(("submit=1&requestHash=" + requestHash).getBytes());
                                    stream.flush();

                                    code = connection.getResponseCode();

                                    if (code == HttpURLConnection.HTTP_OK) {
                                        this.logger.info(name + " 投票成功");
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
            } catch (Throwable e) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            }
        }

    }


}
