package io.github.coolapkcritic;

import java.net.HttpURLConnection;
import java.net.URL;
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

    public List<Critic> critics = new ArrayList<>();

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.94 Safari/537.36";

    public static List<String> list;

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
        logger.setLevel(Level.ALL);
        logger.addHandler(handler);

        try {
            logger.info("正在获取需要批判的软件列表");

            List<String> list = new ArrayList<>();

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

            logger.info("哇，一共有 " + list.size() + "个毒瘤软件");

            Main.list = list;

        } catch (Throwable e) {
            logger.log(Level.SEVERE, null, e);
        }

        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "8888");
        System.setProperty("https.proxyHost", "localhost");
        System.setProperty("https.proxyPort", "8888");

        this.add(new Critic(this, logger));
    }

    public void add(Critic critic) {
        critics.add(critic);
        critic.start();
    }

    public void remove(Critic critic) {
        critics.remove(critic);
    }

    public static void main(String[] args) {
        new Main();
    }
}
