package ru.cloudpayments.checkout.screens;

import android.content.Intent;
import android.os.Bundle;

import butterknife.OnClick;
import ru.cloudpayments.checkout.R;
import ru.cloudpayments.checkout.base.BaseActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        setTitle(R.string.menu_menu);
    }

    @OnClick(R.id.button_one_stage_payment)
    void onOneStagePaymentClick() {
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.EXTRA_PAY_TYPE, CheckoutActivity.PAY_TYPE_CHARGE);
        startActivity(intent);
    }

    @OnClick(R.id.button_two_stage_payment)
    void onTwoStagePaymentClick() {
        Intent intent = new Intent(this, CheckoutActivity.class);
        intent.putExtra(CheckoutActivity.EXTRA_PAY_TYPE, CheckoutActivity.PAY_TYPE_AUTH);
        startActivity(intent);
    }
}
