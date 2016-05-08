package io.github.coolapkcritic.mail;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public abstract class MailProvider implements Comparable<MailProvider> {

    public long time;
    public String address;

    public SSLContext sslContext;

    public MailProvider(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public abstract boolean apply() throws IOException;

    public abstract boolean verify() throws IOException;

    public abstract boolean isValid();

    public abstract Pattern getPatternMail();

    public abstract Pattern getPatternVerify();

    public abstract Pattern getPatternLink();

    public int compareTo(MailProvider o) {
        return Long.compare(o.time, this.time);
    }
}
