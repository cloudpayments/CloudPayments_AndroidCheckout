package ru.cloudpayments.checkout.api;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.cloudpayments.checkout.api.interfaces.CPApi;
import ru.cloudpayments.checkout.api.serializers.GsonDateDeSerializer;

public class ApiFactory {
    //private static final String HOST = "https://api-test.cloudpayments.ru/"; // Test host
    private static final String HOST = "https://api.cloudpayments.ru/";
    private static final String API_URL = "";

    private static final int TIMEOUT = 10;
    private static final int WRITE_TIMEOUT = 20;
    private static final int CONNECT_TIMEOUT = 10;
    private static final HttpLoggingInterceptor LOGGING_INTERCEPTOR = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

    public static final String API_ENDPOINT = HOST + API_URL;


    // API implementations
    public static CPApi getCPApi() {
        return getRetrofit().create(CPApi.class);
    }
     // API implementations

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(LOGGING_INTERCEPTOR)
            .build();

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonDateDeSerializer())
            .create();

    @NonNull
    private static Retrofit getRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(API_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(CLIENT)
                .build();
    }
}
