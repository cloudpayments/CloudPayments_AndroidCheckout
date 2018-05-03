package ru.cloudpayments.checkout.d3s;

public interface D3SDialogListener {

    void onAuthorizationCompleted(final String md, final String paRes);

    void onAuthorizationFailed(final int code, final String message, final String failedUrl);
}