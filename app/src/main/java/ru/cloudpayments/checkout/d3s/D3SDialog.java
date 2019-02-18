package ru.cloudpayments.checkout.d3s;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import ru.cloudpayments.checkout.R;
import ru.cloudpayments.checkout.base.Base3DSDialog;

public class D3SDialog extends Base3DSDialog implements D3SViewAuthorizationListener {

    private D3SView authenticator;
    private View progressBar;
    private String acs, md, pareq, postback;

    private D3SDialogListener authorizationListener;

    private Handler handler;

    public D3SDialog(Context context, int theme) {
        super(context, theme);
    }

    public static D3SDialog newInstance(Context context, final String acsUrl, final String md,
                                        final String paReq, final String postbackUrl, D3SDialogListener listener) {
        D3SDialog dialog = new D3SDialog(context, R.style.Theme_CustomDialog);
        dialog.setContentView(R.layout.progress_dialog);

        dialog.acs = acsUrl;
        dialog.md = md;
        dialog.pareq = paReq;
        dialog.postback = postbackUrl;
        dialog.authorizationListener = listener;

        return dialog;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authenticator = new D3SView(getContext());
        authenticator.setAuthorizationListener(this);
        ((FrameLayout) findViewById(R.id.main)).addView(authenticator, 0);
        progressBar = findViewById(R.id.bar);
        progressBar.setVisibility(View.VISIBLE);
        handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(postback)) {
                    authenticator.authorize(acs, md, pareq);
                } else {
                    authenticator.authorize(acs, md, pareq, postback);
                }
            }
        });
    }

    public void onAuthorizationCompleted(final String md, final String paRes) {
        handler.post(new Runnable() {
            public void run() {
                Log.e("ERORR", "onAuthorizationCompleted");
                dismiss();

                Log.e("MD", md);
                Log.e("PaRes", paRes);

                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationCompleted(md, paRes);
                }
            }
        });
    }

    public void onAuthorizationStarted(final D3SView view) {
        handler.post(new Runnable() {
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void onAuthorizationWebPageLoadingProgressChanged(final int progress) {
        handler.post(new Runnable() {
            public void run() {
                progressBar.setVisibility(progress > 0 && progress < 100 ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void onAuthorizationWebPageLoadingError(final int errorCode, final String description, final String failingUrl) {
        handler.post(new Runnable() {
            public void run() {
                Log.e("ERORR", "onAuthorizationWebPageLoadingError");
                dismiss();
                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationFailed(errorCode, description, failingUrl);
                }
            }
        });
    }

    @Override
    public void onAuthorizationCompletedInStackedMode(final String finalizationUrl)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                dismiss();
                if (authorizationListener != null)
                {
                    //authorizationListener.onAuthorizationCompletedInStackedMode(finalizationUrl);
                }
            }
        });
    }
}
