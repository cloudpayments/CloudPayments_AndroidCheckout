package ru.cloudpayments.checkout.d3s;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class D3SView extends WebView {

    private static String JavaScriptNS = "D3SJS";

    private static Pattern mdFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"MD\\\"[^<>]*>).*?", Pattern.DOTALL);

    private static Pattern paresFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"PaRes\\\"[^<>]*>).*?", Pattern.DOTALL);

    private static Pattern valuePattern = Pattern.compile(".*? value=\\\"(.*?)\\\"", Pattern.DOTALL);

    private boolean urlReturned = false;

    private boolean debugMode = false;

    private String postbackUrl = "cloudpayments.ru";

    private String stackedModePostbackUrl;

    private AtomicBoolean postbackHandled = new AtomicBoolean(false);

    private D3SViewAuthorizationListener authorizationListener = null;

    public D3SView(final Context context) {
        super(context);
        initUI();
    }

    public D3SView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initUI();
    }

    public D3SView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initUI();
    }

    public D3SView(final Context context, final AttributeSet attrs, final int defStyle, final boolean privateBrowsing) {
        super(context, attrs, defStyle);
        initUI();
    }

    private void initUI() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        addJavascriptInterface(new D3SJSInterface(), JavaScriptNS);

        setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final String url) {

                final boolean stackedMode = !TextUtils.isEmpty(stackedModePostbackUrl);

                if (!postbackHandled.get() && (!stackedMode && url.toLowerCase().contains(postbackUrl.toLowerCase()) || (stackedMode
                        && url.toLowerCase().contains(stackedModePostbackUrl.toLowerCase())))) {

                    if (!TextUtils.isEmpty(stackedModePostbackUrl)) {

                        if (postbackHandled.compareAndSet(false, true)) {
                            authorizationListener.onAuthorizationCompletedInStackedMode(url);
                        }
                    } else {
                        view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                    }

                    return true;

                } else {

                    return super.shouldOverrideUrlLoading(view, url);
                }
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {

                String url = request.getUrl().toString();

                final boolean stackedMode = !TextUtils.isEmpty(stackedModePostbackUrl);

                if (!postbackHandled.get() && (!stackedMode && url.toLowerCase().contains(postbackUrl.toLowerCase()) || (stackedMode
                        && url.toLowerCase().contains(stackedModePostbackUrl.toLowerCase())))) {

                    if (!TextUtils.isEmpty(stackedModePostbackUrl)) {

                        if (postbackHandled.compareAndSet(false, true)) {
                            authorizationListener.onAuthorizationCompletedInStackedMode(url);
                        }
                    } else {
                        view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                    }

                    return true;

                } else {

                    return super.shouldOverrideUrlLoading(view, url);
                }
            }

            public void onPageStarted(WebView view, String url, Bitmap icon) {

                final boolean stackedMode = !TextUtils.isEmpty(stackedModePostbackUrl);

                if (!urlReturned && !postbackHandled.get()) {

                    if ((!stackedMode && url.toLowerCase().contains(postbackUrl.toLowerCase())) || (stackedMode && url.toLowerCase().contains(stackedModePostbackUrl.toLowerCase()))) {

                        if (!TextUtils.isEmpty(stackedModePostbackUrl)) {

                            if (postbackHandled.compareAndSet(false, true)) {
                                authorizationListener.onAuthorizationCompletedInStackedMode(url);
                            }
                        } else {
                            view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                        }
                        urlReturned = true;
                    } else {
                        super.onPageStarted(view, url, icon);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.toLowerCase().contains(postbackUrl.toLowerCase())) {
                    return;
                }

                view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (!failingUrl.startsWith(postbackUrl)) {
                    authorizationListener.onAuthorizationWebPageLoadingError(errorCode, description, failingUrl);
                }
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {

                if (debugMode) {
                    handler.proceed();
                }
            }
        });

        setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int newProgress) {
                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationWebPageLoadingProgressChanged(newProgress);
                }
            }
        });
    }

    public void setStackedMode(String stackedModePostbackUrl) {
        this.stackedModePostbackUrl = stackedModePostbackUrl;
    }

    private void completeAuthorizationIfPossible(String html) {

        new MaterialDialog
                .Builder(getContext())
                .title("HTML")
                .content(html)
                .show();

        // If the postback has already been handled, stop now
        if (postbackHandled.get()) {
            return;
        }

        // Try and find the MD and PaRes form elements in the supplied html
        String md = "";
        String pares = "";

        Matcher mdMatcher = mdFinder.matcher(html);
        if (mdMatcher.find()) {
            md = mdMatcher.group(1);
        } else {
            return; // Not Found
        }

        Matcher paresMatcher = paresFinder.matcher(html);
        if (paresMatcher.find()) {
            pares = paresMatcher.group(1);
        } else {
            return; // Not Found
        }

        // Now extract the values from the previously captured form elements
        Matcher mdValueMatcher = valuePattern.matcher(md);
        if (mdValueMatcher.find()) {
            md = mdValueMatcher.group(1);
        } else {
            return; // Not Found
        }

        Matcher paresValueMatcher = valuePattern.matcher(pares);
        if (paresValueMatcher.find()) {
            pares = paresValueMatcher.group(1);
        } else {
            return; // Not Found
        }

        // If we get to this point, we've definitely got values for both the MD and PaRes

        // The postbackHandled check is just to ensure we've not already called back.
        // We don't want onAuthorizationCompleted to be called twice.
        if (postbackHandled.compareAndSet(false, true) && authorizationListener != null) {
            authorizationListener.onAuthorizationCompleted(md, pares);
        }
    }

    /**
     * Checks if debug mode is on. Note, that you must not turn debug mode for production app !
     *
     * @return
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Sets the debug mode state. When set to <b>true</b>, ssl errors will be ignored. Do not turn debug mode ON
     * for production environment !
     *
     * @param debugMode
     */
    public void setDebugMode(final boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Sets the callback to receive authorization events
     *
     * @param authorizationListener
     */
    public void setAuthorizationListener(final D3SViewAuthorizationListener authorizationListener) {
        this.authorizationListener = authorizationListener;
    }

    /**
     * Starts 3DS authorization
     *
     * @param acsUrl ACS server url, returned by the credit card processing gateway
     * @param md     MD parameter, returned by the credit card processing gateway
     * @param paReq  PaReq parameter, returned by the credit card processing gateway
     */
    public void authorize(final String acsUrl, final String md, final String paReq) {
        authorize(acsUrl, md, paReq, null);
    }

    /**
     * Starts 3DS authorization
     *
     * @param acsUrl      ACS server url, returned by the credit card processing gateway
     * @param md          MD parameter, returned by the credit card processing gateway
     * @param paReq       PaReq parameter, returned by the credit card processing gateway
     * @param postbackUrl custom postback url for intercepting ACS server result posting. You may use any url you like
     *                    here, if you need, even non existing ones.
     */
    public void authorize(final String acsUrl, final String md, final String paReq, final String postbackUrl) {

        urlReturned = false;
        postbackHandled.set(false);

        if (authorizationListener != null) {
            authorizationListener.onAuthorizationStarted(this);
        }

        if (!TextUtils.isEmpty(postbackUrl)) {
            this.postbackUrl = postbackUrl;
        }

        String postParams;
        try {
            postParams = String.format(Locale.US, "MD=%1$s&TermUrl=%2$s&PaReq=%3$s", URLEncoder.encode(md, "UTF-8"), URLEncoder.encode(this.postbackUrl, "UTF-8"), URLEncoder.encode(paReq, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        postUrl(acsUrl, postParams.getBytes());
    }

    class D3SJSInterface {

        D3SJSInterface() { }

        @JavascriptInterface
        public void processHTML(final String html) {
            completeAuthorizationIfPossible(html);
        }
    }
}