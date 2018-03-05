package ru.cloudpayments.checkout.api.interfaces;


import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import ru.cloudpayments.checkout.api.ApiMap;
import ru.cloudpayments.checkout.api.response.ApiResponse;
import ru.cloudpayments.checkout.api.models.Transaction;
import rx.Observable;

public interface CPApi {

    @POST("payments/cards/charge")
    Observable<ApiResponse<Transaction>> charge(@Header("Content-Type") String contentType, @Header("Authorization") String authkey, @QueryMap ApiMap args);

    @POST("payments/cards/auth")
    Observable<ApiResponse<Transaction>> auth(@Header("Content-Type") String contentType, @Header("Authorization") String authkey, @QueryMap ApiMap args);

    @POST("payments/cards/post3ds")
    Observable<ApiResponse<Transaction>> post3ds(@Header("Content-Type") String contentType, @Header("Authorization") String authkey, @QueryMap ApiMap args);
}
