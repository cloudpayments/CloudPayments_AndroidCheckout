package ru.cloudpayments.checkout.d3s;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class D3SView extends WebView {

    private static String JavaScriptNS = "D3SJS";

    private static Pattern mdFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"MD\\\"[^<>]*>).*?", Pattern.DOTALL);

    private static Pattern paresFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"PaRes\\\"[^<>]*>).*?", Pattern.DOTALL);

    private static Pattern valuePattern = Pattern.compile(".*? value=\\\"(.*?)\\\"", Pattern.DOTALL);

    private boolean urlReturned = false;

    private String postbackUrl = "cloudpayments.ru";

    private boolean postbackHandled = false;

    private D3SViewAuthorizationListener authorizationListener = null;

    String capturedHtml;

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
        getSettings().setSupportZoom(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW );
        }

        addJavascriptInterface(new D3SJSInterface(), JavaScriptNS);

        setWebViewClient(new WebViewClient() {

            public void onPageStarted(WebView view, String url, Bitmap icon) {
                if (!urlReturned && !postbackHandled) {
                    if (url.toLowerCase().contains(postbackUrl.toLowerCase())) {
                        postbackHandled = true;
                        //view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                        completeAuthorization(capturedHtml);
                        urlReturned = true;
                    } else {
                        super.onPageStarted(view, url, icon);
                    }
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!postbackHandled && url.toLowerCase().contains(postbackUrl.toLowerCase())) {
                    postbackHandled = true;
                    //view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                    completeAuthorization(capturedHtml);
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl().toString());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.toLowerCase().contains(postbackUrl.toLowerCase())) {
                    return;
                }
                view.loadUrl(String.format("javascript:window.%s.captureHtml(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (!failingUrl.startsWith(postbackUrl)) {
                    authorizationListener.onAuthorizationWebPageLoadingError(errorCode, description, failingUrl);
                }
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
                // Redirect to deprecated method, so you can use it in all SDK versions
                onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
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

    private void completeAuthorization(String html) {
        String md = "";
        String pares = "";

        Matcher localMatcher1 = mdFinder.matcher(html);
        Matcher localMatcher2 = paresFinder.matcher(html);

        if (localMatcher1.find()) {
            md = localMatcher1.group(1);
        }

        if (localMatcher2.find()) {
            pares = localMatcher2.group(1);
        }

        if (!TextUtils.isEmpty(md)) {
            Matcher valueMatcher = valuePattern.matcher(md);
            if (valueMatcher.find()) {
                md = valueMatcher.group(1);
            }
        }

        if (!TextUtils.isEmpty(pares)) {
            Matcher valueMatcher = valuePattern.matcher(pares);
            if (valueMatcher.find()) {
                pares = valueMatcher.group(1);
            }
        }

        if (authorizationListener != null) {
            authorizationListener.onAuthorizationCompleted(md, pares);
        }
    }

    /**
     * Sets the callback to receive auhtorization events
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
        if (authorizationListener != null) {
            authorizationListener.onAuthorizationStarted(D3SView.this);
        }

        if (!TextUtils.isEmpty(postbackUrl)) {
            this.postbackUrl = postbackUrl;
        }

        urlReturned = false;

        List<NameValuePair> params = new LinkedList<NameValuePair>();

        params.add(new BasicNameValuePair("MD", md));
        params.add(new BasicNameValuePair("TermUrl", this.postbackUrl));
        params.add(new BasicNameValuePair("PaReq", paReq));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            new UrlEncodedFormEntity(params, HTTP.UTF_8).writeTo(bos);
        } catch (IOException e) {
        }

        postUrl(acsUrl, bos.toByteArray());
    }

    class D3SJSInterface {

        D3SJSInterface() {
        }

        @android.webkit.JavascriptInterface
        public void processHTML(final String paramString) {
            completeAuthorization(paramString);
        }

        @android.webkit.JavascriptInterface
        public void captureHtml(String html) {
            capturedHtml = html;
        }
    }
}