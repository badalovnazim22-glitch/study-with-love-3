package com.studywithlove.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiTutorActivity extends Activity {
    private static final String AI_PREFS = "study_ai_settings";
    private static final String ENDPOINT_KEY = "backend_url";
    private int BG = Color.rgb(255, 249, 247);
    private int PINK = Color.rgb(201, 120, 145);
    private int DARK = Color.rgb(63, 52, 57);
    private int MUTED = Color.rgb(145, 132, 139);
    private int PALE = 0xFFFCEEF2;
    private String fontFamily = "sans-serif";
    private String language = "ru";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<JSONObject> history = new ArrayList<>();
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private Button sendButton;
    private boolean waiting = false;
    private volatile HttpURLConnection activeConnection;
    private volatile boolean destroyed = false;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        loadAppearance();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().setDecorFitsSystemWindows(false);
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                getWindow().setStatusBarContrastEnforced(false);
                getWindow().setNavigationBarContrastEnforced(false);
            }
        } else {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
        }
        updateSystemBars();
        buildUi();
        addBubble(tr("Привет! Я ИИ-помощник Study with Love. Могу объяснить математическую тему, разобрать задачу по шагам или составить тренировку.\n\nНе отправляй сюда пароли, адреса и другие личные данные. История этого чата не сохраняется после закрытия экрана.", "Hi! I’m the Study with Love AI tutor. I can explain a math topic, work through a problem step by step, or create a practice quiz.\n\nDon’t send passwords, addresses, or other personal information. This chat history is not saved after you close the screen.", "Salam! Mən Study with Love süni intellekt köməkçisiyəm. Riyaziyyat mövzusunu izah edə, məsələni addım-addım həll edə və ya məşq testi hazırlaya bilərəm.\n\nŞifrələri, ünvanları və digər şəxsi məlumatları göndərmə. Ekranı bağladıqdan sonra bu söhbətin tarixçəsi saxlanılmır."), false);
        if (endpoint().isEmpty()) main.postDelayed(this::showEndpointDialog, 350);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(14), dp(8), dp(14), dp(10));
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            page.setOnApplyWindowInsetsListener((view, insets) -> {
                int top = 0, bottom = 0;
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                    android.graphics.Insets ime = insets.getInsets(android.view.WindowInsets.Type.ime());
                    top = bars.top;
                    bottom = Math.max(bars.bottom, ime.bottom);
                } else {
                    top = insets.getSystemWindowInsetTop();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                view.setPadding(dp(14), dp(8) + top, dp(14), dp(10) + bottom);
                return insets;
            });
        }

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this);
        back.setText("‹");
        back.setContentDescription(tr("Назад", "Back", "Geri"));
        back.setTextSize(34);
        back.setTextColor(PINK);
        back.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setOnClickListener(v -> finish());
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = label(tr("ИИ-помощник", "AI tutor", "Süni intellekt köməkçisi"), 19, DARK, true);
        TextView subtitle = label(tr("Study with Love · ИИ-репетитор", "Study with Love · AI tutor", "Study with Love · Süni intellekt köməkçisi"), 13, MUTED, false);
        heading.addView(title);
        heading.addView(subtitle);
        top.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = label("⚙", 23, PINK, true);
        settings.setContentDescription(tr("Настройки подключения ИИ", "AI connection settings", "Süni intellekt bağlantı ayarları"));
        settings.setGravity(Gravity.CENTER);
        top.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
        settings.setOnClickListener(v -> showEndpointDialog());
        page.addView(top, new LinearLayout.LayoutParams(-1, dp(54)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(10), 0, dp(12));
        scroll.addView(messages);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        HorizontalScrollView suggestionScroll = new HorizontalScrollView(this);
        suggestionScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout suggestions = new LinearLayout(this);
        suggestions.setOrientation(LinearLayout.HORIZONTAL);
        String[] prompts = {tr("Объясни тему", "Explain a topic", "Mövzunu izah et"), tr("Разбери задачу", "Solve a problem", "Məsələni izah et"), tr("Составь тест", "Create a quiz", "Test hazırla")};
        for (String prompt : prompts) {
            TextView chip = label(prompt, 13, PINK, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(10), dp(9), dp(10), dp(9));
            chip.setBackground(round(PALE, 18, PALE));
            chip.setMinHeight(dp(48));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
            cp.setMargins(dp(2), 0, dp(6), dp(8));
            suggestions.addView(chip, cp);
            chip.setOnClickListener(v -> {
                input.setText(prompt + ": ");
                input.setSelection(input.length());
                input.requestFocus();
            });
        }
        suggestionScroll.addView(suggestions, new HorizontalScrollView.LayoutParams(-2, -2));
        page.addView(suggestionScroll, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(9), dp(5), dp(5), dp(5));
        composer.setBackground(round(PALE, 24, PALE));
        input = new EditText(this);
        input.setTextSize(16);
        input.setTextColor(DARK);
        input.setHintTextColor(MUTED);
        input.setHint(tr("Напиши вопрос…", "Type your question…", "Sualını yaz…"));
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(8), dp(7), dp(5), dp(7));
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setSingleLine(false);
        composer.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
        sendButton = new Button(this);
        sendButton.setText("➤");
        sendButton.setContentDescription(tr("Отправить сообщение", "Send message", "Mesajı göndər"));
        sendButton.setTextSize(18);
        sendButton.setTextColor(Color.WHITE);
        sendButton.setAllCaps(false);
        sendButton.setPadding(0, 0, 0, 0);
        sendButton.setBackground(round(PINK, 20, PINK));
        composer.addView(sendButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        sendButton.setOnClickListener(v -> sendMessage());
        page.addView(composer, new LinearLayout.LayoutParams(-1, -2));

        setContentView(page);
        if (android.os.Build.VERSION.SDK_INT >= 21) page.requestApplyInsets();
    }

    private void sendMessage() {
        if (waiting) return;
        String question = input.getText().toString().trim();
        if (question.isEmpty()) return;
        if (question.length() > 6000) {
            input.setError("Максимум 6000 символов");
            return;
        }
        if (endpoint().isEmpty()) {
            showEndpointDialog();
            return;
        }
        input.setText("");
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
        addHistory("user", question);
        addBubble(question, true);
        waiting = true;
        sendButton.setEnabled(false);
        TextView typing = addBubble("Думаю над ответом…", false);
        JSONArray payloadMessages = new JSONArray();
        int start = Math.max(0, history.size() - 10);
        if (start < history.size() && !"user".equals(history.get(start).optString("role"))) start++;
        int totalChars = 0;
        for (int i = start; i < history.size(); i++) {
            JSONObject item = history.get(i);
            String content = item.optString("content", "");
            if (content.length() > 6000 || totalChars + content.length() > 24000) continue;
            payloadMessages.put(item);
            totalChars += content.length();
        }
        JSONObject payload = new JSONObject();
        try { payload.put("messages", payloadMessages); } catch (Exception ignored) {}
        executor.execute(() -> {
            String answer = null;
            String error = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(endpoint());
                if (!"https".equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("Адрес сервера должен начинаться с https://");
                connection = (HttpURLConnection) url.openConnection();
                activeConnection = connection;
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(90000);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = readAll(stream);
                JSONObject result = null;
                try { result = new JSONObject(response); } catch (Exception ignored) {}
                if (status < 200 || status >= 300) {
                    error = result != null ? result.optString("error", "Сервер вернул ошибку HTTP " + status) : "Сервер вернул некорректный ответ (HTTP " + status + "). Попробуй позже.";
                } else if (result == null) {
                    error = "Сервер вернул ответ в неизвестном формате. Попробуй позже.";
                } else {
                    answer = result.optString("reply", "").trim();
                    if (answer.isEmpty()) error = "ИИ не прислал текстовый ответ. Попробуй ещё раз.";
                }
            } catch (java.net.SocketTimeoutException e) {
                error = "ИИ отвечает слишком долго. Попробуй ещё раз чуть позже.";
            } catch (Exception e) {
                error = destroyed ? "Запрос отменён." : "Ошибка запроса: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            } finally {
                activeConnection = null;
                if (connection != null) connection.disconnect();
            }
            final String finalAnswer = answer;
            final String finalError = error;
            main.post(() -> {
                if (destroyed || isFinishing()) return;
                messages.removeView(typing);
                waiting = false;
                sendButton.setEnabled(true);
                if (finalAnswer != null) {
                    addHistory("model", finalAnswer);
                    addBubble(finalAnswer, false);
                } else {
                    // Не оставляем в контексте сообщение без ответа.
                    if (!history.isEmpty()) {
                        JSONObject last = history.get(history.size() - 1);
                        if ("user".equals(last.optString("role")) &&
                            question.equals(last.optString("content"))) {
                            history.remove(history.size() - 1);
                        }
                    }
                    addBubble("Не получилось получить ответ: " + finalError, false);
                }
            });
        });
    }

    private void addHistory(String role, String content) {
        try {
            JSONObject item = new JSONObject();
            item.put("role", role);
            item.put("content", content);
            history.add(item);
        } catch (Exception ignored) {}
    }

    private TextView addBubble(String value, boolean user) {
        TextView bubble = label(value, 15, user ? Color.WHITE : DARK, false);
        if (!user) {
            String safe = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            safe = safe.replaceAll("(?m)^### (.+)$", "<b>$1</b>").replaceAll("(?m)^## (.+)$", "<big><b>$1</b></big>").replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>").replace("\n", "<br>");
            bubble.setText(Html.fromHtml(safe, Html.FROM_HTML_MODE_COMPACT));
        }
        bubble.setTextIsSelectable(true);
        bubble.setPadding(dp(14), dp(12), dp(14), dp(12));
        bubble.setBackground(round(user ? PINK : PALE, 20, user ? PINK : PALE));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(user ? dp(34) : 0, 0, user ? 0 : dp(34), dp(9));
        messages.addView(bubble, p);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        return bubble;
    }

    private void showEndpointDialog() {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextColor(DARK);
        field.setHint("https://имя.аккаунт.workers.dev/chat");
        field.setText(endpoint());
        new AlertDialog.Builder(this)
            .setTitle(tr("Подключение ИИ", "AI connection", "Süni intellekt bağlantısı"))
            .setMessage(tr("Вставь адрес защищённого сервера Study with Love. Секретный ключ Google сюда вводить не нужно.", "Enter the Study with Love secure server address. Do not enter a Google secret key here.", "Study with Love təhlükəsiz server ünvanını daxil et. Google gizli açarını bura yazma."))
            .setView(field)
            .setNegativeButton(tr("Позже", "Later", "Sonra"), null)
            .setPositiveButton(tr("Сохранить", "Save", "Yadda saxla"), (dialog, which) -> {
                String value = field.getText().toString().trim();
                if (!value.isEmpty() && (!value.startsWith("https://") || !value.endsWith("/chat"))) {
                    Toast.makeText(this, "Нужен HTTPS-адрес сервера, заканчивающийся на /chat", Toast.LENGTH_LONG).show();
                    return;
                }
                getSharedPreferences(AI_PREFS, MODE_PRIVATE).edit().putString(ENDPOINT_KEY, value).apply();
                Toast.makeText(this, value.isEmpty() ? "Адрес не задан" : "Адрес сервера сохранён", Toast.LENGTH_SHORT).show();
            }).show();
    }

    private String endpoint() {
        return getSharedPreferences(AI_PREFS, MODE_PRIVATE).getString(ENDPOINT_KEY, "https://study-with-love-ai.xnemorenight.workers.dev/chat").trim();
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void loadAppearance() {
        SharedPreferences p = getSharedPreferences("study_progress", MODE_PRIVATE);
        String theme = p.getString("app_theme", "pink");
        language = p.getString("app_language", "ru");
        String font = p.getString("app_font", "sans");
        if ("rounded".equals(font)) fontFamily = "sans-serif-rounded";
        else if ("serif".equals(font)) fontFamily = "serif";
        else if ("mono".equals(font)) fontFamily = "monospace";
        else if ("condensed".equals(font)) fontFamily = "sans-serif-condensed";
        else if ("medium".equals(font)) fontFamily = "sans-serif-medium";
        else fontFamily = "sans-serif";
        if ("night".equals(theme)) { BG=0xFF17151D; PINK=0xFFE39AB5; PALE=0xFF302735; DARK=0xFFF4EAF0; MUTED=0xFFB6A8B2; }
        else if ("lavender".equals(theme)) { BG=0xFFF8F6FF; PINK=0xFF8979B8; PALE=0xFFF0ECFA; DARK=0xFF393448; MUTED=0xFF8E879E; }
        else if ("mint".equals(theme)) { BG=0xFFF5FAF7; PINK=0xFF659B86; PALE=0xFFEAF5EF; DARK=0xFF34443D; MUTED=0xFF84958C; }
        else if ("white".equals(theme)) { BG=0xFFF7F8FA; PINK=0xFF65758B; PALE=0xFFE9EDF2; DARK=0xFF303844; MUTED=0xFF87919E; }
        else if ("brown".equals(theme)) { BG=0xFFF7F1EA; PINK=0xFF8B6046; PALE=0xFFEDE0D2; DARK=0xFF49382E; MUTED=0xFF9B8778; }
        else if ("gold".equals(theme)) { BG=0xFFFFF8DF; PINK=0xFFC49318; PALE=0xFFF5E2A6; DARK=0xFF49340B; MUTED=0xFF92712C; }
        else if ("luxury".equals(theme)) { BG=0xFF211B18; PINK=0xFFD6B36A; PALE=0xFF3A3028; DARK=0xFFF3E7D0; MUTED=0xFFB7A68B; }
        else if ("sky".equals(theme)) { BG=0xFFF1F8FC; PINK=0xFF5D91B5; PALE=0xFFE1EFF8; DARK=0xFF304656; MUTED=0xFF8199A8; }
        else if ("peach".equals(theme)) { BG=0xFFFFF5EF; PINK=0xFFD88968; PALE=0xFFFBE5D9; DARK=0xFF513D34; MUTED=0xFFA88D80; }
        else if ("red".equals(theme)) { BG=0xFFFFF5F4; PINK=0xFFB32635; PALE=0xFFF8E2E3; DARK=0xFF4B2026; MUTED=0xFF9D777B; }
        else if ("lv".equals(theme)) { BG=0xFFF4E8D5; PINK=0xFF76502F; PALE=0xFFE7D2B3; DARK=0xFF3B281C; MUTED=0xFF92775E; }
        else if ("system".equals(theme) && (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) { BG=0xFF17151D; PINK=0xFFE39AB5; PALE=0xFF302735; DARK=0xFFF4EAF0; MUTED=0xFFB6A8B2; }
        if ("system".equals(theme) && android.os.Build.VERSION.SDK_INT >= 31) {
            try {
                boolean night=(getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
                int a=getResources().getIdentifier("system_accent1_600","color","android");
                int b=getResources().getIdentifier(night?"system_neutral1_900":"system_neutral1_10","color","android");
                int c=getResources().getIdentifier(night?"system_neutral1_800":"system_neutral1_50","color","android");
                int d=getResources().getIdentifier(night?"system_neutral1_50":"system_neutral1_900","color","android");
                if(a!=0) PINK=getResources().getColor(a,getTheme()); if(b!=0) BG=getResources().getColor(b,getTheme());
                if(c!=0) PALE=getResources().getColor(c,getTheme()); if(d!=0) DARK=getResources().getColor(d,getTheme());
            } catch(Exception ignored) {}
        }
    }

    private void updateSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController c=getWindow().getInsetsController();
            if(c!=null) { int f=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                int appearance=android.graphics.Color.luminance(BG)>0.5f?f:0; c.setSystemBarsAppearance(appearance,f); }
        } else if (android.os.Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(android.graphics.Color.luminance(BG)>0.5f?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0);
        }
    }

    private String tr(String ru, String en, String az) { return "en".equals(language)?en:("az".equals(language)?az:ru); }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTypeface(android.graphics.Typeface.create(fontFamily, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        view.setTextColor(color);
        
        return view;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onDestroy() {
        destroyed = true;
        HttpURLConnection connection = activeConnection;
        if (connection != null) connection.disconnect();
        executor.shutdownNow();
        super.onDestroy();
    }
}
