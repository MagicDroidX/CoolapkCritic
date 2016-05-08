package io.github.coolapkcritic.mail;

/**
 * author: MagicDroidX
 * CoolapkCritic Project
 */
public class MailInfo implements Comparable<MailInfo> {

    public long time;
    public String sessionId;
    public String name;

    public int compareTo(MailInfo o) {
        return Long.compare(o.time, this.time);
    }
}
