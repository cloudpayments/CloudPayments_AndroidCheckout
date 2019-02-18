package ru.cloudpayments.checkout.api.models;

import com.google.gson.annotations.SerializedName;

public class Post3dsRequest {

    @SerializedName("TransactionId")
    private String transactionId;

    @SerializedName("PaRes")
    private String paRes;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaRes() {
        return paRes;
    }

    public void setPaRes(String paRes) {
        this.paRes = paRes;
    }
}
