package com.livegroupbuy.backend.purchase;

public class PurchaseAccessDeniedException extends RuntimeException {

    private final String code;
    private final long retryAfterSeconds;

    public PurchaseAccessDeniedException(String code, String message, long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public String getCode() {
        return code;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
