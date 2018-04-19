package ru.cloudpayments.checkout.api;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

import ru.cloudpayments.checkout.api.response.ApiResponse;
import ru.cloudpayments.checkout.api.models.Transaction;
import ru.cloudpayments.checkout.support.Constants;
import rx.Observable;

public class Api {

    private static final String CONTENT_TYPE = "application/json";

    public static Observable<Transaction> charge(String cardCryptogramPacket, String cardHolderName) {

        ApiMap args = ApiMap
                .builder()
                .build();

        // Параметры:
        args.put("Amount", 1); // Сумма платежа (Обязательный)
        args.put("Currency", "RUB"); // Валюта (Обязательный)
        args.put("Name", cardHolderName); // Имя держателя карты в латинице (Обязательный для всех платежей кроме Apple Pay и Google Pay)
        args.put("IpAddress", "192.168.0.1"); // Необходимый параметр, оставляем его как есть
        args.put("CardCryptogramPacket", cardCryptogramPacket); // Криптограмма платежных данных (Обязательный)
        args.put("InvoiceId", "1111"); // Номер счета или заказа в вашей системе (необязательный)
        args.put("Description", "Оплата книг"); // Описание оплаты в свободной форме (необязательный)
        args.put("AccountId", "222"); // Идентификатор пользователя в вашей системе (необязательный)
        args.put("JsonData", "{\"age\":27,\"name\":\"Ivan\",\"phone\":\"+79998881122\"}"); // Любые другие данные, которые будут связаны с транзакцией (необязательный)

        return ApiFactory.getCPApi()
                .charge(CONTENT_TYPE, getAuthToken(), args)
                .flatMap(ApiResponse::handleError)
                .map(ApiResponse::getData);
    }

    public static Observable<Transaction> auth(String cardCryptogramPacket, String cardHolderName) {

        ApiMap args = ApiMap
                .builder()
                .build();

        // Параметры:
        args.put("Amount", 1); // Сумма платежа (Обязательный)
        args.put("Currency", "RUB"); // Валюта (Обязательный)
        args.put("Name", cardHolderName); // Имя держателя карты в латинице (Обязательный для всех платежей кроме Apple Pay и Google Pay)
        args.put("IpAddress", "192.168.0.1"); // Необходимый параметр, оставляем его как есть
        args.put("CardCryptogramPacket", cardCryptogramPacket); // Криптограмма платежных данных (Обязательный)
        args.put("InvoiceId", "1111"); // Номер счета или заказа в вашей системе (необязательный)
        args.put("Description", "Оплата книг"); // Описание оплаты в свободной форме (необязательный)
        args.put("AccountId", "222"); // Идентификатор пользователя в вашей системе (необязательный)
        args.put("JsonData", "{\"age\":27,\"name\":\"Ivan\",\"phone\":\"+79998881122\"}"); // Любые другие данные, которые будут связаны с транзакцией (необязательный)

        return ApiFactory.getCPApi()
                .auth(CONTENT_TYPE, getAuthToken(), args)
                .flatMap(ApiResponse::handleError)
                .map(ApiResponse::getData);
    }

    public static Observable<Transaction> post3ds(String transactionId, String paRes) {

        ApiMap args = ApiMap
                .builder()
                .build();

        args.put("TransactionId", transactionId);
        args.put("PaRes", paRes);

        return ApiFactory.getCPApi()
                .post3ds(CONTENT_TYPE, getAuthToken(), args)
                .flatMap(ApiResponse::handleError)
                .map(ApiResponse::getData);
    }

    private static String getAuthToken() {
        byte[] data = new byte[0];
        try {
            data = (Constants.MERCHANT_PUBLIC_ID + ":" + Constants.MERCHANT_API_PASS).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "Basic " + Base64.encodeToString(data, Base64.NO_WRAP);
    }
}