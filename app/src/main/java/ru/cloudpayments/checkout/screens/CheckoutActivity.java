package ru.cloudpayments.checkout.screens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodToken;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import butterknife.BindView;
import butterknife.OnClick;
import ru.cloudpayments.checkout.api.models.Transaction;
import ru.cloudpayments.checkout.googlepay.ItemInfo;
import ru.cloudpayments.checkout.googlepay.PaymentsUtil;
import ru.cloudpayments.checkout.R;
import ru.cloudpayments.checkout.api.Api;
import ru.cloudpayments.checkout.base.BaseActivity;
import ru.cloudpayments.checkout.sdk.card.Card;
import ru.cloudpayments.checkout.sdk.card.CardFactory;
import ru.cloudpayments.checkout.sdk.card.CardType;
import ru.cloudpayments.checkout.sdk.d3s.D3SDialog;
import ru.cloudpayments.checkout.sdk.d3s.D3SDialogListener;
import ru.cloudpayments.checkout.support.Constants;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class CheckoutActivity extends BaseActivity {

    public static final String EXTRA_PAY_TYPE = "pay_type";
    public static final int PAY_TYPE_CHARGE = 31;
    public static final int PAY_TYPE_AUTH = 32;
    private int payType;


    // Данные банковской карты
    @BindView(R.id.edit_card_number)
    EditText editTextCardNumber;

    @BindView(R.id.edit_card_exp)
    EditText editTextCardExp;

    @BindView(R.id.edit_card_holder_name)
    EditText editTextCardHolderName;

    @BindView(R.id.edit_card_cvv)
    EditText editTextCardCVV;

    // Arbitrarily-picked result code.
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;

    private PaymentsClient mPaymentsClient;

    @BindView(R.id.pwg_button)
    View mPwgButton;

    @BindView(R.id.pwg_status)
    TextView mPwgStatusText;

    private ItemInfo mBikeItem = new ItemInfo("1 RUB", 1 * 1000000, R.drawable.bike);
    private long mShippingCost = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_checkout;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        payType = getIntent().getIntExtra(EXTRA_PAY_TYPE, PAY_TYPE_AUTH);

        initTitle();

        // Set up the mock information for our item in the UI.
        initItemUI();

        mPwgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPayment(view);
            }
        });

        // It's recommended to create the PaymentsClient object inside of the onCreate method.
        mPaymentsClient = PaymentsUtil.createPaymentsClient(this);

        checkIsReadyToPay();

        // Определение типа платежной системы
        activateCardTypeDetermination();
    }

    private void initTitle() {
        switch (payType) {
            case PAY_TYPE_CHARGE:
                setTitle(R.string.menu_one_stage_payment);
                break;
            case PAY_TYPE_AUTH:
                setTitle(R.string.menu_two_stage_payment);
                break;
        }
    }

    private void checkIsReadyToPay() {
        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        PaymentsUtil.isReadyToPay(mPaymentsClient).addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    public void onComplete(Task<Boolean> task) {
                        try {
                            boolean result = task.getResult(ApiException.class);
                            setPwgAvailable(result);
                        } catch (ApiException exception) {
                            // Process error
                            Log.w("isReadyToPay failed", exception);
                        }
                    }
                });
    }

    private void setPwgAvailable(boolean available) {
        // If isReadyToPay returned true, show the button and hide the "checking" text. Otherwise,
        // notify the user that Pay with Google is not available.
        // Please adjust to fit in with your current user flow. You are not required to explicitly
        // let the user know if isReadyToPay returns false.
        if (available) {
            mPwgStatusText.setVisibility(View.GONE);
            mPwgButton.setVisibility(View.VISIBLE);
        } else {
            mPwgStatusText.setText(R.string.pwg_status_unavailable);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        handlePaymentSuccess(paymentData);
                        break;
                    case Activity.RESULT_CANCELED:
                        // Nothing to here normally - the user simply cancelled without selecting a
                        // payment method.
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        handleError(status.getStatusCode());
                        break;
                }

                // Re-enables the Pay with Google button.
                mPwgButton.setClickable(true);
                break;
        }
    }

    private void handlePaymentSuccess(PaymentData paymentData) {
        // PaymentMethodToken contains the payment information, as well as any additional
        // requested information, such as billing and shipping address.
        //
        // Refer to your processor's documentation on how to proceed from here.
        PaymentMethodToken token = paymentData.getPaymentMethodToken();

        // getPaymentMethodToken will only return null if PaymentMethodTokenizationParameters was
        // not set in the PaymentRequest.
        if (token != null) {
            String billingName = paymentData.getCardInfo().getBillingAddress().getName();
            Toast.makeText(this, getString(R.string.payments_show_name, billingName), Toast.LENGTH_LONG).show();

            // Use token.getToken() to get the token string.
            Log.d("GooglePaymentToken", token.getToken());

            if (payType == PAY_TYPE_CHARGE) {
                charge(token.getToken(), "Google Pay");
            } else {
                auth(token.getToken(), "Google Pay");
            }
        }
    }

    private void handleError(int statusCode) {
        // At this stage, the user has already seen a popup informing them an error occurred.
        // Normally, only logging is required.
        // statusCode will hold the value of any constant from CommonStatusCode or one of the
        // WalletConstants.ERROR_CODE_* constants.
        Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode));
    }

    // This method is called when the Pay with Google button is clicked.
    public void requestPayment(View view) {
        // Disables the button to prevent multiple clicks.
        mPwgButton.setClickable(false);

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        String price = PaymentsUtil.microsToString(mBikeItem.getPriceMicros() + mShippingCost);

        TransactionInfo transaction = PaymentsUtil.createTransaction(price);
        PaymentDataRequest request = PaymentsUtil.createPaymentDataRequest(transaction);
        Task<PaymentData> futurePaymentData = mPaymentsClient.loadPaymentData(request);

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        AutoResolveHelper.resolveTask(futurePaymentData, this, LOAD_PAYMENT_DATA_REQUEST_CODE);
    }

    private void initItemUI() {
        TextView itemName = findViewById(R.id.text_item_name);
        ImageView itemImage = findViewById(R.id.image_item_image);
        TextView itemPrice = findViewById(R.id.text_item_price);

        itemName.setText(mBikeItem.getName());
        itemImage.setImageResource(mBikeItem.getImageResourceId());
        //itemPrice.setText(PaymentsUtil.microsToString(mBikeItem.getPriceMicros()));
    }

    @OnClick(R.id.button_pay)
    void onPayClick() {

        // Получаем введенные данные банковской карты
        String cardNumber = editTextCardNumber.getText().toString();
        String cardExp = editTextCardExp.getText().toString();
        String cardHolderName = editTextCardHolderName.getText().toString();
        String cardCVV = editTextCardCVV.getText().toString();

        // Создаем объект Card
        Card card = CardFactory.create(cardNumber, cardExp, cardCVV);

        // Создаем криптограмму карточных данных
        String cardCryptogram = null;
        try {
            // Чтобы создать криптограмму необходим PublicID (его можно посмотреть в личном кабинете)
            cardCryptogram = card.cardCryptogram(Constants.MERCHANT_PUBLIC_ID);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        // Если данные карты введены корректно и криптограмма успешно созданна
        // используя методы API выполняем оплату по криптограмме
        // (charge (для одностадийного платежа) или auth (для двухстадийного))
        if (cardCryptogram != null) {
            if (payType == PAY_TYPE_CHARGE) {
                charge(cardCryptogram, cardHolderName);
            } else {
                auth(cardCryptogram, cardHolderName);
            }
        } else {
            showToast(R.string.checkout_error_crypto);
        }
    }

    // Это тестовое приложение и запросы выполняются на прямую на сервер CloudPayment
    // Не храните пароль для API в приложении!

    // Правильно выполнять запросы по этой схеме:
    // 1) В приложении необходимо получить данные карты: номер, срок действия, имя держателя и CVV.
    // 2) Создать криптограмму карточных данных при помощи SDK.
    // 3) Отправить криптограмму и все данные для платежа с мобильного устройства на ваш сервер.
    // 4) С сервера выполнить оплату через платежное API CloudPayments.
    private void charge(String cardCryptogramPacket, String cardHolderName) {
        compositeSubscription.add(Api
                .charge(cardCryptogramPacket, cardHolderName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(this::showLoading)
                .doOnEach(notification -> hideLoading())
                .subscribe(transaction -> {
                    checkResponse(transaction);
                }, this::handleError));

    }

    // Это тестовое приложение и запросы выполняются на прямую на сервер CloudPayment
    // Не храните пароль для API в приложении!

    // Правильно выполнять запросы по этой схеме:
    // 1) В приложении необходимо получить данные карты: номер, срок действия, имя держателя и CVV.
    // 2) Создать криптограмму карточных данных при помощи SDK.
    // 3) Отправить криптограмму и все данные для платежа с мобильного устройства на ваш сервер.
    // 4) С сервера выполнить оплату через платежное API CloudPayments.
    private void auth(String cardCryptogramPacket, String cardHolderName) {
        compositeSubscription.add(Api
                .auth(cardCryptogramPacket, cardHolderName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(this::showLoading)
                .doOnEach(notification -> hideLoading())
                .subscribe(transaction -> {
                    checkResponse(transaction);
                }, this::handleError));
    }

    // Проверяем необходимо ли подтверждение с использованием 3DS
    private void checkResponse (Transaction transaction) {
        if (transaction.getPaReq() != null && transaction.getAcsUrl() != null) {
            // Показываем 3DS форму
            show3DS(transaction);
        } else {
            // Показываем результат
            showToast(transaction.getCardHolderMessage());
        }
    }

    private void show3DS(Transaction transaction) {
        // Открываем форму 3ds
        D3SDialog.newInstance(this,
                transaction.getAcsUrl(),
                transaction.getId(),
                transaction.getPaReq(),
                Constants.TERM_URL,
                new D3SDialogListener() {
                    @Override
                    public void onAuthorizationCompleted(String md, String paRes) {
                        // Успешно
                        post3ds(md, paRes);
                    }

                    @Override
                    public void onAuthorizationFailed(int code, String message, String failedUrl) {
                        // Транзакция отклонена
                        showToast("AuthorizationFailed: " + message);
                    }
                }
        ).show();
    }

    // Это тестовое приложение и запросы выполняются на прямую на сервер CloudPayment
    // Правильно выполнять запросы по этой схеме:
    // 1) В приложении необходимо получить данные карты: номер, срок действия, имя держателя и CVV.
    // 2) Создать криптограмму карточных данных при помощи SDK.
    // 3) Отправить криптограмму и все данные для платежа с мобильного устройства на ваш сервер.
    // 4) С сервера выполнить оплату через платежное API CloudPayments.
    private void post3ds(String md, String paRes) {
        compositeSubscription.add(Api
                .post3ds(md, paRes)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(this::showLoading)
                .doOnEach(notification -> hideLoading())
                .subscribe(transaction -> {
                    checkResponse(transaction);
                }, this::handleError));
    }


    // Пример определения типа платежной системы по номеру карты
    private void activateCardTypeDetermination() {
        editTextCardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String cardType = CardType.toString(CardType.getType(s.toString())); // Определяем тип во время ввода номера карты
                log(cardType); // и выводим данные в лог
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
