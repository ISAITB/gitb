package com.gitb.messaging.layer.application.dns;

/**
 * Created by root on 12.01.2015.
 */
public class DNSRecord {
    private final String domain;
    private final String address;

    public DNSRecord(String domain, String address) {
        this.domain = domain;
        this.address = address;
    }

    public String getDomain() {
        return domain;
    }

    public String getAddress() {
        return address;
    }
}
