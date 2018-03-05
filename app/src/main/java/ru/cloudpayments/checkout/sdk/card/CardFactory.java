package ru.cloudpayments.checkout.sdk.card;

public class CardFactory {

    public static Card create(String number) {
        return create(number, null, null);
    }

    public static Card create(String number, String expDate, String cvv) {
        return new Card(number, expDate, cvv);
    }
}
