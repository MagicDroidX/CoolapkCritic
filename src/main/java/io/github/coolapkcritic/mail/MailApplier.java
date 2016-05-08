package io.github.coolapkcritic.mail;

import io.github.coolapkcritic.Critic;
import io.github.coolapkcritic.Main;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class MailApplier extends Thread {

    public Logger logger = Logger.getLogger("Mail Applier");

    public SSLContext sslContext;

    public final List<MailInfo> mails = Collections.synchronizedList(new ArrayList<MailInfo>());

    public static Pattern patternMail = Pattern.compile("(<input type=\"text\" id=\"fe_text\" class=\"mailtext\" value=\")(.*)(\" />)");

    public long timestamp = 0;

    public MailApplier(SSLContext sslContext) {
        this.sslContext = sslContext;

        this.logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            @Override
            public String format(LogRecord record) {
                return String.format("[%s %s Mail Applier]: %s", dateFormat.format(record.getMillis()), record.getLevel(), record.getMessage()) + "\n";
            }
        });

        this.logger.addHandler(handler);
    }

    public synchronized MailInfo getNext() {
        synchronized (this.mails) {
            if (!this.mails.isEmpty()) {
                MailInfo info = this.mails.get(0);
                this.mails.remove(0);
                return info;
            }
            return null;
        }
    }

    @Override
    public void run() {
        while (true) {
            if (System.currentTimeMillis() - timestamp >= 10000) {
                try {
                    URL url = new URL("https://10minutemail.org/");
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

                    connection.addRequestProperty("User-Agent", Main.USER_AGENT);
                    connection.setSSLSocketFactory(this.sslContext.getSocketFactory());

                    int code = connection.getResponseCode();
                    boolean result = false;

                    if (code == HttpsURLConnection.HTTP_OK) {
                        MailInfo mail = new MailInfo();

                        Map<String, List<String>> headers = connection.getHeaderFields();
                        for (String str : headers.getOrDefault("Set-Cookie", new ArrayList<String>())) {
                            if (str.startsWith("PHPSESSID=")) {
                                mail.sessionId = str.substring(0, str.indexOf(";"));

                                Matcher matcher = patternMail.matcher(Critic.getContent(connection.getInputStream()));

                                if (matcher.find()) {
                                    mail.name = matcher.group(2);
                                    this.logger.info("已申请邮箱： " + mail.name);
                                    result = true;
                                    mail.time = System.currentTimeMillis();
                                    this.mails.add(mail);
                                    break;
                                }
                            }
                        }
                    } else {
                        this.logger.warning("请求失败：" + code);
                    }

                    if (!result) {
                        this.logger.info("邮箱申请失败");
                    }

                    timestamp = System.currentTimeMillis();
                } catch (Throwable e) {
                    this.logger.log(Level.SEVERE, null, e);
                }
            }

            for (MailInfo info : mails) {
                if (System.currentTimeMillis() - info.time >= 10 * 60 * 1000) {
                    mails.remove(info);
                }
            }

            try {
                sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
