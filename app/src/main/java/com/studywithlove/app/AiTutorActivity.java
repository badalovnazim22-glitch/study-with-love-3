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
    private static final int BG = Color.rgb(255, 249, 247);
    private static final int PINK = Color.rgb(201, 120, 145);
    private static final int DARK = Color.rgb(63, 52, 57);
    private static final int MUTED = Color.rgb(145, 132, 139);
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
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        addBubble("Привет! Я ИИ-помощник Study with Love. Могу объяснить математическую тему, разобрать задачу по шагам или составить тренировку.\n\nНе отправляй сюда пароли, адреса и другие личные данные. История этого чата не сохраняется после закрытия экрана.", false);
        if (endpoint().isEmpty()) main.postDelayed(this::showEndpointDialog, 350);
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(BG);
        page.setPadding(dp(14), dp(8), dp(14), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(34);
        back.setTextColor(PINK);
        back.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        back.setOnClickListener(v -> finish());
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("ИИ-помощник", 19, DARK, true);
        TextView subtitle = label("Study with Love · ИИ-репетитор", 11, MUTED, false);
        heading.addView(title);
        heading.addView(subtitle);
        top.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = label("⚙", 23, PINK, true);
        settings.setGravity(Gravity.CENTER);
        top.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(44)));
        settings.setOnClickListener(v -> showEndpointDialog());
        page.addView(top, new LinearLayout.LayoutParams(-1, dp(54)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(10), 0, dp(12));
        scroll.addView(messages);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout suggestions = new LinearLayout(this);
        suggestions.setOrientation(LinearLayout.HORIZONTAL);
        String[] prompts = {"Объясни тему", "Разбери задачу", "Составь тест"};
        for (String prompt : prompts) {
            TextView chip = label(prompt, 11, PINK, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(10), dp(9), dp(10), dp(9));
            chip.setBackground(round(Color.WHITE, 18, 0xFFEEDDE3));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1);
            cp.setMargins(dp(2), 0, dp(2), dp(8));
            suggestions.addView(chip, cp);
            chip.setOnClickListener(v -> {
                input.setText(prompt + ": ");
                input.setSelection(input.length());
                input.requestFocus();
            });
        }
        page.addView(suggestions, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(9), dp(5), dp(5), dp(5));
        composer.setBackground(round(Color.WHITE, 24, 0xFFEADDE2));
        input = new EditText(this);
        input.setTextSize(15);
        input.setTextColor(DARK);
        input.setHintTextColor(MUTED);
        input.setHint("Напиши вопрос…");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(8), dp(7), dp(5), dp(7));
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setSingleLine(false);
        composer.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
        sendButton = new Button(this);
        sendButton.setText("➤");
        sendButton.setTextSize(18);
        sendButton.setTextColor(Color.WHITE);
        sendButton.setAllCaps(false);
        sendButton.setPadding(0, 0, 0, 0);
        sendButton.setBackground(round(PINK, 20, PINK));
        composer.addView(sendButton, new LinearLayout.LayoutParams(dp(48), dp(46)));
        sendButton.setOnClickListener(v -> sendMessage());
        page.addView(composer, new LinearLayout.LayoutParams(-1, -2));

        setContentView(page);
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
        TextView bubble = label(value, 14, user ? Color.WHITE : DARK, false);
        if (!user) {
            String safe = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            safe = safe.replaceAll("(?m)^### (.+)$", "<b>$1</b>").replaceAll("(?m)^## (.+)$", "<big><b>$1</b></big>").replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>").replace("\n", "<br>");
            bubble.setText(Html.fromHtml(safe, Html.FROM_HTML_MODE_COMPACT));
        }
        bubble.setTextIsSelectable(true);
        bubble.setPadding(dp(14), dp(12), dp(14), dp(12));
        bubble.setBackground(round(user ? PINK : Color.WHITE, 20, user ? PINK : 0xFFEFE4E8));
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
            .setTitle("Подключение ИИ")
            .setMessage("Вставь адрес защищённого сервера Study with Love. Секретный ключ Google сюда вводить не нужно.")
            .setView(field)
            .setNegativeButton("Позже", null)
            .setPositiveButton("Сохранить", (dialog, which) -> {
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

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
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
