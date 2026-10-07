package com.ninjavu.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.PictureInPictureParams;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String PREFS = "ninjavu";
    private static final String KEY_BASE = "base";
    private static final String DEFAULT_BASE = "https://bingebang.st";
    private static final String GATEWAY = "https://bingebang.club";
    private static final int FILE_CHOOSER = 41;

    private WebView webView;
    private ProgressBar progress;
    private LinearLayout chrome;
    private LinearLayout bottom;
    private FrameLayout videoHolder;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private ValueCallback<Uri[]> fileCallback;
    private String base;
    private boolean desktop;
    private boolean tv;
    private boolean typing;
    private boolean dialogUp;
    private String pendingEdge = "top";
    private long lastBack;
    private TextView brand;
    private TextView searchBtn;
    private TextView refreshBtn;
    private TextView moreBtn;
    private TextView[] tabs;
    private final String[] tabPaths = {"/", "/movies", "/tv", "/explore", "/my-space"};
    private final String[] tabLabels = {"Home", "Movies", "Series", "Explore", "Space"};

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiModeManager um = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        tv = um != null && um.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        base = prefs.getString(KEY_BASE, DEFAULT_BASE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#F7F4EC"));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        chrome = buildChrome();
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        setupWebView();

        bottom = buildBottom();
        column.addView(chrome);
        column.addView(progress);
        column.addView(webView);
        column.addView(bottom);
        root.addView(column);

        videoHolder = new FrameLayout(this);
        videoHolder.setBackgroundColor(Color.BLACK);
        videoHolder.setVisibility(View.GONE);
        root.addView(videoHolder, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        wireFocus();

        String start = savedInstanceState != null
                ? savedInstanceState.getString("url", base + "/")
                : base + "/";
        webView.loadUrl(start);
        markTab(0);
        webView.postDelayed(new Runnable() {
            @Override public void run() { webView.requestFocus(); }
        }, tv ? 700 : 1200);
    }

    private LinearLayout buildChrome() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(tv ? 14 : 10), dp(8), dp(tv ? 12 : 8));
        bar.setBackgroundColor(Color.parseColor("#F7F4EC"));

        brand = new TextView(this);
        brand.setText("NinjaVu");
        brand.setTextColor(Color.parseColor("#10221A"));
        brand.setTextSize(TypedValue.COMPLEX_UNIT_SP, tv ? 22 : 18);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        brand.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { go(base + "/"); markTab(0); }
        });
        bar.addView(brand);

        searchBtn = pill("Search");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(tv ? 42 : 34));
        sp.leftMargin = dp(10);
        searchBtn.setLayoutParams(sp);
        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showSearch(); }
        });
        bar.addView(searchBtn);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        bar.addView(spacer);

        refreshBtn = iconBtn("\u21bb", new View.OnClickListener() {
            @Override public void onClick(View v) { webView.reload(); }
        });
        moreBtn = iconBtn("\u2630", new View.OnClickListener() {
            @Override public void onClick(View v) { showMore(); }
        });
        bar.addView(refreshBtn);
        bar.addView(moreBtn);
        return bar;
    }

    private LinearLayout buildBottom() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(dp(4), dp(tv ? 10 : 6), dp(4), dp(tv ? 14 : 8));
        tabs = new TextView[tabLabels.length];
        for (int i = 0; i < tabLabels.length; i++) {
            final int index = i;
            TextView tab = new TextView(this);
            tab.setText(tabLabels[i]);
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, tv ? 15 : 12);
            tab.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            tab.setPadding(0, dp(tv ? 12 : 8), 0, dp(tv ? 12 : 8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tab.setLayoutParams(lp);
            tab.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    go(base + tabPaths[index]);
                    markTab(index);
                }
            });
            tabs[i] = tab;
            nav.addView(tab);
        }
        return nav;
    }

    private void wireFocus() {
        arm(brand);
        arm(searchBtn);
        arm(refreshBtn);
        arm(moreBtn);
        for (TextView tab : tabs) arm(tab);
        brand.setNextFocusRightId(searchBtn.getId());
        searchBtn.setNextFocusRightId(refreshBtn.getId());
        refreshBtn.setNextFocusRightId(moreBtn.getId());
        for (TextView tab : tabs) tab.setNextFocusUpId(webView.getId());
        brand.setNextFocusDownId(webView.getId());
        searchBtn.setNextFocusDownId(webView.getId());
        refreshBtn.setNextFocusDownId(webView.getId());
        moreBtn.setNextFocusDownId(webView.getId());
        webView.setNextFocusUpId(searchBtn.getId());
        webView.setNextFocusDownId(tabs[0].getId());
        webView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                if (has) webView.evaluateJavascript(
                        "window.NinjaVuNav&&NinjaVuNav.focusEdge('" + pendingEdge + "')", null);
            }
        });
    }

    private void arm(final View v) {
        if (v.getId() == View.NO_ID) v.setId(View.generateViewId());
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean has) {
                view.setScaleX(has ? 1.08f : 1f);
                view.setScaleY(has ? 1.08f : 1f);
                if (view instanceof TextView) {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setCornerRadius(dp(16));
                    bg.setColor(has ? Color.parseColor("#E5F6EF") : Color.TRANSPARENT);
                    bg.setStroke(has ? dp(2) : 0, Color.parseColor("#0F9F6E"));
                    if (view == searchBtn && !has) {
                        bg.setColor(Color.WHITE);
                        bg.setStroke(dp(1), Color.parseColor("#E4DDD0"));
                    }
                    view.setBackground(bg);
                }
            }
        });
    }

    private void markTab(int index) {
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == index;
            tabs[i].setTextColor(Color.parseColor(on ? "#0F9F6E" : "#6B7C73"));
            if (!tabs[i].hasFocus()) {
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(16));
                bg.setColor(on ? Color.parseColor("#E5F6EF") : Color.TRANSPARENT);
                tabs[i].setBackground(bg);
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        String ua = s.getUserAgentString() + " NinjaVu/1.1";
        if (tv) ua += " AndroidTV Leanback";
        s.setUserAgentString(ua);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);
        webView.setBackgroundColor(Color.BLACK);
        webView.addJavascriptInterface(new Keys(), "NinjaVuKeys");

        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
                return handleWebKey(keyCode, event);
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mime, length) -> {
            try {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                req.addRequestHeader("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) req.addRequestHeader("Cookie", cookie);
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                        URLUtil.guessFileName(url, contentDisposition, mime));
                req.setMimeType(mime);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(req);
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                progress.setProgress(newProgress);
                if (newProgress > 80) injectNav();
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customCallback = callback;
                videoHolder.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                videoHolder.setVisibility(View.VISIBLE);
                chrome.setVisibility(View.GONE);
                bottom.setVisibility(View.GONE);
                progress.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                videoHolder.setFocusable(true);
                videoHolder.requestFocus();
            }

            @Override
            public void onHideCustomView() {
                hideVideo();
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if (scheme.equals("http") || scheme.equals("https")) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                syncTab(url);
                injectNav();
                view.postDelayed(new Runnable() {
                    @Override public void run() { injectNav(); }
                }, 600);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadUrl("file:///android_asset/offline.html");
                }
            }
        });
    }

    private void injectNav() {
        try {
            InputStream in = getAssets().open("tv-nav.js");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            String js = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            webView.evaluateJavascript(js, null);
        } catch (Exception ignored) {}
    }

    private boolean handleWebKey(int keyCode, KeyEvent event) {
        if (dialogUp) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return isNavKey(keyCode);
        if (customView != null) return handlePlayerKey(keyCode, event);
        String action = navAction(keyCode);
        if (action == null) return false;
        if (typing && (action.equals("left") || action.equals("right") || action.equals("up") || action.equals("down"))) {
            return false;
        }
        if (action.equals("back")) {
            webView.evaluateJavascript(
                    "(function(){return window.NinjaVuNav?NinjaVuNav.handle('back'):'native';})()",
                    value -> {
                        if (value != null && value.contains("native")) onBackPressed();
                    });
            return true;
        }
        webView.evaluateJavascript(
                "window.NinjaVuNav&&NinjaVuNav.handle(" + JSONObject.quote(action) + ")", null);
        return true;
    }

    private boolean handlePlayerKey(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            hideVideo();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_SPACE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            webView.evaluateJavascript("window.NinjaVuNav&&NinjaVuNav.player('toggle')", null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            webView.evaluateJavascript("window.NinjaVuNav&&NinjaVuNav.player('seek-')", null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            webView.evaluateJavascript("window.NinjaVuNav&&NinjaVuNav.player('seek+')", null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            webView.evaluateJavascript("window.NinjaVuNav&&NinjaVuNav.player('play')", null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            webView.evaluateJavascript("window.NinjaVuNav&&NinjaVuNav.player('pause')", null);
            return true;
        }
        return false;
    }

    private String navAction(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return "left";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "right";
            case KeyEvent.KEYCODE_DPAD_UP: return "up";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "down";
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
                return "ok";
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                return "back";
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: return "media:toggle";
            case KeyEvent.KEYCODE_MEDIA_PLAY: return "media:play";
            case KeyEvent.KEYCODE_MEDIA_PAUSE: return "media:pause";
            case KeyEvent.KEYCODE_MEDIA_REWIND: return "media:seek--";
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD: return "media:seek++";
            case KeyEvent.KEYCODE_MEDIA_NEXT: return "media:seek++";
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: return "media:seek--";
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                return "down";
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_CHANNEL_UP:
                return "up";
            default:
                return null;
        }
    }

    private boolean isNavKey(int keyCode) {
        return navAction(keyCode) != null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_SEARCH || code == KeyEvent.KEYCODE_F) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && !typing && !event.isCtrlPressed()) {
                if (code == KeyEvent.KEYCODE_F && event.getRepeatCount() > 0) return true;
                if (code == KeyEvent.KEYCODE_SEARCH || !webView.hasFocus()) {
                    showSearch();
                    return true;
                }
            }
        }
        if (code == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_DOWN) {
            showMore();
            return true;
        }
        if (!dialogUp && !webView.hasFocus() && event.getAction() == KeyEvent.ACTION_DOWN) {
            View focus = getCurrentFocus();
            if (focus != null && (code == KeyEvent.KEYCODE_DPAD_DOWN) && isChrome(focus)) {
                pendingEdge = "top";
                webView.requestFocus();
                return true;
            }
            if (focus != null && code == KeyEvent.KEYCODE_DPAD_UP && isTab(focus)) {
                pendingEdge = "bottom";
                webView.requestFocus();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean isChrome(View v) {
        return v == brand || v == searchBtn || v == refreshBtn || v == moreBtn;
    }

    private boolean isTab(View v) {
        for (TextView tab : tabs) if (tab == v) return true;
        return false;
    }

    private void hideVideo() {
        if (customView == null) return;
        videoHolder.removeView(customView);
        videoHolder.setVisibility(View.GONE);
        chrome.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);
        if (customCallback != null) customCallback.onCustomViewHidden();
        customView = null;
        customCallback = null;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        webView.requestFocus();
    }

    private void go(String url) {
        webView.loadUrl(url);
        pendingEdge = "top";
        webView.requestFocus();
    }

    private void syncTab(String url) {
        if (url == null) return;
        for (int i = tabPaths.length - 1; i >= 0; i--) {
            if (tabPaths[i].equals("/")) continue;
            if (url.contains(tabPaths[i])) {
                markTab(i);
                return;
            }
        }
        if (url.startsWith(base) && (url.endsWith("/") || url.equals(base))) markTab(0);
    }

    private void showSearch() {
        final EditText input = new EditText(this);
        input.setHint("Search movies and TV shows");
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Search NinjaVu")
                .setView(input)
                .setPositiveButton("Search", (d, w) -> runSearch(input.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> {
            dialogUp = true;
            input.requestFocus();
        });
        dialog.setOnDismissListener(d -> dialogUp = false);
        dialog.show();
    }

    private void runSearch(final String query) {
        if (query.isEmpty()) return;
        String quoted;
        try {
            quoted = JSONObject.quote(query);
        } catch (Exception e) {
            quoted = "\"" + query.replace("\"", "") + "\"";
        }
        final String js = "(function(){var i=document.querySelector('input[type=search], input.search-input, .search-input, input[placeholder*=\"Search\" i], input[name=q]');"
                + "if(!i){return 'missing';} i.focus(); i.value=" + quoted + ";"
                + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                + "i.dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}));"
                + "return 'ok';})()";
        webView.evaluateJavascript(js, value -> {
            if (value != null && value.contains("missing")) {
                webView.loadUrl(base + "/");
                webView.postDelayed(() -> webView.evaluateJavascript(js, null), 1400);
            }
        });
        webView.requestFocus();
    }

    private void showMore() {
        final String[] items = {
                "Trending",
                "TV calendar",
                "Help",
                desktop ? "Mobile site" : "Desktop site",
                "Picture in picture",
                base.contains("bingebang.club") ? "Open bingebang.st" : "Open domain gateway",
                "Clear cache",
                tv ? "Remote help" : "Keyboard and remote help",
                "About NinjaVu"
        };
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("NinjaVu")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0: go(base + "/trending"); break;
                        case 1: go(base + "/tv/calendar"); break;
                        case 2: go(base + "/help"); break;
                        case 3: toggleDesktop(); break;
                        case 4: enterPip(); break;
                        case 5: switchDomain(); break;
                        case 6:
                            webView.clearCache(true);
                            Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show();
                            break;
                        case 7: showRemoteHelp(); break;
                        default: showAbout(); break;
                    }
                }).create();
        dialog.setOnShowListener(d -> dialogUp = true);
        dialog.setOnDismissListener(d -> dialogUp = false);
        dialog.show();
    }

    private void showRemoteHelp() {
        dialogUp = true;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Remote and keyboard")
                .setMessage("Arrows or D-pad move the green ring.\nOK, Enter, or Center selects.\nBack leaves a player, a menu, or the page.\nMenu opens this list. Search opens search.\nPlay/Pause toggles video. Rewind and fast-forward seek.\nFrom the top bar, Down enters the catalogue. From the tabs, Up enters the catalogue.")
                .setPositiveButton("OK", null)
                .create();
        dialog.setOnDismissListener(d -> dialogUp = false);
        dialog.show();
    }

    private void toggleDesktop() {
        desktop = !desktop;
        WebSettings s = webView.getSettings();
        if (desktop) {
            s.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 NinjaVu/1.1");
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
        } else {
            s.setUserAgentString(null);
            String ua = s.getUserAgentString() + " NinjaVu/1.1";
            if (tv) ua += " AndroidTV Leanback";
            s.setUserAgentString(ua);
        }
        webView.reload();
    }

    private void switchDomain() {
        if (base.contains("bingebang.club")) base = DEFAULT_BASE;
        else base = GATEWAY;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_BASE, base).apply();
        Toast.makeText(this, "Opening " + base, Toast.LENGTH_SHORT).show();
        go(base + "/");
        markTab(0);
    }

    private void enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(this, "Picture in picture needs Android 8", Toast.LENGTH_SHORT).show();
            return;
        }
        PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .build();
        enterPictureInPictureMode(params);
    }

    private void showAbout() {
        dialogUp = true;
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("NinjaVu 1.1.0")
                .setMessage("Unofficial viewer for BingeBang, with D-pad and keyboard control for phones, computers, and Android TV. NinjaVu does not host movies or episodes. Playback, servers, and subtitles come from bingebang.st. If that address moves, use the domain gateway at bingebang.club.")
                .setPositiveButton("OK", null)
                .create();
        dialog.setOnDismissListener(d -> dialogUp = false);
        dialog.show();
    }

    private TextView pill(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.parseColor("#10221A"));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, tv ? 15 : 13);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(14), 0, dp(14), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(Color.WHITE);
        bg.setStroke(dp(1), Color.parseColor("#E4DDD0"));
        t.setBackground(bg);
        return t;
    }

    private TextView iconBtn(String label, View.OnClickListener click) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, tv ? 22 : 18);
        t.setTextColor(Color.parseColor("#10221A"));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(tv ? 14 : 10), 0, dp(tv ? 14 : 10), 0);
        t.setOnClickListener(click);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            hideVideo();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        if (System.currentTimeMillis() - lastBack < 1800) {
            super.onBackPressed();
        } else {
            lastBack = System.currentTimeMillis();
            Toast.makeText(this, "Press back again to leave", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", webView.getUrl());
        webView.saveState(outState);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        int vis = isInPictureInPictureMode ? View.GONE : View.VISIBLE;
        chrome.setVisibility(vis);
        bottom.setVisibility(vis);
    }

    @SuppressWarnings("deprecation")
    private boolean online() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private class Keys {
        @JavascriptInterface
        public void onEdge(String dir) {
            runOnUiThread(() -> {
                if ("up".equals(dir)) {
                    pendingEdge = "top";
                    searchBtn.requestFocus();
                } else if ("down".equals(dir)) {
                    pendingEdge = "bottom";
                    tabs[0].requestFocus();
                } else if ("left".equals(dir)) {
                    brand.requestFocus();
                } else {
                    moreBtn.requestFocus();
                }
            });
        }

        @JavascriptInterface
        public void setTyping(boolean on) {
            typing = on;
        }

        @JavascriptInterface
        public void retry() {
            runOnUiThread(() -> {
                if (online()) go(base + "/");
                else webView.reload();
            });
        }
    }
}
