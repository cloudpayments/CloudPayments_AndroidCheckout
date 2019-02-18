package ru.cloudpayments.checkout.api.response;

public class ApiError extends Throwable {

    private String message;

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ApiError(String message) {
        this.message = message;
    }
}
