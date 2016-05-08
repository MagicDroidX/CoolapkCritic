package io.github.coolapkcritic;

import io.github.coolapkcritic.mail.MailApplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class Main {

    public List<Critic> critics = new ArrayList<Critic>();

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Safari/537.36";

    public static List<String> list;

    public static boolean debug;

    public int id = 0;

    public Main() {
        Logger logger = Logger.getLogger("Critic");
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

            @Override
            public String format(LogRecord record) {
                return String.format("[%s %s]: %s", dateFormat.format(record.getMillis()), record.getLevel(), record.getMessage() + "\n");
            }
        });

        if (debug) {
            logger.setLevel(Level.ALL);
        }

        logger.addHandler(handler);

        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "8888");
        System.setProperty("https.proxyHost", "localhost");
        System.setProperty("https.proxyPort", "8888");

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, null);

            MailApplier applier = new MailApplier(sslContext);
            applier.start();

            logger.info("正在获取需要批判的软件列表");

            List<String> list = new ArrayList<String>();

            for (int i = 1; i <= 3; i++) {
                URL url = new URL("http://coolapk.com/apk/search?q=%E7%99%BE%E5%BA%A6&p=" + i);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.addRequestProperty("User-Agent", USER_AGENT);

                int code = connection.getResponseCode();

                if (code == HttpURLConnection.HTTP_OK) {
                    Pattern pattern = Pattern.compile("(data-touch-url=\"/apk/com.baidu.)(.*)(\">)");
                    Matcher matcher = pattern.matcher(Critic.getContent(connection.getInputStream()));

                    while (matcher.find()) {
                        list.add(matcher.group(2));
                    }
                } else {
                    logger.warning("请求错误：" + code);
                }
            }

            logger.info("共有 " + list.size() + " 个百度系列毒瘤软件");

            Main.list = list;

            for (int i = 0; i < 20; i++) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, null, e);
                }
                this.add(new Critic(this, applier, sslContext));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(Critic critic) {
        critics.add(critic);
        critic.start();
    }

    public void remove(Critic critic) {
        critics.remove(critic);
    }

    public int nextId() {
        return this.id++;
    }

    public static void main(String[] args) {
        new Main();

        for (String arg : args) {
            if (arg.equals("d") || arg.equals("debug")) {
                debug = true;
            }
        }
    }
}
