package tv.spelunkr.sideload;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.net.Uri;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Full-screen Fire TV shell. Opens the guide, and Watch fills the screen.
 */
public class MainActivity extends Activity {
    private static final String HOME = "https://spelunkr.tv/?tv=1";
    private static final String PAGE_JS =
            "(function(){"
                    + "document.documentElement.classList.add('tv');"
                    + "try{localStorage.setItem('spelunkr-tuned-in','1');}catch(e){}"
                    + "if(document.querySelector('.hp-tunein-btn')){"
                    + "try{if(sessionStorage.getItem('fire-skipped')!=='1'){"
                    + "sessionStorage.setItem('fire-skipped','1');"
                    + "location.replace('https://spelunkr.tv/?tv=1');"
                    + "return;}}catch(e){}"
                    + "}"
                    + "if(window.__fireGuide)return;"
                    + "window.__fireGuide=1;"
                    + "var style=document.createElement('style');"
                    + "style.textContent='"
                    + ".stage.stage--watch .stage-video-col{position:fixed!important;top:0!important;right:0!important;bottom:0!important;left:0!important;z-index:500!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;border:0!important;background:#000!important}"
                    + ".stage.stage--watch .stage-video-wrap{position:absolute!important;top:0;right:0;bottom:0;left:0;width:100%!important;height:100%!important;padding:0!important}"
                    + ".stage.stage--watch .stage-video{object-fit:contain!important}"
                    + ".stage.stage--watch .stage-player-header,.stage.stage--watch .stage-player-info,.stage.stage--watch .stage-visit-channel,.stage.stage--watch .stage-guide-controls,.stage.stage--watch .stage-branding,.stage.stage--watch .stage-volume-bar,.stage.stage--watch .stage-dvr-bar,.stage.stage--watch .stage-keys,.stage.stage--watch .stage-video-play-hint{display:none!important}"
                    + ".tv-grid .grid-row:focus{outline:3px solid #ffd740;outline-offset:-3px}"
                    + "';"
                    + "document.documentElement.appendChild(style);"
                    + "function rows(){var g=document.querySelector('.tv-grid');return g?[].slice.call(g.querySelectorAll('.grid-row:not(.coming-soon)')):[];}"
                    + "function ring(el){if(!el)return;if(!el.hasAttribute('tabindex')){el.setAttribute('tabindex','0');el.setAttribute('role','button');}if(!el.hasAttribute('data-remote-ring')){el.setAttribute('data-remote-ring','');el.addEventListener('blur',function(){el.removeAttribute('data-remote-ring');},{once:true});}try{el.focus({preventScroll:true});}catch(e){el.focus();}el.scrollIntoView({block:'nearest'});}"
                    + "function marked(){return document.querySelector('.grid-row.highlighted:not(.coming-soon)')||document.querySelector('.grid-row.active:not(.coming-soon)');}"
                    + "function tune(row){if(!row)return;ring(row);row.click();}"
                    + "function step(delta){var list=rows();if(!list.length)return null;var cur=document.activeElement;var idx=list.indexOf(cur);if(idx<0){idx=list.indexOf(marked());}if(idx<0)idx=0;else idx=Math.max(0,Math.min(list.length-1,idx+delta));var row=list[idx];tune(row);return row;}"
                    + "function watchRow(row){if(!row)row=marked();if(!row)return;var id=row.getAttribute('data-channel');tune(row);var tries=0;var timer=setInterval(function(){var now=marked();if((now&&now.getAttribute('data-channel')===id)||++tries>25){clearInterval(timer);var b=document.querySelector('.guide-ctrl-watch,.guide-watch-btn,.cv-watch');if(b)b.click();}},40);}"
                    + "document.addEventListener('keydown',function(e){"
                    + "if(document.querySelector('.stage--watch'))return;"
                    + "if(e.key==='MediaPlay'||e.key==='MediaPlayPause'){e.preventDefault();e.stopPropagation();watchRow(document.activeElement&&document.activeElement.closest?document.activeElement.closest('.grid-row'):null);return;}"
                    + "var grid=document.querySelector('.tv-grid');"
                    + "var inGrid=grid&&grid.contains(document.activeElement);"
                    + "if(e.key==='ArrowDown'){e.preventDefault();e.stopPropagation();step(inGrid?1:0);return;}"
                    + "if(e.key==='ArrowUp'&&inGrid){e.preventDefault();e.stopPropagation();step(-1);return;}"
                    + "if(inGrid&&(e.key==='Enter'||e.key==='NumpadEnter')){e.preventDefault();e.stopPropagation();watchRow(document.activeElement);}"
                    + "},true);"
                    + "})();";
    private static final String PAUSE_JS =
            "(function(){var nodes=document.querySelectorAll('video,audio');"
                    + "for(var i=0;i<nodes.length;i++){try{nodes[i].pause();nodes[i].muted=true;}catch(e){}}})();";
    private static final String UNMUTE_JS =
            "(function(){var nodes=document.querySelectorAll('video,audio');"
                    + "for(var i=0;i<nodes.length;i++){try{nodes[i].muted=false;}catch(e){}}})();";
    private static final String BACK_JS =
            "(function(){"
                    + "if(!document.querySelector('.app--watch,.stage--watch'))return 'guide';"
                    + "var buttons=document.querySelectorAll('button');"
                    + "for(var i=0;i<buttons.length;i++){"
                    + "if((buttons[i].textContent||'').replace(/\\s+/g,' ').trim()==='Guide'){buttons[i].click();return 'watch';}"
                    + "}"
                    + "return 'watch';"
                    + "})();";

    private WebView webView;
    private FrameLayout customViewContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#1C0A57"));

        webView = new WebView(this);
        customViewContainer = new FrameLayout(this);
        FrameLayout.LayoutParams fill = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(webView, fill);
        root.addView(customViewContainer, fill);
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " SpelunkrFireTV/1.0");

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setBackgroundColor(Color.parseColor("#1C0A57"));
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.setWebViewClient(new StationClient());
        webView.setWebChromeClient(new StationChrome());

        webView.loadUrl(HOME);
        webView.requestFocus();
    }

    private void hideSystemUi() {
        View decor = getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        } else {
            stopPlayback();
        }
    }

    private void stopPlayback() {
        if (webView == null) {
            return;
        }
        webView.evaluateJavascript(PAUSE_JS, null);
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView == null) {
            return;
        }
        webView.resumeTimers();
        webView.onResume();
        webView.evaluateJavascript(UNMUTE_JS, null);
    }

    @Override
    protected void onPause() {
        stopPlayback();
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopPlayback();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        String key = remoteKey(event.getKeyCode());
        if (key == null) {
            return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0
                && webView != null) {
            if ("Escape".equals(key)) {
                if (customView != null) {
                    hideCustomView();
                } else {
                    webView.evaluateJavascript(BACK_JS, null);
                }
                return true;
            }
            injectKey(key);
        }
        return true;
    }

    /**
     * Names match the keyboard map on spelunkr.tv. The D-pad already arrives
     * as arrow keys, so only the buttons WebView does not translate are sent.
     */
    private String remoteKey(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_BUTTON_B:
                return "Escape";
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                return "MediaPlayPause";
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                return "MediaPlay";
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                return "MediaPause";
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                return "MediaFastForward";
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                return "MediaRewind";
            case KeyEvent.KEYCODE_CHANNEL_UP:
                return "ChannelUp";
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                return "ChannelDown";
            default:
                return null;
        }
    }

    private void injectKey(String key) {
        String js = "(function(){var fire=function(type){document.dispatchEvent(new KeyboardEvent(type,{key:'"
                + key
                + "',bubbles:true,cancelable:true}));};fire('keydown');fire('keyup');})();";
        webView.evaluateJavascript(js, null);
    }

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        customViewContainer.removeView(customView);
        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        webView.setVisibility(View.VISIBLE);
    }

    private class StationClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return route(view, url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return route(view, request.getUrl().toString());
        }

        private boolean route(WebView view, String url) {
            if (url == null) {
                return true;
            }
            return !(url.startsWith("http://") || url.startsWith("https://"));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.evaluateJavascript(PAGE_JS, null);
            view.requestFocus();
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (!request.isForMainFrame()) {
                return null;
            }
            Uri uri = request.getUrl();
            if (uri == null || !"https".equals(uri.getScheme())) {
                return null;
            }
            String host = uri.getHost();
            if (!"spelunkr.tv".equals(host) && !"www.spelunkr.tv".equals(host)) {
                return null;
            }
            String path = uri.getPath();
            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                return null;
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("Accept", "text/html");
                String cookie = CookieManager.getInstance().getCookie(uri.toString());
                if (cookie != null) {
                    conn.setRequestProperty("Cookie", cookie);
                }
                if (conn.getResponseCode() >= 400) {
                    return null;
                }
                String type = conn.getContentType();
                if (type != null && !type.contains("html")) {
                    return null;
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                InputStream in = conn.getInputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    bos.write(buf, 0, n);
                }
                in.close();
                String html = bos.toString("UTF-8");
                int head = html.toLowerCase().indexOf("<head");
                int end = head >= 0 ? html.indexOf('>', head) : -1;
                if (end >= 0) {
                    String inject = "<script>try{localStorage.setItem('spelunkr-tuned-in','1')}catch(e){}document.documentElement.classList.add('tv');</script>";
                    html = html.substring(0, end + 1) + inject + html.substring(end + 1);
                }
                return new WebResourceResponse(
                        "text/html",
                        "utf-8",
                        new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception ignored) {
                return null;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    private class StationChrome extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }
            customView = view;
            customViewCallback = callback;
            webView.setVisibility(View.GONE);
            customViewContainer.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            WebView popup = new WebView(MainActivity.this);
            popup.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView popupView, String url) {
                    if (webView != null) {
                        webView.loadUrl(url);
                    }
                    return true;
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView popupView, WebResourceRequest request) {
                    if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webView.loadUrl(request.getUrl().toString());
                    }
                    return true;
                }
            });
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            request.deny();
        }
    }
}
