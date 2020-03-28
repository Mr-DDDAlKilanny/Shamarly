package kilanny.shamarlymushaf.fragments;

import android.net.http.SslError;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import kilanny.shamarlymushaf.R;
import kilanny.shamarlymushaf.util.Utils;

/**
 * A simple {@link DialogFragment} subclass.
 * Use the {@link WebViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WebViewFragment extends DialogFragment {

    private static final String ARG_NAME = "name";
    private static final String ARG_URL = "url";

    private String mName;
    private String mUrl;

    public WebViewFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment WebViewFragment.
     */
    public static WebViewFragment newInstance(String name, String url) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mName = getArguments().getString(ARG_NAME);
            mUrl = getArguments().getString(ARG_URL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);
        Button button = view.findViewById(R.id.btnOpenInBrowser);
        TextView textView = view.findViewById(R.id.txtName);
        textView.setText(mName);
        WebView webView = view.findViewById(R.id.webView);
        webView.setWebViewClient(new MyWebViewClient(button));
        WebSettings settings = webView.getSettings();
        settings.setSupportMultipleWindows(false);
        settings.setLoadsImagesAutomatically(false);
        settings.setBlockNetworkImage(true);
        webView.loadUrl(mUrl);
        button.setOnClickListener(view1 -> new AlertDialog.Builder(view1.getContext())
                .setTitle("تحميل من المتصفح")
                .setMessage("يقوم هذا الزر بفتح الموقع. هذا الموقع يحتوي على إعلانات وقد يوجد صور نساء." +
                        " الرجاء المحاولة فتح الموقع من داخل التطبيق مرة أخرى،" +
                        " وإذا استمرت المشكلة فيمكن التحميل من المتصفح. ماذا تريد أن تفعل؟")
                .setPositiveButton("لا بد من فتح المتصفح",
                        (dialogInterface, i) -> Utils.openUrlInChromeOrDefault(view.getContext(), mUrl))
                .setNegativeButton("محاولة مرة أخرى", (dialogInterface, i) -> {
                    webView.loadUrl(mUrl);
                })
                .show());
        return view;
    }
}

class MyWebViewClient extends WebViewClient {

    private final WeakReference<Button> mButton;

    MyWebViewClient(Button button) {
        mButton = new WeakReference<>(button);
    }

    private void showButton() {
        if (mButton.get() != null)
            mButton.get().setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        showButton();
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        showButton();
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
        showButton();
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        super.onReceivedSslError(view, handler, error);
        showButton();
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        showButton();
    }
}
