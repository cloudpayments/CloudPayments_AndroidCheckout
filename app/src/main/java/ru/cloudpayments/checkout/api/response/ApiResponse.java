package ru.cloudpayments.checkout.api.response;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import rx.Observable;

public class ApiResponse<T> {

    @SerializedName("Success")
    private boolean success;

    @SerializedName("Message")
    private String message;

    @Nullable
    @SerializedName("Model")
    private T data;

    @Nullable
    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        if (success == false && data == null)
            return false;
        else if (success == false && data != null)
            return true;
        else
            return success;
    }

    public Observable<ApiResponse<T>> handleError() {
        if (isSuccess()) {
            return Observable.just(this);
        } else {
            return Observable.error(new ApiError(message));
        }
    }
}
