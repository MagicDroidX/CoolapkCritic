package io.github.coolapkcritic.mail;

import javax.net.ssl.SSLContext;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.*;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class MailApplier extends Thread {

    public Logger logger = Logger.getLogger("Mail Applier");

    public SSLContext sslContext;

    public final List<MailProvider> mails = Collections.synchronizedList(new ArrayList<MailProvider>());

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

    public synchronized MailProvider getNext() {
        synchronized (this.mails) {
            if (!this.mails.isEmpty()) {
                MailProvider info = this.mails.get(0);
                this.mails.remove(0);
                return info;
            }
            return null;
        }
    }

    @Override
    public void run() {
        while (true) {
            if (System.currentTimeMillis() - timestamp >= 15000) {
                try {
                    MailProvider mail = new TenMinuteMailProvider(this.sslContext);

                    if (!mail.apply()) {
                        this.logger.info("邮箱申请失败");
                    } else {
                        this.logger.info("已申请邮箱： " + mail.address);
                        this.mails.add(mail);
                    }

                    this.timestamp = System.currentTimeMillis();
                } catch (Throwable e) {
                    this.logger.log(Level.SEVERE, null, e);
                }
            }

            for (MailProvider mail : mails) {
                if (!mail.isValid()) {
                    this.mails.remove(mail);
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
