package com.studywithlove.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.DatePicker;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class MainActivity extends Activity {
    public static volatile boolean activityVisible=false;
    private static volatile MainActivity foregroundActivity;
    private int BG = 0xFFFFF9F7, PINK = 0xFFC97891, PALE = 0xFFFCEEF2, DARK = 0xFF3F3439, MUTED = 0xFF75666D;
    private int progressMonthOffset = 0;
    private LinearLayout progressCalendarHost;
    private LinearLayout root;
    private ScrollView activeScrollView;
    private int pendingSettingsScrollY=0;
    private String currentScreen = "home";
    private String renderedScreen = "";
    private final List<String> screenHistory = new ArrayList<>();
    private boolean navigatingBack = false;
    private boolean lessonsSearchOpen = false;
    private long lastBackPressedAt=0L;
    private SharedPreferences prefs;
    private String selectedTopic = null, selectedChapter = null;
    private TextView topicProgressLabel;
    private ProgressBar topicProgressBar;
    private Button[] progressButtons = new Button[4];
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Handler settingsHandler = new Handler(Looper.getMainLooper());
    private Runnable scheduledThemeTick, ambienceAutoStopTick;
    private boolean pinDialogVisible=false,pinUnlocked=false,pinWasBackgrounded=false;
    private Runnable timerTick;
    private TextView timerDisplay, timerStatus, studyTimeValue, breakTimeValue;
    private TimerRingView timerRing;
    private android.animation.ValueAnimator timerPulseAnimator;
    private android.animation.ObjectAnimator flowerBreatheAnimator;
    private final List<MiniQuestion> miniQuestions=new ArrayList<>();
    private final List<MiniQuestion> miniWrongQuestions=new ArrayList<>();
    private final Map<String,List<MiniQuestion>> questionBank=new HashMap<>();
    private final StringBuilder miniDeferredFeedback=new StringBuilder();
    private int miniScore=0;
    private String miniTopic="";
    private boolean weaknessTrainingMode=false,miniResultSaved=false;
    /** Consistent lightweight outline icons used by the app's primary navigation. */
    private static class NavigationIconView extends View {
        private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path path=new android.graphics.Path();
        private final int type,color;
        NavigationIconView(Context context,int type,int color){super(context);this.type=type;this.color=color;setContentDescription(new String[]{"Главная","Уроки","Прогресс","Экзамен","Дневник","Таймер"}[Math.max(0,Math.min(5,type))]);}
        @Override protected void onDraw(android.graphics.Canvas canvas){
            super.onDraw(canvas);
            float scale=Math.min(getWidth(),getHeight())/24f;
            canvas.save();
            canvas.translate((getWidth()-24f*scale)/2f,(getHeight()-24f*scale)/2f);
            canvas.scale(scale,scale);
            paint.setColor(color);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(1.85f);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            path.reset();
            switch(type){
                case 0: // Home
                    path.moveTo(2.5f,10.5f);path.lineTo(12,3);path.lineTo(21.5f,10.5f);
                    path.moveTo(5,9.5f);path.lineTo(5,20.5f);path.lineTo(19,20.5f);path.lineTo(19,9.5f);
                    path.moveTo(9.5f,20.5f);path.lineTo(9.5f,14);path.lineTo(14.5f,14);path.lineTo(14.5f,20.5f);
                    canvas.drawPath(path,paint);break;
                case 1: // Open book
                    path.moveTo(12,6.5f);path.cubicTo(9,4.5f,5.5f,4.5f,3,5.5f);path.lineTo(3,19);
                    path.cubicTo(6.5f,17.8f,9.5f,18.2f,12,20);
                    path.cubicTo(14.5f,18.2f,17.5f,17.8f,21,19);path.lineTo(21,5.5f);
                    path.cubicTo(18.5f,4.5f,15,4.5f,12,6.5f);path.moveTo(12,6.5f);path.lineTo(12,20);
                    canvas.drawPath(path,paint);break;
                case 2: // Progress chart
                    path.moveTo(3,20.5f);path.lineTo(21,20.5f);
                    path.moveTo(4,16.5f);path.lineTo(9,11.5f);path.lineTo(13,14.5f);path.lineTo(19.5f,6);
                    path.moveTo(16,6);path.lineTo(19.5f,6);path.lineTo(19.5f,9.5f);
                    canvas.drawPath(path,paint);break;
                case 3: // Exam clipboard
                    path.moveTo(8,5.5f);path.lineTo(5,5.5f);path.lineTo(5,21);path.lineTo(19,21);path.lineTo(19,5.5f);path.lineTo(16,5.5f);
                    path.moveTo(9,4);path.lineTo(15,4);path.quadTo(16.5f,4,16.5f,5.5f);path.lineTo(16.5f,7);path.lineTo(7.5f,7);path.lineTo(7.5f,5.5f);path.quadTo(7.5f,4,9,4);
                    path.moveTo(8.5f,13);path.lineTo(10.5f,15);path.lineTo(15.5f,10.5f);
                    canvas.drawPath(path,paint);break;
                case 4: // Diary / notes
                    path.moveTo(5,3.5f);path.lineTo(19,3.5f);path.lineTo(19,20.5f);path.lineTo(5,20.5f);path.close();
                    path.moveTo(8.5f,8);path.lineTo(15.5f,8);path.moveTo(8.5f,12);path.lineTo(15.5f,12);path.moveTo(8.5f,16);path.lineTo(13.5f,16);
                    canvas.drawPath(path,paint);break;
                default: // Focus timer
                    canvas.drawCircle(12,12.5f,8.5f,paint);
                    path.moveTo(12,7);path.lineTo(12,12.5f);path.lineTo(16,15);
                    path.moveTo(9,2.5f);path.lineTo(15,2.5f);
                    path.moveTo(12,2.5f);path.lineTo(12,4);
                    canvas.drawPath(path,paint);break;
            }
            canvas.restore();
        }
    }

    /** Matching outline icons for the Lessons screen action row. */
    private static class HeaderActionIconView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path path=new android.graphics.Path();
        private final int type,color;
        HeaderActionIconView(Context c,int type,int color){super(c);this.type=type;this.color=color;setContentDescription(type==0?"Мои слабые места":type==1?"Поиск уроков и тем":"Сортировка предметов");}
        @Override protected void onDraw(android.graphics.Canvas c){
            super.onDraw(c);
            float d=getResources().getDisplayMetrics().density,iconScale=0.76f;
            c.save();c.translate(getWidth()/2f-12f*d*iconScale,getHeight()/2f-12f*d*iconScale);c.scale(d*iconScale,d*iconScale);
            p.setColor(color);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(1.9f);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            path.reset();
            if(type==0){
                path.moveTo(12,3.2f);path.cubicTo(9.5f,1.8f,7.2f,3.2f,7.1f,5.4f);path.cubicTo(4.3f,5.1f,2.7f,7.2f,3.5f,9.5f);path.cubicTo(1.7f,11.2f,2.8f,14.1f,5.1f,14.7f);path.cubicTo(4.6f,17.5f,7.2f,19.4f,9.5f,18.1f);path.cubicTo(10.2f,20.8f,13.8f,20.8f,14.5f,18.1f);path.cubicTo(16.8f,19.4f,19.4f,17.5f,18.9f,14.7f);path.cubicTo(21.2f,14.1f,22.3f,11.2f,20.5f,9.5f);path.cubicTo(21.3f,7.2f,19.7f,5.1f,16.9f,5.4f);path.cubicTo(16.8f,3.2f,14.5f,1.8f,12,3.2f);path.moveTo(12,3.5f);path.lineTo(12,18.5f);path.moveTo(7.2f,7.2f);path.quadTo(10,7.3f,10,10);path.moveTo(16.8f,7.2f);path.quadTo(14,7.3f,14,10);path.moveTo(5.2f,13);path.quadTo(8,12.2f,9,14.8f);path.moveTo(18.8f,13);path.quadTo(16,12.2f,15,14.8f);
                c.drawPath(path,p);
            }else if(type==1){
                c.drawCircle(10.5f,10.5f,6.3f,p);path.moveTo(15.2f,15.2f);path.lineTo(21,21);c.drawPath(path,p);
            }else{
                p.setStyle(android.graphics.Paint.Style.FILL);c.drawCircle(12,5,1.55f,p);c.drawCircle(12,12,1.55f,p);c.drawCircle(12,19,1.55f,p);
            }
            c.restore();
        }
    }

    private static class TimerRingView extends View {
        private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.RectF arc=new android.graphics.RectF();
        private int trackColor,progressColor,fillColor;
        private float progress=0f,pulse=1f;
        private android.animation.ValueAnimator progressAnimator,pulseAnimator;
        TimerRingView(Context c,int fill,int track,int accent){super(c);fillColor=fill;trackColor=track;progressColor=accent;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void setProgress(float value){float target=Math.max(0f,Math.min(1f,value));if(progressAnimator!=null)progressAnimator.cancel();progressAnimator=android.animation.ValueAnimator.ofFloat(progress,target);progressAnimator.setDuration(280);progressAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());progressAnimator.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});progressAnimator.start();}
        void setRunning(boolean running){if(pulseAnimator!=null){pulseAnimator.cancel();pulseAnimator=null;}if(!running){pulse=1f;invalidate();return;}pulseAnimator=android.animation.ValueAnimator.ofFloat(1f,1.018f);pulseAnimator.setDuration(1350);pulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);pulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);pulseAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());pulseAnimator.addUpdateListener(a->{pulse=(float)a.getAnimatedValue();invalidate();});pulseAnimator.start();}
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,d=getResources().getDisplayMetrics().density,stroke=11f*d,r=Math.min(cx,cy)-stroke*1.25f;if(r<=0)return;c.save();c.scale(pulse,pulse,cx,cy);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(fillColor);paint.clearShadowLayer();c.drawCircle(cx,cy,r-stroke*.55f,paint);arc.set(cx-r,cy-r,cx+r,cy+r);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setColor(trackColor);c.drawArc(arc,0,360,false,paint);if(progress>.0005f){paint.setColor(progressColor);paint.setShadowLayer(12f*d,0,0,progressColor);c.drawArc(arc,-90,Math.min(359.8f,360f*progress),false,paint);paint.clearShadowLayer();float angle=(float)Math.toRadians(-90+360f*progress),x=cx+(float)Math.cos(angle)*r,y=cy+(float)Math.sin(angle)*r;paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(0xFFFFFFFF);c.drawCircle(x,y,stroke*.34f,paint);paint.setColor(progressColor);c.drawCircle(x,y,stroke*.22f,paint);}c.restore();paint.setStyle(android.graphics.Paint.Style.FILL);}
        @Override protected void onDetachedFromWindow(){if(progressAnimator!=null)progressAnimator.cancel();if(pulseAnimator!=null)pulseAnimator.cancel();super.onDetachedFromWindow();}
    }
    private static class MiniQuestion { String prompt,explanation,type,answer,topic; String[] options; int correct; MiniQuestion(String p,String[] o,int c,String e){this(p,o,c,e,"choice",o!=null&&c>=0&&c<o.length?o[c]:"");} MiniQuestion(String p,String[] o,int c,String e,String t,String a){prompt=p;options=o;correct=c;explanation=e;type=t;answer=a;topic="";} }
    private Button timerStartButton, timerPauseButton;
    private EditText customMinutesInput;
    private long appUsageStartedElapsed = 0L;
    private int selectedStudyMinutes = 25, selectedBreakMinutes = 5;
    private long timerEndElapsed = 0L, timerRemainingMs = 0L;
    private boolean timerRunning = false, timerIsBreak = false;
    private long timerSessionStarted = 0L;
    private volatile boolean ambiencePlaying=false;
    private volatile boolean ambienceLoading=false;
    private android.media.AudioManager ambienceAudioManager;
    private Object ambienceFocusRequest;
    private volatile int ambienceGeneration=0;
    private android.graphics.Bitmap cachedBackgroundBitmap;
    private String cachedBackgroundUri="";
    private Thread ambienceThread;
    private android.media.AudioTrack ambienceTrack;
    private android.media.MediaPlayer ambiencePlayer;
    private final android.media.AudioManager.OnAudioFocusChangeListener ambienceFocusListener=change->{if(ambiencePlayer!=null){if(change==android.media.AudioManager.AUDIOFOCUS_LOSS||change==android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){try{if(ambiencePlayer.isPlaying())ambiencePlayer.pause();}catch(Exception ignored){}}else if(change==android.media.AudioManager.AUDIOFOCUS_GAIN){try{ambiencePlayer.start();}catch(Exception ignored){}}}};

    private String pendingReviewKey="";
    private static final String PREFS = "study_progress";
    private MistakeStore mistakeStore;
    private MistakeStore mistakeStore(){if(mistakeStore==null)mistakeStore=new MistakeStore(this);return mistakeStore;}
    private static final int PICK_BACKGROUND = 7412, PICK_TOPIC_PHOTO = 7415, PICK_TOPIC_CAMERA = 7418, DIARY_EXPORT = 7419, DIARY_PDF_EXPORT = 7420, WEEKLY_BACKUP_FOLDER = 7421;
    private static final int BACKUP_EXPORT = 7413, BACKUP_IMPORT = 7414, WEEKLY_IMAGE_EXPORT = 7416, PDF_REPORT_EXPORT = 7417;
    private android.graphics.Bitmap pendingWeeklyBitmap;
    private static final String[] DAY_NAMES = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
    private final List<String> reminderTimes = new ArrayList<>();
    private final List<CheckBox> dayChecks = new ArrayList<>();
    private final Map<String, List<String>> chapters = new LinkedHashMap<>();
    private final String[] messages = {"Ты отлично справляешься! ❤️", "Я верю в тебя, у тебя всё получится! 🌸", "Не сдавайся: маленькие шаги приводят к большим результатам! 💗", "Ты справишься! И не забывай улыбаться ❤️"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::handleBackNavigation);
        }
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // Возвращаем розовую тему после прежнего обновления с лавандовой палитрой.
        if ("lavender".equals(prefs.getString("app_theme", "pink"))) {
            SharedPreferences.Editor themeMigration=prefs.edit().putString("app_theme","pink");
            if (prefs.getInt("app_accent_color",0)==0xFF8070A8) themeMigration.remove("app_accent_color");
            themeMigration.apply();
        }
        selectedStudyMinutes=Math.max(1,Math.min(600,prefs.getInt("selected_study_minutes",25)));
        selectedBreakMinutes=Math.max(1,Math.min(30,prefs.getInt("selected_break_minutes",5)));
        int openedReminder=getIntent()==null?-1:getIntent().getIntExtra("opened_reminder_id",-1);if(openedReminder>0)prefs.edit().putLong("reminder_opened_at_"+openedReminder,System.currentTimeMillis()).apply();
        if(prefs.getBoolean("hide_recent_preview",false))getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        applySavedTheme();
        getWindow().getDecorView().setBackgroundColor(BG);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        updateSystemBarIcons();
        SharedPreferences timerPrefs=getSharedPreferences("timer_runtime",MODE_PRIVATE);if(!timerPrefs.contains("timer_service_end_wall")&&prefs.contains("timer_service_end_wall")){SharedPreferences.Editor migrate=timerPrefs.edit();migrate.putLong("timer_service_end_wall",prefs.getLong("timer_service_end_wall",0)).putLong("timer_service_duration",prefs.getLong("timer_service_duration",60000)).putBoolean("timer_service_break",prefs.getBoolean("timer_service_break",false)).apply();prefs.edit().remove("timer_service_end_wall").remove("timer_service_duration").remove("timer_service_break").apply();}long restoredEnd=timerPrefs.getLong("timer_service_end_wall",0);if(restoredEnd>System.currentTimeMillis()){timerRunning=true;timerIsBreak=timerPrefs.getBoolean("timer_service_break",false);timerRemainingMs=FocusTimerController.remaining(this);timerEndElapsed=FocusTimerController.endTime(this);long duration=timerPrefs.getLong("timer_service_duration",timerRemainingMs);timerSessionStarted=System.currentTimeMillis()-Math.max(0,duration-timerRemainingMs);}
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        Set<String> visits = new TreeSet<>(prefs.getStringSet("visit_days", new TreeSet<String>())); visits.add(todayKey()); prefs.edit().putStringSet("visit_days", visits).apply();
        // Migrate the last recorded study date from older app versions into the calendar history.
        String previousStudyDate = prefs.getString("last_study_date", "");
        if (!previousStudyDate.isEmpty()) {
            Set<String> history = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
            history.add(previousStudyDate);
            prefs.edit().putStringSet("study_days", history).apply();
        }
        loadChapters();
        loadQuestionBank();
        setupDynamicShortcuts();
        scheduleReminders(this); rescheduleFutureLetterReminders(this); WeeklyReportReceiver.schedule(this); WeeklyBackupReceiver.schedule(this); StudyWidgetProvider.updateAll(this);
        if (prefs.getBoolean("notifications_enabled", true) && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        if (!prefs.getBoolean("welcome_done", false)) showWelcome(); else { showHome(); handleQuickAction(getIntent()); }
        if(!prefs.getString("app_pin_hash","").isEmpty())showPinLockDialog();
    }

    private void setupDynamicShortcuts(){
        if(Build.VERSION.SDK_INT<25)return;try{android.content.pm.ShortcutManager manager=getSystemService(android.content.pm.ShortcutManager.class);if(manager==null)return;List<android.content.pm.ShortcutInfo> items=new ArrayList<>();
            String[] ids={"focus25","future_letter","diary"};String[] labels={"Фокус 25 минут","Письмо себе","Дневник"};String[] actions={"com.studywithlove.app.SHORTCUT_FOCUS","com.studywithlove.app.SHORTCUT_LETTER","com.studywithlove.app.SHORTCUT_DIARY"};
            for(int i=0;i<ids.length;i++){Intent intent=new Intent(this,MainActivity.class).setAction(actions[i]).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);android.content.pm.ShortcutInfo info=new android.content.pm.ShortcutInfo.Builder(this,ids[i]).setShortLabel(labels[i]).setLongLabel(labels[i]).setIcon(android.graphics.drawable.Icon.createWithResource(this,R.mipmap.ic_launcher)).setIntent(intent).build();items.add(info);}manager.setDynamicShortcuts(items);
        }catch(Exception ignored){}
    }
    private void handleQuickAction(Intent intent){
        if(intent==null)return;String action=intent.getAction();if("com.studywithlove.app.SHORTCUT_FOCUS".equals(action)){intent.setAction(null);showPomodoro();timerIsBreak=false;selectedStudyMinutes=Math.max(1,prefs.getInt("selected_study_minutes",25));timerRemainingMs=selectedStudyMinutes*60000L;updateTimerDisplay();startTimer();}
        else if(intent.getBooleanExtra("open_pomodoro_from_tile",false)){intent.removeExtra("open_pomodoro_from_tile");showPomodoro();if(timerRunning)updateTimerDisplay();}
        else if("com.studywithlove.app.SHORTCUT_LETTER".equals(action)){intent.setAction(null);showFutureLetters();}
        else if("com.studywithlove.app.SHORTCUT_DIARY".equals(action)){intent.setAction(null);showDiary();}
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleQuickAction(intent);}
    private void launchAiTutor() {
        // Открываем ИИ независимо от дополнительных учебных данных.
        // Контекст формируется уже после открытия экрана, чтобы ошибка
        // в фильтрах или хранилище ошибок не закрывала всё приложение.
        try {
            Intent intent = new Intent(MainActivity.this, AiTutorActivity.class);
            startActivity(intent);
        } catch (Exception error) {
            android.util.Log.e("MainActivity", "Не удалось открыть ИИ-помощника", error);
            android.widget.Toast.makeText(
                    MainActivity.this,
                    "Не удалось открыть ИИ-помощника. Попробуй ещё раз.",
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }
    }

    private void showWelcome() { renderWelcomeStep(0, "", "neutral", "pink", "Математика", 15, 0); }

    private void renderWelcomeStep(int step, String name, String address, String theme, String focus, int minutes, int slide) {
        currentScreen = "welcome";
        applyWelcomeTheme(theme);
        int[] liveAccents={0xFFC97891,0xFF8979B8,0xFF659B86,0xFF5D91B5,0xFFD88968,0xFFC49318};
        if(step==4) PINK=liveAccents[Math.max(0,Math.min(liveAccents.length-1,slide))];
        base("", "");
        root.setPadding(dp(20), dp(16), dp(20), dp(18));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(4), dp(4), dp(4), dp(4));
        root.addView(page, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout progress = new LinearLayout(this);
        progress.setGravity(Gravity.CENTER);
        for (int i=0;i<3;i++) {
            View dot = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(i <= (step==0?0:(step<=2?1:2)) ? PINK : (isDarkWelcome(theme) ? 0xFF554956 : 0xFFEADDE2));
            dot.setBackground(d);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp((step==0?0:(step<=2?1:2))==i?10:7),dp((step==0?0:(step<=2?1:2))==i?10:7));
            dotParams.setMargins(dp(5),0,dp(5),0);
            progress.addView(dot,dotParams);
        }
        page.addView(progress,lp(-1,28,0,0,0,22));

        if (step == 0) {
            TextView flower = text("🌷", 82, PINK, true);
            flower.setGravity(Gravity.CENTER);
            page.addView(flower,lp(-1,142,0,0,0,12));
            android.animation.ObjectAnimator bloom = android.animation.ObjectAnimator.ofPropertyValuesHolder(flower,
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X,0.88f,1.08f,1f),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y,0.88f,1.08f,1f),
                    android.animation.PropertyValuesHolder.ofFloat(View.ROTATION,-5f,4f,0f));
            bloom.setDuration(1400); bloom.setRepeatCount(android.animation.ValueAnimator.INFINITE); bloom.setRepeatMode(android.animation.ValueAnimator.REVERSE); bloom.start();
            addText(page,"Как тебя называть?",25,DARK,true);
            addText(page,"Можно просто ник — имя можно изменить позже в настройках.",14,MUTED,false);
            EditText input = new EditText(this);
            input.setSingleLine(true); input.setText(name); input.setHint("Например, Аня или Sunny");
            input.setTextSize(18); input.setTextColor(DARK); input.setHintTextColor(MUTED);
            input.setPadding(dp(15),dp(10),dp(15),dp(10)); input.setBackground(shape(PALE,16,PINK));
            input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});
            page.addView(input,lp(-1,58,0,20,0,6));
            TextView error = text("Пожалуйста, введи имя или ник.",12,0xFFE05A70,false);
            error.setVisibility(View.GONE); page.addView(error,lp(-1,-2,0,0,0,4));
            page.addView(new android.widget.Space(this),new LinearLayout.LayoutParams(1,0,1f));
            TextView skip=text("Пропустить",14,MUTED,true);skip.setGravity(Gravity.CENTER);skip.setPadding(0,dp(12),0,dp(8));page.addView(skip,lp(-1,42,0,4,0,0));
            Runnable continueName=()->{
                String value=input.getText().toString().trim();
                if(value.isEmpty()){input.setBackground(shape(PALE,16,0xFFE05A70));error.setVisibility(View.VISIBLE);input.requestFocus();return;}
                ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);
                bloom.cancel();renderWelcomeStep(1,value,address,theme,focus,minutes,slide);
            };
            skip.setOnClickListener(v->{bloom.cancel();renderWelcomeStep(1,"Друг",address,theme,focus,minutes,slide);});
            input.addTextChangedListener(new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence s,int st,int c,int a){}
                public void onTextChanged(CharSequence s,int st,int before,int count){flower.setScaleX(1f+Math.min(.22f,s.length()*.012f));flower.setScaleY(1f+Math.min(.22f,s.length()*.012f));if(!s.toString().trim().isEmpty()){error.setVisibility(View.GONE);input.setBackground(shape(PALE,16,PINK));}}
                public void afterTextChanged(android.text.Editable e){}
            });
            input.setOnEditorActionListener((v,action,event)->{continueName.run();return true;});
            input.requestFocus();
            getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        } else if (step == 1) {
            addText(page,"Как к тебе обращаться?",25,DARK,true);
            addText(page,"Выбери один вариант.",14,MUTED,false);
            String[] labels={"Девушка","Парень","Не указывать"};
            String[] values={"female","male","neutral"};
            for(int i=0;i<3;i++){
                final String value=values[i];
                Button choice=button((address.equals(value)?"✓  ":"")+labels[i],address.equals(value));
                page.addView(choice,lp(-1,54,0,12,0,3));
                choice.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);renderWelcomeStep(2,name,value,theme,focus,minutes,slide);});
            }
            page.addView(new android.widget.Space(this),new LinearLayout.LayoutParams(1,0,1f));
            TextView skip=text("Пропустить",14,MUTED,true);skip.setGravity(Gravity.CENTER);skip.setPadding(0,dp(12),0,dp(8));page.addView(skip,lp(-1,42,0,8,0,0));skip.setOnClickListener(v->renderWelcomeStep(2,name,"neutral",theme,focus,minutes,slide));
        } else if (step == 2) {
            addText(page,"Выбери аватарку",25,DARK,true);
            addText(page,"Её можно будет поменять позже.",14,MUTED,false);
            String[] avatars={"🌸","🐰","🦋","🐱","🌷"};
            String[] moreAvatars={"⭐","📚","🌻","🧸","🍓","💗","🦄","🐼","🐨","🍀","🌙","✨","🎀","🐥","🪻","🌈","🍒","🧁","🪴","💜"};
            android.widget.GridLayout avatarGrid=new android.widget.GridLayout(this);avatarGrid.setColumnCount(3);
            String selectedAvatar=prefs.getString("onboarding_avatar","🌸");
            Runnable renderAvatarChoices=()->{
                avatarGrid.removeAllViews();
                for(String avatar:avatars){
                    Button av=button((selectedAvatar.equals(avatar)?"✓  ":"")+avatar,selectedAvatar.equals(avatar));
                    android.widget.GridLayout.LayoutParams ap=new android.widget.GridLayout.LayoutParams();
                    ap.width=0;ap.height=dp(76);ap.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);
                    ap.setMargins(dp(5),dp(6),dp(5),dp(6));avatarGrid.addView(av,ap);
                    av.setOnClickListener(v->{prefs.edit().putString("onboarding_avatar",avatar).apply();renderWelcomeStep(3,name,address,theme,focus,minutes,slide);});
                }
                Button plus=button("＋",false);
                android.widget.GridLayout.LayoutParams pp=new android.widget.GridLayout.LayoutParams();
                pp.width=0;pp.height=dp(76);pp.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);
                pp.setMargins(dp(5),dp(6),dp(5),dp(6));avatarGrid.addView(plus,pp);
                plus.setOnClickListener(v->{
                    android.widget.GridLayout moreGrid=new android.widget.GridLayout(this);moreGrid.setColumnCount(4);
                    for(String emoji:moreAvatars){TextView option=text(emoji,25,DARK,false);option.setGravity(Gravity.CENTER);option.setBackground(shape(selectedAvatar.equals(emoji)?0xFFFFE3EC:PALE,14,selectedAvatar.equals(emoji)?PINK:0));
                        android.widget.GridLayout.LayoutParams ep=new android.widget.GridLayout.LayoutParams();ep.width=dp(54);ep.height=dp(54);ep.setMargins(dp(3),dp(3),dp(3),dp(3));moreGrid.addView(option,ep);
                        option.setOnClickListener(q->{prefs.edit().putString("onboarding_avatar",emoji).apply();renderWelcomeStep(3,name,address,theme,focus,minutes,slide);((android.app.Dialog)moreGrid.getTag()).dismiss();});
                    }
                    android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.setPadding(dp(8),dp(4),dp(8),dp(4));scroll.addView(moreGrid);
                    android.app.AlertDialog picker=new android.app.AlertDialog.Builder(this).setTitle("Другие аватары").setView(scroll).setNegativeButton("Закрыть",null).create();moreGrid.setTag(picker);picker.show();
                });
            };
            renderAvatarChoices.run();
            page.addView(avatarGrid,lp(-1,-2,0,22,0,0));
            page.addView(new android.widget.Space(this),new LinearLayout.LayoutParams(1,0,1f));
            TextView skip=text("Пропустить",14,MUTED,true);skip.setGravity(Gravity.CENTER);skip.setPadding(0,dp(12),0,dp(8));page.addView(skip,lp(-1,42,0,8,0,0));skip.setOnClickListener(v->renderWelcomeStep(3,name,address,theme,focus,minutes,slide));
        } else if (step == 3) {
            addText(page,"Выбери тему приложения",25,DARK,true);
            addText(page,"Можно выбрать светлое или тёмное оформление.",14,MUTED,false);
            String[][] choices={{"pink","☀  Светлая"},{"night","☾  Тёмная"}};
            for(String[] c:choices){
                Button option=button((theme.equals(c[0])?"✓  ":"")+c[1],theme.equals(c[0]));
                page.addView(option,lp(-1,64,0,14,0,4));
                option.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);renderWelcomeStep(4,name,address,c[0],focus,minutes,slide);});
            }
            page.addView(new android.widget.Space(this),new LinearLayout.LayoutParams(1,0,1f));
            TextView skip=text("Пропустить",14,MUTED,true);skip.setGravity(Gravity.CENTER);skip.setPadding(0,dp(12),0,dp(8));page.addView(skip,lp(-1,42,0,8,0,0));skip.setOnClickListener(v->renderWelcomeStep(4,name,address,theme,focus,minutes,slide));
        } else if (step == 4) {
            addText(page,"Выбери цвет акцента",25,DARK,true);
            addText(page,"Он сразу применяется к кнопкам и деталям.",14,MUTED,false);
            int[] accents={0xFFC97891,0xFF8979B8,0xFF659B86,0xFF5D91B5,0xFFD88968,0xFFC49318};
            String[] names={"Розовый","Лиловый","Мятный","Голубой","Персик","Золотой"};
            int accentIndex=Math.max(0,Math.min(accents.length-1,slide));
            LinearLayout preview=card();preview.setPadding(dp(14),dp(12),dp(14),dp(12));preview.setBackground(shape(PALE,20,PINK));
            addText(preview,"Предпросмотр",14,DARK,true);
            Button sample=button("Моя первая цель  ·  1 минута",true);preview.addView(sample,lp(-1,46,0,8,0,0));
            page.addView(preview,lp(-1,-2,0,12,0,14));
            android.widget.GridLayout palette=new android.widget.GridLayout(this);palette.setColumnCount(3);
            for(int i=0;i<accents.length;i++){
                final int index=i;
                TextView swatch=text((i==accentIndex?"✓  ":"")+names[i],13,accents[i],true);
                swatch.setGravity(Gravity.CENTER);swatch.setBackground(shape(PALE,15,accents[i]));
                android.widget.GridLayout.LayoutParams p=new android.widget.GridLayout.LayoutParams();p.width=0;p.height=dp(52);
                p.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);p.setMargins(dp(4),dp(4),dp(4),dp(4));
                palette.addView(swatch,p);
                swatch.setOnClickListener(v->{v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);renderWelcomeStep(5,name,address,theme,focus,minutes,index);});
            }
            page.addView(palette,lp(-1,-2,0,0,0,10));
            page.addView(new android.widget.Space(this),new LinearLayout.LayoutParams(1,0,1f));
            TextView skip=text("Пропустить",14,MUTED,true);skip.setGravity(Gravity.CENTER);skip.setPadding(0,dp(12),0,dp(8));page.addView(skip,lp(-1,42,0,8,0,0));skip.setOnClickListener(v->renderWelcomeStep(5,name,address,theme,focus,minutes,slide));
        } else {
            int[] accents={0xFFC97891,0xFF8979B8,0xFF659B86,0xFF5D91B5,0xFFD88968,0xFFC49318};
            int chosen=accents[Math.max(0,Math.min(accents.length-1,slide))];
            prefs.edit().putString("display_name",name.isEmpty()?"Друг":name)
                    .putString("profile_gender",address).putString("app_theme",theme)
                    .putInt("app_accent_color",chosen).putString("learning_focus",focus)
                    .putInt("daily_goal_minutes",minutes).putInt("weekly_goal_minutes",minutes*5)
                    .putBoolean("welcome_done",true).putBoolean("onboarding_gift_flower",true)
                    .putInt("flower_petals",Math.max(1,prefs.getInt("flower_petals",0)))
                    .putString("onboarding_avatar",prefs.getString("onboarding_avatar","🌸")).apply();
            applySavedTheme();
            TextView flower=text("🌸",82,PINK,true);flower.setGravity(Gravity.CENTER);page.addView(flower,lp(-1,145,0,0,0,8));
            TextView confetti=text("✦   ✧   ✦   ✿   ✧",20,PINK,true);confetti.setGravity(Gravity.CENTER);page.addView(confetti,lp(-1,32,0,0,0,8));
            android.animation.ObjectAnimator confettiAnim=android.animation.ObjectAnimator.ofFloat(confetti,View.TRANSLATION_Y,-dp(8),dp(8));confettiAnim.setDuration(700);confettiAnim.setRepeatCount(5);confettiAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);confettiAnim.start();
            addText(page,"Рада познакомиться, "+(name.isEmpty()?"друг":name)+"!",24,DARK,true);
            addText(page,"Твой первый цветочек уже ждёт тебя в приложении 🌷",14,MUTED,false);
            LinearLayout gift=card();gift.setGravity(Gravity.CENTER);gift.setBackground(shape(PALE,18,PINK));addText(gift,"🎁  Подарок за регистрацию: первая наклейка-цветок",14,DARK,true);page.addView(gift,lp(-1,-2,0,16,0,10));
            addText(page,"Первый маленький успех — сделай 1 минуту фокуса.",14,MUTED,false);
            page.addView(new android.widget.Space(this),new LinearLayout.LayoutParams(1,0,1f));
            Button begin=button("Начать 1 минуту фокуса",true);page.addView(begin,lp(-1,52,0,18,0,6));begin.setOnClickListener(v->{selectedStudyMinutes=1;timerRemainingMs=60000L;showPomodoro();});
            TextView later=text("Пропустить",14,MUTED,true);later.setGravity(Gravity.CENTER);later.setPadding(0,dp(12),0,dp(8));page.addView(later,lp(-1,42,0,4,0,0));later.setOnClickListener(v->showHome());
        }
    }

    private boolean isDarkWelcome(String theme){return "night".equals(theme);}
    
    private void applyWelcomeTheme(String theme) {
        if ("night".equals(theme)) { BG=0xFF17151D; PINK=0xFFE39AB5; PALE=0xFF302735; DARK=0xFFF4EAF0; MUTED=0xFFC8BDC8; }
        else if ("lavender".equals(theme)) { BG=0xFFF8F7FC; PINK=0xFF8070A8; PALE=0xFFF0EDFA; DARK=0xFF292638; MUTED=0xFF858096; }
        else if ("mint".equals(theme)) { BG=0xFFF5FAF7; PINK=0xFF659B86; PALE=0xFFEAF5EF; DARK=0xFF34443D; MUTED=0xFF60786B; }
        else if ("white".equals(theme)) { BG=0xFFF7F8FA; PINK=0xFF65758B; PALE=0xFFE9EDF2; DARK=0xFF303844; MUTED=0xFF65717F; }
        else { BG=0xFFFFF9F7; PINK=0xFFC97891; PALE=0xFFFCEEF2; DARK=0xFF3F3439; MUTED=0xFF75666D; }
        if (getWindow()!=null) { getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); }
    }

    private void loadChapters() {
        chapters.clear();
        add("1️⃣ Натуральные числа и действия с ними", "1.1. Натуральные числа", "1.2. Сравнение и упорядочивание", "1.3. Округление натуральных чисел", "1.4. Сложение и вычитание натуральных чисел", "1.5. Квадрат и куб натурального числа", "1.6. Умножение и деление натуральных чисел", "1.7. Числовые выражения", "1.8. Делители и кратные числа", "1.9. Простые и составные числа", "1.10. Признаки делимости");
        add("2️⃣ Обыкновенные дроби", "2.1. Правильные и неправильные дроби", "2.2. Сравнение и упорядочивание", "2.11. Сокращение дробей", "2.12. Приведение дробей к общему знаменателю", "2.3. Сложение и вычитание дробей с разными знаменателями", "2.4. Сложение смешанных чисел", "2.5. Вычитание смешанных чисел", "2.6. Умножение обыкновенных дробей", "2.7. Умножение смешанных чисел", "2.8. Деление обыкновенных дробей", "2.9. Деление смешанных чисел", "2.10. Нахождение части числа и числа по его части");
        add("3️⃣ Десятичные дроби", "3.1. Десятичные дроби", "3.2. Сравнение и упорядочивание", "3.3. Округление десятичных дробей", "3.4. Перевод обыкновенной дроби в десятичную и наоборот", "3.5. Сложение и вычитание десятичных дробей", "3.6. Умножение и деление десятичных дробей на степени числа 10", "3.7. Умножение десятичной дроби на натуральное число", "3.8. Умножение десятичных дробей", "3.9. Деление десятичной дроби на натуральное число", "3.10. Деление числа на десятичную дробь", "3.11. Действия над обыкновенными и десятичными дробями", "3.12. Среднее арифметическое десятичных дробей", "3.13. Задачи на десятичные дроби");
        add("4️⃣ Процент", "4.1. Процент, обыкновенная дробь, десятичная дробь", "4.2. Процент от числа", "4.3. Нахождение числа по проценту", "4.4. Увеличение и уменьшение величины на определённый процент", "4.5. Отношение и пропорция", "4.6. Решение задач на проценты");
        Set<String> ids = prefs.getStringSet("custom_lesson_ids", new TreeSet<String>());
        for (String id : ids) {
            String name = prefs.getString("custom_lesson_name_" + id, "Мой урок");
            chapters.put("CUSTOM:" + id + ":" + name, new ArrayList<>(prefs.getStringSet("custom_lesson_topics_" + id, new TreeSet<String>())));
        }
        SharedPreferences.Editor reviewMigration=prefs.edit();boolean migratedReviewDates=false;for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue())if(topicProgressForChapter(entry.getKey(),topic)==100){String key="topic_completed_at_"+topicStorageSuffix(entry.getKey(),topic);if(!prefs.contains(key)){reviewMigration.putLong(key,System.currentTimeMillis());migratedReviewDates=true;}}if(migratedReviewDates)reviewMigration.apply();
    }
    private void add(String name, String... topics) { List<String> list = new ArrayList<>(); for (String t : topics) list.add(t); chapters.put(name, list); }

    private static String todayKey() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(AppTime.zone());
        return f.format(new Date());
    }

    private static int reminderRequestCode(int day, int hour, int minute) { return 5000 + day * 10000 + hour * 100 + minute; }

    private void chooseQuietTime(String key){String value=prefs.getString(key,key.endsWith("start")?"22:00":"08:00");String[] p=value.split(":");int h=22,m=0;try{h=Integer.parseInt(p[0]);m=Integer.parseInt(p[1]);}catch(Exception ignored){}new TimePickerDialog(this,(view,hour,minute)->{String time=String.format(Locale.US,"%02d:%02d",hour,minute);prefs.edit().putString(key,time).apply();scheduleReminders(this);showSettings();},h,m,true).show();}
    private static void scheduleEveningRest(Context context,SharedPreferences p,AlarmManager am){
        Intent intent=new Intent(context,ReminderReceiver.class).setAction("study_evening_rest").putExtra("message","На сегодня достаточно. Пора отдохнуть и восстановить силы 🌙").putExtra("id",9810);
        PendingIntent pi=PendingIntent.getBroadcast(context,9810,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(!p.getBoolean("evening_rest_enabled",false)||!p.getBoolean("notifications_enabled",true)){am.cancel(pi);pi.cancel();return;}
        int hour=21,minute=0;try{String[] bits=p.getString("quiet_hours_start","22:00").split(":");hour=Integer.parseInt(bits[0]);minute=Integer.parseInt(bits[1]);if(hour<18){hour=21;minute=0;}}catch(Exception ignored){}
        Calendar next=Calendar.getInstance(AppTime.zone());next.set(Calendar.HOUR_OF_DAY,hour);next.set(Calendar.MINUTE,minute);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);
        if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);else if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
    }
    private static boolean isQuietHour(int hour,int minute,SharedPreferences p){if(!p.getBoolean("quiet_hours_enabled",false))return false;try{String[] a=p.getString("quiet_hours_start","22:00").split(":"),b=p.getString("quiet_hours_end","08:00").split(":");int start=Integer.parseInt(a[0])*60+Integer.parseInt(a[1]),end=Integer.parseInt(b[0])*60+Integer.parseInt(b[1]),now=hour*60+minute;if(start==end)return true;return start<end?(now>=start&&now<end):(now>=start||now<end);}catch(Exception e){return false;}}
    public static void scheduleReminders(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        scheduleEveningRest(context,p,am);
        // Cancel alarms from the previous saved schedule.
        Set<String> oldKeys = p.getStringSet("scheduled_alarm_keys", new TreeSet<String>());
        for (String key : new TreeSet<>(oldKeys)) {
            try {
                String[] bits = key.split("_");
                int day = Integer.parseInt(bits[0]);
                String[] hm = bits[1].split(":");
                int hour = Integer.parseInt(hm[0]), minute = Integer.parseInt(hm[1]);
                int id = reminderRequestCode(day, hour, minute);
                Intent old = new Intent(context, ReminderReceiver.class).setAction("study_reminder_" + key);
                PendingIntent oldPi = PendingIntent.getBroadcast(context, id, old, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.cancel(oldPi); oldPi.cancel();
            } catch (Exception ignored) { }
        }
        // Cancel the four fixed reminders from version 1, if this is an upgrade.
        for (int i = 0; i < 4; i++) {
            Intent old = new Intent(context, ReminderReceiver.class).setAction(null);
            PendingIntent pi = PendingIntent.getBroadcast(context, 300 + i, old, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi); pi.cancel();
        }
        if (!p.getBoolean("notifications_enabled", true) || p.getBoolean("do_not_disturb", false)) { p.edit().putStringSet("scheduled_alarm_keys", new TreeSet<String>()).apply(); return; }
        Set<String> days = p.getStringSet("reminder_days", null);
        if (days == null) { days = new TreeSet<>(); for (int d = 1; d <= 7; d++) days.add(String.valueOf(d)); }
        Set<String> times = p.getStringSet("reminder_times", null);
        if (times == null) times = new TreeSet<>(java.util.Arrays.asList("10:00", "15:00", "19:00", "21:00"));
        Set<String> newKeys = new TreeSet<>();
        TimeZone tz = AppTime.zone();
        for (String dayValue : days) for (String timeValue : times) {
            try {
                int day = Integer.parseInt(dayValue);
                String[] parts = timeValue.split(":");
                int hour = Integer.parseInt(parts[0]), minute = Integer.parseInt(parts[1]);
                if(isQuietHour(hour,minute,p))continue;
                String key = day + "_" + String.format(Locale.US, "%02d:%02d", hour, minute);
                Calendar next = Calendar.getInstance(tz);
                next.set(Calendar.DAY_OF_WEEK, day);
                next.set(Calendar.HOUR_OF_DAY, hour); next.set(Calendar.MINUTE, minute);
                next.set(Calendar.SECOND, 0); next.set(Calendar.MILLISECOND, 0);
                if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 7);
                int id = reminderRequestCode(day, hour, minute);
                Intent intent = new Intent(context, ReminderReceiver.class).setAction("study_reminder_" + key)
                        .putExtra("message", "🌸 Пора немного позаниматься! Ты сможешь!").putExtra("id", id);
                PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (Build.VERSION.SDK_INT >= 23) { if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi); else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi); } else am.setExact(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), pi);
                newKeys.add(key);
            } catch (Exception ignored) { }
        }
        p.edit().putStringSet("scheduled_alarm_keys", newKeys).apply();
    }

    private void compactButtons(View view) {
        if (view instanceof Button) {
            android.view.ViewGroup.LayoutParams params = view.getLayoutParams();
            int compactHeight = dp(44);
            if (params != null && params.height > compactHeight) {
                params.height = compactHeight;
                view.setLayoutParams(params);
            }
            view.setMinimumHeight(compactHeight);
            return;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                compactButtons(group.getChildAt(i));
            }
        }
    }

    private void base(String title, String subtitle) {
        final boolean backNavigation=navigatingBack;
        final boolean screenChanged=!currentScreen.equals(renderedScreen);
        if (!"flower".equals(currentScreen) && flowerBreatheAnimator!=null) { flowerBreatheAnimator.cancel(); flowerBreatheAnimator=null; }
        if (screenChanged) { if (!renderedScreen.isEmpty() && !navigatingBack) screenHistory.add(renderedScreen); renderedScreen=currentScreen; }
        navigatingBack=false;
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        ScrollView scroll = new ScrollView(this);
        activeScrollView = scroll;
        scroll.setFillViewport(true); scroll.setClipToPadding(false);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(15), dp(12), dp(15), dp(28)); root.setBackgroundColor(BG);
        installThemeFontListener(root);
        String customBg=prefs.getString("custom_background_uri","");
        if(!customBg.isEmpty()){try{if(!customBg.equals(cachedBackgroundUri)||cachedBackgroundBitmap==null){java.io.InputStream in=getContentResolver().openInputStream(android.net.Uri.parse(customBg));if(in!=null){android.graphics.Bitmap decoded=android.graphics.BitmapFactory.decodeStream(in);in.close();cachedBackgroundBitmap=decoded;cachedBackgroundUri=customBg;}}if(cachedBackgroundBitmap!=null){Drawable photo=new android.graphics.drawable.BitmapDrawable(getResources(),cachedBackgroundBitmap);photo.setAlpha(42);root.setBackground(photo);}}catch(Exception ignored){}}
        // Keep a consistent reading width on tablets while using the full
        // available width on phones. All dimensions remain density-independent.
        android.widget.FrameLayout contentFrame = new android.widget.FrameLayout(this);
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int contentWidthDp = Math.min(screenWidthDp, 600);
        android.widget.FrameLayout.LayoutParams contentParams =
                new android.widget.FrameLayout.LayoutParams(
                        dp(contentWidthDp), android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        contentFrame.addView(root, contentParams);
        scroll.addView(contentFrame, new android.widget.ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        // Навигация размещается поверх содержимого в прозрачном overlay-контейнере.
        if (!currentScreen.equals("welcome")) {
            android.widget.FrameLayout overlay=new android.widget.FrameLayout(this);
            overlay.addView(shell,new android.widget.FrameLayout.LayoutParams(-1,-1));
            View navigation=bottomNavigation();
            android.widget.FrameLayout.LayoutParams navParams=new android.widget.FrameLayout.LayoutParams(-1,dp(62),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
            navParams.leftMargin=dp(24);navParams.rightMargin=dp(24);navParams.bottomMargin=dp(10);
            overlay.addView(navigation,navParams);
            navigation.setTranslationY(0f);navigation.setAlpha(1f);
            setContentView(overlay);
        } else {
            setContentView(shell);
        }
        // Единая мягкая анимация появления содержимого при переходе между экранами.
        // Повторное обновление той же вкладки не перезапускает анимацию.
        if(screenChanged && !"welcome".equals(currentScreen) && !(prefs!=null && prefs.getBoolean("reduce_motion",false))){
            root.setAlpha(0f);
            root.setScaleX(0.985f);
            root.setScaleY(0.985f);
            root.setTranslationY(dp(8));
            root.post(()->{
                root.animate().cancel();
                root.setLayerType(View.LAYER_TYPE_HARDWARE,null);
                root.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(360)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(1.7f))
                    .withEndAction(()->root.setLayerType(View.LAYER_TYPE_NONE,null))
                    .start();
            });
        }
        if(currentScreen.startsWith("settings_") || "settings".equals(currentScreen)) {
            final int restoreY=pendingSettingsScrollY;
            scroll.post(()->scroll.scrollTo(0,restoreY));
            pendingSettingsScrollY=0;
        }
        // Унифицируем высоту обычных кнопок во всех разделах приложения.
        // Не затрагиваем поля ввода, нижнюю навигацию и специальные элементы.
        compactButtons(shell);
        if (Build.VERSION.SDK_INT >= 21) {
            shell.setOnApplyWindowInsetsListener((view, insets) -> {
                int top,bottom;
                if (Build.VERSION.SDK_INT >= 30) { android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()); top=bars.top; bottom=bars.bottom; }
                else { top=insets.getSystemWindowInsetTop(); bottom=insets.getSystemWindowInsetBottom(); }
                view.setPadding(0,top,0,bottom);
                return insets;
            });
            shell.requestApplyInsets();
        }
        scroll.setAlpha(0f); scroll.setTranslationY(dp(backNavigation?-8:8));
        scroll.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        if (!prefs.getBoolean("reduce_motion", false)) {
            scroll.animate().alpha(1f).translationY(0f).setStartDelay(12).setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            View nav=shell.getChildCount()>1?shell.getChildAt(1):null;
            if(nav!=null){nav.setAlpha(0f);nav.animate().alpha(1f).setStartDelay(35).setDuration(240)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
        } else { scroll.setAlpha(1f); scroll.setTranslationY(0f); }
        if (currentScreen.equals("home")) {
        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        brandRow.setPadding(dp(12), dp(7), dp(8), dp(7));
        brandRow.setBackground(shape(0xFFFCEEF2, 30, 0));
        TextView flower = text("✿", 16, PINK, true);
        brandRow.addView(flower, lp(24, 24, 0, 0, 7, 0));
        TextView brand = text("STUDY WITH LOVE", 9, PINK, true);
        brand.setLetterSpacing(0.10f);
        brandRow.addView(brand, new LinearLayout.LayoutParams(0, dp(24), 1f));
        if (currentScreen.equals("home")) {
            if (todayKey().equals(prefs.getString("daily_mood_date", ""))) {
                String moodValue = prefs.getString("daily_mood", "");
                String[] moodFaces = {"😴", "😌", "🙂", "😊", "⚡"};
                try {
                    int moodIndex = Integer.parseInt(moodValue);
                    if (moodIndex >= 0 && moodIndex < moodFaces.length) {
                        TextView moodBadge = text(moodFaces[moodIndex], 19, DARK, false);
                        moodBadge.setGravity(Gravity.CENTER);
                        moodBadge.setContentDescription("Настроение сегодня");
                        brandRow.addView(moodBadge, lp(38, 42, 2, 0, 4, 0));
                    }
                } catch (Exception ignored) {}
            }
            TextView settings = text("⚙", 19, PINK, false); settings.setGravity(Gravity.CENTER);
            settings.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            settings.setContentDescription("Открыть настройки");
            brandRow.addView(settings, lp(42, 44, 5, 0, 0, 0));
            settings.setOnClickListener(v -> {settings.animate().rotationBy(90f).scaleX(1.18f).scaleY(1.18f).setDuration(170).withEndAction(()->{settings.animate().rotationBy(90f).scaleX(1f).scaleY(1f).setDuration(180).start();showSettings();}).start();});
        }
        root.addView(brandRow, lp(-1, 48, 0, 0, 0, 16));
        }
        if (!title.isEmpty()) {
            TextView h = text(title, 23, DARK, true);
            h.setLetterSpacing(-0.02f);
            root.addView(h, lp(-1, -2, 0, 0, 0, 6));
        }
        if (!subtitle.isEmpty()) {
            TextView sub = text(subtitle, 13, MUTED, false);
            sub.setLineSpacing(dp(2), 1f);
            root.addView(sub, lp(-1, -2, 0, 0, 0, 16));
        }
    }

    private View bottomNavigation() {
        LinearLayout bar=new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(5),dp(4),dp(5),dp(4));
        GradientDrawable navBg=new GradientDrawable();
        navBg.setColor(isDarkAppearance()?0xF5211E29:0xF5FFFFFF);
        navBg.setCornerRadius(dp(31));
        navBg.setStroke(dp(1),isDarkAppearance()?0x55393442:0x55ECEAF2);
        bar.setBackground(navBg);
        bar.setElevation(dp(10));
        bar.setClipToOutline(true);
        String[] labels={"Главная","Уроки","Прогресс","Экзамен","Дневник","Таймер"};
        String active=currentScreen.equals("home")?"home":(currentScreen.equals("chapters")||currentScreen.equals("math_chapters")||currentScreen.equals("topics")||currentScreen.equals("topic"))?"math":currentScreen.equals("progress")||currentScreen.equals("flower")||currentScreen.equals("achievements")||currentScreen.equals("stats")?"progress":currentScreen.equals("exam")?"exam":currentScreen.equals("diary")||currentScreen.equals("letters")?"diary":currentScreen.equals("pomodoro")?"timer":"";
        String[] keys={"home","math","progress","exam","diary","timer"};
        for(int i=0;i<keys.length;i++){
            final String key=keys[i];
            boolean selected=key.equals(active);
            android.widget.FrameLayout item=new android.widget.FrameLayout(this);
            item.setBackground(selected
                ?shape(adjustColor(PINK,0.88f),24,0)
                :shape(0x00FFFFFF,24,0));

            // Иконка и подпись собраны в одну высокую мягкую капсулу активной вкладки.
            android.widget.FrameLayout iconSlot=new android.widget.FrameLayout(this);
            iconSlot.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            NavigationIconView icon=new NavigationIconView(this,i,selected?0xFFFFFFFF:MUTED);
            android.widget.FrameLayout.LayoutParams iconParams=
                new android.widget.FrameLayout.LayoutParams(dp(21),dp(21),Gravity.CENTER);
            iconSlot.addView(icon,iconParams);
            android.widget.FrameLayout.LayoutParams slotParams=
                new android.widget.FrameLayout.LayoutParams(dp(34),dp(25),Gravity.TOP|Gravity.CENTER_HORIZONTAL);
            slotParams.topMargin=dp(5);
            item.addView(iconSlot,slotParams);

            TextView label=textFixed(labels[i],10,selected?0xFFFFFFFF:MUTED,selected);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            label.setSingleLine(true);
            label.setIncludeFontPadding(false);
            android.widget.FrameLayout.LayoutParams labelParams=
                new android.widget.FrameLayout.LayoutParams(-1,dp(13),Gravity.BOTTOM);
            labelParams.bottomMargin=dp(3);
            item.addView(label,labelParams);

            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1f);
            p.setMargins(dp(1),0,dp(1),0);
            bar.addView(item,p);
            item.setOnClickListener(v->{if(key.equals("home"))showHome();else if(key.equals("math"))showChapters();else if(key.equals("progress"))showProgress();else if(key.equals("exam"))showExam();else if(key.equals("diary"))showDiary();else if(key.equals("timer"))showPomodoro();});
        }
        return bar;
    }

    @android.annotation.SuppressLint("WrongConstant")
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==PICK_TOPIC_CAMERA&&resultCode==RESULT_OK&&data!=null&&data.getExtras()!=null){try{android.graphics.Bitmap photo=(android.graphics.Bitmap)data.getExtras().get("data");if(photo!=null){String suffix=topicStorageSuffix(selectedChapter,selectedTopic);java.io.File file=new java.io.File(getFilesDir(),"topic_photo_"+Math.abs(suffix.hashCode())+".jpg");java.io.FileOutputStream out=new java.io.FileOutputStream(file);photo.compress(android.graphics.Bitmap.CompressFormat.JPEG,90,out);out.close();prefs.edit().putString("topic_photo_file_"+suffix,file.getAbsolutePath()).remove("topic_photo_"+suffix).apply();showTopic(selectedTopic);}}catch(Exception e){showAppMessage("Не удалось сохранить фото");}return;}if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();if(requestCode==WEEKLY_BACKUP_FOLDER){try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));prefs.edit().putString("weekly_backup_tree_uri",uri.toString()).apply();WeeklyBackupReceiver.schedule(this);showSettings();showAppMessage("Папка резервных копий сохранена ♡");}catch(Exception e){showAppMessage("Не удалось сохранить доступ к папке");}return;}if(requestCode==PICK_BACKGROUND){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}prefs.edit().putString("custom_background_uri",uri.toString()).apply();showSettings();showAppMessage("Фото установлено как фон ♡");}else if(requestCode==PICK_TOPIC_PHOTO){try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}prefs.edit().putString("topic_photo_"+topicStorageSuffix(selectedChapter,selectedTopic),uri.toString()).apply();showTopic(selectedTopic);}else if(requestCode==WEEKLY_IMAGE_EXPORT){try{java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null||pendingWeeklyBitmap==null)throw new Exception();pendingWeeklyBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);out.close();pendingWeeklyBitmap.recycle();pendingWeeklyBitmap=null;showAppMessage("Итоги недели сохранены ♡");}catch(Exception e){showAppMessage("Не удалось сохранить изображение");}}else if(requestCode==PDF_REPORT_EXPORT){try{writeProgressPdf(uri);showAppMessage("PDF-отчёт сохранён ♡");}catch(Exception e){showAppMessage("Не удалось создать PDF-отчёт");}}else if(requestCode==DIARY_EXPORT){try{java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null)throw new Exception();out.write(createDiaryExportText().getBytes("UTF-8"));out.close();showAppMessage("Дневник экспортирован в TXT ♡");}catch(Exception e){showAppMessage("Не удалось экспортировать дневник");}}else if(requestCode==DIARY_PDF_EXPORT){try{writeDiaryPdf(uri);showAppMessage("Дневник экспортирован в PDF ♡");}catch(Exception e){showAppMessage("Не удалось создать PDF дневника");}}else if(requestCode==BACKUP_EXPORT){try{java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null)throw new Exception();out.write(createBackupJson().getBytes("UTF-8"));out.close();showAppMessage("Резервная копия сохранена ♡");}catch(Exception e){showAppMessage("Не удалось сохранить копию");}}else if(requestCode==BACKUP_IMPORT){try{java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))>0)bytes.write(buffer,0,n);in.close();restoreBackupJson(new String(bytes.toByteArray(),"UTF-8"));showAppMessage("Данные восстановлены ♡");loadChapters();showHome();}catch(Exception e){showAppMessage("Файл копии не удалось прочитать");}}}

    private LinearLayout settingsGroup(String title) {
        LinearLayout group=new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        // Минималистичные настройки: заголовки без разделительных линий и рамок.
        group.setPadding(0,dp(10),0,dp(8));group.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        TextView heading=text(title,18,PINK,true);
        heading.setLetterSpacing(0.035f);
        group.addView(heading,lp(-1,-2,0,0,0,10));
        return group;
    }

    private String pinHash(String pin,String salt){try{javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(pin.toCharArray(),android.util.Base64.decode(salt,android.util.Base64.NO_WRAP),120000,256);byte[] hash=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();spec.clearPassword();return android.util.Base64.encodeToString(hash,android.util.Base64.NO_WRAP);}catch(Exception e){return "";}}
    private void showPinSetupDialog(){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(8),dp(20),dp(4));EditText first=new EditText(this);first.setSingleLine(true);first.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);first.setHint("Новый PIN · 4–8 цифр");body.addView(first,lp(-1,52,0,0,0,8));EditText second=new EditText(this);second.setSingleLine(true);second.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);second.setHint("Повтори PIN");body.addView(second,lp(-1,52,0,0,0,8));android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Защита приложения").setView(body).setNegativeButton("Отмена",null).setNeutralButton(prefs.getString("app_pin_hash","").isEmpty()?"":"Отключить PIN",(d,w)->prefs.edit().remove("app_pin_hash").remove("app_pin_salt").apply()).setPositiveButton("Сохранить PIN",null).create();dialog.setOnShowListener(v->{if(prefs.getString("app_pin_hash","").isEmpty())dialog.getButton(-3).setVisibility(View.GONE);dialog.getButton(-1).setOnClickListener(q->{String a=first.getText().toString(),b=second.getText().toString();if(!a.matches("[0-9]{4,8}")){first.setError("Нужно от 4 до 8 цифр");return;}if(!a.equals(b)){second.setError("PIN-коды не совпадают");return;}byte[] bytes=new byte[16];new java.security.SecureRandom().nextBytes(bytes);String salt=android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP);prefs.edit().putString("app_pin_salt",salt).putString("app_pin_hash",pinHash(a,salt)).apply();dialog.dismiss();showAppMessage("PIN-код установлен");});});dialog.show();
    }
    private void showPinLockDialog(){if(pinDialogVisible||prefs==null||prefs.getString("app_pin_hash","").isEmpty())return;pinDialogVisible=true;LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(8),dp(20),dp(4));EditText pin=new EditText(this);pin.setSingleLine(true);pin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);pin.setHint("Введи PIN-код");body.addView(pin,lp(-1,52,0,0,0,0));android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Приложение защищено").setMessage("Введи свой PIN, чтобы продолжить.").setView(body).setCancelable(false).setPositiveButton("Разблокировать",null).create();dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(q->{String salt=prefs.getString("app_pin_salt","");if(pinHash(pin.getText().toString(),salt).equals(prefs.getString("app_pin_hash",""))){pinUnlocked=true;pinDialogVisible=false;dialog.dismiss();}else{pin.setError("Неверный PIN");pin.setText("");}}));dialog.setOnCancelListener(d->pinDialogVisible=false);dialog.show();}
    private class SettingsCheckDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final boolean checked;
        private final int accent;
        SettingsCheckDrawable(boolean checked,int accent){this.checked=checked;this.accent=accent;}
        @Override public int getIntrinsicWidth(){return dp(20);}
        @Override public int getIntrinsicHeight(){return dp(20);}
        @Override public void draw(android.graphics.Canvas canvas){
            android.graphics.Rect b=getBounds();float side=Math.min(b.width(),b.height());float cx=b.exactCenterX(),cy=b.exactCenterY(),r=side*0.39f;
            paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(checked?accent:0x00FFFFFF);canvas.drawCircle(cx,cy,r,paint);
            paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1.8f,side*0.075f));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);paint.setColor(checked?accent:0xFFBDAEB6);canvas.drawCircle(cx,cy,r,paint);
            if(checked){paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(Math.max(2f,side*0.085f));paint.setColor(android.graphics.Color.WHITE);android.graphics.Path p=new android.graphics.Path();p.moveTo(cx-side*0.19f,cy);p.lineTo(cx-side*0.045f,cy+side*0.14f);p.lineTo(cx+side*0.22f,cy-side*0.17f);canvas.drawPath(p,paint);}
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }

    private void styleRoundCheckBox(CheckBox box) {
        if(box==null)return;
        android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked},new SettingsCheckDrawable(true,PINK));
        states.addState(new int[]{},new SettingsCheckDrawable(false,PINK));
        if(Build.VERSION.SDK_INT>=21)box.setButtonTintList(null);
        box.setButtonDrawable(states);
        box.setTag("round_checkbox");
        box.setCompoundDrawablePadding(dp(8));
        box.setMinHeight(dp(40));
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setIncludeFontPadding(false);
        box.setPadding(0,dp(2),dp(4),dp(2));
        box.setButtonDrawable(states);
        box.setOnTouchListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){v.animate().cancel();v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(75).start();}else if(event.getAction()==android.view.MotionEvent.ACTION_UP||event.getAction()==android.view.MotionEvent.ACTION_CANCEL){v.animate().cancel();v.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(new android.view.animation.OvershootInterpolator(1.15f)).start();}return false;});
        if(Build.VERSION.SDK_INT>=21){
            android.graphics.drawable.GradientDrawable rippleMask=new android.graphics.drawable.GradientDrawable();
            rippleMask.setColor(0xFFFFFFFF);rippleMask.setCornerRadius(dp(13));
            box.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x18C97891),null,rippleMask));
        }
    }

    private CheckBox addSettingsToggle(LinearLayout host,String label,String key,boolean defaultValue,Runnable afterChange){
        CheckBox box=new CheckBox(this);
        box.setText(translate(label));
        box.setTextSize(15);
        box.setTextColor(DARK);
        box.setMinHeight(dp(36));
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setIncludeFontPadding(false);
        box.setPadding(0,0,dp(4),0);
        styleRoundCheckBox(box);
        box.setChecked(prefs.getBoolean(key,defaultValue));
        // Плотная строка переключателя без лишних вертикальных отступов.
        host.addView(box,lp(-1,42,0,0,0,0));
        box.setOnCheckedChangeListener((button,checked)->{prefs.edit().putBoolean(key,checked).apply();if(afterChange!=null)afterChange.run();});
        return box;
    }

    private void showSettings() {
        if ((currentScreen.startsWith("settings_") || "settings".equals(currentScreen)) && activeScrollView != null) pendingSettingsScrollY=activeScrollView.getScrollY();
        if (!currentScreen.startsWith("settings_")) currentScreen="settings";
        String activeSection = currentScreen.startsWith("settings_") ? currentScreen.substring("settings_".length()) : "menu";
        boolean sectionPage = !"menu".equals(activeSection);
        String sectionTitle = "appearance".equals(activeSection) ? "Внешний вид" : "study".equals(activeSection) ? "Учёба" : "notifications_sound".equals(activeSection) ? "Уведомления и звук" : "profile_ai".equals(activeSection) ? "Профиль и ИИ" : "data_security".equals(activeSection) ? "Данные и безопасность" : "О приложении";
        base(sectionPage ? sectionTitle : "Настройки ⚙", sectionPage ? "" : "");
        LinearLayout sectionMenu = new LinearLayout(this);
        sectionMenu.setOrientation(LinearLayout.VERTICAL);
        String[][] sections = {
            {"appearance","🎨","Внешний вид"},
            {"study","📚","Учёба"},
            {"notifications_sound","🔔","Уведомления и звук"},
            {"profile_ai","👤","Профиль и ИИ"},
            {"data_security","🛡","Данные и безопасность"},
            {"about","ⓘ","О приложении"}
        };
        for (String[] section : sections) {
            Button item = button(section[1] + "   " + section[2] + "   ›", false);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setTextColor(DARK);
            item.setTextSize(18);
            item.setAllCaps(false);
            item.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            item.setElevation(0f);
            item.setPadding(dp(2),0,dp(2),0);
            sectionMenu.addView(item, lp(-1, 48, 0, 0, 0, 0));
            item.setOnClickListener(v -> { currentScreen = "settings_" + section[0]; showSettings(); });
        }
        if (!sectionPage) root.addView(sectionMenu, lp(-1, -2, 0, 0, 0, 12));

        LinearLayout profile=settingsGroup("ПРОФИЛЬ");
        profile.setTag("profile_ai");
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextSize(16);name.setTextColor(DARK);
        name.setHint(translate("Как к тебе обращаться?"));name.setText(prefs.getString("display_name",""));
        name.setPadding(dp(14),dp(8),dp(14),dp(8));name.setBackground(shape(0xFFFFF9F7,15,0xFFEADDE2));
        profile.addView(name,lp(-1,50,0,2,0,8));
        addText(profile,"ФОРМА ОБРАЩЕНИЯ",11,PINK,true);
        String gender=prefs.getString("profile_gender","female");LinearLayout genderRow=new LinearLayout(this);genderRow.setOrientation(LinearLayout.HORIZONTAL);
        Button female=button("Девушка","female".equals(gender)),male=button("Парень","male".equals(gender));
        genderRow.addView(female,new LinearLayout.LayoutParams(0,dp(48),1f));genderRow.addView(male,new LinearLayout.LayoutParams(0,dp(48),1f));
        female.setOnClickListener(v->{prefs.edit().putString("profile_gender","female").apply();showSettings();});
        male.setOnClickListener(v->{prefs.edit().putString("profile_gender","male").apply();showSettings();});
        profile.addView(genderRow,lp(-1,48,0,4,0,8));
        addText(profile,"ОБРАЩЕНИЕ",11,PINK,true);
        String addressStyle=prefs.getString("address_style","you");LinearLayout addressRow=new LinearLayout(this);Button informal=button("На ты",!"formal".equals(addressStyle)),formal=button("На вы","formal".equals(addressStyle));addressRow.addView(informal,new LinearLayout.LayoutParams(0,dp(44),1f));addressRow.addView(formal,new LinearLayout.LayoutParams(0,dp(44),1f));informal.setOnClickListener(v->{prefs.edit().putString("address_style","you").apply();showSettings();});formal.setOnClickListener(v->{prefs.edit().putString("address_style","formal").apply();showSettings();});profile.addView(addressRow,lp(-1,44,0,4,0,8));addText(profile,"АВАТАР",11,PINK,true);android.widget.GridLayout avatarGrid=new android.widget.GridLayout(this);avatarGrid.setColumnCount(6);String[] profileAvatars={"🌸","🐰","🦋","🐱","🌷","⭐"};String currentAvatar=prefs.getString("onboarding_avatar","🌸");for(String avatarChoice:profileAvatars){TextView av=text((currentAvatar.equals(avatarChoice)?"✓\n":"")+avatarChoice,21,DARK,true);av.setGravity(Gravity.CENTER);av.setBackground(shape(currentAvatar.equals(avatarChoice)?PALE:0x00FFFFFF,12,currentAvatar.equals(avatarChoice)?PINK:0));android.widget.GridLayout.LayoutParams avp=new android.widget.GridLayout.LayoutParams();avp.width=0;avp.height=dp(48);avp.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);avp.setMargins(dp(2),dp(2),dp(2),dp(2));avatarGrid.addView(av,avp);av.setOnClickListener(v->{prefs.edit().putString("onboarding_avatar",avatarChoice).apply();showSettings();});}TextView moreAvatar=text("＋",21,PINK,true);moreAvatar.setGravity(Gravity.CENTER);moreAvatar.setBackground(shape(0xFFFFF0F4,12,PINK));android.widget.GridLayout.LayoutParams moreAvatarLp=new android.widget.GridLayout.LayoutParams();moreAvatarLp.width=0;moreAvatarLp.height=dp(48);moreAvatarLp.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);moreAvatarLp.setMargins(dp(2),dp(2),dp(2),dp(2));avatarGrid.addView(moreAvatar,moreAvatarLp);moreAvatar.setOnClickListener(v->{String[] extra={"⭐","📚","🌻","🧸","🍓","💗","🦄","🐼","🐨","🍀","🌙","✨","🎀","🐥","🪻","🌈","🍒","🧁","🪴","💜"};android.widget.GridLayout choices=new android.widget.GridLayout(this);choices.setColumnCount(4);android.app.AlertDialog picker=new android.app.AlertDialog.Builder(this).setTitle("Другие аватары").setView(choices).setNegativeButton("Закрыть",null).create();for(String emoji:extra){TextView option=text(emoji,25,DARK,false);option.setGravity(Gravity.CENTER);option.setBackground(shape(PALE,14,0));android.widget.GridLayout.LayoutParams ep=new android.widget.GridLayout.LayoutParams();ep.width=dp(54);ep.height=dp(54);ep.setMargins(dp(3),dp(3),dp(3),dp(3));choices.addView(option,ep);option.setOnClickListener(q->{prefs.edit().putString("onboarding_avatar",emoji).apply();picker.dismiss();showSettings();});}android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.setPadding(dp(8),dp(4),dp(8),dp(4));scroll.addView(choices);picker.setView(scroll);picker.show();});profile.addView(avatarGrid,lp(-1,52,0,3,0,8));
        addSettingsToggle(profile,"Показывать талисман и эмодзи","mascot_enabled",true,null);
        Button saveName=button("Сохранить имя",true);saveName.setOnClickListener(v->{String value=name.getText().toString().trim();if(value.isEmpty()){name.setError("Введи имя");return;}prefs.edit().putString("display_name",value).apply();showAppMessage("Имя сохранено ♡");});
        profile.addView(saveName,lp(-1,48,0,0,0,0));root.addView(profile,lp(-1,-2,0,0,0,4));
        
        LinearLayout appearance=settingsGroup("ВНЕШНИЙ ВИД");
        appearance.setTag("appearance");
        addText(appearance,"Тема приложения",14,DARK,true);
        
        String[] names={"Лавандовая","Сиреневая","Мятная","Ночная","Белая","Кофейная","Золотая","Люкс","Небесная","Персиковая","Красная","LV"};
        String[] keys={"pink","lavender","mint","night","white","brown","gold","luxury","sky","peach","red","lv"};
        int[] colors={0xFF8070A8,0xFF8979B8,0xFF659B86,0xFF302735,0xFFFFFFFF,0xFF8B6046,0xFFC49318,0xFF30251F,0xFF5D91B5,0xFFD88968,0xFFB32635,0xFFD9C19A};
        LinearLayout preview = card();
        preview.setPadding(dp(14), dp(12), dp(14), dp(12));
        preview.setBackground(shape(isDarkAppearance() ? 0xFF302735 : 0xFFFFF0F4, 18, PINK));
        addText(preview, "ПРЕДПРОСМОТР ТЕМЫ", 12, PINK, true);
        TextView previewText = text("Твой прогресс  ·  65%", 16, DARK, true);
        preview.addView(previewText, lp(-1, 30, 0, 4, 0, 5));
        ProgressBar previewBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        previewBar.setMax(100); previewBar.setProgress(65);
        if (Build.VERSION.SDK_INT >= 21) previewBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
        preview.addView(previewBar, lp(-1, 8, 0, 0, 0, 4));
        Button previewButton = button("Продолжить", true);
        preview.addView(previewButton, lp(-1, 42, 0, 5, 0, 0));
        appearance.addView(preview, lp(-1, -2, 0, 7, 0, 8));
        int paletteColumns = getResources().getConfiguration().screenWidthDp < 380 ? 2 : 3;
        android.widget.GridLayout palette=new android.widget.GridLayout(this);palette.setColumnCount(paletteColumns);
        String activeTheme=prefs.getString("app_theme","pink");
        for(int i=0;i<keys.length;i++){final String key=keys[i];final int color=colors[i];final String label=names[i];LinearLayout option=new LinearLayout(this);option.setGravity(Gravity.CENTER_VERTICAL);option.setPadding(dp(8),dp(7),dp(5),dp(7));option.setBackground(shape(activeTheme.equals(key)?blendColors(PALE,BG,0.18f):0x00FFFFFF,13,activeTheme.equals(key)?PINK:blendColors(PALE,BG,0.15f)));
            TextView swatch=text(activeTheme.equals(key)?"✓":"",12,onAccentTextColor(color),true);swatch.setGravity(Gravity.CENTER);GradientDrawable swatchBg=new GradientDrawable();swatchBg.setColor(color);swatchBg.setCornerRadius(dp(30));if(activeTheme.equals(key))swatchBg.setStroke(dp(2),PINK);swatch.setBackground(swatchBg);option.addView(swatch,lp(28,28,0,0,7,0));TextView labelView=text(label,11,DARK,true);option.addView(labelView,new LinearLayout.LayoutParams(0,dp(30),1f));
            android.widget.GridLayout.LayoutParams gp=new android.widget.GridLayout.LayoutParams();gp.width=0;gp.height=dp(48);gp.columnSpec=android.widget.GridLayout.spec(i%paletteColumns,1f);gp.setMargins(dp(2),dp(3),dp(2),dp(3));palette.addView(option,gp);
            option.setOnClickListener(v->{prefs.edit().putString("app_theme",key).remove("app_accent_color").apply();applySavedTheme();showSettings();});
        }
        appearance.addView(palette,lp(-1,-2,0,7,0,8));
        LinearLayout fontGroup=settingsGroup("ШРИФТ ПРИЛОЖЕНИЯ");
        
        String[] fontNames={"Современный","Округлый","С засечками","Моноширинный","Узкий","Средний"};
        String[] fontKeys={"sans","rounded","serif","mono","condensed","medium"};
        String[] fontFamilies={"sans-serif","sans-serif-rounded","serif","monospace","sans-serif-condensed","sans-serif-medium"};
        String activeFont=prefs.getString("app_font","sans");
        android.widget.GridLayout fontGrid=new android.widget.GridLayout(this);fontGrid.setColumnCount(2);
        for(int i=0;i<fontKeys.length;i++){final String key=fontKeys[i],family=fontFamilies[i];Button choice=button((activeFont.equals(key)?"✓  ":"")+fontNames[i],activeFont.equals(key));choice.setTypeface(Typeface.create(family,Typeface.BOLD));if(activeFont.equals(key)){GradientDrawable selectedFontBg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{adjustColor(PINK,0.92f),adjustColor(PINK,0.78f)});selectedFontBg.setCornerRadius(dp(14));choice.setBackground(selectedFontBg);}else choice.setBackground(shape(isDarkAppearance()?blendColors(PALE,BG,0.18f):0xCCFFFFFF,13,blendColors(PINK,BG,isDarkAppearance()?0.42f:0.68f)));android.widget.GridLayout.LayoutParams fp=new android.widget.GridLayout.LayoutParams();fp.width=0;fp.height=dp(44);fp.columnSpec=android.widget.GridLayout.spec(i%2,1f);fp.setMargins(dp(3),dp(3),dp(3),dp(3));fontGrid.addView(choice,fp);choice.setOnClickListener(v->{prefs.edit().putString("app_font",key).apply();showSettings();});}
        fontGroup.addView(fontGrid,lp(-1,-2,0,4,0,0));appearance.addView(fontGroup,lp(-1,-2,0,4,0,6));
        LinearLayout fontSizeGroup=settingsGroup("РАЗМЕР ТЕКСТА");
        fontSizeGroup.setTag("appearance");
        
        float[] textScales={0.9f,1.0f,1.15f,1.3f};
        String[] textScaleNames={"Маленький","Обычный","Крупный","Очень крупный"};
        float activeScale=prefs.getFloat("app_text_scale",1.0f);
        // Единый сегментированный переключатель: четыре значения в общей плашке.
        LinearLayout scaleStrip=new LinearLayout(this);
        scaleStrip.setOrientation(LinearLayout.HORIZONTAL);
        scaleStrip.setPadding(dp(3),dp(3),dp(3),dp(3));
        scaleStrip.setBackground(shape(isDarkAppearance()?0xFF292530:PALE,12,blendColors(PINK,BG,isDarkAppearance()?0.42f:0.68f)));
        for(int i=0;i<textScales.length;i++){
            final float scale=textScales[i];
            boolean selected=Math.abs(activeScale-scale)<0.01f;
            TextView choice=text((selected?"✓ ":"")+textScaleNames[i],11,selected?0xFFFFFFFF:PINK,true);
            choice.setGravity(Gravity.CENTER);
            choice.setMaxLines(1);
            choice.setSingleLine(true);
            choice.setIncludeFontPadding(false);
            choice.setPadding(dp(2),dp(9),dp(2),dp(9));
            if(selected){GradientDrawable activeSegment=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{adjustColor(PINK,1.12f),adjustColor(PINK,0.78f)});activeSegment.setCornerRadius(dp(10));activeSegment.setStroke(dp(1),adjustColor(PINK,0.78f));choice.setBackground(activeSegment);}else choice.setBackground(shape(isDarkAppearance()?0xFF292530:0x00FFFFFF,10,0));
            LinearLayout.LayoutParams scaleLp=new LinearLayout.LayoutParams(0,dp(42),1f);
            scaleLp.setMargins(dp(1),0,dp(1),0);
            scaleStrip.addView(choice,scaleLp);
            choice.setOnClickListener(v->{prefs.edit().putFloat("app_text_scale",scale).apply();showSettings();});
        }
        fontSizeGroup.addView(scaleStrip,lp(-1,-2,0,5,0,0));
        appearance.addView(fontSizeGroup,lp(-1,-2,0,4,0,6));
        LinearLayout appearanceExtras=settingsGroup("ДОПОЛНИТЕЛЬНАЯ НАСТРОЙКА ВИДА");appearanceExtras.setTag("appearance");
        addSettingsToggle(appearanceExtras,"Автоматическая тёмная тема по расписанию","scheduled_dark_enabled",false,()->{applySavedTheme();updateSystemBarIcons();showSettings();});
        Button darkStart=button("Начало ночной темы · "+prefs.getString("scheduled_dark_start","21:00"),false),darkEnd=button("Конец ночной темы · "+prefs.getString("scheduled_dark_end","07:00"),false);darkStart.setOnClickListener(v->chooseClockPreference("scheduled_dark_start","Начало тёмной темы","21:00"));darkEnd.setOnClickListener(v->chooseClockPreference("scheduled_dark_end","Конец тёмной темы","07:00"));appearanceExtras.addView(darkStart,lp(-1,42,0,3,0,2));appearanceExtras.addView(darkEnd,lp(-1,42,0,2,0,5));
        addText(appearanceExtras,"СКРУГЛЕНИЕ УГЛОВ",11,PINK,true);LinearLayout corners=new LinearLayout(this);String corner=prefs.getString("corner_style","round");String[] cornerKeys={"sharp","normal","round"},cornerNames={"Острые","Обычные","Круглые"};for(int i=0;i<3;i++){final String key=cornerKeys[i];Button b=button(cornerNames[i],corner.equals(key));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(42),1f);cp.setMargins(dp(2),dp(3),dp(2),dp(3));corners.addView(b,cp);b.setOnClickListener(v->{prefs.edit().putString("corner_style",key).apply();showSettings();});}appearanceExtras.addView(corners,lp(-1,46,0,2,0,7));
        Button orderHome=button("↕  Изменить порядок блоков",false);orderHome.setOnClickListener(v->showHomeBlockOrderDialog());appearanceExtras.addView(orderHome,lp(-1,44,0,2,0,7));
        addText(appearanceExtras,"БЛОКИ ГЛАВНОГО ЭКРАНА",11,PINK,true);addSettingsToggle(appearanceExtras,"Настроение","home_show_mood",true,null);addSettingsToggle(appearanceExtras,"Статистика","home_show_stats",true,null);addSettingsToggle(appearanceExtras,"Недельная цель","home_show_weekly",true,null);addSettingsToggle(appearanceExtras,"Карточка ИИ","home_show_ai",true,null);addSettingsToggle(appearanceExtras,"Плитки эмоций","home_show_emotions",true,null);addSettingsToggle(appearanceExtras,"Напоминания","home_show_reminders",true,null);root.addView(appearanceExtras,lp(-1,-2,0,0,0,4));
        LinearLayout appearanceActions=card();
        appearanceActions.setPadding(dp(12),dp(5),dp(12),dp(5));
        Button systemTheme=button("Цвета и тёмная тема — как в системе   ›","system".equals(activeTheme));systemTheme.setCompoundDrawablesRelative(settingsIcon(0),null,null,null);systemTheme.setCompoundDrawablePadding(dp(10));systemTheme.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);systemTheme.setBackground(shape(isDarkAppearance()?0xFF25212B:0xFFFFFFFF,12,0));systemTheme.setOnClickListener(v->{prefs.edit().putString("app_theme","system").remove("app_accent_color").apply();applySavedTheme();showSettings();});appearanceActions.addView(systemTheme,lp(-1,42,0,2,0,2));
        View actionDivider=new View(this);actionDivider.setBackgroundColor(isDarkAppearance()?0xFF49404B:0xFFECE5E9);appearanceActions.addView(actionDivider,lp(-1,1,0,3,0,3));
        Button choosePhoto=button("Выбрать фото фона   ›",false);choosePhoto.setCompoundDrawablesRelative(settingsIcon(1),null,null,null);choosePhoto.setCompoundDrawablePadding(dp(10));choosePhoto.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);choosePhoto.setBackground(shape(isDarkAppearance()?0xFF25212B:0xFFFFFFFF,12,0));choosePhoto.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("image/*");pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,PICK_BACKGROUND);});
        appearanceActions.addView(choosePhoto,lp(-1,42,0,2,0,2));
        View actionDivider2=new View(this);actionDivider2.setBackgroundColor(isDarkAppearance()?0xFF49404B:0xFFECE5E9);appearanceActions.addView(actionDivider2,lp(-1,1,0,3,0,3));
        Button clearPhoto=button("Убрать фото фона   ›",false);clearPhoto.setCompoundDrawablesRelative(settingsIcon(2),null,null,null);clearPhoto.setCompoundDrawablePadding(dp(10));clearPhoto.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);clearPhoto.setBackground(shape(isDarkAppearance()?0xFF25212B:0xFFFFFFFF,12,0));clearPhoto.setOnClickListener(v->{prefs.edit().remove("custom_background_uri").apply();showSettings();});appearanceActions.addView(clearPhoto,lp(-1,42,0,2,0,2));
        appearance.addView(appearanceActions,lp(-1,-2,0,5,0,0));
        root.addView(appearance,lp(-1,-2,0,0,0,4));

        LinearLayout language=settingsGroup("ЯЗЫК ПРИЛОЖЕНИЯ");
        language.setTag("appearance");
        
        String[] languageNames={"Русский","English","Azərbaycan dili"};String[] languageKeys={"ru","en","az"};String selectedLanguage=prefs.getString("app_language","ru");
        LinearLayout languageCard=card();languageCard.setPadding(dp(12),dp(4),dp(12),dp(4));
        for(int i=0;i<languageKeys.length;i++){final String key=languageKeys[i];boolean selected=selectedLanguage.equals(key);Button choice=button((selected?"✓  ":"")+"   "+languageNames[i]+"   "+(selected?"":"›"),selected);choice.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);choice.setBackground(shape(isDarkAppearance()?(selected?0xFF342E39:0xFF25212B):(selected?PALE:0xFFFFFFFF),12,0));languageCard.addView(choice,lp(-1,42,0,2,0,2));choice.setOnClickListener(v->{prefs.edit().putString("app_language",key).apply();showSettings();});if(i<languageKeys.length-1){View d=new View(this);d.setBackgroundColor(isDarkAppearance()?0xFF49404B:0xFFECE5E9);languageCard.addView(d,lp(-1,1,0,3,0,3));}}
        language.addView(languageCard,lp(-1,-2,0,5,0,0));
        root.addView(language,lp(-1,-2,0,0,0,4));

        LinearLayout progress=settingsGroup("ОТОБРАЖЕНИЕ ПРОГРЕССА");
        progress.setTag("appearance");
        
        String mode=prefs.getString("progress_display","percent");LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        Button percent=button("С процентами",!"slider".equals(mode));Button slider=button("Полосы прогресса","slider".equals(mode));
        LinearLayout.LayoutParams m1=new LinearLayout.LayoutParams(0,dp(48),1f);m1.setMargins(dp(2),dp(7),dp(3),0);modes.addView(percent,m1);
        LinearLayout.LayoutParams m2=new LinearLayout.LayoutParams(0,dp(48),1f);m2.setMargins(dp(3),dp(7),dp(2),0);modes.addView(slider,m2);
        percent.setOnClickListener(v->{prefs.edit().putString("progress_display","percent").apply();showSettings();});slider.setOnClickListener(v->{prefs.edit().putString("progress_display","slider").apply();showSettings();});
        progress.addView(modes,lp(-1,54,0,0,0,0));root.addView(progress,lp(-1,-2,0,0,0,4));

        LinearLayout weekly=settingsGroup("НЕДЕЛЬНАЯ ЦЕЛЬ");weekly.setTag("study");EditText weeklyInput=new EditText(this);weeklyInput.setSingleLine(true);weeklyInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);weeklyInput.setText(String.valueOf(prefs.getInt("weekly_goal_minutes",300)));weeklyInput.setHint("Например, 300");weeklyInput.setTextColor(DARK);weeklyInput.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));weekly.addView(weeklyInput,lp(-1,48,0,6,0,7));Button saveWeekly=button("Сохранить цель недели",true);saveWeekly.setOnClickListener(v->{try{int value=Integer.parseInt(weeklyInput.getText().toString().trim());if(value<30||value>10080)throw new Exception();prefs.edit().putInt("weekly_goal_minutes",value).apply();showAppMessage("Недельная цель обновлена ♡");}catch(Exception e){weeklyInput.setError("Укажи от 30 до 10080 минут");}});weekly.addView(saveWeekly,lp(-1,43,0,0,0,0));root.addView(weekly,lp(-1,-2,0,0,0,4));
        LinearLayout testOptions=settingsGroup("НАСТРОЙКИ ТЕСТОВ");testOptions.setTag("study");addSettingsToggle(testOptions,"Показывать правильный ответ сразу","show_correct_immediately",true,null);int reviewDays=prefs.getInt("weakness_review_interval_days",1);int[] reviewIntervals={1,3,7,14};int reviewIdx=0;for(int ri=0;ri<reviewIntervals.length;ri++)if(reviewIntervals[ri]==reviewDays)reviewIdx=ri;final int[] reviewSelected={reviewIdx};String[] reviewLabels={"1 день","3 дня","7 дней","14 дней"};Button reviewInterval=button("Интервал повторения ошибок · "+reviewLabels[reviewIdx],false);reviewInterval.setOnClickListener(v->{reviewSelected[0]=(reviewSelected[0]+1)%reviewIntervals.length;prefs.edit().putInt("weakness_review_interval_days",reviewIntervals[reviewSelected[0]]).apply();reviewInterval.setText("Интервал повторения ошибок · "+reviewLabels[reviewSelected[0]]);});testOptions.addView(reviewInterval,lp(-1,44,0,4,0,0));root.addView(testOptions,lp(-1,-2,0,0,0,4));

        LinearLayout reminders=settingsGroup("НАПОМИНАНИЯ");
        reminders.setTag("notifications_sound");
        CheckBox enabled=new CheckBox(this);styleRoundCheckBox(enabled);enabled.setText(translate("Включить напоминания о занятиях"));enabled.setTextSize(14);enabled.setTextColor(DARK);enabled.setChecked(prefs.getBoolean("notifications_enabled",true));reminders.addView(enabled,lp(-1,48,0,0,0,4));
        enabled.setOnCheckedChangeListener((buttonView,checked)->{prefs.edit().putBoolean("notifications_enabled",checked).apply();scheduleReminders(this);if(checked&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},20);});
        CheckBox quietToggle=new CheckBox(this);quietToggle.setText("Тихие часы для уведомлений");quietToggle.setTextColor(DARK);styleRoundCheckBox(quietToggle);quietToggle.setChecked(prefs.getBoolean("quiet_hours_enabled",false));reminders.addView(quietToggle,lp(-1, 48,0,4,0,0));quietToggle.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("quiet_hours_enabled",on).apply();scheduleReminders(this);});
        Button quietStart=button("Начало тишины · "+prefs.getString("quiet_hours_start","22:00"),false);Button quietEnd=button("Конец тишины · "+prefs.getString("quiet_hours_end","08:00"),false);quietStart.setOnClickListener(v->chooseQuietTime("quiet_hours_start"));quietEnd.setOnClickListener(v->chooseQuietTime("quiet_hours_end"));reminders.addView(quietStart,lp(-1,48,0,3,0,3));reminders.addView(quietEnd,lp(-1, 48,0,0,0,5));
        Button reminderSettings=button("Настроить дни и время",false);reminderSettings.setOnClickListener(v->showReminderSettings());reminders.addView(reminderSettings,lp(-1, 48,0,0,0,0));addSettingsToggle(reminders,"Недельный отчёт","weekly_report_enabled",true,()->WeeklyReportReceiver.schedule(this));int repeat=prefs.getInt("reminder_repeat_minutes",0);int[] repeats={0,5,10,15,30};String[] repeatNames={"Не повторять","5 минут","10 минут","15 минут","30 минут"};int ri=0;for(int k=0;k<repeats.length;k++)if(repeats[k]==repeat)ri=k;final int[] repeatIndex={ri};Button repeatButton=button("Повтор напоминания · "+repeatNames[ri],false);repeatButton.setOnClickListener(v->{repeatIndex[0]=(repeatIndex[0]+1)%repeats.length;prefs.edit().putInt("reminder_repeat_minutes",repeats[repeatIndex[0]]).apply();repeatButton.setText("Повтор напоминания · "+repeatNames[repeatIndex[0]]);});reminders.addView(repeatButton,lp(-1,42,0,3,0,3));String tone=prefs.getString("reminder_tone","soft");String[] tones={"soft","neutral","strict"},toneNames={"Мягкий","Нейтральный","Строгий"};int ti=0;for(int k=0;k<tones.length;k++)if(tones[k].equals(tone))ti=k;final int[] toneIndex={ti};Button toneButton=button("Тон напоминаний · "+toneNames[ti],false);toneButton.setOnClickListener(v->{toneIndex[0]=(toneIndex[0]+1)%tones.length;prefs.edit().putString("reminder_tone",tones[toneIndex[0]]).apply();toneButton.setText("Тон напоминаний · "+toneNames[toneIndex[0]]);});reminders.addView(toneButton,lp(-1,42,0,2,0,3));addSettingsToggle(reminders,"Напоминания о письмах себе","future_letters_enabled",true,()->rescheduleFutureLetterReminders(this));addSettingsToggle(reminders,"Вечернее напоминание об отдыхе","evening_rest_enabled",false,()->scheduleReminders(this));root.addView(reminders,lp(-1,-2,0,0,0,4));

        LinearLayout focus=settingsGroup("РЕЖИМ ФОКУСА");
        focus.setTag("notifications_sound");
        CheckBox focusToggle=new CheckBox(this);focusToggle.setText("Не отвлекать");focusToggle.setTextColor(DARK);styleRoundCheckBox(focusToggle);focusToggle.setChecked(prefs.getBoolean("do_not_disturb",false));focus.addView(focusToggle,lp(-1, 42,0,0,0,0));focusToggle.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("do_not_disturb",on).apply();scheduleReminders(this);});root.addView(focus,lp(-1,-2,0,0,0,4));
        LinearLayout feedbackSettings=settingsGroup("ЗВУК И ОТКЛИК");feedbackSettings.setTag("notifications_sound");addSettingsToggle(feedbackSettings,"Вибрация при действиях","haptics_enabled",true,null);addSettingsToggle(feedbackSettings,"Звук завершения темы","topic_sound_enabled",true,null);addSettingsToggle(feedbackSettings,"Звук окончания Pomodoro","timer_sound_enabled",true,null);addSettingsToggle(feedbackSettings,"Звук правильного ответа","correct_answer_sound_enabled",true,null);addSettingsToggle(feedbackSettings,"Уведомление об окончании таймера","timer_notification_enabled",true,null);addSettingsToggle(feedbackSettings,"Конфетти и праздничные эффекты","celebrations_enabled",true,null);
        addText(feedbackSettings,"ГРОМКОСТЬ СИГНАЛОВ",11,PINK,true);android.widget.SeekBar soundVolume=new android.widget.SeekBar(this);soundVolume.setMax(100);soundVolume.setProgress(prefs.getInt("completion_sound_volume",55));if(Build.VERSION.SDK_INT>=21)soundVolume.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));feedbackSettings.addView(soundVolume,lp(-1,38,0,0,0,5));soundVolume.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(android.widget.SeekBar b,int value,boolean user){if(user)prefs.edit().putInt("completion_sound_volume",value).apply();}public void onStartTrackingTouch(android.widget.SeekBar b){}public void onStopTrackingTouch(android.widget.SeekBar b){}});
        addSettingsToggle(feedbackSettings,"Автовключение выбранной атмосферы при открытии","ambience_autostart",false,null);
        addText(feedbackSettings,"АВТООСТАНОВКА АТМОСФЕРЫ",11,PINK,true);int[] autoStops={0,15,30,60,120};String[] autoLabels={"Не останавливать","15 минут","30 минут","1 час","2 часа"};int currentStop=prefs.getInt("ambience_auto_stop",0),stopIndex=0;for(int i=0;i<autoStops.length;i++)if(autoStops[i]==currentStop)stopIndex=i;final int[] selectedStop={stopIndex};Button autoStop=button("Остановить через · "+autoLabels[stopIndex],false);autoStop.setOnClickListener(v->{selectedStop[0]=(selectedStop[0]+1)%autoStops.length;prefs.edit().putInt("ambience_auto_stop",autoStops[selectedStop[0]]).apply();autoStop.setText("Остановить через · "+autoLabels[selectedStop[0]]);});feedbackSettings.addView(autoStop,lp(-1,44,0,3,0,0));root.addView(feedbackSettings,lp(-1,-2,0,0,0,4));

        LinearLayout backup=settingsGroup("РЕЗЕРВНАЯ КОПИЯ");
        backup.setTag("data_security");
        
        Button export=button("Экспортировать копию",true);export.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"study_with_love_backup.json");startActivityForResult(i,BACKUP_EXPORT);});backup.addView(export,lp(-1, 48,0,7,0,7));
        LinearLayout diaryExports=new LinearLayout(this);diaryExports.setOrientation(LinearLayout.HORIZONTAL);diaryExports.setGravity(Gravity.CENTER_VERTICAL);
        Button diaryTxt=button("Дневник · TXT",false);diaryTxt.setTextSize(12);diaryTxt.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/plain");i.putExtra(Intent.EXTRA_TITLE,"study_with_love_diary.txt");startActivityForResult(i,DIARY_EXPORT);});
        Button diaryPdf=button("Дневник · PDF",false);diaryPdf.setTextSize(12);diaryPdf.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"study_with_love_diary.pdf");startActivityForResult(i,DIARY_PDF_EXPORT);});
        diaryExports.addView(diaryTxt,new LinearLayout.LayoutParams(0,dp(44),1f));diaryExports.addView(new View(this),lp(8,1,0,0,0,0));diaryExports.addView(diaryPdf,new LinearLayout.LayoutParams(0,dp(44),1f));backup.addView(diaryExports,lp(-1,44,0,0,0,7));Button resetProgress=button("Сбросить только учебный прогресс",false);resetProgress.setTextColor(0xFFB32635);resetProgress.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("Сбросить прогресс?").setMessage("Будут удалены учебные отметки, серии и статистика. Дневник, письма, настройки и резервные копии останутся.").setNegativeButton("Отмена",null).setPositiveButton("Сбросить",(d,w)->{SharedPreferences.Editor e=prefs.edit();for(String key:prefs.getAll().keySet())if((key.startsWith("progress_")&&!key.equals("progress_display")&&!key.equals("progress_month_offset"))||key.startsWith("done_")||key.startsWith("topic_completed_at_")||key.startsWith("focus_minutes_")||key.startsWith("focus_remainder_ms_"))e.remove(key);e.remove("done_count").remove("study_days").remove("last_study_date").remove("current_streak").remove("longest_streak").remove("focus_total_minutes").apply();loadChapters();showHome();showAppMessage("Учебный прогресс сброшен. Дневник сохранён.");}).show());backup.addView(resetProgress,lp(-1,46,0,2,0,5));Button restore=button("Восстановить из копии",false);restore.setTextColor(0xFFB32635);restore.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setTitle("Восстановить данные?")
            .setMessage("Текущие данные приложения будут заменены содержимым выбранной копии. Перед продолжением убедись, что у тебя есть актуальная резервная копия.")
            .setNegativeButton("Отмена",null).setPositiveButton("Продолжить",(dialog,which)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,BACKUP_IMPORT);}).show());backup.addView(restore,lp(-1,48,0,0,0,0));addSettingsToggle(backup,"Автоматическая копия каждое воскресенье","weekly_backup_enabled",false,()->WeeklyBackupReceiver.schedule(this));Button chooseBackupFolder=button(prefs.getString("weekly_backup_tree_uri","").isEmpty()?"Выбрать папку для копий":"Изменить папку для копий",false);chooseBackupFolder.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,WEEKLY_BACKUP_FOLDER);});backup.addView(chooseBackupFolder,lp(-1,44,0,3,0,5));long backupAt=prefs.getLong("weekly_backup_last_success",0L);addSettingsToggle(backup,"Скрывать содержимое в списке недавних приложений","hide_recent_preview",false,()->{if(prefs.getBoolean("hide_recent_preview",false))getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);});Button pinButton=button(prefs.getString("app_pin_hash","").isEmpty()?"Установить PIN-код":"Изменить или отключить PIN-код",false);pinButton.setOnClickListener(v->showPinSetupDialog());backup.addView(pinButton,lp(-1,46,0,6,0,0));root.addView(backup,lp(-1,-2,0,0,0,4));

        LinearLayout updates=settingsGroup("ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ");
        updates.setTag("about");
        CheckBox reduceMotion = new CheckBox(this);
        reduceMotion.setText("Меньше анимации");
        reduceMotion.setTextSize(14); reduceMotion.setTextColor(DARK);
        styleRoundCheckBox(reduceMotion);
        reduceMotion.setChecked(prefs.getBoolean("reduce_motion", false));
        updates.addView(reduceMotion, lp(-1, 42, 0, 1, 0, 3));
        reduceMotion.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean("reduce_motion", checked).apply());
        String installedVersion="—";
        try { installedVersion=getPackageManager().getPackageInfo(getPackageName(),0).versionName; } catch(Exception ignored) {}
        
        
        TextView versionLabel=text("Версия приложения  ·  "+installedVersion,13,MUTED,false);
        versionLabel.setGravity(Gravity.CENTER_VERTICAL);
        updates.addView(versionLabel,lp(-1,34,0,5,0,2));
        Button checkUpdates=button("Проверить обновления",false);
        checkUpdates.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        checkUpdates.setTextColor(DARK);
        checkUpdates.setElevation(0f);
        checkUpdates.setOnClickListener(v->AppUpdater.checkAndOffer(this));
        updates.addView(checkUpdates,lp(-1,48,0,7,0,0));
        root.addView(updates,lp(-1,-2,0,0,0,4));

        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof String) child.setVisibility(sectionPage && activeSection.equals(tag) ? View.VISIBLE : View.GONE);
        }
        // Настройки: единый плоский стиль без теней у карточек, плиток и вложенных элементов.
        removeSettingsShadows(root);

        // Увеличиваем читаемость текста во всех подразделах настроек.
        enlargeSettingsText(root);
        backHome();
    }

    private void enlargeSettingsText(View view) {
        if(view instanceof TextView){
            TextView label=(TextView)view;
            float size=label.getTextSize()/getResources().getDisplayMetrics().scaledDensity;
            label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,Math.min(38f,Math.max(12f,size+1.5f)));
        }
        if(view instanceof android.view.ViewGroup){
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)enlargeSettingsText(group.getChildAt(i));
        }
    }

    private void removeSettingsShadows(View view) {
        if(view==null)return;
        view.setElevation(0f);
        view.setTranslationZ(0f);
        if(view instanceof android.view.ViewGroup){
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)removeSettingsShadows(group.getChildAt(i));
        }
    }

    private void updateSystemBarIcons() {
        if (Build.VERSION.SDK_INT >= 23 && getWindow() != null) {
            String t=prefs==null?"pink":prefs.getString("app_theme","pink");
            boolean darkIcons=!("night".equals(t)||"luxury".equals(t)||("system".equals(t)&&(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES));
            if (Build.VERSION.SDK_INT >= 30) {
                android.view.WindowInsetsController c=getWindow().getInsetsController();
                if(c!=null){int flags=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS; c.setSystemBarsAppearance(darkIcons?flags:0,flags);}
            } else {
                getWindow().getDecorView().setSystemUiVisibility(darkIcons?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0);
            }
        }
    }

    private int parseClockMinutes(String value,int fallback){try{String[] p=value.split(":");return Math.max(0,Math.min(1439,Integer.parseInt(p[0])*60+Integer.parseInt(p[1])));}catch(Exception e){return fallback;}}
    private void chooseClockPreference(String key,String title,String fallback){String value=prefs.getString(key,fallback);int hour=21,minute=0;try{String[] p=value.split(":");hour=Integer.parseInt(p[0]);minute=Integer.parseInt(p[1]);}catch(Exception ignored){}new TimePickerDialog(this,(view,h,m)->{prefs.edit().putString(key,String.format(Locale.US,"%02d:%02d",h,m)).apply();applySavedTheme();updateSystemBarIcons();showSettings();},hour,minute,true).show();}
    private void applySavedTheme() {
        String theme = prefs == null ? "pink" : prefs.getString("app_theme", "pink");
        if(prefs!=null && prefs.getBoolean("scheduled_dark_enabled",false)){Calendar now=Calendar.getInstance(AppTime.zone());int current=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);int start=parseClockMinutes(prefs.getString("scheduled_dark_start","21:00"),1260),end=parseClockMinutes(prefs.getString("scheduled_dark_end","07:00"),420);if(start<=end?(current>=start&&current<end):(current>=start||current<end))theme="night";}
        if ("system".equals(theme)) {
            boolean darkMode=(getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
            if(Build.VERSION.SDK_INT>=31){try{int accent=getResources().getIdentifier("system_accent1_600","color","android"),bg=getResources().getIdentifier(darkMode?"system_neutral1_900":"system_neutral1_10","color","android"),surface=getResources().getIdentifier(darkMode?"system_neutral1_800":"system_neutral1_50","color","android"),fg=getResources().getIdentifier(darkMode?"system_neutral1_50":"system_neutral1_900","color","android");PINK=getResources().getColor(accent,getTheme());BG=getResources().getColor(bg,getTheme());PALE=getResources().getColor(surface,getTheme());DARK=getResources().getColor(fg,getTheme());MUTED=darkMode?0xFFBDB5C1:0xFF827A83;}catch(Exception e){BG=darkMode?0xFF17151D:0xFFFFF9F7;PINK=0xFFC97891;PALE=darkMode?0xFF302735:0xFFFCEEF2;DARK=darkMode?0xFFF4EAF0:0xFF3F3439;MUTED=darkMode?0xFFC8BDC8:0xFF75666D;}}
            else if(darkMode){BG=0xFF17151D;PINK=0xFFE39AB5;PALE=0xFF302735;DARK=0xFFF4EAF0;MUTED=0xFFC8BDC8;}
            else{BG=0xFFFFF9F7;PINK=0xFFC97891;PALE=0xFFFCEEF2;DARK=0xFF3F3439;MUTED=0xFF75666D;}
        }
        else if ("night".equals(theme)) { BG=0xFF17151D; PINK=0xFFE39AB5; PALE=0xFF302735; DARK=0xFFF4EAF0; MUTED=0xFFC8BDC8; }
        else if ("lavender".equals(theme)) { BG=0xFFF8F7FC; PINK=0xFF8070A8; PALE=0xFFF0EDFA; DARK=0xFF292638; MUTED=0xFF858096; }
        else if ("mint".equals(theme)) { BG=0xFFF5FAF7; PINK=0xFF659B86; PALE=0xFFEAF5EF; DARK=0xFF34443D; MUTED=0xFF60786B; }
        else if ("white".equals(theme)) { BG=0xFFF7F8FA; PINK=0xFF65758B; PALE=0xFFE9EDF2; DARK=0xFF303844; MUTED=0xFF65717F; }
        else if ("brown".equals(theme)) { BG=0xFFF7F1EA; PINK=0xFF8B6046; PALE=0xFFEDE0D2; DARK=0xFF49382E; MUTED=0xFF796657; }
        else if ("gold".equals(theme)) { BG=0xFFFFF8DF; PINK=0xFFC49318; PALE=0xFFF5E2A6; DARK=0xFF49340B; MUTED=0xFF795B1E; }
        else if ("luxury".equals(theme)) { BG=0xFF211B18; PINK=0xFFD6B36A; PALE=0xFF3A3028; DARK=0xFFF3E7D0; MUTED=0xFFC5B598; }
        else if ("sky".equals(theme)) { BG=0xFFF1F8FC; PINK=0xFF5D91B5; PALE=0xFFE1EFF8; DARK=0xFF304656; MUTED=0xFF5E788A; }
        else if ("peach".equals(theme)) { BG=0xFFFFF5EF; PINK=0xFFD88968; PALE=0xFFFBE5D9; DARK=0xFF513D34; MUTED=0xFF856C60; }
        else if ("red".equals(theme)) { BG=0xFFFFF5F4; PINK=0xFFB32635; PALE=0xFFF8E2E3; DARK=0xFF4B2026; MUTED=0xFF81595E; }
        else if ("lv".equals(theme)) { BG=0xFFF4E8D5; PINK=0xFF76502F; PALE=0xFFE7D2B3; DARK=0xFF3B281C; MUTED=0xFF76604A; }
        else { BG=0xFFFFF9F7; PINK=0xFFC97891; PALE=0xFFFCEEF2; DARK=0xFF3F3439; MUTED=0xFF75666D; }
        applyAccentOverlay(theme);
        if (prefs != null && prefs.contains("app_accent_color")) PINK = prefs.getInt("app_accent_color", PINK);
        if (getWindow() != null) { getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG)); getWindow().getDecorView().setBackgroundColor(BG); if(Build.VERSION.SDK_INT>=30){getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);getWindow().setDecorFitsSystemWindows(false);if(Build.VERSION.SDK_INT>=29){getWindow().setStatusBarContrastEnforced(false);getWindow().setNavigationBarContrastEnforced(false);}}else{getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);} updateSystemBarIcons(); }
    }
    private void applyAccentOverlay(String theme){
        int style=R.style.AppAccentPink;
        if("lavender".equals(theme))style=R.style.AppAccentLavender;
        else if("mint".equals(theme))style=R.style.AppAccentMint;
        else if("night".equals(theme))style=R.style.AppAccentNight;
        else if("white".equals(theme))style=R.style.AppAccentWhite;
        else if("brown".equals(theme))style=R.style.AppAccentBrown;
        else if("gold".equals(theme))style=R.style.AppAccentGold;
        else if("luxury".equals(theme))style=R.style.AppAccentLuxury;
        else if("sky".equals(theme))style=R.style.AppAccentSky;
        else if("peach".equals(theme))style=R.style.AppAccentPeach;
        else if("red".equals(theme))style=R.style.AppAccentRed;
        else if("lv".equals(theme))style=R.style.AppAccentLv;
        getTheme().applyStyle(style,true);
    }

        private void addDailyMoodCard(LinearLayout host) {
        LinearLayout moodCard = card();
        moodCard.setPadding(dp(13), dp(10), dp(13), dp(10));
        addText(moodCard, "КАК ТЫ СЕБЯ ЧУВСТВУЕШЬ?", 12, PINK, true);
        TextView hint = text("Выбери настроение — подскажу, с чего начать.", 12, MUTED, false);
        moodCard.addView(hint, lp(-1, -2, 0, 2, 0, 7));
        String[] faces = {"😴", "😌", "🙂", "😊", "⚡"};
        String[] names = {"Усталость", "Спокойно", "Нормально", "Радостно", "Много энергии"};
        String[] hints = {"Предлагаю короткую сессию на 5 минут.", "Можно спокойно повторить знакомую тему.", "Выбери одну небольшую задачу.", "Хороший момент для новой темы.", "Попробуй сложную задачу или длинную сессию."};
        String today = todayKey();
        String selected = prefs.getString("daily_mood_date", "").equals(today) ? prefs.getString("daily_mood", "") : "";
        if (!selected.isEmpty()) return;
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < faces.length; i++) {
            final int index = i;
            LinearLayout option = new LinearLayout(this);
            option.setOrientation(LinearLayout.VERTICAL);
            option.setGravity(Gravity.CENTER);
            option.setPadding(dp(2), dp(4), dp(2), dp(4));
            boolean active = selected.equals(String.valueOf(i));
            option.setBackground(shape(active ? PALE : 0x00FFFFFF, 14, active ? PINK : 0));
            TextView face = text(faces[i], 22, DARK, false);
            face.setGravity(Gravity.CENTER);
            option.addView(face, lp(-1, 32, 0, 0, 0, 1));
            TextView label = text(names[i], 9, active ? PINK : MUTED, active);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            option.addView(label, lp(-1, 18, 0, 0, 0, 0));
            option.setContentDescription(names[i] + ". " + hints[i]);
            option.setFocusable(true);
            choices.addView(option, new LinearLayout.LayoutParams(0, dp(56), 1f));
            option.setOnClickListener(v -> {
                String moodDate = todayKey();
                String entryId = prefs.getString("daily_mood_entry_" + moodDate, "");
                if (entryId.isEmpty()) entryId = String.valueOf(System.currentTimeMillis());
                Set<String> ids = new TreeSet<>(prefs.getStringSet("diary_ids", new TreeSet<String>()));
                ids.add(entryId);
                String diaryText = "Самочувствие: " + names[index] + ". " + hints[index];
                prefs.edit().putString("daily_mood_date", moodDate).putString("daily_mood", String.valueOf(index))
                    .putString("daily_mood_entry_" + moodDate, entryId)
                    .putString("diary_" + entryId, diaryText).putString("diary_tag_" + entryId, "Самочувствие")
                    .putStringSet("diary_ids", ids).apply();
                showHome();
                showAppMessage("Настроение сохранено в дневнике ♡");
            });
        }
        moodCard.addView(choices, lp(-1, 56, 0, 0, 0, 0));
        host.addView(moodCard, lp(-1, -2, 0, 0, 0, 12));
    }

    private void showHome() {
        currentScreen = "home";
        StudyWidgetProvider.updateAll(this);
        String displayName = prefs.getString("display_name", "Друг");
        int hourNow = Calendar.getInstance(AppTime.zone()).get(Calendar.HOUR_OF_DAY);
        String avatar = prefs.getString("onboarding_avatar", "");
        String greetingTitle = (!prefs.getBoolean("mascot_enabled",true)||avatar.isEmpty() ? "" : avatar + "  ") + (hourNow < 12 ? "Доброе утро, " + displayName : hourNow < 18 ? "Добрый день, " + displayName : "Как прошёл день, " + displayName + "?");
        String lastStudy = prefs.getString("last_study_date", "");
        boolean returningAfterGap = !lastStudy.isEmpty() && !todayKey().equals(lastStudy) && !isYesterday(lastStudy);
        int dailyGoal = prefs.getInt("daily_goal_minutes", 15);
        int dailyDone = prefs.getInt("focus_minutes_" + todayKey(), 0);
        int remaining = Math.max(0, dailyGoal - dailyDone);
        String homeSubtitle = returningAfterGap ? "Рада тебя видеть. Давай начнём с малого. · До цели " + remaining + " мин."
                : (currentStreak() > 0 ? "🔥 Серия: " + currentStreak() + " " + dayWord(currentStreak()) : "Начни новую серию сегодня") + " · Осталось " + remaining + " мин. до цели дня";
        base(greetingTitle, homeSubtitle);
        addDailyMoodCard(root);
        if(root.getChildCount()>0)root.getChildAt(root.getChildCount()-1).setTag("home_mood");

        int totalTopics = 0, completedTopics = 0;
        for (Map.Entry<String, List<String>> entry : chapters.entrySet()) {
            for (String topic : entry.getValue()) {
                if (!isTopicInExamScope(entry.getKey(), topic)) continue;
                totalTopics++;
                if (topicProgressForChapter(entry.getKey(), topic) == 100) completedTopics++;
            }
        }
        int overall = totalTopics == 0 ? 0 : completedTopics * 100 / totalTopics;
        int streak = currentStreak();
        int done = prefs.getBoolean("exam_topic_filter_enabled", false)
                ? completedTopics : prefs.getInt("done_count", 0);

        String resumeTopic = prefs.getString("recent_topic", "");
        String resumeChapter = prefs.getString("recent_chapter", "");
        boolean resumeFound = !resumeTopic.isEmpty() && !resumeChapter.isEmpty()
                && chapters.containsKey(resumeChapter)
                && chapters.get(resumeChapter).contains(resumeTopic);
        if (!resumeFound) {
            for (Map.Entry<String, List<String>> entry : chapters.entrySet()) {
                for (String topic : entry.getValue()) {
                    if (!isTopicInExamScope(entry.getKey(), topic)) continue;
                    if (topicProgressForChapter(entry.getKey(), topic) < 100) {
                        resumeTopic = topic;
                        resumeChapter = entry.getKey();
                        resumeFound = true;
                        break;
                    }
                }
                if (resumeFound) break;
            }
        }
        if (!resumeFound) {
            for (Map.Entry<String, List<String>> entry : chapters.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    resumeTopic = entry.getValue().get(0);
                    resumeChapter = entry.getKey();
                    resumeFound = true;
                    break;
                }
            }
        }

        final String topicToResume = resumeTopic;
        final String chapterToResume = resumeChapter;
        final boolean hasResumeTopic = resumeFound;

        // Compact motivational progress card: consistent small vertical rhythm.
        LinearLayout continueCard = card();
        continueCard.setPadding(dp(16), dp(12), dp(16), dp(12));
        continueCard.setBackground(shape(0xFFF8E8EE, 24, 0));
        TextView progressCaption=text("ТВОЙ ПРОГРЕСС",13,PINK,true);
        continueCard.addView(progressCaption,lp(-1,-2,0,0,0,2));
        TextView encouragement=text("Ты справляешься.\nПродолжай в своём темпе.",21,DARK,true);
        encouragement.setLineSpacing(dp(2),1.02f);
        continueCard.addView(encouragement,lp(-1,-2,0,0,0,2));
        TextView praise=text("✧  Ты отлично справляешься! ❤️",14,PINK,true);
        continueCard.addView(praise,lp(-1,-2,0,0,0,4));
        LinearLayout overallHead = new LinearLayout(this);
        overallHead.setGravity(Gravity.CENTER_VERTICAL);
        overallHead.addView(text("Учебный план", 15, DARK, true),
                new LinearLayout.LayoutParams(0, dp(22), 1f));
        overallHead.addView(text(overall + "%", 20, PINK, true));
        continueCard.addView(overallHead, lp(-1, 28, 0, 5, 0, 2));
        ProgressBar overallBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        overallBar.setMax(100);
        overallBar.setProgress(overall);
        if (Build.VERSION.SDK_INT >= 21) {
            overallBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
            overallBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFDCE6));
        }
        continueCard.addView(overallBar, lp(-1, 5, 0, 0, 0, 4));
        TextView completedLabel=text(completedTopics + " из " + totalTopics + " тем завершено",13,MUTED,false);
        continueCard.addView(completedLabel,lp(-1,-2,0,0,0,0));
        root.addView(continueCard, lp(-1, -2, 0, 1, 0, 6));

        // Three compact home summary tiles restored from the earlier design.
        LinearLayout homeStats = new LinearLayout(this);
        homeStats.setOrientation(LinearLayout.HORIZONTAL);
        homeStats.setGravity(Gravity.CENTER_VERTICAL);
        int[] statValues = {streak, done, prefs.getInt("longest_streak", streak)};
        String[] statLabels = {"дней подряд", "уроков", "личный рекорд"};
        for (int statIndex = 0; statIndex < statValues.length; statIndex++) {
            LinearLayout statCard = card();
            statCard.setOrientation(LinearLayout.VERTICAL);
            statCard.setGravity(Gravity.CENTER);
            statCard.setPadding(dp(4), dp(8), dp(4), dp(8));
            statCard.setBackground(shape(PALE, 16, PINK));
            TextView statValue = text(String.valueOf(statValues[statIndex]), 22, DARK, true);
            statValue.setGravity(Gravity.CENTER);
            statCard.addView(statValue, lp(-1, 30, 0, 0, 0, 3));
            TextView statLabel = text(statLabels[statIndex], 12, DARK, true);
            statLabel.setGravity(Gravity.CENTER);
            statCard.addView(statLabel, lp(-1, -2, 0, 0, 0, 0));
            LinearLayout.LayoutParams statLp = new LinearLayout.LayoutParams(0, dp(82), 1f);
            statLp.setMargins(dp(3), 0, dp(3), 0);
            homeStats.addView(statCard, statLp);
        }
        root.addView(homeStats, lp(-1, -2, 0, 0, 0, 6));homeStats.setTag("home_stats");

        // Weekly goal with a Monday-to-Sunday activity strip on the home screen.
        int weeklyGoal = prefs.getInt("weekly_goal_minutes", 300);
        int weeklyDone = weeklyMinutesLast7Days();
        LinearLayout weeklyCard = card();
        weeklyCard.setPadding(dp(13), dp(12), dp(13), dp(12));
        addText(weeklyCard, "🎯  ЦЕЛЬ НЕДЕЛИ", 11, PINK, true);
        addText(weeklyCard, weeklyDone + " из " + weeklyGoal + " минут", 14, DARK, true);
        addProgressLine(weeklyCard, "Твой недельный результат",
                weeklyGoal <= 0 ? 0 : Math.min(100, weeklyDone * 100 / weeklyGoal));
        LinearLayout daysRow = new LinearLayout(this);
        daysRow.setOrientation(LinearLayout.HORIZONTAL);
        daysRow.setGravity(Gravity.CENTER_VERTICAL);
        Calendar weekDay = Calendar.getInstance(AppTime.zone());
        weekDay.setFirstDayOfWeek(Calendar.MONDAY);
        weekDay.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        SimpleDateFormat weekDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String[] weekLabels = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
        String todayDate = todayKey();
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            String dateKey = weekDateFormat.format(weekDay.getTime());
            int dayMinutes = prefs.getInt("focus_minutes_" + dateKey, 0);
            boolean isToday = todayDate.equals(dateKey);
            LinearLayout dayCell = new LinearLayout(this);
            dayCell.setOrientation(LinearLayout.VERTICAL);
            dayCell.setGravity(Gravity.CENTER);
            TextView dayName = text(weekLabels[dayIndex], 10, isToday ? PINK : MUTED, true);
            dayName.setGravity(Gravity.CENTER);
            dayCell.addView(dayName, lp(-1, 16, 0, 0, 0, 2));
            if (isToday) dayCell.setBackground(shape(PALE, 12, PINK));
            TextView dayMark = text(dayMinutes > 0 ? "♥" : (isToday ? "●" : "·"),
                    dayMinutes > 0 ? 18 : 17, (dayMinutes > 0 || isToday) ? PINK : MUTED, true);
            dayMark.setGravity(Gravity.CENTER);
            dayCell.addView(dayMark, lp(-1, 22, 0, 0, 0, 0));
            TextView minutesLabel = text(dayMinutes > 0 ? dayMinutes + "м" : "—",
                    10, MUTED, false);
            minutesLabel.setGravity(Gravity.CENTER);
            dayCell.addView(minutesLabel, lp(-1, 14, 0, 0, 0, 0));
            daysRow.addView(dayCell, new LinearLayout.LayoutParams(0, dp(64), 1f));
            weekDay.add(Calendar.DAY_OF_YEAR, 1);
        }
        weeklyCard.addView(daysRow, lp(-1, 64, 0, 7, 0, 0));
        root.addView(weeklyCard, lp(-1, -2, 0, 0, 0, 8));weeklyCard.setTag("home_weekly");

        LinearLayout aiCard = card();
        aiCard.setOrientation(LinearLayout.HORIZONTAL);
        aiCard.setGravity(Gravity.CENTER_VERTICAL);
        aiCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        aiCard.setBackground(shape(0xFFF3F0FF, 24, 0));
        TextView aiIcon = text("✧", 28, PINK, true);
        aiIcon.setGravity(Gravity.CENTER);
        aiCard.addView(aiIcon, lp(42, 44, 0, 0, 10, 0));
        LinearLayout aiWords = new LinearLayout(this);
        aiWords.setOrientation(LinearLayout.VERTICAL);
        addText(aiWords, "Есть вопросы по теме?", 15, DARK, true);
        addText(aiWords, "Спроси ИИ-помощника", 12, MUTED, false);
        aiCard.addView(aiWords, new LinearLayout.LayoutParams(0, -2, 1f));
        aiCard.addView(text("›", 27, PINK, true));
        aiCard.setOnClickListener(v -> launchAiTutor());
        root.addView(aiCard, lp(-1, -2, 0, 0, 0, 8));aiCard.setTag("home_ai");

        TextView emotionsTitle=text("ЭМОЦИИ И МОТИВАЦИЯ",12,PINK,true);emotionsTitle.setTag("home_emotions");root.addView(emotionsTitle,lp(-1,-2,0,0,0,0));
        LinearLayout feelings2 = new LinearLayout(this);
        feelings2.setOrientation(LinearLayout.HORIZONTAL);
        ritualTile(feelings2, "✿", "Цветочек", "Растёт с тобой", this::showFlower);
        ritualTile(feelings2, "♫", "Атмосфера", "Звуки для учёбы", this::showAtmosphere);
        root.addView(feelings2, lp(-1, -2, 0, 0, 0, 6));feelings2.setTag("home_emotions");

        // Compact three-column square shortcuts; planner remains easy to find.
        // Planner shortcuts are self-explanatory; no extra section caption.
        LinearLayout plannerAwardsRow = new LinearLayout(this);
        plannerAwardsRow.setOrientation(LinearLayout.HORIZONTAL);
        plannerAwardsRow.setGravity(Gravity.CENTER_VERTICAL);
        ritualTile(plannerAwardsRow, "▦", "Планировщик", "План на день", this::showPlanner);
        ritualTile(plannerAwardsRow, "✧", "Достижения", "Твои награды", this::showAchievements);
        ritualTile(plannerAwardsRow, "♧", "Напарник", "Общая цель", this::showStudyBuddy);
        root.addView(plannerAwardsRow,lp(-1,-2,0,0,0,4));
        plannerAwardsRow.setTag("home_planner");

        LinearLayout reminder = card();
        reminder.setOrientation(LinearLayout.HORIZONTAL);
        reminder.setGravity(Gravity.CENTER_VERTICAL);
        reminder.setPadding(dp(8), dp(2), dp(7), dp(2));
        reminder.setBackground(shape(PALE, 15, PINK));
        LinearLayout reminderWords = new LinearLayout(this);
        reminderWords.setOrientation(LinearLayout.VERTICAL);
        addText(reminderWords, "НАПОМИНАНИЯ", 8, PINK, true);
        addText(reminderWords, reminderSummary(), 9, DARK, false);
        reminder.addView(reminderWords, new LinearLayout.LayoutParams(0, -2, 1f));
        reminder.addView(text("›", 19, PINK, true), lp(20, 24, 2, 0, 0, 0));
        reminder.setOnClickListener(v -> showReminderSettings());
        root.addView(reminder, lp(-1, dp(30), 0, 2, 0, 1));reminder.setTag("home_reminders");

        TextView made = text("Сделано с любовью ♡", 11, DARK, false);
        made.setGravity(Gravity.CENTER);
        root.addView(made, lp(-1, 24, 0, 2, 0, 0));
        reorderHomeBlocks();
        applyHomeBlockVisibility();
        animateHomeCascade();
    }

    private void reorderHomeBlocks(){
        if(root==null)return;String[] defaults={"stats","weekly","ai","emotions","planner","reminders"};String saved=prefs.getString("home_block_order","stats,weekly,ai,emotions,planner,reminders");List<String> order=new ArrayList<>();for(String key:saved.split(","))if(java.util.Arrays.asList(defaults).contains(key)&&!order.contains(key))order.add(key);for(String key:defaults)if(!order.contains(key))order.add(key);order.remove("reminders");order.add("reminders");Map<String,List<View>> groups=new LinkedHashMap<>();for(String key:defaults)groups.put(key,new ArrayList<View>());int first=-1;for(int i=0;i<root.getChildCount();i++){Object tag=root.getChildAt(i).getTag();if(tag instanceof String&&((String)tag).startsWith("home_")){String key=((String)tag).substring(5);if(groups.containsKey(key)){if(first<0)first=i;groups.get(key).add(root.getChildAt(i));}}}if(first<0)return;for(List<View> group:groups.values())for(View v:group)root.removeView(v);int at=Math.min(first,root.getChildCount());for(String key:order)for(View v:groups.get(key))root.addView(v,at++);
    }
    private void showHomeBlockOrderDialog(){
        final String[] keys={"stats","weekly","ai","emotions","reminders"};final String[] names={"Статистика","Недельная цель","Карточка ИИ","Плитки эмоций","Напоминания"};List<String> order=new ArrayList<>();for(String k:prefs.getString("home_block_order","stats,weekly,ai,emotions,reminders").split(","))if(java.util.Arrays.asList(keys).contains(k)&&!order.contains(k))order.add(k);for(String k:keys)if(!order.contains(k))order.add(k);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(8),dp(12),dp(8));android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Порядок главной страницы").setView(body).setNegativeButton("Закрыть",null).setPositiveButton("Сохранить",(d,w)->{prefs.edit().putString("home_block_order",android.text.TextUtils.join(",",order)).apply();showHome();}).create();Runnable[] render={null};render[0]=()->{body.removeAllViews();for(int i=0;i<order.size();i++){final int index=i;String key=order.get(i);int pos=java.util.Arrays.asList(keys).indexOf(key);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView label=text((i+1)+". "+names[pos],14,DARK,true);row.addView(label,new LinearLayout.LayoutParams(0,dp(44),1f));Button up=button("↑",false),down=button("↓",false);up.setEnabled(index>0);down.setEnabled(index<order.size()-1);row.addView(up,lp(42,40,2,0,2,0));row.addView(down,lp(42,40,2,0,0,0));up.setOnClickListener(v->{java.util.Collections.swap(order,index,index-1);render[0].run();});down.setOnClickListener(v->{java.util.Collections.swap(order,index,index+1);render[0].run();});body.addView(row,lp(-1,46,0,1,0,1));}};render[0].run();dialog.show();
    }
    private void applyHomeBlockVisibility(){
        if(root==null)return;
        String[] keys={"home_mood","home_stats","home_weekly","home_ai","home_emotions","home_reminders"};
        String[] prefsKeys={"home_show_mood","home_show_stats","home_show_weekly","home_show_ai","home_show_emotions","home_show_reminders"};
        for(int i=0;i<keys.length;i++)for(int j=0;j<root.getChildCount();j++){View child=root.getChildAt(j);if(keys[i].equals(child.getTag()))child.setVisibility(prefs.getBoolean(prefsKeys[i],true)?View.VISIBLE:View.GONE);}
    }

    // Endless arithmetic challenge. Best scores and unlocked awards are stored locally.
    private final Random quizRandom = new Random();
    private final Handler quizHandler = new Handler(Looper.getMainLooper());
    private Runnable quizTick;
    private boolean quizActive = false, quizTimed = true;
    private int quizDifficulty = 0, quizScore = 0, quizStreak = 0, quizBestStreak = 0;
    private int quizAnswer = 0, quizSeconds = 15, quizLevel = 1;
    private long quizDeadline = 0L;
    private TextView quizTimerLabel, quizQuestionLabel, quizScoreLabel, quizLevelLabel;
    private ProgressBar quizTimerBar;

    private int quizTimeForDifficulty() { return quizDifficulty == 0 ? 15 : quizDifficulty == 1 ? 10 : 6; }
    private String quizDifficultyName() { return quizDifficulty == 0 ? "Лёгкая" : quizDifficulty == 1 ? "Средняя" : "Тяжёлая"; }
    private String quizModeName() { return quizTimed ? "С таймером" : "Без времени"; }

    private void showMathQuiz() {
        stopQuizTimer();
        quizActive = false;
        currentScreen = "math_quiz";
        base("Математическая викторина 🧮", "Бесконечная практика в твоём темпе");
        LinearLayout intro = card();
        intro.setPadding(dp(18),dp(18),dp(18),dp(18));
        intro.setBackground(shape(0xFFFFF0F4,24,0));
        addText(intro,"ТВОЙ МАТЕМАТИЧЕСКИЙ ЧЕЛЛЕНДЖ",11,PINK,true);
        addText(intro,"Сколько примеров ты решишь подряд?",22,DARK,true);
        addText(intro,"Выбери режим и сложность. Набирай очки, проходи уровни и собирай достижения. Ошибка завершит забег.",13,MUTED,false);
        root.addView(intro,lp(-1,-2,0,0,0,14));
        addText(root,"РЕЖИМ ИГРЫ",12,PINK,true);
        LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        Button timedMode=button("⏱ С таймером",quizTimed),freeMode=button("☁ Без времени",!quizTimed);
        LinearLayout.LayoutParams modeLp=new LinearLayout.LayoutParams(0,dp(48),1f);modeLp.setMargins(0,0,dp(5),0);modes.addView(timedMode,modeLp);
        LinearLayout.LayoutParams freeLp=new LinearLayout.LayoutParams(0,dp(48),1f);freeLp.setMargins(dp(5),0,0,0);modes.addView(freeMode,freeLp);
        timedMode.setOnClickListener(v->{quizTimed=true;showMathQuiz();});freeMode.setOnClickListener(v->{quizTimed=false;showMathQuiz();});
        root.addView(modes,lp(-1,50,0,0,0,14));
        addText(root,"ВЫБЕРИ СЛОЖНОСТЬ",12,PINK,true);
        String[] names={"🌱  Лёгкая","⚡  Средняя","🔥  Тяжёлая"};
        String[] details={"До 20 · таблица умножения 2–5 · простые примеры","До 100 · умножение и деление на числа до 12","До 500 · крупные числа · умножение до 30"};
        for(int i=0;i<3;i++){
            final int d=i;
            LinearLayout item=card();item.setPadding(dp(15),dp(12),dp(15),dp(12));
            LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
            addText(words,names[i],16,DARK,true);addText(words,details[i],12,MUTED,false);
            line.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
            TextView arrow=text("›",27,PINK,true);line.addView(arrow);item.addView(line);
            item.setOnClickListener(v->{quizDifficulty=d;startMathQuiz();});
            root.addView(item,lp(-1,-2,0,0,0,9));
        }
        LinearLayout stats=card();stats.setPadding(dp(15),dp(13),dp(15),dp(13));
        addText(stats,"ТВОЙ РЕКОРД",11,PINK,true);
        addText(stats,prefs.getInt("quiz_best_score",0)+" очков  ·  "+prefs.getInt("quiz_total_correct",0)+" правильных ответов за всё время",14,DARK,true);
        root.addView(stats,lp(-1,-2,0,8,0,10));
        Button awards=button("🏅  Посмотреть достижения",false);awards.setOnClickListener(v->showMathQuizAchievements());root.addView(awards,lp(-1,48,0,0,0,6));
    }

    private void startMathQuiz() {
        stopQuizTimer();quizActive=true;quizScore=0;quizStreak=0;quizBestStreak=0;quizLevel=1;
        showMathQuizQuestion();
    }

    private int[] makeQuizProblem() {
        int scale=Math.min(40,quizLevel-1), op=quizRandom.nextInt(4),a,b,result;
        int max=quizDifficulty==0?Math.min(30,20+scale):quizDifficulty==1?Math.min(180,100+scale*2):Math.min(700,300+scale*10);
        if(op==0){a=1+quizRandom.nextInt(max);b=1+quizRandom.nextInt(max);result=a+b;return new int[]{a,b,op,result};}
        if(op==1){a=1+quizRandom.nextInt(max);b=1+quizRandom.nextInt(a);result=a-b;return new int[]{a,b,op,result};}
        if(op==2){
            int low=quizDifficulty==0?2:quizDifficulty==1?2:12;
            int high=quizDifficulty==0?5:quizDifficulty==1?12:30;
            a=low+quizRandom.nextInt(high-low+1);b=low+quizRandom.nextInt(high-low+1);
            if(quizDifficulty==2&&quizRandom.nextBoolean())a+=quizRandom.nextInt(20);
            result=a*b;return new int[]{a,b,op,result};
        }
        int divisor,quotient;
        if(quizDifficulty==0){divisor=2+quizRandom.nextInt(4);quotient=2+quizRandom.nextInt(9);}
        else if(quizDifficulty==1){divisor=2+quizRandom.nextInt(11);quotient=5+quizRandom.nextInt(16);}
        else {divisor=12+quizRandom.nextInt(19);quotient=12+quizRandom.nextInt(29);}
        a=divisor*quotient;b=divisor;result=quotient;return new int[]{a,b,op,result};
    }

    private void showMathQuizQuestion() {
        currentScreen="math_quiz";
        int[] problem=makeQuizProblem();quizAnswer=problem[3];
        String sign=problem[2]==0?"+":problem[2]==1?"−":problem[2]==2?"×":"÷";
        base("Уровень "+quizLevel+"  ·  "+quizDifficultyName(),"Режим: "+quizModeName()+(quizTimed?" · реши до окончания времени":" · решай в своём темпе"));
        LinearLayout top=card();top.setPadding(dp(15),dp(12),dp(15),dp(12));
        LinearLayout topLine=new LinearLayout(this);topLine.setGravity(Gravity.CENTER_VERTICAL);
        quizScoreLabel=text("⭐ "+quizScore+" очков",14,DARK,true);topLine.addView(quizScoreLabel,new LinearLayout.LayoutParams(0,-2,1f));
        quizLevelLabel=text("УРОВЕНЬ "+quizLevel,12,PINK,true);topLine.addView(quizLevelLabel);top.addView(topLine);
        root.addView(top,lp(-1,-2,0,0,0,12));
        quizTimerLabel=null;quizTimerBar=null;
        if(quizTimed){
            LinearLayout timerCard=card();timerCard.setGravity(Gravity.CENTER);timerCard.setPadding(dp(14),dp(14),dp(14),dp(14));
            quizTimerLabel=text(quizTimeForDifficulty()+" сек",30,PINK,true);quizTimerLabel.setGravity(Gravity.CENTER);
            timerCard.addView(quizTimerLabel,lp(-1, 48,0,0,0,5));
            quizTimerBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);quizTimerBar.setMax(1000);quizTimerBar.setProgress(1000);
            if(Build.VERSION.SDK_INT>=21){quizTimerBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));quizTimerBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE2EA));}
            timerCard.addView(quizTimerBar,lp(-1,8,0,0,0,0));root.addView(timerCard,lp(-1,-2,0,0,0,18));
        }else{
            LinearLayout relaxed=card();relaxed.setGravity(Gravity.CENTER);addText(relaxed,"☁  Время не ограничено",14,PINK,true);root.addView(relaxed,lp(-1,48,0,0,0,14));
        }
        LinearLayout question=card();question.setGravity(Gravity.CENTER);question.setPadding(dp(12),dp(25),dp(12),dp(25));
        addText(question,"СКОЛЬКО БУДЕТ?",11,MUTED,true);
        quizQuestionLabel=text(problem[0]+" "+sign+" "+problem[1]+" = ?",36,DARK,true);quizQuestionLabel.setGravity(Gravity.CENTER);
        question.addView(quizQuestionLabel,lp(-1,72,0,5,0,0));root.addView(question,lp(-1,-2,0,0,0,16));
        List<Integer> choices=new ArrayList<>();choices.add(quizAnswer);
        while(choices.size()<4){int delta=1+quizRandom.nextInt(Math.max(4,quizDifficulty==0?8:quizDifficulty==1?20:40));int candidate=quizAnswer+(quizRandom.nextBoolean()?delta:-delta);if(candidate>=0&&!choices.contains(candidate))choices.add(candidate);}
        java.util.Collections.shuffle(choices,quizRandom);
        for(int i=0;i<4;i+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=i;j<i+2;j++){final int answer=choices.get(j);Button option=button(String.valueOf(answer),false);option.setTextSize(20);option.setOnClickListener(v->{if(!quizActive)return;boolean correct=answer==quizAnswer;option.setBackground(shape(correct?0xFFE4F4E8:0xFFFFE8E8,22,correct?0xFF75B987:0xFFE58A8A));option.setTextColor(correct?0xFF287A43:0xFFB64242);if(!correct){for(int k=0;k<row.getChildCount();k++){View child=row.getChildAt(k);if(child instanceof Button&&((Button)child).getText().toString().equals(String.valueOf(quizAnswer))){child.setBackground(shape(0xFFE4F4E8,22,0xFF75B987));((Button)child).setTextColor(0xFF287A43);}}}haptic(option,correct);new Handler(Looper.getMainLooper()).postDelayed(()->submitMathQuizAnswer(answer),250);});LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(0,dp(62),1f);op.setMargins(dp(3),0,dp(3),0);row.addView(option,op);}
            root.addView(row,lp(-1,-2,0,0,0,8));
        }
        TextView streak=text("🔥 Серия правильных ответов: "+quizStreak,13,PINK,true);streak.setGravity(Gravity.CENTER);root.addView(streak,lp(-1, 48,0,8,0,0));
        if(quizTimed){
            quizSeconds=quizTimeForDifficulty();quizDeadline=SystemClock.elapsedRealtime()+quizSeconds*1000L;
            quizTick=()->{
                if(!quizActive||!"math_quiz".equals(currentScreen))return;
                long left=quizDeadline-SystemClock.elapsedRealtime();
                if(left<=0){if(quizTimerLabel!=null)quizTimerLabel.setText("0 сек");if(quizTimerBar!=null)quizTimerBar.setProgress(0);endMathQuiz(true);return;}
                int sec=(int)((left+999)/1000);if(quizTimerLabel!=null)quizTimerLabel.setText(sec+" сек");
                if(quizTimerBar!=null)quizTimerBar.setProgress((int)Math.max(0,Math.min(1000,left*1000L/(quizSeconds*1000L))));
                quizHandler.postDelayed(quizTick,80);
            };
            quizHandler.post(quizTick);
        }
    }

    private void submitMathQuizAnswer(int answer) {
        if(!quizActive)return;
        stopQuizTimer();
        if(answer!=quizAnswer){endMathQuiz(false);return;}
        quizScore+=10;quizStreak++;quizBestStreak=Math.max(quizBestStreak,quizStreak);haptic(root,true);
        prefs.edit().putInt("quiz_total_correct",prefs.getInt("quiz_total_correct",0)+1).apply();
        if(quizScore>prefs.getInt("quiz_best_score",0)){prefs.edit().putInt("quiz_best_score",quizScore).apply();celebrate();}
        quizLevel=quizScore/50+1;
        unlockQuizAwards();
        showMathQuizQuestion();
    }

    private void stopQuizTimer(){if(quizTick!=null){quizHandler.removeCallbacks(quizTick);quizTick=null;}}

    private void endMathQuiz(boolean timeout) {
        if(!quizActive)return;quizActive=false;stopQuizTimer();
        unlockQuizAwards();
        currentScreen="math_quiz_result";
        base(timeout?"Время вышло! ⏰":"Игра окончена",timeout?"Нужно отвечать чуть быстрее — попробуешь ещё раз?":"Ничего страшного! Ошибки помогают учиться.");
        LinearLayout result=card();result.setGravity(Gravity.CENTER);result.setPadding(dp(18),dp(22),dp(18),dp(22));
        TextView medal=text(quizScore>=200?"🏆":quizScore>=100?"🥇":quizScore>=50?"🥈":"🌸",54,PINK,true);medal.setGravity(Gravity.CENTER);result.addView(medal,lp(-1,76,0,0,0,8));
        addText(result,"ТВОЙ РЕЗУЛЬТАТ",12,PINK,true);
        TextView score=text(String.valueOf(quizScore),42,DARK,true);score.setGravity(Gravity.CENTER);result.addView(score,lp(-1,54,0,3,0,2));
        TextView points=text("очков",13,MUTED,false);points.setGravity(Gravity.CENTER);result.addView(points);
        addText(result,"Правильных ответов: "+(quizScore/10),14,DARK,true);
        addText(result,"Достигнутый уровень: "+quizLevel,14,DARK,true);
        addText(result,"Лучшая серия: "+quizBestStreak,14,DARK,true);
        addText(result,"Рекорд: "+prefs.getInt("quiz_best_score",0)+" очков",13,PINK,true);
        root.addView(result,lp(-1,-2,0,0,0,15));
        Button retry=button("↻  Играть ещё раз",true);retry.setOnClickListener(v->showMathQuiz());root.addView(retry,lp(-1,52,0,0,0,8));
        Button awards=button("🏅  Мои достижения",false);awards.setOnClickListener(v->showMathQuizAchievements());root.addView(awards,lp(-1,48,0,0,0,8));
    }

    private void unlockQuizAward(String key){if(!prefs.getBoolean(key,false)){String date=new SimpleDateFormat("d MMMM yyyy",new Locale("ru")).format(new Date());prefs.edit().putBoolean(key,true).putString("achievement_date_"+key,date).apply();haptic(root,true);celebrate();}}
    private void unlockQuizAwards() {
        int total=prefs.getInt("quiz_total_correct",0),best=prefs.getInt("quiz_best_score",0);
        int[] thresholds={1,5,10,25,50,100};
        for(int i=0;i<thresholds.length;i++)if(total>=thresholds[i])unlockQuizAward("quiz_award_correct_"+thresholds[i]);
        if(quizBestStreak>=5)unlockQuizAward("quiz_award_streak_5");
        if(quizBestStreak>=10)unlockQuizAward("quiz_award_streak_10");
        if(best>=100)unlockQuizAward("quiz_award_score_100");
        if(best>=250)unlockQuizAward("quiz_award_score_250");
        if(quizDifficulty==2&&quizScore>=100)unlockQuizAward("quiz_award_hard");
    }

    private void showMathQuizAchievements() {
        stopQuizTimer();quizActive=false;currentScreen="math_quiz_awards";
        base("Награды викторины 🏅","Каждая открытая награда — твоя маленькая победа");
        int total=prefs.getInt("quiz_total_correct",0);
        addText(root,"ОТКРЫТО "+countQuizAwards()+" ИЗ 11",12,PINK,true);
        String[][] awards={
            {"🌱","Первый шаг","Решить первый пример","quiz_award_correct_1"},
            {"⭐","Пять побед","Дать 5 правильных ответов","quiz_award_correct_5"},
            {"🧠","Разминка окончена","Дать 10 правильных ответов","quiz_award_correct_10"},
            {"🔥","Двадцать пять","Дать 25 правильных ответов","quiz_award_correct_25"},
            {"💎","Полсотни","Дать 50 правильных ответов","quiz_award_correct_50"},
            {"🌟","Сотня ответов","Дать 100 правильных ответов","quiz_award_correct_100"},
            {"⚡","На одном дыхании","Серия из 5 верных ответов","quiz_award_streak_5"},
            {"🚀","Без остановки","Серия из 10 верных ответов","quiz_award_streak_10"},
            {"🥇","Сотня очков","Набрать 100 очков за забег","quiz_award_score_100"},
            {"🏆","Большая победа","Набрать 250 очков за забег","quiz_award_score_250"},
            {"🔥","Эксперт","Набрать 100 очков на тяжёлой сложности","quiz_award_hard"}
        };
        for(String[] a:awards){
            boolean unlocked=prefs.getBoolean(a[3],false);
            LinearLayout item=card();item.setOrientation(LinearLayout.HORIZONTAL);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(13),dp(11),dp(13),dp(11));
            TextView icon=text(unlocked?a[0]:"🔒",27,unlocked?PINK:MUTED,true);item.addView(icon,lp(46,48,0,0,10,0));
            LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
            addText(words,a[1],14,unlocked?DARK:MUTED,true);addText(words,unlocked?"Открыто · "+a[2]:a[2],11,MUTED,false);
            item.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
            TextView state=text(unlocked?"✓":"···",17,unlocked?PINK:MUTED,true);item.addView(state);
            root.addView(item,lp(-1,-2,0,0,0,7));
        }
    }

    private int countQuizAwards(){
        String[] keys={"quiz_award_correct_1","quiz_award_correct_5","quiz_award_correct_10","quiz_award_correct_25","quiz_award_correct_50","quiz_award_correct_100","quiz_award_streak_5","quiz_award_streak_10","quiz_award_score_100","quiz_award_score_250","quiz_award_hard"};
        int n=0;for(String key:keys)if(prefs.getBoolean(key,false))n++;return n;
    }

    private String dailyMotivation() {String last=prefs.getString("last_study_date","");if(!last.isEmpty()&&!todayKey().equals(last)&&!isYesterday(last))return "♡  Давно не занимались? Ничего страшного — начни с 5 минут. Ты справишься.";return "✧  "+messages[Math.floorMod(todayKey().hashCode(),messages.length)]; }
     private String word(int n) { int x=n%10, y=n%100; if(x==1 && y!=11)return "урок"; if(x>=2&&x<=4&&(y<12||y>14))return "урока"; return "уроков"; }
    private boolean viewContainsText(View view,String query){
        if(view==null)return false;
        if(view instanceof TextView){
            CharSequence value=((TextView)view).getText();
            if(value!=null&&value.toString().toLowerCase(Locale.ROOT).contains(query))return true;
        }
        if(view instanceof android.view.ViewGroup){
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)if(viewContainsText(group.getChildAt(i),query))return true;
        }
        return false;
    }

    private void showChapters() {
        currentScreen = "chapters";
        lessonsSearchOpen=false;
        base("", "");
        LinearLayout lessonTop=new LinearLayout(this);
        lessonTop.setGravity(Gravity.CENTER_VERTICAL);
        TextView pageTitle=text("Уроки 📚",25,DARK,true);
        pageTitle.setGravity(Gravity.CENTER_VERTICAL);pageTitle.setSingleLine(true);pageTitle.setIncludeFontPadding(false);
        lessonTop.addView(pageTitle,new LinearLayout.LayoutParams(0,dp(44),1f));
        LinearLayout lessonActions=new LinearLayout(this);lessonActions.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        HeaderActionIconView weaknessAction=new HeaderActionIconView(this,0,PINK);
        lessonActions.addView(weaknessAction,lp(40,40,0,0,0,0));weaknessAction.setOnClickListener(v->showWeaknessTrainer());
        HeaderActionIconView search=new HeaderActionIconView(this,1,PINK);
        lessonActions.addView(search,lp(40,40,0,0,0,0));
        HeaderActionIconView orderButton=new HeaderActionIconView(this,2,PINK);
        lessonActions.addView(orderButton,lp(40,40,0,0,0,0));lessonTop.addView(lessonActions,lp(-2,40,0,0,0,0));
        root.addView(lessonTop,lp(-1,46,0,0,0,1));

        LinearLayout inlineSearch=new LinearLayout(this);
        inlineSearch.setGravity(Gravity.CENTER_VERTICAL);
        inlineSearch.setPadding(dp(12),0,dp(8),0);
        inlineSearch.setBackground(shape(0xFFFFF5F7,15,0xFFF0DCE4));
        TextView inlineSearchIcon=text("⌕",22,PINK,true);
        inlineSearchIcon.setVisibility(View.GONE);
        EditText inlineQuery=new EditText(this);
        inlineQuery.setSingleLine(true);
        inlineQuery.setTextSize(14);
        inlineQuery.setTextColor(DARK);
        inlineQuery.setHintTextColor(MUTED);
        inlineQuery.setHint("Найти предмет или тему");
        inlineQuery.setBackgroundColor(0x00000000);
        inlineQuery.setPadding(0,0,0,0);
        inlineSearch.addView(inlineQuery,new LinearLayout.LayoutParams(0,dp(44),1f));
        TextView closeSearch=text("×",26,PINK,true);
        closeSearch.setGravity(Gravity.CENTER);
        closeSearch.setContentDescription("Закрыть поиск");
        inlineSearch.addView(closeSearch,0,lp(38,44,0,0,8,0));
        TextView clearSearch=text("×",22,MUTED,true);
        clearSearch.setGravity(Gravity.CENTER);
        clearSearch.setVisibility(View.GONE);
        inlineSearch.addView(clearSearch,lp(32,44,0,0,0,0));
        clearSearch.setOnClickListener(v->inlineQuery.setText(""));
        inlineSearch.setVisibility(View.GONE);
        root.addView(inlineSearch,lp(-1,48,0,0,0,5));
        TextView orderLabel=text("МОИ ПРЕДМЕТЫ",12,PINK,true);
        orderLabel.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(orderLabel,lp(-1,27,0,0,0,5));
        inlineSearch.setAlpha(0f);inlineSearch.setTranslationY(-dp(8));
        Runnable closeInlineSearch=()->{
            if(!lessonsSearchOpen)return;
            lessonsSearchOpen=false;
            android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(imm!=null)imm.hideSoftInputFromWindow(inlineQuery.getWindowToken(),0);
            inlineSearch.animate().alpha(0f).translationY(-dp(7)).setDuration(150).withEndAction(()->{
                inlineQuery.setText("");root.removeView(inlineSearch);root.removeView(lessonTop);
                root.addView(lessonTop,0);root.addView(inlineSearch,1);
                inlineSearch.setVisibility(View.GONE);inlineSearch.setAlpha(1f);inlineSearch.setTranslationY(0f);
                for(int i=0;i<root.getChildCount();i++)root.getChildAt(i).setVisibility(View.VISIBLE);
            }).start();
        };
        closeSearch.setOnClickListener(v->closeInlineSearch.run());
        search.setOnClickListener(v->{
            if(lessonsSearchOpen){closeInlineSearch.run();return;}
            lessonsSearchOpen=true;
            root.removeView(lessonTop);root.removeView(inlineSearch);
            inlineSearch.setVisibility(View.VISIBLE);inlineSearch.setAlpha(0f);inlineSearch.setTranslationY(-dp(7));
            root.addView(inlineSearch,0);
            inlineSearch.animate().alpha(1f).translationY(0f).setDuration(210).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            inlineQuery.requestFocus();
            inlineQuery.post(()->{android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.showSoftInput(inlineQuery,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
        });
        inlineQuery.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){
                String q=s.toString().trim().toLowerCase(Locale.ROOT);
                clearSearch.setVisibility(q.isEmpty()?View.GONE:View.VISIBLE);
                for(int i=0;i<root.getChildCount();i++){
                    View child=root.getChildAt(i);
                    if(child==lessonTop||child==inlineSearch||child==orderLabel)continue;
                    child.setVisibility(q.isEmpty()||viewContainsText(child,q)?View.VISIBLE:View.GONE);
                }
            }
            public void afterTextChanged(android.text.Editable e){}
        });
        orderButton.setOnClickListener(v->{
            String[] labels={"По умолчанию","По названию А—Я","Сначала новые","По прогрессу"};
            String[] values={"default","alpha","new","progress"};
            LinearLayout menuBody=new LinearLayout(this);menuBody.setOrientation(LinearLayout.VERTICAL);menuBody.setPadding(dp(16),dp(10),dp(16),dp(12));menuBody.setBackground(shape(0xFFFFFBFC,22,0xFFF0DCE4));
            TextView menuTitle=text("СОРТИРОВКА ПРЕДМЕТОВ",11,PINK,true);menuTitle.setPadding(dp(5),dp(5),dp(5),dp(10));menuBody.addView(menuTitle);
            android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setView(menuBody).create();
            for(int i=0;i<labels.length;i++){final String value=values[i];LinearLayout option=new LinearLayout(this);option.setGravity(Gravity.CENTER_VERTICAL);option.setPadding(dp(12),0,dp(12),0);option.setBackground(shape(prefs.getString("lesson_sort","default").equals(value)?0xFFFFEAF0:0xFFFFFFFF,14,0xFFF3E2E8));TextView label=text(labels[i],14,DARK,prefs.getString("lesson_sort","default").equals(value));option.addView(label,new LinearLayout.LayoutParams(0,dp(48),1f));TextView check=text(prefs.getString("lesson_sort","default").equals(value)?"✓":"",17,PINK,true);option.addView(check);LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,dp(48));op.bottomMargin=dp(7);menuBody.addView(option,op);option.setOnClickListener(q->{prefs.edit().putString("lesson_sort",value).apply();dialog.dismiss();showChapters();});}
            dialog.setOnShowListener(q->{if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setDimAmount(0.28f);}});dialog.show();
        });
        List<String> mathTopics=chapters.get("1️⃣ Натуральные числа и действия с ними");int total=0,done=0;for(List<String> topics:chapters.values()){total+=topics.size();done+=countCompleted(topics);}
        int mathTotal=0,mathDone=0;for(Map.Entry<String,List<String>> e:chapters.entrySet())if(!e.getKey().startsWith("CUSTOM:")){mathTotal+=e.getValue().size();mathDone+=countCompleted(e.getValue());}
        LinearLayout math=new LinearLayout(this);math.setOrientation(LinearLayout.VERTICAL);math.setBackgroundColor(android.graphics.Color.TRANSPARENT);math.setPadding(dp(2),dp(8),dp(2),dp(8));math.setOnClickListener(v->animateLessonTap(math,()->showMathChapters()));
        LinearLayout mathTop=new LinearLayout(this);mathTop.setGravity(Gravity.CENTER_VERTICAL);TextView mathIcon=text("📐",27,PINK,true);mathTop.addView(mathIcon,lp(42,42,0,0,10,0));LinearLayout mathWords=new LinearLayout(this);mathWords.setOrientation(LinearLayout.VERTICAL);addText(mathWords,"Математика",16,DARK,true);mathTop.addView(mathWords,new LinearLayout.LayoutParams(0,-2,1f));TextView arrow=text("›",25,PINK,false);mathTop.addView(arrow);math.addView(mathTop,lp(-1,-2,0,0,0,7));
        TextView mathCount=text(mathDone+" из "+mathTotal+" тем",12,MUTED,true);mathCount.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);mathCount.setIncludeFontPadding(false);mathCount.setSingleLine(true);math.addView(mathCount,lp(-1,24,0,0,0,2));ProgressBar mathBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);mathBar.setMax(Math.max(1,mathTotal));mathBar.setProgress(mathDone);mathBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));mathBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF3E5EA));math.addView(mathBar,lp(-1,6,0,3,0,0));root.addView(math,lp(-1,-2,0,0,0,8));
        Set<String> ids=prefs.getStringSet("custom_lesson_ids",new TreeSet<String>());List<String> lessonOrder=new ArrayList<>(ids);String sort=prefs.getString("lesson_sort","default");if("alpha".equals(sort))java.util.Collections.sort(lessonOrder,(a,b)->prefs.getString("custom_lesson_name_"+a,"").compareToIgnoreCase(prefs.getString("custom_lesson_name_"+b,"")));else if("new".equals(sort))java.util.Collections.sort(lessonOrder,java.util.Collections.reverseOrder());else if("progress".equals(sort))java.util.Collections.sort(lessonOrder,(a,b)->{String na=prefs.getString("custom_lesson_name_"+a,""),nb=prefs.getString("custom_lesson_name_"+b,"");List<String> ta=chapters.get("CUSTOM:"+a+":"+na),tb=chapters.get("CUSTOM:"+b+":"+nb);int ca=ta==null?0:countCompleted(ta),cb=tb==null?0:countCompleted(tb);int sa=ta==null?0:ta.size(),sb=tb==null?0:tb.size();double pa=sa==0?0:(double)ca/sa,pb=sb==0?0:(double)cb/sb;return Double.compare(pb,pa);});
        for(String id:lessonOrder){String name=prefs.getString("custom_lesson_name_"+id,"Мой урок");List<String> topics=chapters.get("CUSTOM:"+id+":"+name);if(topics==null)topics=new ArrayList<>();int c=countCompleted("CUSTOM:"+id+":"+name,topics),n=topics.size();LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setBackgroundColor(android.graphics.Color.TRANSPARENT);item.setPadding(dp(3),dp(8),dp(3),dp(8));LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView emoji=text(prefs.getString("custom_lesson_emoji_"+id,"📚"),25,PINK,true);row.addView(emoji,lp(40,40,0,0,9,0));LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);addText(words,prefs.getString("custom_lesson_emoji_"+id,"📚")+"  "+name,15,DARK,true);addText(words,c+" из "+n+" тем",11,MUTED,false);row.addView(words,new LinearLayout.LayoutParams(0,-2,1f));TextView go=text("›",24,PINK,false);row.addView(go);item.addView(row);ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(Math.max(1,n));bar.setProgress(c);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF3E5EA));item.addView(bar,lp(-1,5,0,7,0,0));item.setOnClickListener(v->animateLessonTap(item,()->{selectedChapter="CUSTOM:"+id+":"+name;showTopics(selectedChapter);}));item.setOnLongClickListener(v->{confirmDeleteCustomLesson(id,name);return true;});root.addView(item,lp(-1,-2,0,0,0,7));}
        TextView add=text("＋  Добавить свой урок",14,PINK,true);add.setGravity(Gravity.CENTER);add.setBackgroundColor(android.graphics.Color.TRANSPARENT);add.setPadding(dp(8),dp(5),dp(8),dp(5));add.setOnClickListener(v->addCustomLesson());root.addView(add,lp(-1,42,0,3,0,9));
        String recent=prefs.getString("recent_topic","");String recentChapter=prefs.getString("recent_chapter","");if(!recent.isEmpty()&&!recentChapter.isEmpty()&&chapters.containsKey(recentChapter)){Button last=button("↗  Продолжить: "+recent,false);last.setOnClickListener(v->{selectedChapter=recentChapter;selectedTopic=recent;showTopic(recent);});root.addView(last,lp(-1,46,0,0,0,3));}
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.VERTICAL);stats.setPadding(dp(2),dp(7),dp(2),dp(4));stats.setBackgroundColor(android.graphics.Color.TRANSPARENT);addText(stats,"ТВОЙ УЧЕБНЫЙ РИТМ",10,PINK,true);addText(stats,done+" из "+total+" тем завершено",15,DARK,true);addText(stats,"Продолжай в своём темпе — каждый пройденный раздел приближает к цели ♡",10,MUTED,false);ProgressBar allBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);allBar.setMax(Math.max(1,total));allBar.setProgress(done);allBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));allBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF3E5EA));stats.addView(allBar,lp(-1,5,0,5,0,0));root.addView(stats,lp(-1,-2,0,5,0,2));
    }

    private void showMathChapters() {
        currentScreen = "math_chapters"; base("Математика 📐", "Выбери раздел и продолжай обучение");
        int number = 1;
        for (String chapter : chapters.keySet()) {
            if (chapter.startsWith("CUSTOM:")) continue;
            final String selected = chapter; String pretty = chapter.replaceFirst("^[^ ]+\\s*", "");
            actionRegular("РАЗДЕЛ " + number + "\n" + pretty, chapters.get(chapter).size() + " учебных тем", () -> { selectedChapter = selected; showTopics(selected); }); number++;
        }
    }
    private void addCustomLesson() {
        final String[] chosenEmoji={"📚"};String[] emojis={"📚","📐","🌸","🧪","🌍","🎨","🎵","📝","💡","⭐","📖","✏️","🧠","🔬","🧬","🧮","🗺️","🏛️","🎭","🎹","🪴","🦋","☀️","🌙","💗","💜","🩵","🍀","🌷","🪻","🧸","🐱","🐰","🍓","🍒","🍉","🍰","🧁","☕","🫧","✨","🔥","🏆","🎯","💫","🪄","🧩","🪐","🌈","🦄"};
        LinearLayout dialogBody=new LinearLayout(this);dialogBody.setOrientation(LinearLayout.VERTICAL);dialogBody.setPadding(dp(20),dp(8),dp(20),dp(4));TextView hint=text("Создай отдельный предмет и добавляй в него свои темы. Например: английский, история или биология.",13,MUTED,false);hint.setLineSpacing(dp(3),1f);dialogBody.addView(hint,lp(-1,-2,0,0,0,10));
        LinearLayout emojiRow=new LinearLayout(this);emojiRow.setGravity(Gravity.CENTER_VERTICAL);String[] basicEmojis={"📚","📐","🌸","🧪"};TextView[] basicViews=new TextView[basicEmojis.length];
        Runnable refreshBasic=()->{for(int i=0;i<basicViews.length;i++){boolean on=chosenEmoji[0].equals(basicEmojis[i]);basicViews[i].setBackground(shape(on?0xFFFFE3EC:PALE,13,on?PINK:0));}};
        for(int i=0;i<basicEmojis.length;i++){final String emoji=basicEmojis[i];TextView option=text(emoji,24,DARK,false);option.setGravity(Gravity.CENTER);basicViews[i]=option;LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(48),1f);ep.setMargins(dp(2),0,dp(2),0);emojiRow.addView(option,ep);option.setOnClickListener(v->{chosenEmoji[0]=emoji;refreshBasic.run();});}
        TextView more=text("＋",25,PINK,true);more.setGravity(Gravity.CENTER);more.setContentDescription(translate("Добавить эмодзи к теме"));more.setBackground(shape(0xFFFFF0F4,13,0xFFF2DDE4));emojiRow.addView(more,lp(48,48,3,0,0,0));more.setOnClickListener(v->{android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(5);for(String emoji:emojis){TextView option=text(emoji,23,DARK,false);option.setGravity(Gravity.CENTER);option.setBackground(shape(chosenEmoji[0].equals(emoji)?0xFFFFE3EC:PALE,12,chosenEmoji[0].equals(emoji)?PINK:0));android.widget.GridLayout.LayoutParams ep=new android.widget.GridLayout.LayoutParams();ep.width=dp(48);ep.height=dp(44);ep.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(option,ep);option.setOnClickListener(q->{chosenEmoji[0]=emoji;refreshBasic.run();showAppMessage("Эмодзи выбрано");((android.app.Dialog)grid.getTag()).dismiss();});}android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.setPadding(dp(12),dp(8),dp(12),dp(8));scroll.addView(grid);android.app.AlertDialog picker=new android.app.AlertDialog.Builder(this).setTitle("Выбери эмодзи").setView(scroll).setNegativeButton("Закрыть",null).create();grid.setTag(picker);picker.show();});
        dialogBody.addView(emojiRow,lp(-1,52,0,0,0,10));EditText input = new EditText(this); input.setSingleLine(true); input.setTextSize(16);input.setPadding(dp(14),dp(8),dp(14),dp(8));input.setTextColor(DARK);input.setHint("Как назовём урок?");input.setBackground(shape(PALE,15,0));input.setHintTextColor(MUTED);dialogBody.addView(input,lp(-1,52,0,0,0,0));
        android.app.AlertDialog addDialog=new android.app.AlertDialog.Builder(this).setTitle("♡  Добавить свой урок").setView(dialogBody)
            .setNegativeButton("Отмена", null).setPositiveButton("Создать урок", (d,w) -> {
                String name=input.getText().toString().trim(); if(name.isEmpty()){showAppMessage("Введи название урока");return;}
                String id=String.valueOf(System.currentTimeMillis()); Set<String> ids=new TreeSet<>(prefs.getStringSet("custom_lesson_ids",new TreeSet<String>()));ids.add(id);
                prefs.edit().putStringSet("custom_lesson_ids",ids).putString("custom_lesson_name_"+id,name).putString("custom_lesson_emoji_"+id,chosenEmoji[0]).putStringSet("custom_lesson_topics_"+id,new TreeSet<String>()).apply();
                selectedChapter="CUSTOM:"+id+":"+name;chapters.put(selectedChapter,new ArrayList<String>());showTopics(selectedChapter);
            }).create();
        addDialog.setOnShowListener(v->{if(addDialog.getWindow()!=null)addDialog.getWindow().setBackgroundDrawable(shape(BG,22,0));addDialog.getButton(-1).setTextColor(PINK);addDialog.getButton(-2).setTextColor(MUTED);});
        addDialog.show();
    }
    private String topicContentKey(String lessonId,String topic){return "custom_topic_content_"+lessonId+"_"+android.util.Base64.encodeToString(topic.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP);}
    private void openCustomTopicDialog(String chapter,String oldName,String oldContent){
        String[] parts=chapter.split(":",3);if(parts.length<3)return;String lessonId=parts[1];
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(8),dp(20),dp(4));
        TextView note=text("Название и материал темы можно будет изменить позже.",12,MUTED,false);body.addView(note,lp(-1,-2,0,0,0,8));
        EditText name=new EditText(this);name.setSingleLine(true);name.setTextSize(16);name.setTextColor(DARK);name.setHint("Название темы");name.setText(oldName==null?"":oldName);name.setPadding(dp(13),dp(8),dp(13),dp(8));name.setBackground(shape(PALE,14,0));name.setHintTextColor(MUTED);body.addView(name,lp(-1,50,0,0,0,9));
        EditText content=new EditText(this);content.setTextSize(15);content.setTextColor(DARK);content.setHint("Добавь конспект, объяснение или заметки…");content.setGravity(Gravity.TOP);content.setMinLines(4);content.setText(oldContent);content.setPadding(dp(13),dp(12),dp(13),dp(12));content.setBackground(shape(PALE,14,0));content.setHintTextColor(MUTED);body.addView(content,lp(-1,118,0,0,0,0));
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle(oldName==null?"♡  Новая тема":"✎  Изменить тему").setView(body).setNegativeButton("Отмена",null).setPositiveButton(oldName==null?"Добавить":"Сохранить",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(q->{
            String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Введи название");name.requestFocus();return;}
            Set<String> topics=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+lessonId,new TreeSet<String>()));
            if(topics.contains(n)&&(oldName==null||!oldName.equals(n))){name.setError("Такая тема уже существует");name.requestFocus();return;}
            SharedPreferences.Editor e=prefs.edit();
            if(oldName!=null&&!oldName.equals(n)){topics.remove(oldName);topics.add(n);int oldProgress=prefs.getInt("progress_"+oldName,0);e.remove("progress_"+oldName).remove("done_"+oldName);if(oldProgress>0)e.putInt("progress_"+n,oldProgress);e.remove(topicContentKey(lessonId,oldName));}else topics.add(n);
            e.putStringSet("custom_lesson_topics_"+lessonId,topics).putString(topicContentKey(lessonId,n),content.getText().toString().trim()).apply();chapters.put(chapter,new ArrayList<>(topics));if(oldName!=null&&oldName.equals(selectedTopic))selectedTopic=n;dialog.dismiss();showTopics(chapter);
        }));
        dialog.show();
    }
    private void addCustomTopic(String chapter){openCustomTopicDialog(chapter,null,"");}

    private void showTopics(String chapter) {
        currentScreen = "topics";
        boolean customLesson = chapter.startsWith("CUSTOM:");
        base(customLesson ? chapter.split(":", 3)[2] : chapter, "Выбери тему, которую хочешь изучить:");
        List<String> topicOrder=new ArrayList<>(chapters.get(chapter));
        if(customLesson&&"alpha".equals(prefs.getString("lesson_sort","default")))java.util.Collections.sort(topicOrder,String.CASE_INSENSITIVE_ORDER);
        for(String topic:topicOrder){
            int percent=topicProgressForChapter(chapter,topic);
            LinearLayout topicItem=action((percent==100?"✓  ":"♡  ")+topic,"Изучено на "+percent+"%",()->{selectedTopic=topic;showTopic(topic);});
            if(customLesson)topicItem.setOnLongClickListener(v->{confirmDeleteCustomTopic(chapter,topic);return true;});
        }
        if(customLesson){Button addTopic=button("＋  Добавить тему",true);addTopic.setOnClickListener(v->addCustomTopic(chapter));root.addView(addTopic,lp(-1,50,0,10,0,8));}
        // Возврат выполняется стрелкой в верхней панели.
    }

    private void confirmDeleteCustomLesson(String id,String name){
        new android.app.AlertDialog.Builder(this).setTitle("Удалить предмет?")
            .setMessage("«"+name+"» и все его темы будут удалены. Это действие нельзя отменить.")
            .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
                Set<String> ids=new TreeSet<>(prefs.getStringSet("custom_lesson_ids",new TreeSet<String>()));ids.remove(id);
                Set<String> topics=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+id,new TreeSet<String>()));
                SharedPreferences.Editor e=prefs.edit().putStringSet("custom_lesson_ids",ids).remove("custom_lesson_name_"+id).remove("custom_lesson_emoji_"+id).remove("custom_lesson_topics_"+id);
                int removedCompleted=0;for(String t:topics){if(prefs.getInt("progress_"+t,0)>=100||prefs.getBoolean("done_"+t,false))removedCompleted++;e.remove(topicContentKey(id,t)).remove("progress_"+t).remove("done_"+t);}if(removedCompleted>0)e.putInt("done_count",Math.max(0,prefs.getInt("done_count",0)-removedCompleted));
                e.apply();chapters.remove("CUSTOM:"+id+":"+name);
                if(selectedChapter!=null&&selectedChapter.startsWith("CUSTOM:"+id+":")){selectedChapter=null;selectedTopic=null;}
                showChapters();showAppMessage("Предмет удалён");
            }).show();
    }

    private void confirmDeleteCustomTopic(String chapter,String topic){
        String[] parts=chapter.split(":",3);if(parts.length<2)return;String id=parts[1];
        new android.app.AlertDialog.Builder(this).setTitle("Удалить тему?")
            .setMessage("Тема «"+topic+"» и её учебный материал будут удалены.")
            .setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{
                Set<String> topics=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+id,new TreeSet<String>()));topics.remove(topic);
                prefs.edit().putStringSet("custom_lesson_topics_"+id,topics).remove(topicContentKey(id,topic)).remove("progress_"+topic).remove("done_"+topic).apply();
                chapters.put(chapter,new ArrayList<>(topics));showTopics(chapter);showAppMessage("Тема удалена");
            }).show();
    }

    private void showTopic(String topic) {
        currentScreen = "topic";
        if(selectedChapter!=null)prefs.edit().putString("recent_chapter",selectedChapter).putString("recent_topic",topic).apply();
        selectedTopic=topic;
        base("Твоя тема 📖", "Учись в своём темпе — каждый шаг важен");
        int percent = topicProgress(topic);
        LinearLayout box = card();
        addText(box, topic, 20, DARK, true);
        String customBody="";if(selectedChapter!=null&&selectedChapter.startsWith("CUSTOM:")){String[] customParts=selectedChapter.split(":",3);if(customParts.length>=2)customBody=prefs.getString(topicContentKey(customParts[1],topic),"");}addText(box,customBody.isEmpty()?"Открой соответствующий урок в учебнике, изучи материал и реши задания. Отмечай свой прогресс — он сохранится автоматически.":customBody,15,MUTED,false);
        Button mini=button("📝  Мини-тест по теме · 5 вопросов",true);mini.setOnClickListener(v->startMiniTest(topic));root.addView(mini,lp(-1,50,0,0,0,12));
        topicProgressLabel = text("ТВОЙ ПРОГРЕСС: " + percent + "%", 13, PINK, true); box.addView(topicProgressLabel, lp(-1, -2, 0, 0, 0, 5));
        topicProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); topicProgressBar.setMax(100); topicProgressBar.setProgress(percent);
        if (Build.VERSION.SDK_INT >= 21) { topicProgressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK)); topicProgressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE2EA)); }
        box.addView(topicProgressBar, lp(-1, 8, 0, 0, 0, 5));
        root.addView(box, lp(-1, -2, 0, 0, 0, 14));
        addTopicStudyCard(topic);
        addTopicReflection(topic);
        addOwnQuestionCard(topic);
        addTopicPhoto(topic);
        if(selectedChapter!=null&&selectedChapter.startsWith("CUSTOM:")){String[] cp=selectedChapter.split(":",3);if(cp.length>=2){LinearLayout manage=new LinearLayout(this);Button editTopic=button("✎  Изменить содержание",false),deleteTopic=button("Удалить тему",false);manage.addView(editTopic,new LinearLayout.LayoutParams(0,dp(48),1f));manage.addView(deleteTopic,new LinearLayout.LayoutParams(0,dp(48),1f));editTopic.setOnClickListener(v->openCustomTopicDialog(selectedChapter,topic,prefs.getString(topicContentKey(cp[1],topic),"")));deleteTopic.setOnClickListener(v->{new android.app.AlertDialog.Builder(this).setTitle("Удалить тему?").setMessage("Тема и её содержание будут удалены.").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{Set<String> ts=new TreeSet<>(prefs.getStringSet("custom_lesson_topics_"+cp[1],new TreeSet<String>()));ts.remove(topic);prefs.edit().putStringSet("custom_lesson_topics_"+cp[1],ts).remove(topicContentKey(cp[1],topic)).remove("progress_"+topic).remove("done_"+topic).apply();chapters.put(selectedChapter,new ArrayList<>(ts));showTopics(selectedChapter);}).show();});root.addView(manage,lp(-1,48,0,0,0,12));}}

        addText(root, "ОТМЕТИТЬ ПРОГРЕСС", 12, PINK, true);
        LinearLayout levels = new LinearLayout(this);
        levels.setOrientation(LinearLayout.HORIZONTAL);
        int[] values = {25, 50, 75, 100};
        for (int index = 0; index < values.length; index++) {
            final int value = values[index];
            Button b = button(value + "%", percent >= value);
            progressButtons[index] = b;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
            params.setMargins(dp(2), dp(4), dp(2), dp(10));
            levels.addView(b, params);
            b.setOnClickListener(v -> {
                int oldProgress=topicProgress(topic); saveTopicProgress(topic,value);
                if(value==100)recordStudyDay(); if(oldProgress<100&&value==100)playTopicCompleteSound();
                refreshTopicProgress(value);
                if (value == 100) showPraise();
            });
        }
        root.addView(levels, lp(-1, -2, 0, 0, 0, 8));

        Button done = button("✅  "+(g("Я позанималась сегодня","Я позанимался сегодня")), true);
        done.setOnClickListener(v -> markDone());
        root.addView(done, lp(-1, 54, 0, 4, 0, 10));
    }

    private String topicRule(String topic){
        String t=topic.toLowerCase(new Locale("ru"));
        if(t.contains("процент"))return "1% — это одна сотая часть числа. Чтобы найти p% от числа, умножь число на p/100. Чтобы найти число по его проценту, раздели известную часть на p/100.";
        if(t.contains("пропорц")||t.contains("отношение"))return "В пропорции a/b = c/d произведение крайних членов равно произведению средних: a · d = b · c. В отношении сохраняй один и тот же множитель для обеих величин.";
        if(t.contains("десятич")||t.contains("округлен")||t.contains("среднее"))return "Записывай числа так, чтобы запятые стояли друг под другом. При умножении учитывай общее число знаков после запятой, при делении перенеси запятую в делителе и делимом одинаково.";
        if(t.contains("сокращ"))return "Раздели числитель и знаменатель на одно и то же общее число, отличное от нуля. Значение дроби при этом не меняется.";
        if(t.contains("знаменател"))return "Чтобы сложить или вычесть дроби, сначала приведи их к общему знаменателю. Затем выполни действие с числителями.";
        if(t.contains("сравнен"))return "Сначала приведи числа к сопоставимому виду. Для дробей с одинаковым знаменателем сравни числители; для десятичных дробей сравни разряды слева направо.";
        if(t.contains("смешан"))return "Для сложения или вычитания смешанных чисел отдельно работай с целыми и дробными частями. Если нужно, преобразуй смешанное число в неправильную дробь.";
        if(t.contains("умнож"))return "При умножении дробей перемножь числители и знаменатели. Перед вычислением можно сократить общий множитель по диагонали.";
        if(t.contains("делен"))return "Деление на дробь замени умножением на обратную дробь. Переверни вторую дробь, затем перемножь.";
        if(t.contains("части числа"))return "Чтобы найти часть числа, раздели число на знаменатель и умножь на числитель. Чтобы найти целое по части, выполни обратные действия.";
        if(t.contains("округлен"))return "Посмотри на цифру справа от нужного разряда: 0–4 — оставь разряд без изменения, 5–9 — увеличь его на единицу.";
        if(t.contains("делим")||t.contains("делител")||t.contains("кратн"))return "Делитель делит число без остатка. Кратные получаются умножением числа на натуральные множители.";
        if(t.contains("прост")||t.contains("состав"))return "Простое число имеет ровно два натуральных делителя: 1 и само число. У составного делителей больше двух.";
        if(t.contains("квадрат")||t.contains("куб"))return "Квадрат числа — произведение числа на себя. Куб — произведение трёх одинаковых множителей.";
        if(t.contains("выражен"))return "Соблюдай порядок действий: сначала скобки, затем умножение и деление, после этого сложение и вычитание.";
        if(t.contains("сложен")||t.contains("вычит"))return "При сложении и вычитании выравнивай разряды. Проверяй вычитание обратным сложением.";
        if(t.contains("умножен")||t.contains("делен"))return "Умножение можно проверить делением, а деление — умножением. Следи за разрядами и остатком.";
        return "Прочитай условие, выдели известные данные, выбери подходящее действие и проверь результат обратным действием.";
    }
    private String topicExample(String topic){
        String t=topic.toLowerCase(new Locale("ru"));
        if(t.contains("процент"))return "20% от 150: 150 · 20/100 = 30.";
        if(t.contains("пропорц")||t.contains("отношение"))return "2 : 3 = 8 : 12, потому что обе части первого отношения умножили на 4.";
        if(t.contains("десятич"))return "2,4 + 0,35 = 2,75. Запятые записываем друг под другом.";
        if(t.contains("сокращ"))return "6/8 = 3/4: делим числитель и знаменатель на 2.";
        if(t.contains("знаменател"))return "1/3 + 1/4 = 4/12 + 3/12 = 7/12.";
        if(t.contains("сравнен"))return "3/8 меньше 5/8, потому что знаменатели одинаковы, а 3 меньше 5.";
        if(t.contains("смешан"))return "2 1/3 = (2 · 3 + 1)/3 = 7/3.";
        if(t.contains("умнож"))return "2/3 · 3/5 = 6/15 = 2/5.";
        if(t.contains("делен"))return "2/3 ÷ 4/5 = 2/3 · 5/4 = 10/12 = 5/6.";
        if(t.contains("части числа"))return "3/4 от 20: 20 ÷ 4 · 3 = 15.";
        if(t.contains("округлен"))return "7,46 до десятых = 7,5.";
        if(t.contains("квадрат"))return "6² = 6 · 6 = 36; 6³ = 6 · 6 · 6 = 216.";
        if(t.contains("выражен"))return "8 + 3 · 4 = 8 + 12 = 20.";
        return "Если 24 разделить на 6, получится 4; проверка: 4 · 6 = 24.";
    }
    private void addTopicStudyCard(String topic){
        LinearLayout c=card();addText(c,"💡  КАРТОЧКА ТЕМЫ",11,PINK,true);addText(c,"Главное правило",15,DARK,true);addText(c,topicRule(topic),13,MUTED,false);addText(c,"Пример решения",14,DARK,true);addText(c,topicExample(topic),14,DARK,false);
        addText(c,"ШАГИ РЕШЕНИЯ",10,PINK,true);String lower=topic.toLowerCase(new Locale("ru"));String[] steps=lower.contains("знаменател")?new String[]{"Найди общий знаменатель для дробей","Преобразуй дроби, сохранив их значения","Выполни действие с числителями и сократи ответ"}:lower.contains("процент")?new String[]{"Переведи процент в дробь со знаменателем 100","Умножь исходное число на эту дробь","Проверь, соответствует ли результат условию"}:lower.contains("десятич")?new String[]{"Выравняй запятые и разряды","Выполни действие по разрядам","Проверь положение запятой в результате"}:lower.contains("смешан")?new String[]{"Преобразуй смешанные числа в неправильные дроби","Выполни действие с дробями","Сократи ответ и при необходимости верни смешанное число"}:lower.contains("делен")?new String[]{"Запиши обратную дробь для делителя","Замени деление умножением","Перемножь и сократи результат"}:new String[]{"Прочитай условие и выдели известные данные","Выбери подходящее правило и выполни действие","Проверь ответ обратным действием"};
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<steps.length;i++){TextView step=text((i+1)+".  "+steps[i],12,DARK,false);step.setPadding(dp(10),dp(8),dp(10),dp(8));step.setBackground(shape(0xFFFFF4F7,13,0));line.addView(step,lp(-1,-2,0,0,0,5));step.setAlpha(0f);step.setTranslationY(dp(8));final TextView animated=step;new Handler(Looper.getMainLooper()).postDelayed(()->animated.animate().alpha(1f).translationY(0).setDuration(220).start(),i*180L);}
        c.addView(line);root.addView(c,lp(-1,-2,0,0,0,12));
    }
    private void addTopicReflection(String topic){
        LinearLayout c=card();addText(c,"✍️  ОБЪЯСНИ СВОИМИ СЛОВАМИ",11,PINK,true);addText(c,"Представь, что объясняешь правило другу. Что самое важное нужно запомнить?",13,MUTED,false);
        EditText explanation=new EditText(this);explanation.setHint("Я понял(а), что...");explanation.setTextSize(14);explanation.setTextColor(DARK);explanation.setGravity(Gravity.TOP);explanation.setMinLines(3);explanation.setPadding(dp(12),dp(9),dp(12),dp(9));explanation.setBackground(shape(0xFFFFFBFC,14,0xFFEADDE2));String saved=prefs.getString("topic_explanation_"+topicStorageSuffix(selectedChapter,topic),"");explanation.setText(saved);c.addView(explanation,lp(-1,96,0,7,0,8));
        Button save=button("Сохранить объяснение в дневник",false);save.setOnClickListener(v->{String value=explanation.getText().toString().trim();if(value.isEmpty()){explanation.setError("Напиши объяснение");return;}String id=String.valueOf(System.currentTimeMillis());String entry="Моё объяснение темы «"+topic+"»:\\n"+value;Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));ids.add(id);prefs.edit().putString("topic_explanation_"+topicStorageSuffix(selectedChapter,topic),value).putString("diary_"+id,entry).putString("diary_tag_"+id,"Учёба").putStringSet("diary_ids",ids).apply();showAppMessage("Объяснение сохранено в дневник ♡");});c.addView(save,lp(-1, 48,0,0,0,0));root.addView(c,lp(-1,-2,0,0,0,12));
    }
    private void addTopicPhoto(String topic){
        String suffix=topicStorageSuffix(selectedChapter,topic),key="topic_photo_"+suffix,uri=prefs.getString(key,""),file=prefs.getString("topic_photo_file_"+suffix,"");LinearLayout c=card();addText(c,"📷  ФОТО ЗАДАЧИ",11,PINK,true);addText(c,"Сохрани снимок задания из учебника, чтобы вернуться к нему позже.",12,MUTED,false);
        if(!uri.isEmpty()||!file.isEmpty()){try{ImageView image=new ImageView(this);if(!file.isEmpty())image.setImageBitmap(android.graphics.BitmapFactory.decodeFile(file));else image.setImageURI(android.net.Uri.parse(uri));image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);c.addView(image,lp(-1,190,0,6,0,7));}catch(Exception ignored){}}
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);Button camera=button("📷  Камера",false),gallery=button("Из галереи",false);actions.addView(camera,new LinearLayout.LayoutParams(0,dp(48),1f));actions.addView(gallery,new LinearLayout.LayoutParams(0,dp(48),1f));camera.setOnClickListener(v->{Intent take=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);if(take.resolveActivity(getPackageManager())!=null)startActivityForResult(take,PICK_TOPIC_CAMERA);else showAppMessage("Камера недоступна");});gallery.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("image/*");pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,PICK_TOPIC_PHOTO);});c.addView(actions,lp(-1,48,0,5,0,0));root.addView(c,lp(-1,-2,0,0,0,12));
    }
    private void loadQuestionBank(){
        questionBank.clear();
        try(java.io.InputStream in=getAssets().open("question_bank.json")){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))>0)out.write(buffer,0,n);
            org.json.JSONObject data=new org.json.JSONObject(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));
            org.json.JSONArray all=data.getJSONArray("questions");
            for(int i=0;i<all.length();i++){
                org.json.JSONObject item=all.getJSONObject(i);String topic=item.getString("topic"),type=item.optString("type","choice"),answer=item.optString("answer","");
                org.json.JSONArray arr=item.optJSONArray("options");String[] options=null;int correct=-1;
                if(arr!=null){options=new String[arr.length()];for(int k=0;k<arr.length();k++){options[k]=arr.getString(k);if(options[k].trim().equalsIgnoreCase(answer.trim()))correct=k;}}
                MiniQuestion q=new MiniQuestion(item.getString("prompt"),options,correct,item.optString("explanation",""),type,answer);q.topic=topic;
                List<MiniQuestion> list=questionBank.get(topic);if(list==null){list=new ArrayList<>();questionBank.put(topic,list);}list.add(q);
            }
        }catch(Exception e){android.util.Log.e("StudyWithLove","Question bank load failed",e);}
        Set<String> customCards=prefs.getStringSet("custom_question_cards",new TreeSet<String>());
        for(String payload:customCards){try{org.json.JSONObject item=new org.json.JSONObject(payload);String topic=item.optString("topic",""),prompt=item.optString("prompt","").trim(),answer=item.optString("answer","").trim();if(topic.isEmpty()||prompt.isEmpty()||answer.isEmpty())continue;List<MiniQuestion> list=questionBank.get(topic);if(list==null){list=new ArrayList<>();questionBank.put(topic,list);}boolean duplicate=false;for(MiniQuestion existing:list)if(normalizeAnswer(existing.prompt).equals(normalizeAnswer(prompt))){duplicate=true;break;}if(!duplicate){MiniQuestion q=new MiniQuestion(prompt,null,-1,item.optString("note",""),"input",answer);q.topic=topic;list.add(q);}}catch(Exception ignored){}}
    }
    private void addOwnQuestionCard(String topic){
        Set<String> saved=prefs.getStringSet("custom_question_cards",new TreeSet<String>());int count=0;for(String payload:saved)try{if(topic.equals(new org.json.JSONObject(payload).optString("topic","")))count++;}catch(Exception ignored){}
        LinearLayout section=card();addText(section,"✎  МОИ КАРТОЧКИ И ЗАМЕТКИ",11,PINK,true);addText(section,"Добавляй свои вопросы прямо во время учёбы. Они сохраняются на устройстве и попадут в мини-тесты этой темы.",12,MUTED,false);addText(section,"Сохранено карточек: "+count,12,DARK,true);
        Button add=button("＋  Быстро добавить карточку",true);section.addView(add,lp(-1, 48,0,5,0,0));
        add.setOnClickListener(v->{
            LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(4),dp(4),dp(4),dp(2));
            EditText prompt=new EditText(this);prompt.setHint("Вопрос или формулировка правила");prompt.setMinLines(2);prompt.setGravity(Gravity.TOP);prompt.setTextSize(14);prompt.setTextColor(DARK);form.addView(prompt,lp(-1,78,0,0,0,8));
            EditText answer=new EditText(this);answer.setHint("Краткий правильный ответ");answer.setSingleLine(true);answer.setTextSize(14);answer.setTextColor(DARK);form.addView(answer,lp(-1,48,0,0,0,8));
            EditText note=new EditText(this);note.setHint("Пояснение или заметка (необязательно)");note.setMinLines(2);note.setGravity(Gravity.TOP);note.setTextSize(13);note.setTextColor(DARK);form.addView(note,lp(-1,68,0,0,0,0));
            android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Новая карточка").setView(form).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v2->{
                String p=prompt.getText().toString().trim(),a=answer.getText().toString().trim(),n=note.getText().toString().trim();
                if(p.length()<5||p.length()>240){prompt.setError("Вопрос должен содержать от 5 до 240 символов");return;}
                if(a.isEmpty()||a.length()>120){answer.setError("Ответ должен содержать от 1 до 120 символов");return;}
                if(n.length()>400){note.setError("Пояснение — не более 400 символов");return;}
                List<MiniQuestion> existing=questionBank.get(topic);if(existing!=null)for(MiniQuestion q:existing)if(normalizeAnswer(q.prompt).equals(normalizeAnswer(p))){prompt.setError("Такой вопрос уже есть в этой теме");return;}
                int ownCount=0;for(String raw:prefs.getStringSet("custom_question_cards",new TreeSet<String>()))try{if(topic.equals(new org.json.JSONObject(raw).optString("topic","")))ownCount++;}catch(Exception ignored){}
                if(ownCount>=100){showAppMessage("В одной теме можно сохранить не более 100 карточек");return;}
                try{org.json.JSONObject item=new org.json.JSONObject();item.put("topic",topic);item.put("prompt",p);item.put("answer",a);item.put("note",n);Set<String> cards=new TreeSet<>(prefs.getStringSet("custom_question_cards",new TreeSet<String>()));cards.add(item.toString());prefs.edit().putStringSet("custom_question_cards",cards).apply();loadQuestionBank();dialog.dismiss();showTopic(topic);showAppMessage("Карточка сохранена ♡");}catch(Exception ex){showAppMessage("Не удалось сохранить карточку");}
            }));dialog.show();
        });
        Button manage=button("Мои карточки · посмотреть и удалить",false);section.addView(manage,lp(-1, 48,0,6,0,0));
        manage.setOnClickListener(v->{List<String> payloads=new ArrayList<>(),labels=new ArrayList<>();for(String raw:prefs.getStringSet("custom_question_cards",new TreeSet<String>()))try{org.json.JSONObject item=new org.json.JSONObject(raw);if(topic.equals(item.optString("topic",""))){payloads.add(raw);labels.add(item.optString("prompt","Карточка"));}}catch(Exception ignored){}if(payloads.isEmpty()){showAppMessage("В этой теме пока нет своих карточек");return;}new android.app.AlertDialog.Builder(this).setTitle("Мои карточки").setItems(labels.toArray(new String[0]),(dlg,index)->{String raw=payloads.get(index);try{org.json.JSONObject item=new org.json.JSONObject(raw);new android.app.AlertDialog.Builder(this).setTitle(item.optString("prompt","Карточка")).setMessage("Ответ: "+item.optString("answer","")+(item.optString("note","").isEmpty()?"":"\\n\\nЗаметка: "+item.optString("note",""))).setNegativeButton("Закрыть",null).setPositiveButton("Удалить",(d,w)->{Set<String> cards=new TreeSet<>(prefs.getStringSet("custom_question_cards",new TreeSet<String>()));cards.remove(raw);prefs.edit().putStringSet("custom_question_cards",cards).apply();loadQuestionBank();showTopic(topic);}).show();}catch(Exception ignored){}}).show();});
        root.addView(section,lp(-1,-2,0,0,0,12));
    }
    private void startMiniTest(String topic){
        miniTopic=topic;miniQuestions.clear();miniWrongQuestions.clear();miniDeferredFeedback.setLength(0);miniScore=0;weaknessTrainingMode=false;miniResultSaved=false;
        List<MiniQuestion> bank=questionBank.get(topic);
        if(bank!=null&&!bank.isEmpty()){
            List<MiniQuestion> shuffled=new ArrayList<>(bank);java.util.Collections.shuffle(shuffled,quizRandom);
            miniQuestions.addAll(shuffled.subList(0,Math.min(5,shuffled.size())));
        }else for(int i=0;i<5;i++)miniQuestions.add(makeMiniQuestion(topic));
        renderMiniTest(0,false);
    }
    private void startWeaknessTraining(){
        miniTopic="Мои слабые места";miniQuestions.clear();miniWrongQuestions.clear();miniDeferredFeedback.setLength(0);miniScore=0;weaknessTrainingMode=true;miniResultSaved=false;
        mistakeStore().migrateLegacy(prefs);
        long nextDue=Long.MAX_VALUE;for(String payload:mistakeStore().newestPayloads(10)){try{org.json.JSONObject o=new org.json.JSONObject(payload);long due=o.optLong("next_review_at",0L);if(due>System.currentTimeMillis()){nextDue=Math.min(nextDue,due);continue;}org.json.JSONArray a=o.optJSONArray("options");String[] opts=null;int correct=-1;if(a!=null){opts=new String[a.length()];for(int i=0;i<a.length();i++){opts[i]=a.getString(i);if(opts[i].equalsIgnoreCase(o.optString("answer")))correct=i;}}MiniQuestion restored=new MiniQuestion(o.optString("prompt"),opts,correct,o.optString("explanation"),o.optString("type","choice"),o.optString("answer"));restored.topic=o.optString("topic","");miniQuestions.add(restored);}catch(Exception ignored){}}
        if(miniQuestions.isEmpty()){showWeaknessTrainer();if(nextDue!=Long.MAX_VALUE)showAppMessage("Следующее повторение запланировано на "+new SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(new Date(nextDue)));return;}renderMiniTest(0,false);
    }
    private String classifyMistake(MiniQuestion q,String userAnswer){String t=(q.topic+" "+q.prompt).toLowerCase(new Locale("ru"));if(t.contains("сокращ"))return "Сокращение";if(t.contains("знаменател"))return "Общий знаменатель";if(t.contains("округлен")||t.contains("десятич")||t.contains("запят"))return "Запятая или разряд";if(t.contains("вычит")||q.prompt.contains("−")||q.prompt.contains(" - "))return "Знак действия";if(t.contains("делен"))return "Деление";return "Вычисление";}
    private void recordMistake(MiniQuestion q,String userAnswer){
        try{
            org.json.JSONObject o=new org.json.JSONObject();o.put("topic",q.topic==null||q.topic.isEmpty()?miniTopic:q.topic);o.put("next_review_at",System.currentTimeMillis()+Math.max(1,prefs.getInt("weakness_review_interval_days",1))*86400000L);o.put("kind",classifyMistake(q,userAnswer));o.put("type",q.type);o.put("prompt",q.prompt);o.put("answer",q.answer);o.put("user_answer",userAnswer);o.put("explanation",q.explanation);
            org.json.JSONArray a=new org.json.JSONArray();if(q.options!=null)for(String option:q.options)a.put(option);o.put("options",a);
            String topic=q.topic==null||q.topic.isEmpty()?miniTopic:q.topic;mistakeStore().migrateLegacy(prefs);mistakeStore().save(topic,q.prompt,o.toString());
        }catch(Exception ignored){}
    }
    private void clearMistake(MiniQuestion q){
        try{mistakeStore().migrateLegacy(prefs);String topic=q.topic==null?"":q.topic;mistakeStore().delete(topic,q.prompt);}catch(Exception ignored){}
    }
    private String normalizeAnswer(String value){return value==null?"":value.trim().toLowerCase(new Locale("ru")).replace(" ","").replace(",",".").replace("−","-");}
    private void acceptMiniAnswer(MiniQuestion q,String userAnswer,int index,boolean wrongOnly,View source){
        boolean correct=normalizeAnswer(userAnswer).equals(normalizeAnswer(q.answer));
        if(correct){miniScore++;clearMistake(q);haptic(source,true);playCorrectAnswerSound();}else{miniWrongQuestions.add(q);recordMistake(q,userAnswer);haptic(source,false);if(source!=null)source.animate().translationX(dp(8)).setDuration(45).withEndAction(()->source.animate().translationX(-dp(8)).setDuration(45).withEndAction(()->source.animate().translationX(0).setDuration(45).start()).start()).start();}
        if(source instanceof Button){Button chosen=(Button)source;chosen.setBackground(shape(correct?0xFFE4F4E8:0xFFFFE8E8,22,correct?0xFF75B987:0xFFE58A8A));chosen.setTextColor(correct?0xFF287A43:0xFFB64242);new Handler(android.os.Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()){chosen.setBackground(shape(0xFFFFFFFF,22,0xFFEADDE2));chosen.setTextColor(DARK);}},1050);}
        boolean immediate=prefs.getBoolean("show_correct_immediately",true);
        if(!correct&&!immediate){if(miniDeferredFeedback.length()>0)miniDeferredFeedback.append("\\n\\n");miniDeferredFeedback.append("• ").append(q.prompt).append("\\nПравильный ответ: ").append(q.answer).append("\\n").append(q.explanation);}
        TextView feedback=text(correct?"✓  Верно! "+q.explanation:immediate?("✦  Правильный ответ: "+q.answer+"\\n"+q.explanation):"Ответ сохранён. Разбор появится в конце теста.",13,correct?0xFF287A43:0xFF9B4A58,true);feedback.setPadding(dp(12),dp(10),dp(12),dp(10));feedback.setBackground(shape(correct?0xFFF0F8F1:0xFFFFF2F2,14,0));root.addView(feedback,lp(-1,-2,0,7,0,4));
        if(!correct){
            Button explainAi=button("✧  Объясни эту ошибку",false);
            explainAi.setContentDescription("Попросить ИИ объяснить ошибку в задании");
            explainAi.setOnClickListener(v->{
                Intent intent=new Intent(this,AiTutorActivity.class);
                String context="Помоги разобраться с ошибкой в учебном задании. Тема: "+(q.topic==null||q.topic.isEmpty()?miniTopic:q.topic)+". Вопрос: "+q.prompt+". Мой неверный ответ: "+userAnswer+". Правильный ответ: "+q.answer+". Краткое объяснение: "+q.explanation+". Объясни пошагово и доброжелательно, не просто сообщай ответ.";
                intent.putExtra("study_context",context);
                intent.putExtra("offline_fallback","Нет интернета — вот разбор из банка:\n\nТема: "+(q.topic==null||q.topic.isEmpty()?miniTopic:q.topic)+"\nЗадание: "+q.prompt+"\nПравильный ответ: "+q.answer+"\n\n"+q.explanation);
                startActivity(intent);
            });
            root.addView(explainAi,lp(-1,48,0,0,0,4));
        }
        if(!wrongOnly){Button similar=button("↻  Дай похожую задачу",false);similar.setOnClickListener(v->{MiniQuestion next=makeMiniQuestion(q.topic==null||q.topic.isEmpty()?miniTopic:q.topic);miniQuestions.add(Math.min(index+1,miniQuestions.size()),next);showAppMessage("Похожая задача добавлена в тест");});root.addView(similar,lp(-1,46,0,0,0,4));}
        new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing())renderMiniTest(index+1,wrongOnly);},correct?1050:3500);
    }
    private MiniQuestion makeMiniQuestion(String topic){
        String t=topic.toLowerCase(new Locale("ru"));
        boolean pct=t.contains("процент")||t.contains("отношение")||t.contains("пропорц");
        boolean dec=t.contains("десятич")||t.contains("округлен")||t.contains("среднее арифмет");
        boolean frac=t.contains("дроб")||t.contains("смешан")||t.contains("часть числа")||t.contains("числа по его части");
        int answer;String prompt,explain;boolean decimalAnswer=false;
        if(pct){int a=20+quizRandom.nextInt(181);if(t.contains("пропорц")||t.contains("отношение")){int unitPrice=2+quizRandom.nextInt(6),baseCount=2+quizRandom.nextInt(5);answer=unitPrice*2;prompt=baseCount+" одинаковых предмета стоят "+(unitPrice*baseCount)+". Сколько стоят "+(baseCount*2)+" таких предметов?";explain="Количество увеличилось в 2 раза, значит стоимость тоже: "+(unitPrice*baseCount)+" · 2 = "+answer+".";}else{int p=10*(1+quizRandom.nextInt(8));answer=a*p/100;prompt="Найди "+p+"% от "+a;explain=p+"% от "+a+" = "+answer+".";}}
        else if(dec){decimalAnswer=true;int a=12+quizRandom.nextInt(80),b=11+quizRandom.nextInt(70);answer=a+b;prompt="Вычисли "+String.format(Locale.GERMANY,"%.1f",a/10.0)+" + "+String.format(Locale.GERMANY,"%.1f",b/10.0);explain=String.format(Locale.GERMANY,"%.1f + %.1f = %.1f",a/10.0,b/10.0,answer/10.0);}
        else if(frac){int d=2+quizRandom.nextInt(8),n=2+quizRandom.nextInt(9);answer=n;prompt="Найди 1/"+d+" от "+(d*n);explain="Одна "+1+"/"+d+" часть от "+(d*n)+" равна "+answer+".";}
        else{int a=17+quizRandom.nextInt(70),b=6+quizRandom.nextInt(30);answer=a+b;prompt="Вычисли "+a+" + "+b;explain=a+" + "+b+" = "+answer+".";}
        List<Integer> order=new ArrayList<>();order.add(answer);int attempts=0;while(order.size()<4&&attempts++<100){int delta=1+quizRandom.nextInt(9);int candidate=answer+(quizRandom.nextBoolean()?delta:-delta);if(candidate>=0&&!order.contains(candidate))order.add(candidate);}while(order.size()<4)order.add(answer+order.size()+attempts++);
        java.util.Collections.shuffle(order,quizRandom);String[] opts=new String[4];int correct=0;for(int i=0;i<4;i++){opts[i]=decimalAnswer?String.format(Locale.GERMANY,"%.1f",order.get(i)/10.0):String.valueOf(order.get(i));if(order.get(i)==answer)correct=i;}
        MiniQuestion generated=new MiniQuestion(prompt,opts,correct,explain,"choice",opts[correct]);generated.topic=topic;return generated;
    }
    private void renderMiniTest(int index,boolean wrongOnly){
        List<MiniQuestion> list=wrongOnly?miniWrongQuestions:miniQuestions;
        if(index>=list.size()){showMiniTestResult(wrongOnly);return;}
        currentScreen="mini_test";base((weaknessTrainingMode?"Мои слабые места":"Мини-тест")+" · "+(index+1)+"/"+list.size(),"Отвечай в своём темпе — после ответа будет короткое объяснение");
        MiniQuestion original=list.get(index);MiniQuestion q=new MiniQuestion(original.prompt,original.options==null?null:original.options.clone(),original.correct,original.explanation,original.type,original.answer);q.topic=original.topic;if(q.options!=null)java.util.Collections.shuffle(java.util.Arrays.asList(q.options),quizRandom);LinearLayout panel=card();panel.setPadding(dp(16),dp(18),dp(16),dp(18));addText(panel,"ВОПРОС "+(index+1)+" ИЗ "+list.size(),11,PINK,true);addText(panel,q.type.equals("input")?"✍️  ВПИШИ ОТВЕТ":q.type.equals("true_false")?"⚖️  ВЕРНО ИЛИ НЕВЕРНО":q.type.equals("find_error")?"🔎  НАЙДИ ОШИБКУ":"ВЫБЕРИ ОТВЕТ",10,MUTED,true);addText(panel,q.prompt,20,DARK,true);root.addView(panel,lp(-1,-2,0,0,0,14));
        final boolean[] answered={false};
        if("input".equals(q.type)){
            EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(19);input.setTextColor(DARK);input.setHint("Твой ответ");input.setPadding(dp(14),dp(8),dp(14),dp(8));input.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));root.addView(input,lp(-1,52,0,0,0,9));
            Button submit=button("Проверить ответ",true);root.addView(submit,lp(-1,48,0,0,0,8));submit.setOnClickListener(v->{if(answered[0])return;String value=input.getText().toString();if(value.trim().isEmpty()){input.setError("Введи ответ");return;}answered[0]=true;acceptMiniAnswer(q,value,index,wrongOnly,submit);});
        }else{
            String[] opts=q.options==null?new String[]{"Верно","Неверно"}:q.options;Button[] answerButtons=new Button[opts.length];
            for(int i=0;i<opts.length;i++){final String answer=opts[i];Button b=button(answer,false);answerButtons[i]=b;b.setTextSize(17);root.addView(b,lp(-1,50,0,0,0,8));b.setOnClickListener(v->{if(answered[0])return;answered[0]=true;for(int k=0;k<opts.length;k++){Button choice=answerButtons[k];boolean isRight=normalizeAnswer(opts[k]).equals(normalizeAnswer(q.answer));if(isRight){choice.setBackground(shape(0xFFE4F4E8,22,0xFF75B987));choice.setTextColor(0xFF287A43);}else if(choice==b){choice.setBackground(shape(0xFFFFE8E8,22,0xFFE58A8A));choice.setTextColor(0xFFB64242);}}acceptMiniAnswer(q,answer,index,wrongOnly,b);new Handler(android.os.Looper.getMainLooper()).postDelayed(()->{for(Button option:answerButtons){option.setBackground(shape(0xFFFFFFFF,22,0xFFEADDE2));option.setTextColor(DARK);}},260);});}
        }
        if(!q.explanation.isEmpty()){TextView hint=text("После ответа появится разбор решения",11,MUTED,false);root.addView(hint,lp(-1,-2,0,3,0,0));}
        Button quit=button("Завершить тест",false);quit.setOnClickListener(v->showMiniTestResult(wrongOnly));root.addView(quit,lp(-1,48,0,8,0,0));
    }
    private void saveMiniTestResult(){try{org.json.JSONObject o=new org.json.JSONObject();o.put("topic",miniTopic);o.put("score",miniScore);o.put("total",miniQuestions.size());o.put("wrong",miniWrongQuestions.size());o.put("date",todayKey());String key="test_result_"+System.currentTimeMillis();prefs.edit().putString(key,o.toString()).apply();List<String> keys=new ArrayList<>();for(String k:prefs.getAll().keySet())if(k.startsWith("test_result_"))keys.add(k);java.util.Collections.sort(keys);for(int i=0;i<keys.size()-100;i++)prefs.edit().remove(keys.get(i)).apply();}catch(Exception ignored){}}
    private void showMiniTestResult(boolean wrongOnly){
        currentScreen="mini_test_result";base(weaknessTrainingMode?"Результат тренажёра":"Результат мини-теста","Ошибки — это подсказка, что повторить");if(!miniResultSaved){saveMiniTestResult();miniResultSaved=true;}
        int total=miniQuestions.size();if(miniDeferredFeedback.length()>0){LinearLayout review=card();addText(review,"РАЗБОР ОТВЕТОВ",12,PINK,true);addText(review,miniDeferredFeedback.toString(),13,DARK,false);root.addView(review,lp(-1,-2,0,0,0,12));}LinearLayout resultCard=card();resultCard.setGravity(Gravity.CENTER);addText(resultCard,"ТВОЙ РЕЗУЛЬТАТ",11,PINK,true);TextView score=text(miniScore+" / "+total,38,DARK,true);score.setGravity(Gravity.CENTER);resultCard.addView(score,lp(-1,60,0,6,0,4));addText(resultCard,miniWrongQuestions.isEmpty()?"Все ответы верные — отлично!":"Ошибок: "+miniWrongQuestions.size()+". Они сохранены в журнале.",13,MUTED,false);root.addView(resultCard,lp(-1,-2,0,0,0,14));
        if(miniWrongQuestions.isEmpty()&&!pendingReviewKey.isEmpty()){prefs.edit().putBoolean(pendingReviewKey,true).apply();pendingReviewKey="";}
        if(!miniWrongQuestions.isEmpty()){Button repeat=button("↻  Повторить ошибки",true);repeat.setOnClickListener(v->{miniQuestions.clear();miniQuestions.addAll(miniWrongQuestions);miniWrongQuestions.clear();miniScore=0;miniResultSaved=false;renderMiniTest(0,false);});root.addView(repeat,lp(-1,50,0,0,0,8));}
        Button again=button(weaknessTrainingMode?"Пройти тренажёр ещё раз":"Пройти тест заново",false);again.setOnClickListener(v->{if(weaknessTrainingMode)startWeaknessTraining();else startMiniTest(miniTopic);});root.addView(again,lp(-1,48,0,0,0,8));backHome();
    }
    private int countMistakes(){int n=0;for(String k:prefs.getAll().keySet())if(k.startsWith("mistake_"))n++;return n;}
    private void showWeaknessTrainer(){
        currentScreen="weakness_trainer";base("Мои слабые места","Повторяй именно те задания, в которых ошибалась");
        Map<String,Integer> byTopic=new TreeMap<>();Map<String,Map<String,Integer>> kinds=new TreeMap<>();List<String> keys=new ArrayList<>();
        for(String key:prefs.getAll().keySet())if(key.startsWith("mistake_")){keys.add(key);try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(key,"{}"));String topic=o.optString("topic","Другая тема"),kind=o.optString("kind","Вычисление");byTopic.put(topic,byTopic.containsKey(topic)?byTopic.get(topic)+1:1);Map<String,Integer> group=kinds.get(topic);if(group==null){group=new TreeMap<>();kinds.put(topic,group);}group.put(kind,group.containsKey(kind)?group.get(kind)+1:1);}catch(Exception ignored){}}
        LinearLayout summary=card();summary.setBackgroundColor(android.graphics.Color.TRANSPARENT);summary.setElevation(0f);addText(summary,"ЗАПИСЕЙ В ЖУРНАЛЕ",11,PINK,true);addText(summary,String.valueOf(keys.size()),34,DARK,true);addText(summary,"Сохраняются неверные ответы и тип затруднения.",12,MUTED,false);root.addView(summary,lp(-1,-2,0,0,0,12));
        if(keys.isEmpty()) {
            LinearLayout calmEmpty=new LinearLayout(this);calmEmpty.setOrientation(LinearLayout.VERTICAL);calmEmpty.setGravity(Gravity.CENTER_HORIZONTAL);calmEmpty.setPadding(dp(12),dp(16),dp(12),dp(16));calmEmpty.setBackgroundColor(android.graphics.Color.TRANSPARENT);calmEmpty.setElevation(0f);
            TextView calmFlower=text("🌷",44,PINK,true);calmFlower.setGravity(Gravity.CENTER);calmEmpty.addView(calmFlower,lp(-1,62,0,0,0,8));
            TextView calmTitle=text("Пока всё получается!",18,DARK,true);calmTitle.setGravity(Gravity.CENTER);calmEmpty.addView(calmTitle,lp(-1,-2,0,0,0,7));
            TextView calmDescription=text("Здесь появятся темы, которые стоит повторить. Начни с любого урока.",14,MUTED,false);calmDescription.setGravity(Gravity.CENTER);calmEmpty.addView(calmDescription,lp(-1,-2,0,0,0,12));
            Button startLessons=button("Перейти к урокам",true);startLessons.setOnClickListener(v->showChapters());calmEmpty.addView(startLessons,lp(-1,48,0,0,0,0));
            root.addView(calmEmpty,lp(-1,-2,0,0,0,10));
        } else for(Map.Entry<String,Integer> e:byTopic.entrySet()){LinearLayout item=card();item.setBackgroundColor(android.graphics.Color.TRANSPARENT);item.setElevation(0f);addText(item,e.getKey(),14,DARK,true);addText(item,e.getValue()+" ошибок в журнале",12,MUTED,false);Map<String,Integer> group=kinds.get(e.getKey());if(group!=null){StringBuilder types=new StringBuilder("Типы: ");for(Map.Entry<String,Integer> kind:group.entrySet()){if(types.length()>6)types.append(" · ");types.append(kind.getKey()).append(" (").append(kind.getValue()).append(")");}addText(item,types.toString(),11,PINK,false);}root.addView(item,lp(-1,-2,0,0,0,6));}
        if(!keys.isEmpty()) {
            Button train=button("🧠  Тренировать мои ошибки",true);
            train.setOnClickListener(v->startWeaknessTraining());
            root.addView(train,lp(-1,50,0,10,0,8));
        }
    }
    private void addEmptyState(LinearLayout host, String emoji, String title, String description, String actionLabel, Runnable action) {
        LinearLayout empty = card();
        empty.setGravity(Gravity.CENTER_HORIZONTAL);
        empty.setPadding(dp(18), dp(18), dp(18), dp(18));
        TextView art = text(emoji, 44, PINK, true); art.setGravity(Gravity.CENTER); art.setContentDescription(title);
        empty.addView(art, lp(-1, 66, 0, 0, 0, 8));
        TextView heading = text(title, 17, DARK, true); heading.setGravity(Gravity.CENTER);
        empty.addView(heading, lp(-1, -2, 0, 0, 0, 6));
        TextView detail = text(description, 13, MUTED, false); detail.setGravity(Gravity.CENTER);
        empty.addView(detail, lp(-1, -2, 0, 0, 0, 12));
        Button actionButton = button(actionLabel, true); empty.addView(actionButton, lp(-1, 48, 0, 0, 0, 0));
        actionButton.setOnClickListener(v -> action.run()); host.addView(empty, lp(-1, -2, 0, 0, 0, 10));
    }

    private void addSpacedRepetitionSection(){
        LinearLayout section=card();addText(section,"🔁  ПОРА ПОВТОРИТЬ",12,PINK,true);int count=0;long now=System.currentTimeMillis();
        for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue()){
            String suffix=topicStorageSuffix(entry.getKey(),topic);long completed=prefs.getLong("topic_completed_at_"+suffix,0);if(completed<=0)continue;long age=(now-completed)/86400000L;
            for(int interval:new int[]{3,7,14})if(age>=interval&&!prefs.getBoolean("review_done_"+suffix+"_"+interval,false)){
                final int dueInterval=interval;count++;TextView item=text("•  "+topic+"  ·  "+interval+" "+dayWord(interval),13,DARK,true);section.addView(item,lp(-1,34,0,1,0,1));Button review=button("Повторить тему",false);review.setOnClickListener(v->{selectedChapter=entry.getKey();selectedTopic=topic;pendingReviewKey="review_done_"+suffix+"_"+dueInterval;startMiniTest(topic);});section.addView(review,lp(-1, 48,0,0,0,8));
            }
        }
        if(count==0)addText(section,"Пока всё повторено. Следующая проверка появится через 3, 7 и 14 дней после завершения темы ♡",12,MUTED,false);
        root.addView(section,lp(-1,-2,0,0,0,12));
    }

    private void refreshTopicProgress(int value) {
        if (topicProgressLabel != null) topicProgressLabel.setText("ТВОЙ ПРОГРЕСС: " + value + "%");
        if (topicProgressBar != null) topicProgressBar.setProgress(value);
        int[] values = {25, 50, 75, 100};
        for (int i = 0; i < progressButtons.length; i++) {
            if (progressButtons[i] != null) progressButtons[i].setBackground(shape(value >= values[i] ? PINK : PALE, 18, value >= values[i] ? PINK : 0xFFFFDCE6));
            if (progressButtons[i] != null) progressButtons[i].setTextColor(value >= values[i] ? 0xFFFFFFFF : PINK);
        }
    }

    private void playTimerFinishSound(boolean focusFinished){if(prefs==null||!prefs.getBoolean("timer_sound_enabled",true))return;LocalSoundPlayer.play(this,focusFinished,prefs.getInt("completion_sound_volume",55));}
    private void playCorrectAnswerSound(){if(prefs!=null&&prefs.getBoolean("correct_answer_sound_enabled",true))LocalSoundPlayer.play(this,false,prefs.getInt("completion_sound_volume",55));}
    private void playTopicCompleteSound(){if(prefs==null||!prefs.getBoolean("topic_sound_enabled",true))return;try{android.media.ToneGenerator tone=new android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION,prefs.getInt("completion_sound_volume",55));tone.startTone(android.media.ToneGenerator.TONE_PROP_ACK,160);new Handler(Looper.getMainLooper()).postDelayed(tone::release,260);}catch(Exception ignored){}}

    private void showPraise() {
        showAppMessage(g("Умничка! ❤️", "Молодец! ❤️"));
    }

    private String topicStorageSuffix(String chapter,String topic) {
        if(chapter!=null&&chapter.startsWith("CUSTOM:")){String[] p=chapter.split(":",3);if(p.length>=2)return "custom_"+p[1]+"_"+android.util.Base64.encodeToString(topic.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP);}
        return topic;
    }
    private int topicProgressForChapter(String chapter,String topic) {
        String suffix=topicStorageSuffix(chapter,topic),pk="progress_"+suffix,dk="done_"+suffix;
        if(chapter!=null&&chapter.startsWith("CUSTOM:")&&!prefs.contains(pk)&&!prefs.contains(dk)){int old=prefs.getInt("progress_"+topic,0);boolean wasDone=prefs.getBoolean("done_"+topic,false);if(old>0||wasDone)prefs.edit().putInt(pk,old).putBoolean(dk,wasDone).remove("progress_"+topic).remove("done_"+topic).apply();}
        if(prefs.getBoolean(dk,false))return 100;return Math.max(0,Math.min(100,prefs.getInt(pk,0)));
    }
    private int topicProgress(String topic){return topicProgressForChapter(selectedChapter,topic);}
    private void saveTopicProgressForChapter(String chapter,String topic,int value){
        int old=topicProgressForChapter(chapter,topic),next=Math.max(0,Math.min(100,value));String suffix=topicStorageSuffix(chapter,topic);
        SharedPreferences.Editor e=prefs.edit().putInt("progress_"+suffix,next).putBoolean("done_"+suffix,next==100);
        if(old<100&&next==100){e.putInt("done_count",prefs.getInt("done_count",0)+1).putLong("topic_completed_at_"+suffix,System.currentTimeMillis());haptic(root,true);celebrate();}if(old==100&&next<100)e.putInt("done_count",Math.max(0,prefs.getInt("done_count",0)-1)).remove("topic_completed_at_"+suffix);e.apply();updateAchievementDates();
    }
    private void saveTopicProgress(String topic,int value){saveTopicProgressForChapter(selectedChapter,topic,value);}

    private void recordStudyDay() {
        String today = todayKey();
        String last = prefs.getString("last_study_date", "");
        Set<String> history = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
        history.add(today);
        if (!today.equals(last)) {
            int streak = prefs.getInt("current_streak", 0);
            if (isYesterday(last)) streak++; else if(isOneDayGap(last)&&!weekKey().equals(prefs.getString("streak_freeze_week",""))){streak++;prefs.edit().putString("streak_freeze_week",weekKey()).putString("streak_freeze_used_date",today).apply();}else streak = 1;
            prefs.edit().putInt("current_streak", streak)
                    .putInt("longest_streak", Math.max(streak, prefs.getInt("longest_streak", 0)))
                    .putString("last_study_date", today)
                    .putStringSet("study_days", history).apply();
            StudyWidgetProvider.updateAll(this);updateAchievementDates();
        } else {
            prefs.edit().putStringSet("study_days", history).apply();
        }
    }

    private void markDone() {
        recordStudyDay();updateAchievementDates();
        showAppMessage("Сегодняшнее занятие отмечено ♡");
    }


    private void renderProgressCalendar() {
        if(progressCalendarHost==null)return;progressCalendarHost.removeAllViews();
        LinearLayout calendarCard = card();
        addText(calendarCard, "📅  ДНИ ЗАНЯТИЙ", 12, PINK, true);
        Calendar month = Calendar.getInstance(AppTime.zone());
        month.add(Calendar.MONTH, progressMonthOffset);
        int todayDay = month.get(Calendar.DAY_OF_MONTH);
        int monthNumber = month.get(Calendar.MONTH);
        int yearNumber = month.get(Calendar.YEAR);
        String monthLabel = new SimpleDateFormat("LLLL yyyy", new Locale("ru")).format(month.getTime());
        LinearLayout monthNav = new LinearLayout(this); monthNav.setGravity(Gravity.CENTER_VERTICAL);
        Button prevMonth = button("‹", false); prevMonth.setContentDescription(translate("Предыдущий месяц")); prevMonth.setTextSize(22);
        monthNav.addView(prevMonth, lp(40, 40, 0, 0, 6, 0));
        TextView monthTitle = text(monthLabel.substring(0, 1).toUpperCase(new Locale("ru")) + monthLabel.substring(1), 17, DARK, true);
        monthTitle.setGravity(Gravity.CENTER);
        monthNav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(36), 1f));
        Button nextMonth = button("›", false); nextMonth.setContentDescription(translate("Следующий месяц")); nextMonth.setTextSize(22);
        nextMonth.setEnabled(progressMonthOffset < 0);
        monthNav.addView(nextMonth, lp(40, 40, 6, 0, 0, 0));
        calendarCard.addView(monthNav, lp(-1, 40, 0, 5, 0, 2));
        prevMonth.setOnClickListener(v -> { progressMonthOffset--; renderProgressCalendar(); });
        nextMonth.setOnClickListener(v -> { if (progressMonthOffset < 0) { progressMonthOffset++; renderProgressCalendar(); } });
        Set<String> studiedDays = new TreeSet<>(prefs.getStringSet("study_days", new TreeSet<String>()));
        Set<String> visitedDays = new TreeSet<>(prefs.getStringSet("visit_days", new TreeSet<String>()));
        Calendar first = (Calendar) month.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        LinearLayout weekHeader = new LinearLayout(this);
        weekHeader.setOrientation(LinearLayout.HORIZONTAL);
        String[] shortDays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String dayLabel : shortDays) {
            TextView cell = text(dayLabel, 11, MUTED, true);
            cell.setGravity(Gravity.CENTER);
            weekHeader.addView(cell, new LinearLayout.LayoutParams(0, dp(30), 1f));
        }
        calendarCard.addView(weekHeader, lp(-1, 24, 0, 4, 0, 2));
        LinearLayout week = null;
        for (int slot = 0; slot < 42; slot++) {
            if (slot % 7 == 0) { week = new LinearLayout(this); week.setOrientation(LinearLayout.HORIZONTAL); week.setGravity(Gravity.CENTER_VERTICAL); calendarCard.addView(week, lp(-1, 38, 0, 1, 0, 1)); }
            TextView cell = text("", 12, DARK, true); cell.setGravity(Gravity.CENTER); cell.setIncludeFontPadding(false); cell.setMinHeight(dp(36)); cell.setMinWidth(dp(32));
            if (slot >= offset && slot < offset + daysInMonth) {
                int day = slot - offset + 1;
                String dateKey = String.format(Locale.US, "%04d-%02d-%02d", yearNumber, monthNumber + 1, day);
                boolean studied = studiedDays.contains(dateKey), visited = visitedDays.contains(dateKey), isToday = progressMonthOffset == 0 && day == todayDay;
                cell.setText(String.valueOf(day)); int minutes=prefs.getInt("focus_minutes_"+dateKey,0);int heatColor=minutes>=60?0xFFFFC4D5:minutes>=30?0xFFFFD8E3:minutes>=15?0xFFFFE8EE:studied?0xFFFFF2F5:(isToday?PALE:(visited?0xFFF1F0F2:0xFFFFFFFF));cell.setTextColor(studied?PINK:DARK);
                cell.setBackground(shape(heatColor,11,isToday?PINK:(studied?0xFFFFC6D7:0x00FFFFFF)));
                cell.setElevation(dp(1)); if (isToday) cell.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
            LinearLayout.LayoutParams dayCell=new LinearLayout.LayoutParams(0,dp(36),1f); dayCell.gravity=Gravity.CENTER; dayCell.setMargins(dp(2),0,dp(2),0); week.addView(cell,dayCell);
        }
        addText(calendarCard, "♥ Учебные дни: " + countStudyDaysThisMonth(studiedDays, yearNumber, monthNumber) + "   ·   Заходила: " + countStudyDaysThisMonth(visitedDays, yearNumber, monthNumber), 12, MUTED, false);
        progressCalendarHost.addView(calendarCard, lp(-1, -2, 0, 0, 0, 0));

    }
    private void showProgress() {
        saveForegroundUsage();
        currentScreen = "progress";
        base("Мой прогресс 📊", "Здесь видно, сколько уже изучено по каждой теме");
        int totalTopics = 0, completedTopics = 0;
        for (String chapter : chapters.keySet()) {
            for (String topic : chapters.get(chapter)) {
                if(!isTopicInExamScope(chapter,topic))continue;
                totalTopics++;
                if (topicProgressForChapter(chapter,topic) == 100) completedTopics++;
            }
        }
        int overall = totalTopics == 0 ? 0 : completedTopics * 100 / totalTopics;

        progressCalendarHost=new LinearLayout(this);progressCalendarHost.setOrientation(LinearLayout.VERTICAL);renderProgressCalendar();root.addView(progressCalendarHost,lp(-1,-2,0,0,0,14));

        LinearLayout summary = card();
        addText(summary, "🌸  ОБЩИЙ РЕЗУЛЬТАТ", 12, PINK, true);
        addText(summary, overall + "% изучено", 27, DARK, true);
        addText(summary, completedTopics + " из " + totalTopics + " тем завершено", 14, MUTED, false);
        addText(summary, "⏱ Сегодня: " + prefs.getInt("focus_minutes_" + todayKey(), 0) + " минут по таймеру", 14, PINK, true);
        addText(summary, "Всего: " + totalStudyMinutes() + " минут", 13, MUTED, false);
        addProgressLine(summary, "Весь учебный план", overall);
        root.addView(summary, lp(-1, -2, 0, 0, 0, 8));
        TextView detailedStatsAction=text("◷  Моя статистика  ›",15,PINK,true);
        detailedStatsAction.setGravity(Gravity.CENTER_VERTICAL);
        detailedStatsAction.setPadding(dp(4),dp(8),dp(4),dp(8));
        detailedStatsAction.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        detailedStatsAction.setOnClickListener(v->showDetailedStats());
        root.addView(detailedStatsAction,lp(-1,dp(42),0,0,0,12));

        for (String chapter : chapters.keySet()) {
            List<String> topics = new ArrayList<>();
            for(String topic:chapters.get(chapter))if(isTopicInExamScope(chapter,topic))topics.add(topic);
            if(topics.isEmpty())continue;
            int sum = 0;
            for (String topic : topics) sum += topicProgressForChapter(chapter,topic);
            int chapterPercent = sum / topics.size();
            LinearLayout section = card();
            TextView chapterBadge = text("✿  РАЗДЕЛ " + (new ArrayList<>(chapters.keySet()).indexOf(chapter) + 1), 12, PINK, true);
            chapterBadge.setBackground(shape(0xFFFFEEF3, 16, 0xFFFFD5E1));
            chapterBadge.setPadding(dp(10), dp(7), dp(10), dp(7));
            section.addView(chapterBadge, lp(-2, -2, 0, 0, 0, 8));
            addText(section, chapter.replaceFirst("^[^ ]+\\s*", ""), 17, DARK, true);
            addText(section, chapterPercent + "% · " + countCompleted(chapter,topics) + " из " + topics.size() + " тем полностью изучено", 13, MUTED, false);
            addProgressLine(section, "Прогресс раздела", chapterPercent);
            for (String topic : topics) {
                int value = topicProgressForChapter(chapter,topic);
                addProgressLine(section, topic + " — " + value + "%", value);
            }
            root.addView(section, lp(-1, -2, 0, 0, 0, 12));
        }
    }

    private int totalStudyMinutes() { return prefs.getInt("focus_total_minutes",0); }

    private void showPomodoro() {
        currentScreen="pomodoro";
        base("Фокус","Время для себя и своих целей");
        int bg=isDarkAppearance()?0xFF25212B:0xFFFFFFFF;
        int track=isDarkAppearance()?0xFF49404D:PALE;
        int fg=DARK;
        int muted=MUTED;
        if(timerRemainingMs<=0)timerRemainingMs=(timerIsBreak?selectedBreakMinutes:selectedStudyMinutes)*60000L;
        int diameter=Math.min(dp(326),Math.max(dp(250),getResources().getDisplayMetrics().widthPixels-dp(56)));
        LinearLayout screen=new LinearLayout(this);screen.setOrientation(LinearLayout.VERTICAL);screen.setGravity(Gravity.CENTER_HORIZONTAL);screen.setPadding(0,dp(4),0,dp(10));
        FrameLayout dial=new FrameLayout(this);dial.setBackground(shape(bg,28,0));dial.setElevation(dp(2));
        timerRing=new TimerRingView(this,bg,track,PINK);
        dial.addView(timerRing,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout center=new LinearLayout(this);center.setOrientation(LinearLayout.VERTICAL);center.setGravity(Gravity.CENTER);
        TextView phase=text(timerIsBreak?"ПЕРЕРЫВ":"ВРЕМЯ ФОКУСА",11,muted,true);phase.setLetterSpacing(0.16f);phase.setGravity(Gravity.CENTER);center.addView(phase,lp(-1,-2,0,0,0,dp(10)));
        timerDisplay=text(formatTimer(timerRemainingMs),52,fg,true);timerDisplay.setGravity(Gravity.CENTER);timerDisplay.setIncludeFontPadding(false);timerDisplay.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));center.addView(timerDisplay,lp(-1,-2,0,0,0,dp(8)));
        TextView hint=text(timerRunning?"Ты в потоке":"Нажми, чтобы начать",13,muted,false);hint.setGravity(Gravity.CENTER);center.addView(hint,lp(-1,-2,0,0,0,0));
        dial.addView(center,new FrameLayout.LayoutParams(-1,-1));screen.addView(dial,new LinearLayout.LayoutParams(diameter,diameter));
        timerStatus=text(timerIsBreak?"Отдых тоже часть успеха":"Один шаг за раз",14,muted,false);timerStatus.setGravity(Gravity.CENTER);timerStatus.setPadding(dp(12),dp(16),dp(12),dp(12));screen.addView(timerStatus,lp(-1,-2,0,0,0,dp(4)));
        TextView presetsTitle=text("ДЛИТЕЛЬНОСТЬ ФОКУСА",10,muted,true);presetsTitle.setGravity(Gravity.CENTER);screen.addView(presetsTitle,lp(-1,-2,0,dp(3),0,dp(3)));
        LinearLayout focusPresets=new LinearLayout(this);focusPresets.setGravity(Gravity.CENTER);focusPresets.setOrientation(LinearLayout.HORIZONTAL);
        for(int minutes:new int[]{15,25,30,45,60}){Button chip=button(minutes+"м",selectedStudyMinutes==minutes);chip.setTextSize(12);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(38),1f);cp.setMargins(dp(2),0,dp(2),0);focusPresets.addView(chip,cp);chip.setOnClickListener(v->{if(timerRunning)return;selectedStudyMinutes=minutes;prefs.edit().putInt("selected_study_minutes",minutes).apply();if(!timerIsBreak)timerRemainingMs=minutes*60000L;showPomodoro();});}
        screen.addView(focusPresets,lp(-1,dp(40),0,0,0,dp(4)));
        TextView breakTitle=text("КОРОТКИЙ И ДЛИННЫЙ ПЕРЕРЫВ",10,muted,true);breakTitle.setGravity(Gravity.CENTER);screen.addView(breakTitle,lp(-1,-2,0,dp(3),0,dp(3)));
        LinearLayout breakPresets=new LinearLayout(this);breakPresets.setGravity(Gravity.CENTER);breakPresets.setOrientation(LinearLayout.HORIZONTAL);
        for(int minutes:new int[]{3,5,10}){Button chip=button("Короткий "+minutes,selectedBreakMinutes==minutes);chip.setTextSize(11);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(38),1f);cp.setMargins(dp(2),0,dp(2),0);breakPresets.addView(chip,cp);chip.setOnClickListener(v->{if(timerRunning)return;selectedBreakMinutes=minutes;prefs.edit().putInt("selected_break_minutes",minutes).apply();showPomodoro();});}
        screen.addView(breakPresets,lp(-1,dp(40),0,0,0,dp(2)));
        LinearLayout longBreakPresets=new LinearLayout(this);longBreakPresets.setGravity(Gravity.CENTER);longBreakPresets.setOrientation(LinearLayout.HORIZONTAL);
        int longBreak=Math.max(15,prefs.getInt("pomodoro_long_break_minutes",15));
        for(int minutes:new int[]{15,20,30}){Button chip=button("Длинный "+minutes,longBreak==minutes);chip.setTextSize(11);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(38),1f);cp.setMargins(dp(2),0,dp(2),0);longBreakPresets.addView(chip,cp);chip.setOnClickListener(v->{if(timerRunning)return;prefs.edit().putInt("pomodoro_long_break_minutes",minutes).apply();showPomodoro();});}
        screen.addView(longBreakPresets,lp(-1,dp(40),0,0,0,dp(7)));
        int todaySessions=prefs.getInt("pomodoro_completed_sessions_"+todayKey(),0),weekSessions=0;Calendar weekCursor=Calendar.getInstance(AppTime.zone());SimpleDateFormat weekFmt=new SimpleDateFormat("yyyy-MM-dd",Locale.US);weekFmt.setTimeZone(AppTime.zone());for(int day=0;day<7;day++){weekSessions+=prefs.getInt("pomodoro_completed_sessions_"+weekFmt.format(weekCursor.getTime()),0);weekCursor.add(Calendar.DAY_OF_YEAR,-1);}
        int pauseCount=prefs.getInt("pomodoro_pause_count_"+todayKey(),0);long pauseTotal=prefs.getLong("pomodoro_pause_total_ms_"+todayKey(),0L);String averagePause=pauseCount==0?"—":String.format(Locale.getDefault(),"%.1f мин",(pauseTotal/60000.0)/pauseCount);
        TextView stats=text("Сегодня: "+todaySessions+" полных сессий   ·   7 дней: "+weekSessions+"\nСреднее время до паузы сегодня: "+averagePause,11,muted,false);stats.setGravity(Gravity.CENTER);stats.setPadding(dp(4),dp(5),dp(4),dp(5));screen.addView(stats,lp(-1,-2,0,0,0,dp(3)));
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setOrientation(LinearLayout.HORIZONTAL);
        timerStartButton=button(timerRunning?"Продолжается":"Начать фокус",true);timerStartButton.setEnabled(!timerRunning);timerStartButton.setTextSize(14);
        timerStartButton.setOnClickListener(v->{if(!timerIsBreak&&timerRemainingMs>=selectedStudyMinutes*60000L-1000L)showStudyRitual();else startTimer();});
        timerPauseButton=button("Пауза",false);timerPauseButton.setTextSize(14);timerPauseButton.setEnabled(timerRunning);timerPauseButton.setOnClickListener(v->pauseTimer());
        Button reset=button("Сбросить",false);reset.setTextSize(14);reset.setOnClickListener(v->{
            long elapsed=timerRunning&&!timerIsBreak?Math.max(0,System.currentTimeMillis()-timerSessionStarted):0;
            FocusTimerController.clearIfMatches(this,timerEndElapsed);
            timerHandler.removeCallbacks(timerTick);timerRunning=false;cancelTimerFinishAlarm();
            try{startService(new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_STOP).putExtra("elapsed_ms",elapsed).putExtra("break",timerIsBreak).putExtra("expected_end_wall",timerEndElapsed));}catch(Exception ignored){}
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;}
            if(timerDisplay!=null)timerDisplay.animate().cancel();timerIsBreak=false;timerRemainingMs=selectedStudyMinutes*60000L;showPomodoro();
        });
        LinearLayout.LayoutParams main=new LinearLayout.LayoutParams(0,dp(52),1f);main.setMargins(dp(4),0,dp(4),0);controls.addView(timerStartButton,main);
        LinearLayout.LayoutParams side=new LinearLayout.LayoutParams(0,dp(52),0.72f);side.setMargins(dp(4),0,dp(4),0);controls.addView(timerPauseButton,side);controls.addView(reset,side);
        screen.addView(controls,lp(-1,dp(58),0,dp(12),0,0));
        timerDisplay.setOnLongClickListener(v->{if(timerRunning)return true;new TimePickerDialog(this,(picker,hour,minute)->{selectedStudyMinutes=Math.max(1,Math.min(600,hour*60+minute));prefs.edit().putInt("selected_study_minutes",selectedStudyMinutes).apply();if(!timerIsBreak)timerRemainingMs=selectedStudyMinutes*60000L;showPomodoro();},selectedStudyMinutes/60,selectedStudyMinutes%60,true).show();return true;});
        root.addView(screen,lp(-1,-2,0,dp(6),0,dp(10)));
        updateTimerRingProgress(timerRemainingMs);
        if(timerRunning){updateTimerDisplay();timerStartButton.setEnabled(false);timerPauseButton.setEnabled(true);if(timerTick==null){timerTick=new Runnable(){public void run(){updateTimerDisplay();long left=FocusTimerController.remainingUntil(timerEndElapsed);if(left<=0)finishTimerPhase();else timerHandler.postDelayed(this,250);}};timerHandler.post(timerTick);}}
    }

    private String formatTimer(long ms) {
        long sec = Math.max(0, ms / 1000); return String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60);
    }

    private void startTimer() {
        if (timerRunning) return;
        if (timerRemainingMs <= 0) timerRemainingMs = (timerIsBreak ? selectedBreakMinutes : selectedStudyMinutes) * 60000L;
        long controllerEnd=FocusTimerController.startSegment(this,timerRemainingMs,timerIsBreak);if(controllerEnd<=0L){controllerEnd=FocusTimerController.endTime(this);if(controllerEnd<=System.currentTimeMillis())return;timerIsBreak=FocusTimerController.isBreak(this);timerRemainingMs=FocusTimerController.remaining(this);}timerRunning = true; timerSessionStarted = System.currentTimeMillis()-Math.max(0,FocusTimerController.duration(this)-timerRemainingMs); timerEndElapsed = controllerEnd; if(timerRing!=null)timerRing.setRunning(true);
        if(!timerIsBreak)prefs.edit().putLong("focus_started_wall",System.currentTimeMillis()).putBoolean("focus_left_app",false).apply();
        try{Intent service=new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_START).putExtra("duration_ms",timerRemainingMs).putExtra("break",timerIsBreak);if(Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);}catch(Exception ignored){}
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timerTick = new Runnable() { @Override public void run() { updateTimerDisplay(); long left = FocusTimerController.remainingUntil(timerEndElapsed); if (left <= 0) finishTimerPhase(); else timerHandler.postDelayed(this, 250); } };
        if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;}if(timerDisplay!=null){timerDisplay.setScaleX(1f);timerDisplay.setScaleY(1f);if(!prefs.getBoolean("reduce_motion",false)){timerPulseAnimator=android.animation.ValueAnimator.ofFloat(1f,1.045f);timerPulseAnimator.setDuration(1050);timerPulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);timerPulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);timerPulseAnimator.addUpdateListener(a->{if(timerDisplay!=null){float s=(float)a.getAnimatedValue();timerDisplay.setScaleX(s);timerDisplay.setScaleY(s);}});timerPulseAnimator.start();}}
        if (timerStartButton != null) timerStartButton.setEnabled(false);
        if (timerPauseButton != null) timerPauseButton.setEnabled(true);
        timerHandler.post(timerTick);
    }

    private void pauseTimer() {
        if (!timerRunning) return;
        timerRemainingMs = FocusTimerController.remainingUntil(timerEndElapsed);
        FocusTimerController.clearIfMatches(this,timerEndElapsed);
        timerRunning = false; timerHandler.removeCallbacks(timerTick); cancelTimerFinishAlarm(); try{Intent pause=new Intent(this,FocusTimerService.class).setAction(FocusTimerService.ACTION_PAUSE).putExtra("elapsed_ms",Math.max(0,System.currentTimeMillis()-timerSessionStarted)).putExtra("break",timerIsBreak).putExtra("expected_end_wall",timerEndElapsed);startService(pause);}catch(Exception ignored){} getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;}if(timerRing!=null)timerRing.setRunning(false);if (timerDisplay != null) { timerDisplay.setText(formatTimer(timerRemainingMs)); timerDisplay.animate().scaleX(1f).scaleY(1f).setDuration(220).start(); }
        if (timerStartButton != null) { timerStartButton.setEnabled(true); timerStartButton.setText("▶  Продолжить"); }
        if (timerPauseButton != null) timerPauseButton.setEnabled(false);
    }

    private boolean isMaleProfile(){return prefs!=null && "male".equals(prefs.getString("profile_gender","female"));}
    private String g(String feminine,String masculine){return isMaleProfile()?masculine:feminine;}
    private void updateTimerDisplay() {
        long left=FocusTimerController.remainingUntil(timerEndElapsed);
        if (timerDisplay != null) timerDisplay.setText(formatTimer(left));
        updateTimerRingProgress(left);
    }
    private void updateTimerRingProgress(long remainingMs) {
        if(timerRing==null)return;
        long total=Math.max(1L,(timerIsBreak?Math.max(1,prefs.getInt("pomodoro_current_break_minutes",selectedBreakMinutes)):selectedStudyMinutes)*60000L);
        timerRing.setProgress(1f-Math.max(0L,Math.min(total,remainingMs))/(float)total);
        timerRing.setRunning(timerRunning);
    }

    private void cancelTimerFinishAlarm() {
        try { Intent intent=new Intent(this,ReminderReceiver.class).setAction("study_timer_finish");PendingIntent pi=PendingIntent.getBroadcast(this,910,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);pi.cancel(); } catch(Exception ignored) { }
    }

    private void showTimerNotification(String title,String message) {
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        String channel="study_timer_finished";
        android.app.NotificationManager manager=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){android.app.NotificationChannel c=new android.app.NotificationChannel(channel,"Окончание таймера",android.app.NotificationManager.IMPORTANCE_DEFAULT);c.setDescription("Уведомления о завершении учёбы и перерыва");manager.createNotificationChannel(c);}
        Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,910,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification.Builder builder=Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(this,channel):new android.app.Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(message).setAutoCancel(true).setContentIntent(pi);
        manager.notify(910,builder.build());
    }

    private void finishTimerPhase() {
        boolean focusFinished=!timerIsBreak;
        playTimerFinishSound(!timerIsBreak);
        timerHandler.removeCallbacks(timerTick); timerRunning = false; cancelTimerFinishAlarm(); getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;} timerRemainingMs = 0;
        if (!timerIsBreak) { timerIsBreak = true; int completed=prefs.getInt("pomodoro_cycle_count",0);if(prefs.getLong("pomodoro_last_finish_wall",0L)!=timerEndElapsed)completed=(completed+1)%4;int breakMinutes=(completed==0)?Math.max(15,prefs.getInt("pomodoro_long_break_minutes",15)):selectedBreakMinutes; timerRemainingMs = breakMinutes * 60000L;prefs.edit().putInt("pomodoro_current_break_minutes",breakMinutes).apply(); showAppMessage(g("Ты большая умничка! Время отдохнуть 🌸","Ты большой молодец! Время отдохнуть 🌸")); }
        else { timerIsBreak = false; timerRemainingMs = selectedStudyMinutes * 60000L; showAppMessage(g("Перерыв окончен. Ты готова продолжать! 💗","Перерыв окончен. Ты готов продолжать! 💗")); }
        if ("pomodoro".equals(currentScreen)) showPomodoro();
        if(focusFinished){prefs.edit().putBoolean("diary_prefill_timer_win",true).putBoolean("timer_finish_prompt_pending",false).apply();showTimerFinishPrompt();}
    }

    private void showTimerFinishPrompt(){new android.app.AlertDialog.Builder(this).setTitle("Фокус-сессия завершена 🌸").setMessage("Хочешь записать эту маленькую победу в дневник?").setNegativeButton("Позже",null).setPositiveButton("Записать",(d,w)->showDiary()).show();}

    // App usage is tracked separately; only an explicit study action records a study day.
    private void saveForegroundUsage() { appUsageStartedElapsed=0L; }

    @Override protected void onResume() { super.onResume(); foregroundActivity=this; AppUpdater.resumePendingInstall(this); activityVisible=true; appUsageStartedElapsed=SystemClock.elapsedRealtime(); if(prefs!=null){int pendingReminder=prefs.getInt("pending_reminder_id",-1);long pendingAt=prefs.getLong("pending_reminder_at",0L);if(pendingReminder>0){prefs.edit().putLong("reminder_opened_at_"+pendingReminder,System.currentTimeMillis()).remove("pending_reminder_id").remove("pending_reminder_at").apply();}if(pinWasBackgrounded&&!prefs.getString("app_pin_hash","").isEmpty()){pinWasBackgrounded=false;showPinLockDialog();}boolean wasDark=isDarkAppearance();applySavedTheme();updateSystemBarIcons();if(wasDark!=isDarkAppearance())showScreenByKey(currentScreen);if(scheduledThemeTick!=null)settingsHandler.removeCallbacks(scheduledThemeTick);scheduledThemeTick=new Runnable(){public void run(){if(isFinishing())return;boolean before=isDarkAppearance();applySavedTheme();if(before!=isDarkAppearance()){updateSystemBarIcons();showScreenByKey(currentScreen);}settingsHandler.postDelayed(this,60000L);}};settingsHandler.postDelayed(scheduledThemeTick,60000L);updateAchievementDates();scheduleReminders(this);rescheduleFutureLetterReminders(this);WeeklyReportReceiver.schedule(this);WeeklyBackupReceiver.schedule(this);StudyWidgetProvider.updateAll(this);if(prefs.getBoolean("timer_finish_prompt_pending",false)){prefs.edit().putBoolean("timer_finish_prompt_pending",false).apply();showTimerFinishPrompt();}} }
    @Override protected void onPause() { if(scheduledThemeTick!=null)settingsHandler.removeCallbacks(scheduledThemeTick);if(!pinDialogVisible&&!prefs.getString("app_pin_hash","").isEmpty()){pinUnlocked=false;pinWasBackgrounded=true;}if(foregroundActivity==this)foregroundActivity=null;activityVisible=false;saveForegroundUsage();appUsageStartedElapsed=0L;super.onPause(); }

    private void showMessage() {
        currentScreen = "message";
        base("Послание для тебя 💌", "Маленькое напоминание с любовью:");
        String messageText=messages[new Random().nextInt(messages.length)];if(!prefs.getBoolean("mascot_enabled",true))messageText=messageText.replaceAll("[\\p{So}\\uFE0F]","");TextView message = text(messageText, 22, PINK, true);
        message.setGravity(Gravity.CENTER); message.setPadding(dp(12), dp(26), dp(12), dp(26));
        LinearLayout messageCard = card(); messageCard.setGravity(Gravity.CENTER); messageCard.addView(message, lp(-1, -2, 0, 0, 0, 0));
        root.addView(messageCard, lp(-1, -2, 0, 8, 0, 14));
        Button generate = button("✨  Сгенерировать ещё послание", true);
        generate.setOnClickListener(v -> message.setText(messages[new Random().nextInt(messages.length)]));
        root.addView(generate, lp(-1, 52, 0, 0, 0, 10)); backHome();
    }

    private void showExam() {
        currentScreen="exam";base("Мой экзамен ✨","Выбери главную цель и следи за оставшимися днями");
        Set<String> savedIds=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));
        if(savedIds.isEmpty()&&!prefs.getString("exam_title","").isEmpty()&&!prefs.getString("exam_date","").isEmpty()){
            String legacy="legacy_exam";savedIds.add(legacy);
            prefs.edit().putString("exam_goal_title_"+legacy,prefs.getString("exam_title","")).putString("exam_goal_date_"+legacy,prefs.getString("exam_date","")).putStringSet("exam_goal_ids",savedIds).apply();
        }
        LinearLayout hero=card();hero.setBackground(shape(0xFFFFF0F4,24,0xFFF4DCE5));hero.setPadding(dp(18),dp(16),dp(18),dp(16));
        addText(hero,"ГЛАВНЫЙ ЭКЗАМЕН",11,PINK,true);
        LinearLayout heroRow=new LinearLayout(this);heroRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heroWords=new LinearLayout(this);heroWords.setOrientation(LinearLayout.VERTICAL);
        TextView heroName=text("Главная цель пока не выбрана",15,DARK,true);heroWords.addView(heroName);
        TextView heroDate=text("Выбери её в списке ниже",12,MUTED,false);heroWords.addView(heroDate);
        heroRow.addView(heroWords,new LinearLayout.LayoutParams(0,-2,1f));
        LinearLayout countdown=new LinearLayout(this);countdown.setOrientation(LinearLayout.VERTICAL);countdown.setGravity(Gravity.CENTER);
        TextView heroDays=text("—",38,PINK,true);heroDays.setGravity(Gravity.CENTER);
        TextView heroCaption=text("ДНЕЙ",10,MUTED,true);heroCaption.setGravity(Gravity.CENTER);
        countdown.addView(heroDays);countdown.addView(heroCaption);heroRow.addView(countdown,lp(88,-2,8,0,0,0));
        hero.addView(heroRow,lp(-1,-2,0,5,0,0));root.addView(hero,lp(-1,-2,0,0,0,14));
        Button newGoal=button("＋  Новая цель",false);
        newGoal.setOnClickListener(v->showExamGoalForm(null));
        root.addView(newGoal,lp(-1,48,0,0,0,12));
        Button topicsButton=button("📚  Темы для экзамена   ›",false);
        topicsButton.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        topicsButton.setBackground(shape(0x00FFFFFF,16,blendColors(PINK,BG,0.55f)));
        topicsButton.setOnClickListener(v->showExamTopics());
        root.addView(topicsButton,lp(-1,50,0,0,0,12));
        LinearLayout goals=new LinearLayout(this);goals.setOrientation(LinearLayout.VERTICAL);root.addView(goals,lp(-1,-2,0,0,0,0));
        final Runnable[] render={null};
        final boolean[] formatWeeks={prefs.getBoolean("exam_countdown_weeks",false)};
        TextView formatToggle=text("Только дни  ·  Недели + дни",11,PINK,true);formatToggle.setGravity(Gravity.CENTER);
        formatToggle.setPadding(dp(8),dp(5),dp(8),dp(5));formatToggle.setBackground(shape(0x00FFFFFF,12,0xFFEAD4DE));
        formatToggle.setOnClickListener(v->{formatWeeks[0]=!formatWeeks[0];prefs.edit().putBoolean("exam_countdown_weeks",formatWeeks[0]).apply();if(render[0]!=null)render[0].run();});
        hero.addView(formatToggle,lp(-2,30,0,8,0,0));
        android.widget.ProgressBar prepProgress=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);prepProgress.setMax(100);prepProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));prepProgress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEADDE2));
        TextView prepLabel=text("Подготовка начнётся после добавления цели",11,MUTED,false);hero.addView(prepLabel,lp(-1,20,0,8,0,1));hero.addView(prepProgress,lp(-1,dp(5),0,0,0,0));
        render[0]=()->{
            goals.removeAllViews();
            Set<String> ids=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));
            List<String> order=new ArrayList<>(ids);java.util.Collections.sort(order,java.util.Collections.reverseOrder());
            String mainId=prefs.getString("exam_main_goal_id","");
            if(!ids.contains(mainId))mainId="";
            if(mainId.isEmpty()&&!order.isEmpty())mainId=order.get(0);
            final String selectedMain=mainId;
            if(!selectedMain.isEmpty()){
                String mainTitle=prefs.getString("exam_goal_title_"+selectedMain,""),mainDate=prefs.getString("exam_goal_date_"+selectedMain,"");
                long remain=daysUntil(mainDate);heroName.setText(mainTitle);heroDate.setText("Дата экзамена: "+mainDate);
                if(remain<0){heroDays.setText("—");heroCaption.setText("ДАТА ПРОШЛА");}
                else if(remain==0){heroDays.setText("0");heroCaption.setText("СЕГОДНЯ");}
                else if(formatWeeks[0]){heroDays.setTextSize(22);heroDays.setText((remain/7)+" нед. "+(remain%7)+" дн.");heroCaption.setText("ДО ЭКЗАМЕНА");}
                else{heroDays.setTextSize(38);heroDays.setText(String.valueOf(remain));heroCaption.setText("ДНЕЙ ОСТАЛОСЬ");}
                formatToggle.setText(formatWeeks[0]?"Недели + дни  ·  Только дни":"Только дни  ·  Недели + дни");
                long total=0,elapsed=0;try{java.text.SimpleDateFormat df=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);df.setTimeZone(AppTime.zone());java.util.Date created=df.parse(prefs.getString("exam_goal_created_"+selectedMain,todayKey()));java.util.Date exam=df.parse(mainDate);java.util.Date today=df.parse(todayKey());if(created!=null&&exam!=null&&today!=null){total=Math.max(1,(exam.getTime()-created.getTime())/86400000L+1);elapsed=Math.max(0,Math.min(total,(today.getTime()-created.getTime())/86400000L+1));}}catch(Exception ignored){}
                prepProgress.setProgress(total>0?(int)Math.min(100,elapsed*100/total):0);prepLabel.setText(total>0?"Прошло "+elapsed+" из "+total+" дней подготовки · "+(elapsed*100/total)+"%":"Прогресс подготовки появится после выбора даты");
                prefs.edit().putString("exam_main_goal_id",selectedMain).apply();
            }else{heroName.setText("Главная цель пока не выбрана");heroDate.setText("Добавь цель и отметь её главной");heroDays.setText("—");heroCaption.setText("ДНЕЙ");prepProgress.setProgress(0);prepLabel.setText("Добавь цель, чтобы отслеживать подготовку");formatToggle.setText(formatWeeks[0]?"Недели + дни  ·  Только дни":"Только дни  ·  Недели + дни");}
            newGoal.setVisibility(View.VISIBLE);
            if(order.isEmpty()){LinearLayout empty=card();empty.setBackgroundColor(android.graphics.Color.TRANSPARENT);empty.setGravity(Gravity.CENTER);addText(empty,"Пока нет целей",15,DARK,true);addText(empty,"Добавь первую дату экзамена или важную цель.",13,MUTED,false);goals.addView(empty,lp(-1,-2,0,0,0,10));}
            for(String id:order){
                String goalTitle=prefs.getString("exam_goal_title_"+id,""),goalDate=prefs.getString("exam_goal_date_"+id,"");if(goalTitle.isEmpty()||goalDate.isEmpty())continue;
                long remain=daysUntil(goalDate);boolean isMain=id.equals(selectedMain);
                LinearLayout item=card();item.setBackgroundColor(android.graphics.Color.TRANSPARENT);item.setOrientation(LinearLayout.VERTICAL);
                LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
                addText(words,isMain?"ГЛАВНАЯ ЦЕЛЬ":"ТВОЯ ЦЕЛЬ",11,isMain?PINK:MUTED,true);addText(words,goalTitle,17,DARK,true);addText(words,"Экзамен: "+goalDate,12,MUTED,false);addText(words,"Добавлена: "+prefs.getString("exam_goal_created_"+id,"—"),11,MUTED,false);
                top.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
                LinearLayout number=new LinearLayout(this);number.setOrientation(LinearLayout.VERTICAL);number.setGravity(Gravity.CENTER);number.setBackgroundColor(android.graphics.Color.TRANSPARENT);TextView big=text(remain<0?"—":String.valueOf(remain),30,PINK,true);big.setGravity(Gravity.CENTER);TextView unit=text(remain==0?"СЕГОДНЯ":"ДНЕЙ",9,MUTED,true);unit.setGravity(Gravity.CENTER);number.addView(big);number.addView(unit);top.addView(number,lp(82,78,8,0,0,0));item.addView(top);
                LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
                Button main=button(isMain?"★  Главная цель":"☆  Сделать главной",isMain);main.setGravity(Gravity.CENTER);main.setIncludeFontPadding(false);main.setPadding(dp(2),0,dp(2),0);Button edit=button("Изменить",false),delete=button("Удалить",false);
                main.setOnClickListener(v->{prefs.edit().putString("exam_main_goal_id",id).apply();render[0].run();});
                edit.setOnClickListener(v->showExamGoalForm(id));
                delete.setOnClickListener(v->{TextView sparkle=text("✦   ✧   ✦",25,PINK,true);sparkle.setGravity(Gravity.CENTER);item.addView(sparkle,0,lp(-1,34,0,0,0,0));sparkle.setAlpha(0f);sparkle.animate().alpha(1f).translationY(-dp(8)).setDuration(180).start();item.animate().alpha(0f).scaleX(.82f).scaleY(.82f).translationX(dp(28)).setDuration(300).withEndAction(()->{Set<String> updated=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));updated.remove(id);SharedPreferences.Editor e=prefs.edit().remove("exam_goal_title_"+id).remove("exam_goal_date_"+id).remove("exam_goal_created_"+id).putStringSet("exam_goal_ids",updated);if(id.equals(prefs.getString("exam_main_goal_id","")))e.remove("exam_main_goal_id");e.apply();render[0].run();});});
                LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(48),1f);mp.setMargins(0,dp(8),dp(3),0);actions.addView(main,mp);
                LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(48),.75f);ep.setMargins(dp(3),dp(8),dp(3),0);actions.addView(edit,ep);
                LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(0,dp(48),.65f);dpv.setMargins(dp(3),dp(8),0,0);actions.addView(delete,dpv);
                item.addView(actions);goals.addView(item,lp(-1,-2,0,0,0,10));
            }
        };
        render[0].run();
    }
    private void showExamGoalForm(String editId) {
        currentScreen="exam";
        boolean isEdit=editId!=null;
        if(root==null)return;
        for(int i=root.getChildCount()-1;i>=0;i--){View child=root.getChildAt(i);if("exam_goal_inline_form".equals(child.getTag()))root.removeViewAt(i);}
        LinearLayout form=card();
        form.setTag("exam_goal_inline_form");
        form.setPadding(dp(16),dp(14),dp(16),dp(14));
        form.setBackground(shape(0xFFFFF7F8,20,blendColors(PINK,BG,0.55f)));
        addText(form,isEdit?"✎  ИЗМЕНИТЬ ЦЕЛЬ":"✨  НОВАЯ ЦЕЛЬ",12,PINK,true);
        EditText title=new EditText(this);title.setSingleLine(true);title.setTextSize(15);title.setTextColor(DARK);title.setHint("Например, экзамен по математике");title.setPadding(dp(13),dp(8),dp(13),dp(8));title.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));
        if(isEdit)title.setText(prefs.getString("exam_goal_title_"+editId,""));
        form.addView(title,lp(-1,48,0,7,0,10));
        addText(form,"ДАТА ЭКЗАМЕНА",11,PINK,true);
        EditText date=new EditText(this);date.setSingleLine(true);date.setFocusable(false);date.setClickable(true);date.setTextSize(14);date.setTextColor(DARK);date.setHint("Выбери дату");date.setPadding(dp(13),dp(8),dp(13),dp(8));date.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));
        if(isEdit)date.setText(prefs.getString("exam_goal_date_"+editId,""));
        form.addView(date,lp(-1,48,0,6,0,10));
        date.setOnClickListener(v->{Calendar now=Calendar.getInstance(AppTime.zone());Calendar min=(Calendar)now.clone();min.set(Calendar.HOUR_OF_DAY,0);min.set(Calendar.MINUTE,0);min.set(Calendar.SECOND,0);min.set(Calendar.MILLISECOND,0);android.app.DatePickerDialog dialog=new android.app.DatePickerDialog(this,(p,y,m,d)->date.setText(String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d)),now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH));dialog.getDatePicker().setMinDate(min.getTimeInMillis());dialog.show();});
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel=button("Отмена",false),save=button(isEdit?"Сохранить изменения":"＋  Добавить цель",true);
        actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(46),.72f));
        LinearLayout.LayoutParams saveLp=new LinearLayout.LayoutParams(0,dp(46),1.28f);saveLp.setMargins(dp(6),0,0,0);actions.addView(save,saveLp);
        form.addView(actions,lp(-1,46,0,3,0,0));
        cancel.setOnClickListener(v->root.removeView(form));
        save.setOnClickListener(v->{String n=title.getText().toString().trim(),d=date.getText().toString().trim();if(n.isEmpty()){title.setError("Введи название цели");return;}if(d.isEmpty()){showSavedBanner("Выбери дату","Укажи день экзамена ♡");return;}if(daysUntil(d)<0){showSavedBanner("Неверная дата","Выбери сегодняшнюю или будущую дату");return;}String id=isEdit?editId:String.valueOf(System.currentTimeMillis());Set<String> ids=new TreeSet<>(prefs.getStringSet("exam_goal_ids",new TreeSet<String>()));ids.add(id);SharedPreferences.Editor e=prefs.edit().putString("exam_goal_title_"+id,n).putString("exam_goal_date_"+id,d).putStringSet("exam_goal_ids",ids);if(!isEdit)e.putString("exam_goal_created_"+id,todayKey());if(prefs.getString("exam_main_goal_id","").isEmpty())e.putString("exam_main_goal_id",id);e.apply();StudyWidgetProvider.updateAll(this);showSavedBanner("Цель сохранена","Она появилась в твоём списке ♡");showExam();});
        root.addView(form,Math.min(2,root.getChildCount()),lp(-1,-2,0,0,0,12));
        form.setAlpha(0f);form.setTranslationY(-dp(8));form.animate().alpha(1f).translationY(0f).setDuration(190).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }
    private void showExamTopics(){
        currentScreen="exam_topics";
        base("Темы для экзамена","Выбери темы, которые входят в твой экзамен");
        Button back=button("←  Назад к экзамену",false);
        back.setOnClickListener(v->showExam());
        root.addView(back,lp(-1,46,0,0,0,10));
        addExamTopicSelector();
    }
    private String examTopicKey(String chapter,String topic){return topicStorageSuffix(chapter,topic);}
    private boolean isTopicInExamScope(String chapter,String topic){
        if(!prefs.getBoolean("exam_topic_filter_enabled",false))return true;
        return prefs.getStringSet("exam_topic_ids",new TreeSet<String>()).contains(examTopicKey(chapter,topic));
    }
    private void addExamTopicSelector(){
        LinearLayout section=card();addText(section,"📚  ТЕМЫ ДЛЯ ЭКЗАМЕНА",12,PINK,true);
        addText(section,"Отметь только те темы, которые входят в твой экзамен. При включённом фильтре общий прогресс и планировщик будут учитывать именно их.",12,MUTED,false);
        CheckBox enabled=new CheckBox(this);enabled.setText("Считать прогресс и план только по отмеченным темам");enabled.setTextColor(DARK);enabled.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));enabled.setChecked(prefs.getBoolean("exam_topic_filter_enabled",false));section.addView(enabled,lp(-1,-2,0,5,0,6));
        enabled.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("exam_topic_filter_enabled",on).apply();showAppMessage(on?"Фильтр тем экзамена включён":"Весь учебный план снова учитывается");});
        LinearLayout actions=new LinearLayout(this);Button all=button("Выбрать все",false),none=button("Снять выбор",false);actions.addView(all,new LinearLayout.LayoutParams(0,dp(48),1f));actions.addView(none,new LinearLayout.LayoutParams(0,dp(48),1f));section.addView(actions,lp(-1, 48,0,0,0,6));
        all.setOnClickListener(v->{Set<String> ids=new TreeSet<>();for(Map.Entry<String,List<String>> e:chapters.entrySet())for(String topic:e.getValue())ids.add(examTopicKey(e.getKey(),topic));prefs.edit().putStringSet("exam_topic_ids",ids).apply();showExam();});
        none.setOnClickListener(v->{prefs.edit().putStringSet("exam_topic_ids",new TreeSet<String>()).apply();showExam();});
        Set<String> selected=new TreeSet<>(prefs.getStringSet("exam_topic_ids",new TreeSet<String>()));
        int allTopicCount=0;for(List<String> topics:chapters.values())allTopicCount+=topics.size();final int totalTopicCount=allTopicCount;
        TextView selectionSummary=text("Отмечено тем: "+selected.size()+" из "+totalTopicCount,12,MUTED,false);section.addView(selectionSummary,lp(-1,24,0,0,0,4));
        if(enabled.isChecked()&&selected.isEmpty())addText(section,"Сначала отметь хотя бы одну тему — иначе экзаменационный прогресс будет равен 0%.",12,PINK,true);
        for(Map.Entry<String,List<String>> entry:chapters.entrySet()){
            addText(section,entry.getKey().replaceFirst("^[^ ]+\\s*",""),13,DARK,true);
            for(String topic:entry.getValue()){
                String key=examTopicKey(entry.getKey(),topic);CheckBox check=new CheckBox(this);check.setText(topic);check.setTextSize(12);check.setTextColor(DARK);check.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));check.setChecked(selected.contains(key));section.addView(check,lp(-1,-2,0,0,0,1));
                check.setOnCheckedChangeListener((v,checked)->{Set<String> ids=new TreeSet<>(prefs.getStringSet("exam_topic_ids",new TreeSet<String>()));if(checked)ids.add(key);else ids.remove(key);prefs.edit().putStringSet("exam_topic_ids",ids).apply();selectionSummary.setText("Отмечено тем: "+ids.size()+" из "+totalTopicCount);});
            }
        }
        root.addView(section,lp(-1,-2,0,0,0,14));
    }
    private String createBuddyCode(String sticker){
        try{org.json.JSONObject o=new org.json.JSONObject();o.put("version",1);o.put("alias",prefs.getString("buddy_alias",prefs.getString("display_name","Напарник")));o.put("weekly_goal",prefs.getInt("weekly_goal_minutes",300));o.put("weekly_done",weeklyMinutesLast7Days());o.put("sticker",sticker==null?"🌷":sticker);o.put("message","Рада учиться рядом, даже если мы занимаемся в разное время ♡");o.put("date",todayKey());return "SWL_BUDDY_V1:"+android.util.Base64.encodeToString(o.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);}catch(Exception e){return "";}}
    private boolean importBuddyCode(String raw){
        try{String code=raw.trim();if(code.startsWith("SWL_BUDDY_V1:"))code=code.substring("SWL_BUDDY_V1:".length());byte[] bytes=android.util.Base64.decode(code,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP);org.json.JSONObject o=new org.json.JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));if(o.optInt("version",0)!=1)return false;String alias=o.optString("alias","Напарник").trim();int goal=o.optInt("weekly_goal",0),done=o.optInt("weekly_done",0);if(alias.isEmpty()||alias.length()>30||goal<30||goal>10080||done<0||done>10080)return false;prefs.edit().putString("buddy_partner_alias",alias).putInt("buddy_partner_goal",goal).putInt("buddy_partner_done",done).putString("buddy_partner_sticker",o.optString("sticker","🌷")).putString("buddy_partner_message",o.optString("message","")).putString("buddy_partner_date",o.optString("date","")).apply();return true;}catch(Exception e){return false;}}
    private void showStudyBuddy(){
        currentScreen="study_buddy";base("Напарник по учёбе ♡","Поддерживайте друг друга без соревнования и спешки");
        LinearLayout own=card();addText(own,"ТВОЯ НЕДЕЛЯ",11,PINK,true);int ownGoal=prefs.getInt("weekly_goal_minutes",300),ownDone=weeklyMinutesLast7Days();addText(own,ownDone+" из "+ownGoal+" минут",22,DARK,true);addProgressLine(own,"Твой вклад в общую цель",ownGoal<=0?0:Math.min(100,ownDone*100/ownGoal));EditText alias=new EditText(this);alias.setSingleLine(true);alias.setText(prefs.getString("buddy_alias",prefs.getString("display_name","")));alias.setHint("Как тебя называть напарнику");alias.setTextColor(DARK);alias.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));own.addView(alias,lp(-1,48,0,7,0,6));Button saveAlias=button("Сохранить имя для обмена",false);saveAlias.setOnClickListener(v->{String n=alias.getText().toString().trim();if(n.isEmpty()||n.length()>30){alias.setError("До 30 символов");return;}prefs.edit().putString("buddy_alias",n).apply();showAppMessage("Имя сохранено");});own.addView(saveAlias,lp(-1, 48,0,0,0,0));root.addView(own,lp(-1,-2,0,0,0,10));
        String partner=prefs.getString("buddy_partner_alias","");if(!partner.isEmpty()){LinearLayout shared=card();addText(shared,"ВАШ ОБЩИЙ РИТМ",11,PINK,true);int partnerDone=prefs.getInt("buddy_partner_done",0),partnerGoal=prefs.getInt("buddy_partner_goal",300),combinedGoal=ownGoal+partnerGoal,combinedDone=ownDone+partnerDone;addText(shared,"Ты + "+partner,18,DARK,true);addText(shared,combinedDone+" из "+combinedGoal+" минут за неделю",15,DARK,true);addProgressLine(shared,"Общая цель",combinedGoal<=0?0:Math.min(100,combinedDone*100/combinedGoal));String sticker=prefs.getString("buddy_partner_sticker","🌷");addText(shared,"Стикер от напарника: "+sticker,14,PINK,true);addText(shared,prefs.getString("buddy_partner_message",""),12,MUTED,false);addText(shared,"Последнее обновление: "+prefs.getString("buddy_partner_date","—"),11,MUTED,false);root.addView(shared,lp(-1,-2,0,0,0,10));}
        LinearLayout exchange=card();addText(exchange,"ОБМЕН ОБНОВЛЕНИЯМИ",11,PINK,true);addText(exchange,"Создай короткий код и отправь его напарнику любым удобным способом. Он сможет прислать ответный код — синхронизация асинхронная, без аккаунтов и публичного профиля.",12,MUTED,false);
        Button share=button("↗  Отправить недельное обновление",true);exchange.addView(share,lp(-1,48,0,7,0,6));share.setOnClickListener(v->{String code=createBuddyCode("🌷");if(code.isEmpty())return;Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_SUBJECT,"Недельное обновление");send.putExtra(Intent.EXTRA_TEXT,code);startActivity(Intent.createChooser(send,"Отправить напарнику"));});
        Button sticker=button("♡  Отправить тёплый стикер",false);exchange.addView(sticker,lp(-1, 48,0,0,0,8));sticker.setOnClickListener(v->{String[] options={"🌷 Поддерживаю!","✨ У тебя получится!","☕ Время маленького перерыва","💗 Спасибо за совместную учёбу"};new android.app.AlertDialog.Builder(this).setTitle("Выбери стикер").setItems(options,(d,w)->{String code=createBuddyCode(options[w].substring(0,2));Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,code);startActivity(Intent.createChooser(send,"Отправить стикер"));}).show();});
        EditText incoming=new EditText(this);incoming.setHint("Вставь код, который прислал напарник");incoming.setMinLines(2);incoming.setGravity(Gravity.TOP);incoming.setTextSize(12);incoming.setTextColor(DARK);incoming.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));exchange.addView(incoming,lp(-1,76,0,0,0,6));Button accept=button("Получить обновление",false);exchange.addView(accept,lp(-1, 48,0,0,0,0));accept.setOnClickListener(v->{if(importBuddyCode(incoming.getText().toString())){showAppMessage("Обновление напарника принято ♡");showStudyBuddy();}else incoming.setError("Код не распознан. Проверь, что скопирован весь текст.");});root.addView(exchange,lp(-1,-2,0,0,0,10));
    }

    private long daysUntil(String isoDate) {
        try {SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);f.setTimeZone(AppTime.zone());Date target=f.parse(isoDate);Calendar a=Calendar.getInstance(AppTime.zone());a.set(Calendar.HOUR_OF_DAY,0);a.set(Calendar.MINUTE,0);a.set(Calendar.SECOND,0);a.set(Calendar.MILLISECOND,0);Calendar b=Calendar.getInstance(AppTime.zone());b.setTime(target);b.set(Calendar.HOUR_OF_DAY,0);b.set(Calendar.MINUTE,0);b.set(Calendar.SECOND,0);b.set(Calendar.MILLISECOND,0);return (b.getTimeInMillis()-a.getTimeInMillis())/86400000L;}catch(Exception e){return -1;}
    }

    // Supports legacy timestamp IDs and semantic IDs such as quote-2026-11-24.
    private long diaryTimestamp(String id) {
        if (id == null) return 0L;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ignored) {
            try {
                java.util.regex.Matcher matcher = java.util.regex.Pattern
                        .compile("(\\d{4}-\\d{2}-\\d{2})").matcher(id);
                if (!matcher.find()) return 0L;
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                format.setLenient(false);
                Date parsed = format.parse(matcher.group(1));
                return parsed == null ? 0L : parsed.getTime();
            } catch (Exception invalidDate) {
                return 0L;
            }
        }
    }

    private void showDiary() {
        currentScreen="diary";
        base("","");
        LinearLayout diaryTitleRow=new LinearLayout(this);diaryTitleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView diaryTitle=text("Дневник успехов ♡",23,DARK,true);diaryTitle.setLetterSpacing(-0.02f);diaryTitleRow.addView(diaryTitle,new LinearLayout.LayoutParams(0,dp(42),1f));
        HeaderActionIconView diaryTopSearch=new HeaderActionIconView(this,1,PINK);diaryTopSearch.setContentDescription("Поиск по дневнику");diaryTitleRow.addView(diaryTopSearch,lp(40,40,0,0,0,0));root.addView(diaryTitleRow,lp(-1,46,0,0,0,3));
        addText(root,"МАЛЕНЬКАЯ ПОБЕДА СЕГОДНЯ",11,PINK,true);
        EditText note=new EditText(this);note.setHint("Сегодня у меня получилось...");if(prefs.getBoolean("diary_prefill_timer_win",false)){note.setText(g("Я завершила фокус-сессию и уделила время своей цели. ♡","Я завершил фокус-сессию и уделил время своей цели. ♡"));prefs.edit().remove("diary_prefill_timer_win").apply();}note.setTextSize(17);note.setTextColor(DARK);note.setHintTextColor(MUTED);note.setGravity(Gravity.TOP);note.setMinLines(3);note.setPadding(dp(2),dp(10),dp(2),dp(10));note.setBackgroundColor(0x00000000);
        root.addView(note,lp(-1,104,0,0,0,7));
        final String[] selectedTag={"Учёба"}, editingId={null}, activeFilter={"Все"}, queryValue={""};
        Set<String> customTags=new TreeSet<>(prefs.getStringSet("diary_custom_tags",new TreeSet<String>()));
        LinearLayout tags=new LinearLayout(this);tags.setOrientation(LinearLayout.HORIZONTAL);tags.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView tagScroll=new HorizontalScrollView(this);tagScroll.setHorizontalScrollBarEnabled(false);tagScroll.addView(tags);root.addView(tagScroll,lp(-1,38,0,0,0,7));
        Runnable[] render={null};
        Runnable[] refreshTags=new Runnable[1];
        refreshTags[0]=()->{
            tags.removeAllViews();
            List<String> names=new ArrayList<>();names.add("Учёба");names.add("Дисциплина");names.add("Отдых");names.add("Прогресс");for(String t:customTags)if(!names.contains(t))names.add(t);
            for(String tag:names){TextView chip=text("#"+tag,11,tag.equals(selectedTag[0])?PINK:MUTED,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(8),dp(4),dp(8),dp(4));chip.setBackground(shape(tag.equals(selectedTag[0])?0xFFFFE8EF:0xFFF7F3F4,16,tag.equals(selectedTag[0])?PINK:0xFFEDE3E7));tags.addView(chip,lp(-2,36,0,0,6,0));chip.setOnClickListener(v->{selectedTag[0]=tag;for(int k=0;k<tags.getChildCount();k++){TextView q=(TextView)tags.getChildAt(k);boolean on=q.getText().toString().equals("#"+tag);q.setTextColor(on?PINK:MUTED);q.setBackground(shape(on?0xFFFFE8EF:0xFFF7F3F4,18,on?PINK:0xFFEDE3E7));}});}
            TextView addTag=text("＋ Свой тег",10,PINK,true);addTag.setGravity(Gravity.CENTER);addTag.setPadding(dp(8),dp(4),dp(8),dp(4));addTag.setBackground(shape(PALE,16,0xFFEADDE2));tags.addView(addTag,lp(-2,36,0,0,0,0));
            addTag.setOnClickListener(v->{EditText input=new EditText(this);input.setSingleLine(true);input.setTextSize(15);input.setTextColor(DARK);input.setHint("Например: Книги");input.setPadding(dp(12),dp(8),dp(12),dp(8));input.setBackground(shape(0xFFFFFFFF,14,0xFFEADDE2));LinearLayout wrap=new LinearLayout(this);wrap.setPadding(dp(20),dp(8),dp(20),0);wrap.addView(input,lp(-1,48,0,0,0,0));new android.app.AlertDialog.Builder(this).setTitle("Новый хэштег").setView(wrap).setNegativeButton("Отмена",null).setPositiveButton("Добавить",(d,w)->{String tag=input.getText().toString().trim().replace("#","").replaceAll("\\s+","");if(tag.isEmpty()){showAppMessage("Введи название тега");return;}customTags.add(tag);prefs.edit().putStringSet("diary_custom_tags",customTags).apply();selectedTag[0]=tag;refreshTags[0].run();if(render[0]!=null)render[0].run();}).show();});
        };
        refreshTags[0].run();
        Button save=button("✦  Сохранить победу",true);save.setTextSize(15);save.setElevation(dp(4));root.addView(save,lp(-1,51,0,4,0,15));
        TextView memoryTitle=text("Вспомнить успех ✨",16,DARK,true);root.addView(memoryTitle,lp(-1,-2,0,0,0,7));
        Set<String> all=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));List<String> old=new ArrayList<>(all);java.util.Collections.sort(old,java.util.Collections.reverseOrder());
        if(old.size()>1){String pick=old.get(1);LinearLayout memory=new LinearLayout(this);memory.setOrientation(LinearLayout.VERTICAL);memory.setPadding(dp(13),dp(10),dp(13),dp(10));memory.setBackground(shape(0xFFFFF2F6,17,0));addText(memory,prefs.getString("diary_"+pick,""),13,DARK,false);addText(memory,"Твоя прошлая маленькая победа ♡",10,PINK,true);root.addView(memory,lp(-1,-2,0,0,0,14));}
        else addText(root,"Здесь появится одна из твоих прошлых записей — пока сохрани первую победу.",11,MUTED,false);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("ТВОИ ЗАПИСИ",11,PINK,true);heading.addView(title,new LinearLayout.LayoutParams(0,dp(36),1f));
        TextView counter=text("",10,MUTED,true);heading.addView(counter,lp(-2,36,0,0,5,0));
        TextView searchToggle=text("⌕",25,PINK,true);searchToggle.setGravity(Gravity.CENTER);searchToggle.setContentDescription("Поиск по победам");heading.addView(searchToggle,lp(40,40,0,0,0,0));
        root.addView(heading,lp(-1,40,0,0,0,4));
        LinearLayout searchWrap=new LinearLayout(this);searchWrap.setVisibility(View.GONE);searchWrap.setGravity(Gravity.CENTER_VERTICAL);searchWrap.setPadding(dp(11),0,dp(8),0);searchWrap.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));
        TextView magnify=text("⌕",23,PINK,true);magnify.setGravity(Gravity.CENTER);searchWrap.addView(magnify,lp(27,42,0,0,5,0));
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(13);search.setTextColor(DARK);search.setHintTextColor(MUTED);search.setHint("Поиск по победам...");search.setPadding(0,0,0,0);search.setBackgroundColor(0x00000000);searchWrap.addView(search,new LinearLayout.LayoutParams(0,dp(48),1f));
        TextView clear=text("×",22,MUTED,true);clear.setContentDescription(translate("Очистить поиск"));clear.setGravity(Gravity.CENTER);clear.setVisibility(View.GONE);searchWrap.addView(clear,lp(40,48,0,0,0,0));
        TextView closeDiarySearch=text("×",25,PINK,true);closeDiarySearch.setGravity(Gravity.CENTER);closeDiarySearch.setContentDescription("Закрыть поиск");searchWrap.addView(closeDiarySearch,lp(38,48,0,0,0,0));root.addView(searchWrap,lp(-1, 48,0,0,0,7));
        searchWrap.setVisibility(View.GONE);searchWrap.setAlpha(0f);searchWrap.setTranslationY(-dp(7));
        LinearLayout filterRow=new LinearLayout(this);filterRow.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView filterScroll=new HorizontalScrollView(this);filterScroll.setHorizontalScrollBarEnabled(false);filterScroll.addView(filterRow);root.addView(filterScroll,lp(-1, 38,0,0,0,4));
        LinearLayout entries=new LinearLayout(this);entries.setOrientation(LinearLayout.VERTICAL);root.addView(entries,lp(-1,-2,0,0,0,0));
        render[0]=()->{
            entries.removeAllViews();filterRow.removeAllViews();if(heading.getChildCount()>2)heading.removeViews(2,heading.getChildCount()-2);
            List<String> filters=new ArrayList<>();filters.add("Все");filters.add("Учёба");filters.add("Дисциплина");filters.add("Отдых");filters.add("Прогресс");for(String t:customTags)if(!filters.contains(t))filters.add(t);
            for(String filter:filters){TextView chip=text(filter.equals("Все")?"Все":"#"+filter,11,activeFilter[0].equals(filter)?PINK:MUTED,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(8),dp(4),dp(8),dp(4));chip.setBackground(shape(activeFilter[0].equals(filter)?0xFFFFE8EF:0xFFF7F3F4,15,activeFilter[0].equals(filter)?PINK:0xFFEDE3E7));filterRow.addView(chip,lp(-2,36,0,0,5,0));chip.setOnClickListener(v->{activeFilter[0]=filter;render[0].run();});}
            String q=queryValue[0].trim().toLowerCase(new Locale("ru"));
            Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));List<String> sorted=new ArrayList<>(ids);java.util.Collections.sort(sorted,java.util.Collections.reverseOrder());
            List<String> matches=new ArrayList<>();
            for(String id:sorted){String value=prefs.getString("diary_"+id,"");if(value.isEmpty())continue;String tag=prefs.getString("diary_tag_"+id,"Учёба");String date=new SimpleDateFormat("d MMMM yyyy · HH:mm",new Locale("ru")).format(new Date(diaryTimestamp(id)));String iso=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(diaryTimestamp(id)));if(!activeFilter[0].equals("Все")&&!activeFilter[0].equals(tag))continue;if(!q.isEmpty()&&!value.toLowerCase(new Locale("ru")).contains(q)&&!date.toLowerCase(new Locale("ru")).contains(q)&&!iso.contains(q))continue;matches.add(id);}
            boolean filtering=!q.isEmpty()||!activeFilter[0].equals("Все");
            counter.setText(filtering?"Найдено: "+matches.size():"Всего: "+ids.size());
            if(filtering){TextView reset=text("Сбросить",12,PINK,true);reset.setGravity(Gravity.CENTER);reset.setPadding(dp(8),0,0,0);heading.addView(reset,lp(-2,48,5,0,0,0));reset.setOnClickListener(v->{queryValue[0]="";search.setText("");activeFilter[0]="Все";render[0].run();});}
            if(matches.isEmpty()){
                if(filtering) addEmptyState(entries, "⌕", "Ничего не найдено", "Попробуй изменить запрос или фильтр.", "Сбросить поиск", () -> { queryValue[0]=""; search.setText(""); activeFilter[0]="Все"; render[0].run(); });
                else {
                    LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER_HORIZONTAL);empty.setPadding(dp(10),dp(12),dp(10),dp(12));empty.setBackgroundColor(android.graphics.Color.TRANSPARENT);empty.setElevation(0f);
                    TextView art=text("🌷",38,PINK,true);art.setGravity(Gravity.CENTER);empty.addView(art,lp(-1,52,0,0,0,5));
                    TextView emptyTitle=text("Здесь будут твои победы",16,DARK,true);emptyTitle.setGravity(Gravity.CENTER);empty.addView(emptyTitle,lp(-1,-2,0,0,0,5));
                    TextView emptyText=text("Сохрани первую запись — и возвращайся к ней, чтобы вспомнить, как много уже получилось.",12,MUTED,false);emptyText.setGravity(Gravity.CENTER);empty.addView(emptyText,lp(-1,-2,0,0,0,7));
                    TextView addFirst=text("＋  Написать первую запись",12,PINK,true);addFirst.setGravity(Gravity.CENTER);empty.addView(addFirst,lp(-1,38,0,0,0,0));
                    addFirst.setOnClickListener(v->{note.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(note,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
                    entries.addView(empty,lp(-1,-2,0,0,0,8));
                }
                return;
            }
            String currentMonth="";
            Set<String> collapsed=new TreeSet<>(prefs.getStringSet("diary_collapsed_months",new TreeSet<String>()));
            for(String id:matches){long stamp=diaryTimestamp(id);Calendar cal=Calendar.getInstance();cal.setTimeInMillis(stamp);String monthKey=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date(stamp));
                if(!monthKey.equals(currentMonth)){currentMonth=monthKey;String monthTitle=new SimpleDateFormat("LLLL yyyy",new Locale("ru")).format(new Date(stamp));monthTitle=monthTitle.substring(0,1).toUpperCase(new Locale("ru"))+monthTitle.substring(1);LinearLayout monthHead=new LinearLayout(this);monthHead.setGravity(Gravity.CENTER_VERTICAL);TextView month=text(monthTitle,12,PINK,true);monthHead.addView(month,new LinearLayout.LayoutParams(0,dp(36),1f));TextView arrow=text(collapsed.contains(monthKey)?"＋":"−",17,MUTED,true);arrow.setGravity(Gravity.CENTER);monthHead.addView(arrow,lp(48,48,0,0,0,0));entries.addView(monthHead,lp(-1,38,0,5,0,2));monthHead.setOnClickListener(v->{Set<String> state=new TreeSet<>(prefs.getStringSet("diary_collapsed_months",new TreeSet<String>()));if(state.contains(monthKey))state.remove(monthKey);else state.add(monthKey);prefs.edit().putStringSet("diary_collapsed_months",state).apply();render[0].run();});}
                if(collapsed.contains(monthKey))continue;
                String value=prefs.getString("diary_"+id,""),tag=prefs.getString("diary_tag_"+id,"Учёба");
                LinearLayout entry=new LinearLayout(this);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(dp(2),dp(7),dp(2),dp(10));
                LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView date=text("✿  "+new SimpleDateFormat("d MMMM yyyy · HH:mm",new Locale("ru")).format(new Date(stamp)),10,PINK,true);top.addView(date,new LinearLayout.LayoutParams(0,dp(48),1f));
                TextView edit=text("✎",19,MUTED,true);edit.setGravity(Gravity.CENTER);edit.setContentDescription("Редактировать запись");top.addView(edit,lp(48,48,0,0,2,0));TextView del=text("⌫",18,MUTED,true);del.setGravity(Gravity.CENTER);del.setContentDescription("Удалить запись");top.addView(del,lp(48,48,0,0,0,0));entry.addView(top,lp(-1,48,0,0,0,2));
                TextView tagView=text("#"+tag,10,PINK,true);entry.addView(tagView,lp(-1,19,0,0,0,2));TextView body=text(value,14,DARK,false);body.setGravity(Gravity.TOP);body.setPadding(0,dp(2),0,dp(7));entry.addView(body,lp(-1,-2,0,0,0,0));
                LinearLayout editArea=new LinearLayout(this);editArea.setOrientation(LinearLayout.VERTICAL);editArea.setVisibility(View.GONE);EditText editText=new EditText(this);editText.setText(value);editText.setTextSize(14);editText.setTextColor(DARK);editText.setGravity(Gravity.TOP);editText.setMinLines(3);editText.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));editArea.addView(editText,lp(-1,92,0,0,0,6));
                Button saveInline=button("Сохранить",true),cancel=button("Отмена",false);editArea.addView(saveInline,lp(-1, 48,0,0,0,3));editArea.addView(cancel,lp(-1,38,0,0,0,0));entry.addView(editArea,lp(-1,-2,0,0,0,0));
                edit.setOnClickListener(v->{body.setVisibility(View.GONE);tagView.setVisibility(View.GONE);editArea.setVisibility(View.VISIBLE);});cancel.setOnClickListener(v->{editArea.setVisibility(View.GONE);body.setVisibility(View.VISIBLE);tagView.setVisibility(View.VISIBLE);});saveInline.setOnClickListener(v->{String updated=editText.getText().toString().trim();if(updated.isEmpty()){editText.setError("Напиши пару слов");return;}prefs.edit().putString("diary_"+id,updated).apply();render[0].run();});
                del.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setMessage("Удалить эту запись?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{Set<String> latest=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));latest.remove(id);prefs.edit().remove("diary_"+id).remove("diary_tag_"+id).putStringSet("diary_ids",latest).apply();render[0].run();}).show());
                View line=new View(this);line.setBackgroundColor(0xFFEFE5E8);entry.addView(line,lp(-1,1,0,5,0,0));entries.addView(entry,lp(-1,-2,0,0,0,5));
            }
        };
        searchToggle.setVisibility(View.GONE);
        Runnable closeDiarySearch=()->{
            if(searchWrap.getParent()!=root)return;
            android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(imm!=null)imm.hideSoftInputFromWindow(search.getWindowToken(),0);
            search.setText("");
            searchWrap.animate().alpha(0f).translationY(-dp(7)).setDuration(150).withEndAction(()->{
                root.removeView(searchWrap);
                root.addView(diaryTitleRow,0);
                searchWrap.setAlpha(0f);searchWrap.setTranslationY(-dp(7));
                searchToggle.setText("⌕");searchToggle.setContentDescription("Поиск по победам");
            }).start();
        };
        diaryTopSearch.setOnClickListener(v->{
            if(searchWrap.getParent()==root){closeDiarySearch.run();return;}
            root.removeView(diaryTitleRow);root.removeView(searchWrap);
            searchWrap.setVisibility(View.VISIBLE);searchWrap.setAlpha(0f);searchWrap.setTranslationY(-dp(7));
            root.addView(searchWrap,0);
            searchWrap.animate().alpha(1f).translationY(0f).setDuration(210).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            search.requestFocus();
            search.post(()->{android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(imm!=null)imm.showSoftInput(search,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
        });
        closeDiarySearch.setOnClickListener(v->closeDiarySearch.run());
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){queryValue[0]=s.toString();clear.setVisibility(s.length()>0?View.VISIBLE:View.GONE);render[0].run();}public void afterTextChanged(android.text.Editable e){}});
        clear.setOnClickListener(v->search.setText(""));
        save.setOnClickListener(v->{String value=note.getText().toString().trim();if(value.isEmpty()){note.setError("Напиши хотя бы пару слов");return;}Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));String id=editingId[0]==null?String.valueOf(System.currentTimeMillis()):editingId[0];ids.add(id);prefs.edit().putString("diary_"+id,value).putString("diary_tag_"+id,selectedTag[0]).putStringSet("diary_ids",ids).apply();editingId[0]=null;note.setText("");save.setText("✦  Сохранить победу");render[0].run();showSavedBanner("Запись сохранена","Твоя маленькая победа осталась в дневнике ♡");});
        render[0].run();
    }

    private void showAppMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        String lower = message.toLowerCase(new Locale("ru"));
        boolean warning = lower.contains("не удалось") || lower.contains("ошиб") || lower.contains("недоступ") || lower.contains("не распознан");
        showSavedBanner(warning ? "Обрати внимание" : "Study with Love", message);
    }

    public static boolean showForegroundReminder(String message) {
        MainActivity activity=foregroundActivity;
        if(activity==null||!activityVisible||activity.isFinishing())return false;
        activity.runOnUiThread(()->activity.showBottomReminderBanner(message));
        return true;
    }
    private void showBottomReminderBanner(String message) {
        android.view.View decorView=getWindow().getDecorView();
        if(!(decorView instanceof android.view.ViewGroup))return;
        android.view.ViewGroup decor=(android.view.ViewGroup)decorView;
        android.widget.FrameLayout overlay=new android.widget.FrameLayout(this);
        LinearLayout banner=new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(16),dp(12),dp(18),dp(12));
        banner.setBackground(shape(PALE,18,blendColors(PINK,BG,0.45f)));banner.setElevation(dp(10));
        TextView icon=text("✉",23,PINK,true);icon.setGravity(Gravity.CENTER);
        banner.addView(icon,lp(34,36,0,0,10,0));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
        addText(words,"Study with Love",14,DARK,true);
        addText(words,message==null?"Твоё письмо себе уже можно открыть ✉":message,12,MUTED,false);
        banner.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
        overlay.addView(banner,new android.widget.FrameLayout.LayoutParams(-1,-2));
        android.widget.FrameLayout.LayoutParams params=new android.widget.FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(420)),-2,
                Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        params.setMargins(dp(16),0,dp(16),dp(112));decor.addView(overlay,params);
        overlay.setAlpha(0f);overlay.setTranslationY(dp(18));
        overlay.animate().alpha(1f).translationY(0).setDuration(240).start();
        overlay.postDelayed(()->{if(overlay.getParent()!=null)overlay.animate().alpha(0f).translationY(dp(12)).setDuration(180).withEndAction(()->{if(overlay.getParent()!=null)decor.removeView(overlay);}).start();},4000);
    }

    private void showSavedBanner(String title, String subtitle) {
        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPadding(dp(16), dp(11), dp(18), dp(11));
        banner.setBackground(shape(PALE, 20, 0));
        banner.setElevation(dp(8));
        TextView icon = text("♡", 25, PINK, true);
        icon.setGravity(Gravity.CENTER);
        banner.addView(icon, lp(34, 36, 0, 0, 10, 0));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        addText(words, title, 14, DARK, true);
        addText(words, subtitle, 12, MUTED, false);
        banner.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        overlay.addView(banner, new android.widget.FrameLayout.LayoutParams(-1, -2));
        android.view.ViewGroup decor = (android.view.ViewGroup) getWindow().getDecorView();
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420)), -2,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        params.setMargins(dp(16), dp(12), dp(16), 0);
        decor.addView(overlay, params);
        overlay.setAlpha(0f);
        overlay.setTranslationY(dp(18));
        overlay.animate().alpha(1f).translationY(0).setDuration(220).start();
        overlay.postDelayed(() -> {
            if (overlay.getParent() != null) {
                overlay.animate().alpha(0f).translationY(dp(12)).setDuration(180)
                        .withEndAction(() -> { if (overlay.getParent() != null) decor.removeView(overlay); }).start();
            }
        }, 2400);
    }
    private void renderDiaryEntries(LinearLayout entries,EditText note,Button save,String[] editingId){
        entries.removeAllViews();Set<String> ids=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));List<String> sorted=new ArrayList<>(ids);java.util.Collections.sort(sorted,java.util.Collections.reverseOrder());
        if(sorted.isEmpty()){addText(entries,"🌷 Здесь появятся твои маленькие победы. Сохрани первую запись — и возвращайся к ней в трудные дни.",14,MUTED,false);}
        for(String id:sorted){String value=prefs.getString("diary_"+id,"");if(value.isEmpty())continue;
            LinearLayout entry=new LinearLayout(this);entry.setOrientation(LinearLayout.VERTICAL);entry.setPadding(dp(2),dp(9),dp(2),dp(12));
            LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
            TextView date=text("✿  "+new SimpleDateFormat("d MMMM yyyy · HH:mm",new Locale("ru")).format(new Date(diaryTimestamp(id))),11,PINK,true);top.addView(date,new LinearLayout.LayoutParams(0,dp(34),1f));
            TextView edit=text("✎",21,MUTED,true);edit.setGravity(Gravity.CENTER);edit.setContentDescription("Редактировать запись");top.addView(edit,lp(48,48,0,0,2,0));
            TextView del=text("⌫",20,MUTED,true);del.setGravity(Gravity.CENTER);del.setContentDescription("Удалить запись");top.addView(del,lp(48,48,0,0,0,0));
            entry.addView(top,lp(-1,36,0,0,0,3));
            String tag=prefs.getString("diary_tag_"+id,"Учёба");TextView tagView=text("#"+tag,11,PINK,true);entry.addView(tagView,lp(-1,22,0,0,0,4));
            TextView body=text(value,15,DARK,false);body.setGravity(Gravity.TOP);body.setPadding(0,dp(3),0,dp(8));entry.addView(body,lp(-1,-2,0,0,0,0));
            LinearLayout editArea=new LinearLayout(this);editArea.setOrientation(LinearLayout.VERTICAL);editArea.setVisibility(View.GONE);EditText editText=new EditText(this);editText.setText(value);editText.setTextSize(15);editText.setTextColor(DARK);editText.setGravity(Gravity.TOP);editText.setMinLines(3);editText.setBackground(shape(0xFFFFF9F7,14,0xFFEADDE2));editArea.addView(editText,lp(-1,100,0,0,0,7));Button saveInline=button("Сохранить",true),cancel=button("Отмена",false);editArea.addView(saveInline,lp(-1, 48,0,0,0,4));editArea.addView(cancel,lp(-1, 48,0,0,0,0));entry.addView(editArea,lp(-1,-2,0,0,0,0));
            edit.setOnClickListener(v->{body.setVisibility(View.GONE);tagView.setVisibility(View.GONE);editArea.setVisibility(View.VISIBLE);});cancel.setOnClickListener(v->{editArea.setVisibility(View.GONE);body.setVisibility(View.VISIBLE);tagView.setVisibility(View.VISIBLE);});saveInline.setOnClickListener(v->{String updated=editText.getText().toString().trim();if(updated.isEmpty()){editText.setError("Напиши пару слов");return;}prefs.edit().putString("diary_"+id,updated).apply();renderDiaryEntries(entries,note,save,editingId);});
            del.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setMessage("Удалить эту запись?").setNegativeButton("Отмена",null).setPositiveButton("Удалить",(d,w)->{Set<String> latest=new TreeSet<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));latest.remove(id);prefs.edit().remove("diary_"+id).remove("diary_tag_"+id).putStringSet("diary_ids",latest).apply();renderDiaryEntries(entries,note,save,editingId);}).show());
            View line=new View(this);line.setBackgroundColor(0xFFEFE5E8);entry.addView(line,lp(-1,1,0,5,0,0));entries.addView(entry,lp(-1,-2,0,0,0,7));
        }
    }


    private String mascotMessage(){
        int hour=Calendar.getInstance(AppTime.zone()).get(Calendar.HOUR_OF_DAY);
        if(currentStreak()>=3)return "🐰 Я горжусь твоей серией! Каждый шаг важен.";
        if(hour<12)return "🐰 Доброе утро! Начнём день с маленькой победы?";
        if(hour<18)return "🐰 Я рядом, если захочешь немного позаниматься.";
        return "🐰 Вечер — хорошее время спокойно повторить пройденное.";
    }
    private void showKnowledgeGarden(){
        currentScreen="garden";base("Сад знаний","Каждая тема — отдельное растение. Повторение помогает ему расцветать.");
        int total=0,done=0;
        for(Map.Entry<String,List<String>> e:chapters.entrySet())for(String topic:e.getValue()){total++;if(topicProgressForChapter(e.getKey(),topic)==100)done++;}
        LinearLayout summary=card();summary.setGravity(Gravity.CENTER);addText(summary,"🌼  "+done+" из "+total+" растений расцвели",18,PINK,true);addText(summary,"Продолжай учиться в своём темпе — сад будет расти.",13,MUTED,false);root.addView(summary,lp(-1,-2,0,0,0,12));
        for(Map.Entry<String,List<String>> e:chapters.entrySet()){
            LinearLayout group=card();addText(group,e.getKey(),15,DARK,true);
            for(String topic:e.getValue()){
                int p=topicProgressForChapter(e.getKey(),topic);LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
                TextView plant=text(p==100?"🌸":p>0?"🌿":"🌱",25,PINK,true);line.addView(plant,lp(42,42,0,0,8,0));
                LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.addView(text(topic,13,DARK,true));words.addView(text(p==100?"Цветёт · тема завершена":p>0?"Растёт · прогресс "+p+"%":"Росток · начни изучение",11,MUTED,false));line.addView(words,new LinearLayout.LayoutParams(0,-2,1f));group.addView(line,lp(-1,-2,0,3,0,3));
            }root.addView(group,lp(-1,-2,0,0,0,9));
        }
        Button water=button("💧  Полить сад повторением",true);water.setOnClickListener(v->{prefs.edit().putInt("garden_water_count",prefs.getInt("garden_water_count",0)+1).putLong("garden_last_water",System.currentTimeMillis()).apply();showAppMessage("Сад полит! Повторение помогает знаниям 🌿");});root.addView(water,lp(-1,50,0,4,0,10));
    }
    private void showPostcards(){
        currentScreen="postcards";base("Коллекция открыток","Открытки сохраняют важные учебные моменты.");
        int days=prefs.getStringSet("study_days",new TreeSet<String>()).size(),completed=prefs.getInt("done_count",0),sessions=prefs.getInt("postcard_focus_count",0);
        String[][] cards={{"🌱","Первый шаг","За первый учебный день",days>=1?"1":"0"},{"🌷","Неделя заботы","За 7 дней занятий",days>=7?"1":"0"},{"🌸","Цветущий сад","За первую завершённую тему",completed>=1?"1":"0"},{"✨","Сила фокуса","За первую завершённую фокус-сессию",sessions>=1?"1":"0"},{"💐","Постоянство","За 30 дней занятий",days>=30?"1":"0"}};
        for(String[] item:cards){boolean unlocked="1".equals(item[3]);LinearLayout c=card();c.setGravity(Gravity.CENTER);c.setPadding(dp(18),dp(20),dp(18),dp(20));c.setBackground(shape(unlocked?0xFFFFF0F4:0xFFF1ECEE,22,0));addText(c,item[0],34,PINK,true);addText(c,item[1],18,DARK,true);addText(c,item[2],12,MUTED,false);addText(c,unlocked?"Открытка открыта ♡":"Пока закрыта",12,unlocked?PINK:MUTED,true);
            if(unlocked){Button share=button("Поделиться открыткой",false);share.setOnClickListener(v->{Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,item[0]+" "+item[1]+" — "+item[2]+" ♡");startActivity(Intent.createChooser(send,"Поделиться открыткой"));});c.addView(share,lp(-1, 48,0,8,0,0));}root.addView(c,lp(-1,-2,0,0,0,10));
        }
    }


    private void showStudyRitual(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(4),dp(8),dp(4));
        TextView breathing=text("🌬  Сделай спокойный вдох и выдох",16,PINK,true);breathing.setGravity(Gravity.CENTER);panel.addView(breathing,lp(-1,48,0,0,0,8));
        EditText goal=new EditText(this);goal.setSingleLine(true);goal.setHint("Моя цель на эту сессию");goal.setText(prefs.getString("focus_goal",""));panel.addView(goal,lp(-1,52,0,0,0,8));
        new android.app.AlertDialog.Builder(this).setTitle("Ритуал начала занятия").setMessage("Первые 30 секунд — спокойное дыхание. Затем сформулируй цель и переходи к фокусу.").setView(panel).setNegativeButton("Позже",null).setPositiveButton("Начать фокус",(d,w)->{prefs.edit().putString("focus_goal",goal.getText().toString().trim()).apply();startTimer();}).show();
    }
    @Override public void onUserLeaveHint(){super.onUserLeaveHint();if(timerRunning&&!timerIsBreak)prefs.edit().putBoolean("focus_left_app",true).apply();}
    private void showFlower() {
        currentScreen="flower";
        Set<String> days=new TreeSet<>(prefs.getStringSet("study_days",new TreeSet<String>()));
        int count=days.size(),waterCount=prefs.getInt("flower_water_count",0),petals=prefs.getInt("flower_petals",0);
        int flowerIndex=Math.floorMod(prefs.getInt("flower_type",0),5);int streak=currentStreak();
        String stage=count>=30?"Пышное цветение":count>=14?"Цветочек окреп":count>=7?"Появились бутоны":count>=3?"Первые листочки":"Маленький росток";
        base("Мой цветочек","Он растёт вместе с твоими учебными победами");
        LinearLayout c=card();c.setGravity(Gravity.CENTER);
        android.widget.FrameLayout garden=new android.widget.FrameLayout(this);garden.setClipChildren(false);garden.setClipToPadding(false);
        FlowerPotView pot=new FlowerPotView(this);android.widget.FrameLayout.LayoutParams potLp=new android.widget.FrameLayout.LayoutParams(dp(142),dp(112),Gravity.CENTER);potLp.topMargin=dp(70);garden.addView(pot,potLp);
        FlowerArtView art=new FlowerArtView(this,flowerIndex,count,streak);android.widget.FrameLayout.LayoutParams flowerLp=new android.widget.FrameLayout.LayoutParams(dp(150),dp(150),Gravity.CENTER);flowerLp.topMargin=-dp(27);garden.addView(art,flowerLp);
        // Draw the pot rim and soil in front of the lower stem so the flower visibly grows from inside the pot.
        android.widget.FrameLayout.LayoutParams frontPotLp=new android.widget.FrameLayout.LayoutParams(dp(142),dp(112),Gravity.CENTER);frontPotLp.topMargin=dp(70);garden.addView(new FlowerPotView(this),frontPotLp);
        WaterCanView can=new WaterCanView(this);can.setVisibility(View.GONE);android.widget.FrameLayout.LayoutParams canLp=new android.widget.FrameLayout.LayoutParams(dp(66),dp(58),Gravity.RIGHT|Gravity.TOP);canLp.setMargins(0,dp(20),dp(12),0);garden.addView(can,canLp);
        WaterStreamView stream=new WaterStreamView(this);stream.setVisibility(View.GONE);android.widget.FrameLayout.LayoutParams streamLp=new android.widget.FrameLayout.LayoutParams(dp(74),dp(92),Gravity.RIGHT|Gravity.TOP);streamLp.setMargins(0,dp(65),dp(57),0);garden.addView(stream,streamLp);
        if(flowerBreatheAnimator!=null){flowerBreatheAnimator.cancel();flowerBreatheAnimator=null;}
        if(!prefs.getBoolean("reduce_motion",false)){flowerBreatheAnimator=android.animation.ObjectAnimator.ofFloat(art,View.SCALE_Y,.985f,1.025f,.985f);flowerBreatheAnimator.setDuration(2400);flowerBreatheAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);flowerBreatheAnimator.start();}
        c.addView(garden,lp(-1,230,0,0,0,8));addText(c,stage,21,DARK,true);addText(c,count+" дней занятий · "+totalStudyMinutes()+" минут вместе",13,MUTED,false);addText(c,"✿  "+petals+" лепестков заботы",14,PINK,true);root.addView(c,lp(-1,-2,0,0,0,12));
        LinearLayout care=card();addText(care,"УХОД ЗА ЦВЕТОЧКОМ",12,PINK,true);addText(care,"Поливай цветочек раз в день. Вода появится только после нажатия.",13,MUTED,false);
        boolean watered=prefs.getString("flower_water_date","").equals(todayKey());Button waterButton=button(watered?"Уже полит сегодня ✓":"Полить цветочек",true);waterButton.setEnabled(!watered);
        waterButton.setOnClickListener(v->{
            haptic(waterButton,true);
            waterButton.setEnabled(false);waterButton.setText("Цветочек пьёт воду…");
            if(!prefs.getBoolean("reduce_motion",false)){
                can.setVisibility(View.VISIBLE);stream.setVisibility(View.VISIBLE);
                can.setPivotX(dp(55));can.setPivotY(dp(45));
                android.animation.ObjectAnimator pour=android.animation.ObjectAnimator.ofFloat(can,"rotation",0f,-28f,-18f,0f);pour.setDuration(550);
                android.animation.ObjectAnimator streamMove=android.animation.ObjectAnimator.ofFloat(stream,View.TRANSLATION_Y,0f,dp(24));streamMove.setDuration(500);
                android.animation.ObjectAnimator streamFade=android.animation.ObjectAnimator.ofFloat(stream,View.ALPHA,0f,1f,.9f,0f);streamFade.setDuration(500);
                android.animation.ObjectAnimator bloom=android.animation.ObjectAnimator.ofFloat(art,View.SCALE_X,1f,1.06f,1f);bloom.setDuration(500);
                pour.start();streamMove.start();streamFade.start();bloom.start();
            }
            prefs.edit().putString("flower_water_date",todayKey()).putInt("flower_water_count",waterCount+1).putInt("flower_petals",petals+1).apply();
            new Handler(Looper.getMainLooper()).postDelayed(()->{if(!isFinishing()&&"flower".equals(currentScreen))showFlower();},1450);
        });
        care.addView(waterButton,lp(-1,48,0,8,0,8));
        Button change=button("Сменить вид цветка",false);change.setOnClickListener(v->{prefs.edit().putInt("flower_type",(flowerIndex+1)%5).apply();showFlower();});care.addView(change,lp(-1,48,0,0,0,0));root.addView(care,lp(-1,-2,0,0,0,12));
        addText(root,"Твой цветочек растёт в своём горшочке — так же, как твои маленькие победы ♡",14,MUTED,false);backHome();
    }

    private class FlowerPotView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);
        FlowerPotView(android.content.Context c){super(c);}
        @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();p.setShader(new android.graphics.LinearGradient(0,0,w,h,0xFFD58A68,0xFF9D5948,android.graphics.Shader.TileMode.CLAMP));android.graphics.Path pot=new android.graphics.Path();pot.moveTo(w*.12f,h*.12f);pot.lineTo(w*.88f,h*.12f);pot.lineTo(w*.76f,h*.94f);pot.quadTo(w*.5f,h*1.04f,w*.24f,h*.94f);pot.close();canvas.drawPath(pot,p);p.setShader(null);p.setColor(0xFFE8B08D);canvas.drawRoundRect(w*.08f,h*.08f,w*.92f,h*.24f,h*.08f,h*.08f,p);p.setColor(0xFF704638);canvas.drawOval(w*.18f,h*.13f,w*.82f,h*.23f,p);p.setColor(0xFF5D4933);canvas.drawOval(w*.19f,h*.145f,w*.81f,h*.225f,p);p.setColor(0xFF6D9B4C);for(int i=0;i<13;i++){float x=w*(.22f+i*.047f);float y=h*(.17f+(i%3)*.018f);canvas.drawLine(x,y,x-3,y-8,p);canvas.drawLine(x,y,x+4,y-7,p);}}
    }
    private class FlowerArtView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);private final int type,days,streak;
        FlowerArtView(android.content.Context c,int t,int d,int st){super(c);type=t;days=d;streak=st;}
        private void petal(android.graphics.Canvas c,float x,float y,float rx,float ry,int light,int mid,int dark){
            p.setStyle(android.graphics.Paint.Style.FILL);p.setShader(new android.graphics.LinearGradient(x-rx,y-ry,x+rx,y+ry,light,mid,android.graphics.Shader.TileMode.CLAMP));c.drawOval(x-rx,y-ry,x+rx,y+ry,p);p.setShader(null);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(dark);c.drawLine(x,y-ry*.65f,x,y+ry*.55f,p);p.setStyle(android.graphics.Paint.Style.FILL);
        }
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),stemX=w*.5f,baseY=h*.91f,headY=h*.29f;int[][] palettes={{0xFFFFD6E1,0xFFE879A0,0xFFB94B70},{0xFFFFE4EE,0xFFF3A9C1,0xFFC86C8A},{0xFFFFF1B8,0xFFEABF45,0xFFB77C20},{0xFFF3D9F4,0xFFC78BCB,0xFF89558F},{0xFFFFDCCB,0xFFEB9875,0xFFB95F48}};int[] col=palettes[type%palettes.length];
            p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(5));p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setColor(0xFF397A45);android.graphics.Path stem=new android.graphics.Path();stem.moveTo(stemX,baseY);stem.cubicTo(stemX-4,baseY-55,stemX+8,headY+55,stemX,headY+8);c.drawPath(stem,p);p.setStyle(android.graphics.Paint.Style.FILL);
            android.graphics.Path left=new android.graphics.Path();left.moveTo(stemX,baseY*.79f);left.cubicTo(stemX-42,baseY*.69f,stemX-48,baseY*.59f,stemX-12,baseY*.64f);left.cubicTo(stemX-4,baseY*.67f,stemX-1,baseY*.73f,stemX,baseY*.79f);p.setShader(new android.graphics.LinearGradient(stemX-45,baseY*.6f,stemX+3,baseY*.8f,0xFF9BCB70,0xFF347A43,android.graphics.Shader.TileMode.CLAMP));c.drawPath(left,p);p.setShader(null);p.setColor(0xFFDBE9A4);p.setStrokeWidth(dp(1));c.drawLine(stemX-5,baseY*.73f,stemX-37,baseY*.65f,p);
            android.graphics.Path right=new android.graphics.Path();right.moveTo(stemX,baseY*.69f);right.cubicTo(stemX+40,baseY*.56f,stemX+49,baseY*.49f,stemX+13,baseY*.55f);right.cubicTo(stemX+4,baseY*.59f,stemX+1,baseY*.64f,stemX,baseY*.69f);p.setShader(new android.graphics.LinearGradient(stemX,baseY*.5f,stemX+45,baseY*.7f,0xFFB5D77C,0xFF397D48,android.graphics.Shader.TileMode.CLAMP));c.drawPath(right,p);p.setShader(null);p.setColor(0xFFDDEBA7);c.drawLine(stemX+5,baseY*.63f,stemX+38,baseY*.55f,p);
            float fx=stemX,fy=headY;float openness=.58f+.42f*Math.min(7,Math.max(0,streak))/7f;c.save();c.scale(openness,openness,fx,fy);for(int ring=0;ring<2;ring++){int count=ring==0?12:10;for(int j=0;j<count;j++){c.save();c.rotate(j*360f/count+(ring==0?0:18),fx,fy);float yy=ring==0?-23:-18;petal(c,fx,fy+yy,ring==0?8.5f:7f,ring==0?27:21,ring==0?col[0]:col[1],col[1],col[2]);c.restore();}}
            p.setShader(new android.graphics.RadialGradient(fx-3,fy-4,14,new int[]{0xFFFFF4B2,0xFFFFC84D,0xFFB66A22},null,android.graphics.Shader.TileMode.CLAMP));c.drawCircle(fx,fy,13,p);p.setShader(null);p.setColor(0xFF8F5523);for(int k=0;k<18;k++){double a=k*2.399;float r=3+(k%4)*2;c.drawCircle(fx+(float)Math.cos(a)*r,fy+(float)Math.sin(a)*r,1.15f,p);}
            c.restore();if(days<3){p.setColor(0xFF6C9D4F);c.drawOval(fx-8,fy+20,fx+8,fy+43,p);}
        }
    }

    private class WaterCanView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);
        WaterCanView(android.content.Context c){super(c);}
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setColor(0xFF9AB9C8);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(5);c.drawArc(w*.02f,h*.15f,w*.55f,h*.92f,80,200,false,p);p.setStyle(android.graphics.Paint.Style.FILL);p.setColor(0xFF82AFC0);c.drawRoundRect(w*.2f,h*.35f,w*.82f,h*.85f,10,10,p);p.setColor(0xFFAED0DA);c.drawRoundRect(w*.17f,h*.27f,w*.83f,h*.42f,8,8,p);p.setColor(0xFF82AFC0);android.graphics.Path spout=new android.graphics.Path();spout.moveTo(w*.72f,h*.42f);spout.lineTo(w*.98f,h*.16f);spout.lineTo(w*.88f,h*.12f);spout.lineTo(w*.62f,h*.36f);spout.close();c.drawPath(spout,p);}
    }
    private class WaterStreamView extends View {
        private final android.graphics.Paint p=new android.graphics.Paint(3);
        WaterStreamView(android.content.Context c){super(c);}
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);p.setColor(0xFF70BDE0);p.setStrokeWidth(4);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);for(int i=0;i<5;i++){float x=dp(10+i*11);c.drawLine(x,dp(i%2*8),x-5,dp(48+i%3*9),p);p.setStyle(android.graphics.Paint.Style.FILL);c.drawCircle(x-5,dp(53+i%3*9),dp(3),p);p.setStyle(android.graphics.Paint.Style.STROKE);}}
    }

    private void updateAchievementDates(){
        int days=prefs.getStringSet("study_days",new TreeSet<String>()).size(),minutes=totalStudyMinutes(),completed=0;for(List<String> topics:chapters.values())completed+=countCompleted(topics);int sessions=prefs.getInt("done_count",0);
        String[][] values={{"Первые шаги",String.valueOf(days>=1)},{"Неделя стараний",String.valueOf(days>=7)},{"Две недели",String.valueOf(days>=14)},{"Месяц успеха",String.valueOf(days>=30)},{"Два месяца",String.valueOf(days>=60)},{"Сто дней",String.valueOf(days>=100)},{"Маленький математик",String.valueOf(completed>=1)},{"Счёт — это просто",String.valueOf(completed>=5)},{"Знаток математики",String.valueOf(completed>=10)},{"Мастер знаний",String.valueOf(completed>=41)},{"Первый час",String.valueOf(minutes>=60)},{"10 часов учёбы",String.valueOf(minutes>=600)},{"Настойчивость",String.valueOf(sessions>=25)},{"Пятьдесят побед",String.valueOf(completed+sessions>=50)},{"Возвращаюсь к цели",String.valueOf(days>=3)},{"Ритм учёбы",String.valueOf(days>=5)}};
        SharedPreferences.Editor e=prefs.edit();String date=new SimpleDateFormat("d MMMM yyyy",new Locale("ru")).format(new Date());boolean changed=false;
        for(String[] v:values)if(Boolean.parseBoolean(v[1])&&!prefs.contains("achievement_date_"+v[0])){e.putString("achievement_date_"+v[0],date);changed=true;}if(changed)e.apply();
    }

    private android.graphics.drawable.Drawable roundedAchievementProgress(int trackColor,int fillColor){
        GradientDrawable track=new GradientDrawable();track.setColor(trackColor);track.setCornerRadius(dp(20));
        GradientDrawable fill=new GradientDrawable();fill.setColor(fillColor);fill.setCornerRadius(dp(20));
        android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{track,new android.graphics.drawable.ClipDrawable(fill,Gravity.LEFT,android.graphics.drawable.ClipDrawable.HORIZONTAL)});
        layers.setId(0,android.R.id.background);layers.setId(1,android.R.id.progress);
        return new android.graphics.drawable.InsetDrawable(layers,dp(2),0,dp(2),0);
    }

    private void showAchievements() {
        currentScreen="achievements";base("Коллекция достижений","Каждый маленький шаг достоин похвалы");
        int days=prefs.getStringSet("study_days",new TreeSet<String>()).size(),minutes=totalStudyMinutes(),completed=0;for(List<String> topics:chapters.values())completed+=countCompleted(topics);int sessions=prefs.getInt("done_count",0);
        String[][] a={{"🌱","Первые шаги","Первый день занятий",String.valueOf(days>=1),"1"},{"🌷","Неделя стараний","7 дней занятий",String.valueOf(days>=7),"7"},{"🌸","Две недели","14 дней занятий",String.valueOf(days>=14),"14"},{"💐","Месяц успеха","30 дней занятий",String.valueOf(days>=30),"30"},{"🏵️","Два месяца","60 дней занятий",String.valueOf(days>=60),"60"},{"🌻","Сто дней","100 дней занятий",String.valueOf(days>=100),"100"},{"📚","Маленький математик","Завершена первая тема",String.valueOf(completed>=1),"1"},{"🧮","Счёт — это просто","Завершено 5 тем",String.valueOf(completed>=5),"5"},{"🎓","Знаток математики","Завершено 10 тем",String.valueOf(completed>=10),"10"},{"🌟","Мастер знаний","Завершены все 41 темы",String.valueOf(completed>=41),"41"},{"⏳","Первый час","60 минут занятий",String.valueOf(minutes>=60),"60"},{"⌛","10 часов учёбы","600 минут занятий",String.valueOf(minutes>=600),"600"},{"🚀","Настойчивость","25 учебных занятий",String.valueOf(sessions>=25),"25"},{"💎","Пятьдесят побед","50 тем или занятий",String.valueOf(completed+sessions>=50),"50"},{"💗","Возвращаюсь к цели","Занятия в 3 разных дня",String.valueOf(days>=3),"3"},{"✨","Ритм учёбы","Занятия в 5 разных дней",String.valueOf(days>=5),"5"}};
        int unlocked=0;for(String[] x:a)if(Boolean.parseBoolean(x[3]))unlocked++;
        
        int xp=totalStudyMinutes()*10,level=xp/1000+1,levelProgress=xp%1000;
        LinearLayout levelCard=new LinearLayout(this);levelCard.setOrientation(LinearLayout.VERTICAL);levelCard.setPadding(0,dp(3),0,dp(3));levelCard.setBackgroundColor(android.graphics.Color.TRANSPARENT);addText(levelCard,"✦  ТВОЙ УРОВЕНЬ",10,PINK,true);
        LinearLayout levelRow=new LinearLayout(this);levelRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView levelNumber=text(String.valueOf(level),34,PINK,true);levelNumber.setGravity(Gravity.CENTER);levelNumber.setBackground(shape(PALE,50,blendColors(PINK,BG,0.35f)));levelRow.addView(levelNumber,lp(64,64,0,0,12,0));
        LinearLayout levelWords=new LinearLayout(this);levelWords.setOrientation(LinearLayout.VERTICAL);addText(levelWords,levelName(level),17,DARK,true);addText(levelWords,xp+" XP · до следующего уровня "+(1000-levelProgress)+" XP",11,MUTED,false);levelRow.addView(levelWords,new LinearLayout.LayoutParams(0,-2,1f));levelCard.addView(levelRow,lp(-1,-2,0,6,0,5));
        ProgressBar xpBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);xpBar.setMax(1000);xpBar.setProgress(levelProgress);xpBar.setProgressDrawable(roundedAchievementProgress(PALE,PINK));LinearLayout.LayoutParams xpBarLp=lp(-1,8,0,3,0,0);xpBarLp.leftMargin=dp(5);xpBarLp.rightMargin=dp(5);levelCard.addView(xpBar,xpBarLp);
        addText(levelCard,"За каждую минуту фокус-занятия начисляется 10 XP",10,MUTED,false);root.addView(levelCard,lp(-1,-2,0,0,0,10));
        addText(root,unlocked+" / "+a.length+" открыто",15,DARK,true);
        ProgressBar overall=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);overall.setMax(a.length);overall.setProgress(unlocked);overall.setProgressDrawable(roundedAchievementProgress(0xFFE9D4DE,PINK));LinearLayout.LayoutParams overallLp=lp(-1,9,0,7,0,9);overallLp.leftMargin=dp(5);overallLp.rightMargin=dp(5);root.addView(overall,overallLp);
        int nextIndex=-1;for(int i=0;i<a.length;i++)if(!Boolean.parseBoolean(a[i][3])){nextIndex=i;break;}
        if(nextIndex<0)addText(root,"Все достижения открыты — невероятно! ♡",12,PINK,true);else{String nextName=a[nextIndex][1];int target=Integer.parseInt(a[nextIndex][4]);int current=achievementCurrent(nextIndex,days,minutes,completed,sessions);addText(root,"До достижения «"+nextName+"»: осталось "+Math.max(0,target-current)+(nextIndex>=10&&nextIndex<=11?" мин":(nextIndex<=5||nextIndex>=14)?" дней":" шагов"),12,DARK,true);}
        android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(3);grid.setUseDefaultMargins(false);
        for(int ai=0;ai<a.length;ai++){String[] x=a[ai];boolean open=Boolean.parseBoolean(x[3]);int target=Integer.parseInt(x[4]);int current=achievementCurrent(ai,days,minutes,completed,sessions);
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(7),dp(7),dp(7),dp(7));tile.setBackground(shape(open?0xFFFFF0F5:0xFFF7F5F5,16,open?PINK:0xFFE4DDE0));
            android.widget.FrameLayout emblem=new android.widget.FrameLayout(this);emblem.setClipChildren(false);emblem.setClipToPadding(false);TextView icon=text(x[0],25,open?PINK:0xFF8F8790,true);icon.setGravity(Gravity.CENTER);android.widget.FrameLayout.LayoutParams iconLp=new android.widget.FrameLayout.LayoutParams(dp(44),dp(44),Gravity.CENTER);emblem.addView(icon,iconLp);if(!open){TextView lock=text("🔒",10,MUTED,true);lock.setGravity(Gravity.CENTER);android.widget.FrameLayout.LayoutParams lockLp=new android.widget.FrameLayout.LayoutParams(dp(18),dp(18),Gravity.TOP|Gravity.END);emblem.addView(lock,lockLp);}tile.addView(emblem,lp(-1,48,0,0,0,3));
            TextView name=text(x[1],10,open?DARK:0xFF514A50,true);name.setGravity(Gravity.CENTER);name.setMaxLines(2);name.setMinHeight(dp(29));tile.addView(name,lp(-1,-2,0,0,0,2));
            String progress=target>=100?String.format(Locale.US,"%.0f%%",Math.min(100,current*100f/target)):Math.min(current,target)+"/"+target;
            TextView state=text(open?"Открыто ✓":progress,10,open?PINK:0xFF625A61,true);state.setGravity(Gravity.CENTER);tile.addView(state,lp(-1,18,0,0,0,2));
            if(!open){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setIndeterminate(false);bar.setMax(Math.max(1,target));bar.setProgressDrawable(roundedAchievementProgress(0xFFECE1E5,PINK));bar.setProgress(0);bar.setLayerType(View.LAYER_TYPE_SOFTWARE,null);LinearLayout.LayoutParams barLp=lp(-1,8,0,6,0,0);barLp.leftMargin=dp(8);barLp.rightMargin=dp(8);tile.addView(bar,barLp);final int tileProgress=Math.max(0,Math.min(current,target));bar.post(()->{bar.setMax(Math.max(1,target));bar.setProgress(0);android.animation.ObjectAnimator progressAnimator=android.animation.ObjectAnimator.ofInt(bar,"progress",tileProgress);progressAnimator.setDuration(700);progressAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());progressAnimator.start();});}
            android.widget.GridLayout.LayoutParams gp=new android.widget.GridLayout.LayoutParams();gp.width=0;gp.height=dp(116);gp.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);gp.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(tile,gp);
            tile.setOnClickListener(v->animateLessonTap(tile,()->showAchievementDetail(x[0],x[1],x[2],open,current,target)));
        }
        root.addView(grid,lp(-1,-2,0,8,0,8));backHome();
    }

    private void showAchievementDetail(String emoji,String name,String description,boolean unlocked,int current,int target){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setGravity(Gravity.CENTER_HORIZONTAL);panel.setPadding(dp(22),dp(18),dp(22),dp(18));
        panel.setBackground(shape(isDarkAppearance()?0xFF292431:0xFFFFF7FA,24,isDarkAppearance()?0xFF514454:0xFFF0D7E1));
        TextView icon=text(emoji,54,PINK,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(isDarkAppearance()?0xFF3B3040:0xFFFFE8F0,48,0));panel.addView(icon,lp(104,104,0,0,0,12));
        TextView title=text(name,20,DARK,true);title.setGravity(Gravity.CENTER);panel.addView(title,lp(-1,-2,0,0,0,7));
        TextView desc=text(description,14,MUTED,false);desc.setGravity(Gravity.CENTER);panel.addView(desc,lp(-1,-2,0,0,0,12));
        if(unlocked){String date=prefs.getString("achievement_date_"+name,"Дата получения не сохранена");TextView status=text("✦  ДОСТИЖЕНИЕ ОТКРЫТО",11,PINK,true);status.setGravity(Gravity.CENTER);status.setBackground(shape(isDarkAppearance()?0xFF493344:0xFFFFE8F0,18,0));panel.addView(status,lp(-1,38,0,0,0,7));TextView when=text("Получено: "+date,12,MUTED,true);when.setGravity(Gravity.CENTER);panel.addView(when,lp(-1,26,0,0,0,0));}
        else{TextView progress=text("Прогресс · "+Math.min(current,target)+" из "+target,13,PINK,true);progress.setGravity(Gravity.CENTER);panel.addView(progress,lp(-1,30,0,0,0,5));ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(Math.max(1,target));bar.setIndeterminate(false);bar.setProgressDrawable(roundedAchievementProgress(0xFFEADDE4,PINK));bar.setProgress(0);LinearLayout.LayoutParams barLp=lp(-1,9,0,0,0,0);barLp.leftMargin=dp(8);barLp.rightMargin=dp(8);panel.addView(bar,barLp);final int detailProgress=Math.max(0,Math.min(current,target));bar.post(()->{bar.setMax(Math.max(1,target));android.animation.ObjectAnimator fillAnimator=android.animation.ObjectAnimator.ofInt(bar,"progress",detailProgress);fillAnimator.setDuration(650);fillAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());fillAnimator.start();});}
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setView(panel).setPositiveButton("Понятно",null).create();
        dialog.setOnShowListener(d->{if(dialog.getWindow()!=null){android.view.Window window=dialog.getWindow();window.setBackgroundDrawableResource(android.R.color.transparent);window.setDimAmount(0.24f);window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(40),dp(360)),-2);View decor=window.getDecorView();decor.setAlpha(0f);decor.setScaleX(0.92f);decor.setScaleY(0.92f);decor.setPivotX(decor.getWidth()/2f);decor.setPivotY(decor.getHeight()/2f);decor.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}});
        dialog.show();
    }

    private int achievementCurrent(int index,int days,int minutes,int completed,int sessions){if(index<=5||index>=14)return days;if(index<=9)return completed;if(index<=11)return minutes;if(index==12)return sessions;if(index==13)return completed+sessions;return 0;}

    private void scheduleFutureLetterReminder(String id,String date) {
        try {
            java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);
            f.setTimeZone(AppTime.zone());
            Date parsed=f.parse(date);if(parsed==null)return;
            Calendar when=Calendar.getInstance(AppTime.zone());when.setTime(parsed);when.set(Calendar.HOUR_OF_DAY,9);when.set(Calendar.MINUTE,0);when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);
            if(when.getTimeInMillis()<=System.currentTimeMillis())return;
            Intent intent=new Intent(this,ReminderReceiver.class).setAction("future_letter_"+id)
                    .putExtra("message","Твоё письмо себе уже можно открыть ✉").putExtra("id",Math.abs(id.hashCode())+10000);
            PendingIntent pi=PendingIntent.getBroadcast(this,Math.abs(id.hashCode())+10000,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);
        } catch(Exception ignored) { }
    }

    public static void rescheduleFutureLetterReminders(Context context){
        SharedPreferences p=context.getSharedPreferences(PREFS,MODE_PRIVATE);Set<String> ids=p.getStringSet("future_letter_ids",new TreeSet<String>());AlarmManager am=(AlarmManager)context.getSystemService(ALARM_SERVICE);
        if(!p.getBoolean("future_letters_enabled",true)){for(String id:ids){int code=Math.abs(id.hashCode())+10000;Intent old=new Intent(context,ReminderReceiver.class).setAction("future_letter_"+id);PendingIntent pi=PendingIntent.getBroadcast(context,code,old,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);pi.cancel();}return;}
        for(String id:ids){if(!p.getBoolean("future_remind_"+id,false))continue;String date=p.getString("future_date_"+id,"");try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(AppTime.zone());Date parsed=f.parse(date);if(parsed==null)continue;Calendar when=Calendar.getInstance(AppTime.zone());when.setTime(parsed);when.set(Calendar.HOUR_OF_DAY,9);when.set(Calendar.MINUTE,0);when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);if(when.getTimeInMillis()<=System.currentTimeMillis())continue;int code=Math.abs(id.hashCode())+10000;Intent intent=new Intent(context,ReminderReceiver.class).setAction("future_letter_"+id).putExtra("message","Твоё письмо себе уже можно открыть ✉").putExtra("id",code);PendingIntent pi=PendingIntent.getBroadcast(context,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);else am.setExact(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);}catch(Exception ignored){}}
    }

    private void showFutureLetters() {
        currentScreen="letters";base("✉ Письмо себе в будущее",g("Запечатай важные мысли для себя будущей","Запечатай важные мысли для себя будущего"));
        LinearLayout steps=new LinearLayout(this);steps.setOrientation(LinearLayout.HORIZONTAL);String[] stepLabels={"1. Напиши","2. Выбери дату","3. Отправь"};for(int i=0;i<3;i++){TextView st=text(stepLabels[i],10,i==0?PINK:MUTED,true);st.setGravity(Gravity.CENTER);st.setPadding(dp(5),dp(9),dp(5),dp(9));st.setBackground(shape(i==0?0xFFFFE8EF:0xFFF7F3F4,14,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(38),1f);p.setMargins(dp(2),0,dp(2),0);steps.addView(st,p);}root.addView(steps,lp(-1, 48,0,0,0,18));
        TextView prompt=text("✒  Напиши письмо",17,DARK,true);root.addView(prompt,lp(-1,-2,0,0,0,5));
        EditText body=new EditText(this);body.setHint("Дорогая я, хочу сказать тебе...");body.setMinLines(5);body.setGravity(Gravity.TOP);body.setTextSize(16);body.setTextColor(DARK);body.setHintTextColor(MUTED);body.setPadding(dp(2),dp(12),dp(2),dp(12));body.setBackgroundColor(0x00000000);root.addView(body,lp(-1,145,0,0,0,18));
        TextView dateLabel=text("📅  Когда открыть письмо?",15,DARK,true);root.addView(dateLabel,lp(-1,-2,0,0,0,7));
        EditText date=new EditText(this);date.setHint("Например, 14 июня 2027");date.setSingleLine(true);date.setFocusable(false);date.setClickable(true);date.setTextSize(15);date.setTextColor(DARK);date.setPadding(dp(14),dp(8),dp(14),dp(8));date.setBackground(shape(0xFFFFFFFF,16,0xFFEADDE2));root.addView(date,lp(-1,50,0,0,0,5));
        TextView countdown=text("Выбери дату — я посчитаю, сколько осталось дней",12,MUTED,false);root.addView(countdown,lp(-1,26,0,0,0,8));
        final Calendar[] chosen={null};Runnable updateCountdown=()->{if(chosen[0]==null)return;Calendar now=Calendar.getInstance(AppTime.zone());Calendar target=(Calendar)chosen[0].clone();long daysLeft=Math.max(0,(target.getTimeInMillis()-now.getTimeInMillis())/86400000L);countdown.setText("До открытия: через "+daysLeft+" дней");date.setText(new SimpleDateFormat("d MMMM yyyy",new Locale("ru")).format(target.getTime()));};
        date.setOnClickListener(v->{Calendar now=Calendar.getInstance(AppTime.zone());Calendar initial=chosen[0]==null?(Calendar)now.clone():(Calendar)chosen[0].clone();android.app.DatePickerDialog picker=new android.app.DatePickerDialog(this,(p,y,m,d)->{Calendar c=Calendar.getInstance(AppTime.zone());c.set(y,m,d,9,0,0);c.set(Calendar.MILLISECOND,0);chosen[0]=c;updateCountdown.run();},initial.get(Calendar.YEAR),initial.get(Calendar.MONTH),initial.get(Calendar.DAY_OF_MONTH));picker.getDatePicker().setMinDate(now.getTimeInMillis());picker.show();});
        LinearLayout quick=new LinearLayout(this);quick.setOrientation(LinearLayout.HORIZONTAL);String[] quickNames={"Через месяц","Через полгода","Через год","В конце уч. года"};int[] months={1,6,12,7};for(int i=0;i<quickNames.length;i++){final int index=i;TextView chip=text(quickNames[i],12,PINK,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(7),dp(8),dp(7),dp(8));chip.setBackground(shape(0xFFFFF0F4,14,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1f);p.setMargins(dp(2),0,dp(2),0);p.height=dp(48);quick.addView(chip,p);chip.setOnClickListener(v->{Calendar c=Calendar.getInstance(AppTime.zone());if(index==3){int y=c.get(Calendar.YEAR);int m=c.get(Calendar.MONTH);c.set(y,m<=5?Calendar.JUNE:Calendar.JUNE,m<=5?30:30);if(m>5)c.add(Calendar.YEAR,1);}else c.add(Calendar.MONTH,months[index]);chosen[0]=c;updateCountdown.run();});}root.addView(quick,lp(-1,48,0,0,0,15));
        CheckBox hidden=new CheckBox(this);hidden.setText("🔒  Скрыть письмо до даты открытия");hidden.setTextColor(DARK);hidden.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));hidden.setChecked(true);root.addView(hidden,lp(-1, 48,0,0,0,2));
        CheckBox remind=new CheckBox(this);remind.setText("🔔  Напомнить, когда придёт время");remind.setTextColor(DARK);remind.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));root.addView(remind,lp(-1, 48,0,0,0,14));
        Button save=button("✉  Запечатать письмо",true);save.setTextSize(15);root.addView(save,lp(-1,54,0,0,0,18));
        save.setOnClickListener(v->{String msg=body.getText().toString().trim();if(msg.isEmpty()||chosen[0]==null){showAppMessage("Напиши письмо и выбери дату");return;}String id=String.valueOf(System.currentTimeMillis());String d=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(chosen[0].getTime());Set<String> ids=new TreeSet<>(prefs.getStringSet("future_letter_ids",new TreeSet<String>()));ids.add(id);prefs.edit().putString("future_letter_"+id,msg).putString("future_date_"+id,d).putBoolean("future_hidden_"+id,hidden.isChecked()).putBoolean("future_remind_"+id,remind.isChecked()).putStringSet("future_letter_ids",ids).apply();if(remind.isChecked())scheduleFutureLetterReminder(id,d);body.setText("");chosen[0]=null;date.setText("");countdown.setText("Выбери дату — я посчитаю, сколько осталось дней");showSavedBanner("Письмо запечатано","Оно будет ждать тебя в назначенный день ✉");showFutureLetters();});
        Set<String> ids=new TreeSet<>(prefs.getStringSet("future_letter_ids",new TreeSet<String>()));List<String> sorted=new ArrayList<>(ids);java.util.Collections.sort(sorted,java.util.Collections.reverseOrder());addText(root,"📬 ТВОИ ЗАПЕЧАТАННЫЕ ПИСЬМА",11,PINK,true);
        for(String id:sorted){String d=prefs.getString("future_date_"+id,"");String msg=prefs.getString("future_letter_"+id,"");boolean open=d.compareTo(todayKey())<=0;boolean concealed=prefs.getBoolean("future_hidden_"+id,true)&&!open;LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(dp(13),dp(12),dp(13),dp(12));item.setBackground(shape(0xFFFFF5F8,18,0));addText(item,(open?"✉️  Можно открыть":"🔒  Открывается "+d),13,PINK,true);if(!concealed)addText(item,msg,14,DARK,false);else addText(item,"Твоё письмо ждёт назначенного дня ♡",12,MUTED,false);root.addView(item,lp(-1,-2,0,0,0,8));}
        addText(root,"📬 У тебя уже есть "+ids.size()+" запечатанное письмо"+(ids.size()==1?"":"(а)")+".",12,MUTED,false);backHome();
    }

    
    
    private String createBackupJson() throws Exception {
        org.json.JSONObject json=new org.json.JSONObject();json.put("app","Study with Love");json.put("version",2);org.json.JSONObject data=new org.json.JSONObject(),photos=new org.json.JSONObject();
        Map<String,?> all=prefs.getAll();
        for(Map.Entry<String,?> entry:all.entrySet()){
            String key=entry.getKey();if(key.startsWith("timer_service_")||key.startsWith("topic_photo_"))continue;
            Object value=entry.getValue();if(value instanceof Set){org.json.JSONArray arr=new org.json.JSONArray();for(Object item:(Set<?>)value)arr.put(String.valueOf(item));data.put(key,arr);}else data.put(key,value);
        }
        Set<String> suffixes=new TreeSet<>();for(String key:all.keySet()){if(key.startsWith("topic_photo_file_"))suffixes.add(key.substring("topic_photo_file_".length()));else if(key.startsWith("topic_photo_"))suffixes.add(key.substring("topic_photo_".length()));}
        for(String suffix:suffixes){
            String path=(String)all.get("topic_photo_file_"+suffix),uri=(String)all.get("topic_photo_"+suffix);java.io.InputStream in=null;
            try{if(path!=null&&!path.isEmpty())in=new java.io.FileInputStream(path);else if(uri!=null&&!uri.isEmpty())in=getContentResolver().openInputStream(android.net.Uri.parse(uri));
                if(in==null)throw new Exception("Фото темы недоступно: "+suffix);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);photos.put(suffix,android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP));
            }finally{if(in!=null)try{in.close();}catch(Exception ignored){}}
        }
        json.put("data",data);json.put("topic_photos",photos);return json.toString(2);
    }
    private void restoreBackupJson(String raw) throws Exception {
        org.json.JSONObject json=new org.json.JSONObject(raw);if(!"Study with Love".equals(json.optString("app")))throw new Exception("Wrong backup");
        org.json.JSONObject data=json.getJSONObject("data"),photos=json.optJSONObject("topic_photos");SharedPreferences.Editor editor=prefs.edit().clear();
        java.util.Iterator<String> keys=data.keys();while(keys.hasNext()){
            String key=keys.next();Object value=data.get(key);
            if(value instanceof org.json.JSONArray){org.json.JSONArray arr=(org.json.JSONArray)value;Set<String> set=new TreeSet<>();for(int i=0;i<arr.length();i++)set.add(arr.getString(i));editor.putStringSet(key,set);}
            else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
            else if(value instanceof Number){Number n=(Number)value;if(key.startsWith("app_usage_remainder_")||key.startsWith("focus_remainder_ms_")||key.endsWith("_elapsed_ms")||key.endsWith("_time_ms"))editor.putLong(key,n.longValue());else if(value instanceof Integer)editor.putInt(key,n.intValue());else if(value instanceof Long)editor.putLong(key,n.longValue());else editor.putFloat(key,n.floatValue());}
            else if(value instanceof String)editor.putString(key,(String)value);
        }
        if(photos!=null){java.util.Iterator<String> photoKeys=photos.keys();while(photoKeys.hasNext()){String suffix=photoKeys.next();byte[] bytes=android.util.Base64.decode(photos.getString(suffix),android.util.Base64.DEFAULT);java.io.File file=new java.io.File(getFilesDir(),"topic_photo_"+Math.abs(suffix.hashCode())+".jpg");try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){out.write(bytes);out.flush();}editor.putString("topic_photo_file_"+suffix,file.getAbsolutePath()).remove("topic_photo_"+suffix);}}
        editor.commit();scheduleReminders(this);
    }
    private void showSearchDialog() {
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(10),dp(18),dp(6));
        LinearLayout field=new LinearLayout(this);field.setGravity(Gravity.CENTER_VERTICAL);field.setPadding(dp(12),0,dp(8),0);field.setBackground(shape(0xFFFFF5F7,17,0xFFF0DCE4));
        TextView icon=text("⌕",23,PINK,true);field.addView(icon,lp(28,48,0,0,7,0));
        EditText query=new EditText(this);query.setSingleLine(true);query.setTextSize(15);query.setTextColor(DARK);query.setHintTextColor(MUTED);query.setHint("Найти предмет или тему");query.setBackgroundColor(0x00000000);query.setPadding(0,0,0,0);field.addView(query,new LinearLayout.LayoutParams(0,dp(48),1f));
        TextView clear=text("×",22,MUTED,true);clear.setGravity(Gravity.CENTER);clear.setVisibility(View.GONE);field.addView(clear,lp(48,48,0,0,0,0));clear.setOnClickListener(v->query.setText(""));
        body.addView(field,lp(-1,50,0,0,0,8));addText(body,"Поиск по названию предмета или любой теме",11,MUTED,false);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("Поиск уроков").setView(body).setNegativeButton("Закрыть",null).setPositiveButton("Найти",(d,w)->{
            String q=query.getText().toString().trim().toLowerCase(Locale.ROOT);if(q.isEmpty())return;List<String> matches=new ArrayList<>();
            for(Map.Entry<String,List<String>> e:chapters.entrySet()){
                String ch=e.getKey();String display=ch.startsWith("CUSTOM:")?ch.split(":",3)[2]:ch.replaceFirst("^[^ ]+\\s*","");
                if(display.toLowerCase(Locale.ROOT).contains(q))matches.add(display+"\t"+ch);
                for(String topic:e.getValue())if(topic.toLowerCase(Locale.ROOT).contains(q))matches.add(display+"  •  "+topic+"\t"+ch+"\n"+topic);
            }
            if(matches.isEmpty()){new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Ничего не найдено").setMessage("Попробуй изменить запрос ♡").setPositiveButton("Понятно",null).show();return;}
            String[] rawItems=matches.toArray(new String[0]);String[] items=rawItems.clone();
            for(int i=0;i<items.length;i++){String raw=items[i];int tab=raw.indexOf("\t");int nl=raw.indexOf("\n");items[i]=raw.substring(0,tab>=0?tab:(nl>=0?nl:raw.length()));}
            new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Результаты поиска").setItems(items,(dlg,which)->{
                String[] parts=rawItems[which].split("\\t",2);if(parts.length<2)return;selectedChapter=parts[1];
                if(rawItems[which].contains("\n")){selectedTopic=rawItems[which].substring(rawItems[which].indexOf("\n")+1);showTopic(selectedTopic);}else showTopics(selectedChapter);
            }).show();
        }).create();
        query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){clear.setVisibility(s.length()>0?View.VISIBLE:View.GONE);}public void afterTextChanged(android.text.Editable e){}});
        dialog.show();
    }
    private String mainExamDate(){
        String id=prefs.getString("exam_main_goal_id","");Set<String> ids=prefs.getStringSet("exam_goal_ids",new TreeSet<String>());if(!ids.contains(id)){id="";for(String candidate:ids){if(id.isEmpty()||candidate.compareTo(id)>0)id=candidate;}}
        return id.isEmpty()?"":prefs.getString("exam_goal_date_"+id,"");
    }
    private int ensureExamPlan(){
        String examDate=mainExamDate();long daysUntilExam=examDate.isEmpty()?-1:daysUntil(examDate);if(examDate.isEmpty()||daysUntilExam<0){SharedPreferences.Editor cleanup=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith("planner_tasks_")){Set<String> old=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));boolean changed=false;java.util.Iterator<String> taskIt=old.iterator();while(taskIt.hasNext())if(taskIt.next().startsWith("📘 Экзамен:")){taskIt.remove();changed=true;}if(changed)cleanup.putStringSet(key,old);}cleanup.apply();return 0;}int days=(int)Math.min(3650L,Math.max(1L,daysUntilExam+1));
        List<String> pending=new ArrayList<>();for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue())if(isTopicInExamScope(entry.getKey(),topic)&&topicProgressForChapter(entry.getKey(),topic)<100)pending.add(topic);
        Map<String,Set<String>> planned=new LinkedHashMap<>();Calendar start=Calendar.getInstance(AppTime.zone());
        for(int i=0;i<pending.size();i++){int offset=Math.min(days-1,(int)(((long)i*days)/Math.max(1,pending.size())));Calendar day=(Calendar)start.clone();day.add(Calendar.DAY_OF_YEAR,offset);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(day.getTime());Set<String> tasks=planned.get(date);if(tasks==null){tasks=new TreeSet<>();planned.put(date,tasks);}tasks.add("📘 Экзамен: "+pending.get(i)+" · 20 мин · Повторить правило и решить задачи");}
        SharedPreferences.Editor edit=prefs.edit();for(String key:prefs.getAll().keySet())if(key.startsWith("planner_tasks_")){Set<String> old=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));boolean changed=false;java.util.Iterator<String> it=old.iterator();while(it.hasNext())if(it.next().startsWith("📘 Экзамен:")){it.remove();changed=true;}if(changed)edit.putStringSet(key,old);}
        for(Map.Entry<String,Set<String>> e:planned.entrySet()){String key="planner_tasks_"+e.getKey();Set<String> merged=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));merged.addAll(e.getValue());edit.putStringSet(key,merged);}
        edit.putString("exam_plan_last_built",todayKey()).apply();return pending.size();
    }
    private void showPlanner() {
        currentScreen="planner";base("Умный планировщик","Небольшой план — уже хороший шаг");
        String[] selectedDate={todayKey()},duration={""},action={""};
        String[] durations={"15 мин","30 мин","1 час"},actions={"Повторить тему","Решить 5 задач","Сделать конспект"};
        LinearLayout switcher=new LinearLayout(this);switcher.setGravity(Gravity.CENTER);switcher.setBackground(shape(PALE,16,0));
        TextView todayChip=text("Сегодня",13,PINK,true),tomorrowChip=text("Завтра",13,MUTED,true);todayChip.setGravity(Gravity.CENTER);tomorrowChip.setGravity(Gravity.CENTER);
        switcher.addView(todayChip,new LinearLayout.LayoutParams(0,dp(48),1f));switcher.addView(tomorrowChip,new LinearLayout.LayoutParams(0,dp(48),1f));root.addView(switcher,lp(-1, 48,0,0,0,10));
        addSpacedRepetitionSection();
        int examPlanTopics=ensureExamPlan();if(!mainExamDate().isEmpty()){boolean noExamTopics=prefs.getBoolean("exam_topic_filter_enabled",false)&&prefs.getStringSet("exam_topic_ids",new TreeSet<String>()).isEmpty();String planMessage=daysUntil(mainExamDate())<0?"Дата экзамена уже прошла.":noExamTopics?"Отметь темы для экзамена в разделе «Мой экзамен» — тогда появится план.":examPlanTopics==0?"Все выбранные темы уже завершены.":"План автоматически распределил "+examPlanTopics+" незавершённых тем до "+mainExamDate()+". При пропуске план пересчитывается при следующем открытии.";LinearLayout planCard=card();addText(planCard,"🎯  ПЛАН ДО ЭКЗАМЕНА",11,PINK,true);addText(planCard,planMessage,13,DARK,false);root.addView(planCard,lp(-1,-2,0,0,0,10));}
        LinearLayout form=card();addText(form,"НОВАЯ ЗАДАЧА",12,PINK,true);TextView counter=text("0 / 3 важных дела",11,MUTED,true);form.addView(counter,lp(-1,22,0,2,0,0));
        EditText task=new EditText(this);task.setSingleLine(true);task.setHint("Например: Дроби");task.setTextColor(DARK);task.setBackground(shape(0xFFFFFFFF,15,0xFFEADDE2));form.addView(task,lp(-1,48,0,7,0,8));
        addText(form,"СКОЛЬКО ВРЕМЕНИ?",10,MUTED,true);
        LinearLayout durationRow=new LinearLayout(this);durationRow.setGravity(Gravity.CENTER_VERTICAL);form.addView(durationRow,lp(-1,38,0,3,0,7));
        TextView[] durationViews=new TextView[durations.length];
        Runnable refreshDuration=()->{for(int i=0;i<durations.length;i++){boolean on=duration[0].equals(durations[i]);durationViews[i].setTextColor(on?0xFFFFFFFF:PINK);durationViews[i].setBackground(shape(on?PINK:0xFFFFF0F4,15,on?PINK:0));}};
        for(int i=0;i<durations.length;i++){final String value=durations[i];TextView chip=text(value,12,PINK,true);chip.setGravity(Gravity.CENTER);durationViews[i]=chip;LinearLayout.LayoutParams durationLp=new LinearLayout.LayoutParams(0,dp(48),1f);durationLp.setMargins(0,0,dp(6),0);durationRow.addView(chip,durationLp);chip.setOnClickListener(v->{duration[0]=value;refreshDuration.run();});}
        addText(form,"ЧТО ДЕЛАЕМ?",10,MUTED,true);
        LinearLayout actionRow=new LinearLayout(this);actionRow.setGravity(Gravity.CENTER_VERTICAL);form.addView(actionRow,lp(-1,38,0,3,0,7));
        TextView[] actionViews=new TextView[actions.length];
        Runnable refreshAction=()->{for(int i=0;i<actions.length;i++){boolean on=action[0].equals(actions[i]);actionViews[i].setTextColor(on?0xFFFFFFFF:PINK);actionViews[i].setBackground(shape(on?PINK:0xFFFFF0F4,14,on?PINK:0));}};
        for(int i=0;i<actions.length;i++){final String value=actions[i];TextView chip=text(value,12,PINK,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(3),0,dp(3),0);actionViews[i]=chip;actionRow.addView(chip,new LinearLayout.LayoutParams(0,dp(48),1f));chip.setOnClickListener(v->{action[0]=value;refreshAction.run();});}
        Button add=button("＋  Добавить в план",true);add.setEnabled(false);add.setAlpha(.45f);form.addView(add,lp(-1,48,0,4,0,0));root.addView(form,lp(-1,-2,0,0,0,10));
        task.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){boolean valid=s.toString().trim().length()>0;add.setEnabled(valid);add.setAlpha(valid?1f:.45f);}public void afterTextChanged(android.text.Editable e){}});
        TextView overload=text("",11,PINK,true);root.addView(overload,lp(-1,-2,0,0,0,5));LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list,lp(-1,-2,0,0,0,8));
        Button move=button("↗  Перенести незавершённое на завтра",false);root.addView(move,lp(-1, 48,0,4,0,8));Runnable[] render=new Runnable[1];
        Runnable updateDate=()->{boolean isToday=selectedDate[0].equals(todayKey());todayChip.setTextColor(isToday?PINK:MUTED);tomorrowChip.setTextColor(isToday?MUTED:PINK);todayChip.setBackground(shape(isToday?0xFFFFFFFF:PALE,13,isToday?PINK:0));tomorrowChip.setBackground(shape(isToday?PALE:0xFFFFFFFF,13,isToday?0:PINK));};
        todayChip.setOnClickListener(v->{selectedDate[0]=todayKey();updateDate.run();render[0].run();});
        tomorrowChip.setOnClickListener(v->{Calendar cal=Calendar.getInstance(AppTime.zone());cal.add(Calendar.DAY_OF_YEAR,1);selectedDate[0]=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.getTime());updateDate.run();render[0].run();});
        render[0]=()->{list.removeAllViews();String key="planner_tasks_"+selectedDate[0];Set<String> tasks=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));counter.setText(tasks.size()+" / 3 важных дела");overload.setText(tasks.size()>=3?"Уже "+tasks.size()+" задач — точно хватит сил на ещё одну? ♡":"");
            move.setVisibility(tasks.isEmpty()||!selectedDate[0].equals(todayKey())?View.GONE:View.VISIBLE);
            if(tasks.isEmpty()){addText(list,"Можно добавить одно важное дело, если есть силы ♡",12,MUTED,false);}
            for(String item:tasks){LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);CheckBox done=new CheckBox(this);done.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));done.setText(item);done.setTextColor(DARK);row.addView(done,new LinearLayout.LayoutParams(0,dp(48),1f));Button del=button("×",false);del.setMinWidth(dp(38));row.addView(del);
                done.setOnCheckedChangeListener((b,on)->{if(on){Set<String> latest=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));latest.remove(item);prefs.edit().putStringSet(key,latest).putInt("planner_completed_"+selectedDate[0],prefs.getInt("planner_completed_"+selectedDate[0],0)+1).apply();row.animate().alpha(0).setDuration(180).withEndAction(render[0]::run).start();}});
                del.setOnClickListener(v->{Set<String> latest=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));latest.remove(item);prefs.edit().putStringSet(key,latest).apply();render[0].run();});
                final float[] down={0};row.setOnTouchListener((v,event)->{if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){down[0]=event.getX();return true;}if(event.getAction()==android.view.MotionEvent.ACTION_UP){float dx=event.getX()-down[0];if(Math.abs(dx)>dp(55)){Set<String> latest=new TreeSet<>(prefs.getStringSet(key,new TreeSet<String>()));latest.remove(item);if(dx>0)prefs.edit().putInt("planner_completed_"+selectedDate[0],prefs.getInt("planner_completed_"+selectedDate[0],0)+1).apply();prefs.edit().putStringSet(key,latest).apply();row.animate().alpha(0).setDuration(160).withEndAction(render[0]::run).start();return true;}return false;}return true;});
                list.addView(row,lp(-1,-2,0,0,0,7));}
        };
        move.setOnClickListener(v->{if(selectedDate[0].equals(todayKey())){Set<String> pending=new TreeSet<>(prefs.getStringSet("planner_tasks_"+selectedDate[0],new TreeSet<String>()));if(pending.isEmpty())return;Calendar cal=Calendar.getInstance(AppTime.zone());cal.add(Calendar.DAY_OF_YEAR,1);String tomorrow=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.getTime());Set<String> next=new TreeSet<>(prefs.getStringSet("planner_tasks_"+tomorrow,new TreeSet<String>()));next.addAll(pending);prefs.edit().putStringSet("planner_tasks_"+tomorrow,next).putStringSet("planner_tasks_"+selectedDate[0],new TreeSet<String>()).apply();showSavedBanner("План обновлён","Незавершённые задачи перенесены на завтра ♡");render[0].run();}});
        add.setOnClickListener(v->{String value=task.getText().toString().trim();if(value.isEmpty())return;StringBuilder entry=new StringBuilder(value);if(!action[0].isEmpty())entry.append(" · ").append(action[0]);if(!duration[0].isEmpty())entry.append(" · ").append(duration[0]);Set<String> tasks=new TreeSet<>(prefs.getStringSet("planner_tasks_"+selectedDate[0],new TreeSet<String>()));tasks.add(entry.toString());prefs.edit().putStringSet("planner_tasks_"+selectedDate[0],tasks).apply();task.setText("");duration[0]="";action[0]="";refreshDuration.run();refreshAction.run();render[0].run();});
        updateDate.run();render[0].run();
    }
    private int weeklyMinutesLast7Days(){int total=0;Calendar c=Calendar.getInstance(AppTime.zone());for(int i=0;i<7;i++){String key=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());total+=prefs.getInt("focus_minutes_"+key,0);c.add(Calendar.DAY_OF_YEAR,-1);}return total;}
    private class WeeklyGoalRing extends View{private final android.graphics.Paint paint=new android.graphics.Paint(3);private final int done,goal;WeeklyGoalRing(Context c,int d,int g){super(c);done=d;goal=Math.max(1,g);}@Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.38f;paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(8));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setColor(isDarkAppearance()?0xFF49404B:PALE);canvas.drawCircle(cx,cy,r,paint);paint.setColor(PINK);canvas.drawArc(new android.graphics.RectF(cx-r,cy-r,cx+r,cy+r),-90,360f*Math.min(1f,(float)done/goal),false,paint);paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(DARK);paint.setTextAlign(android.graphics.Paint.Align.CENTER);paint.setTextSize(dp(15));canvas.drawText(Math.min(100,done*100/goal)+"%",cx,cy+dp(5),paint);}}
    private void exportWeeklySummaryImage(){int minutes=weeklyMinutesLast7Days(),goal=prefs.getInt("weekly_goal_minutes",300);android.graphics.Bitmap bmp=android.graphics.Bitmap.createBitmap(1080,1350,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas c=new android.graphics.Canvas(bmp);c.drawColor(BG);android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(PINK);p.setTextSize(54);p.setTypeface(android.graphics.Typeface.create(selectedFontFamily(),android.graphics.Typeface.BOLD));c.drawText("Моя учебная неделя",72,150,p);p.setColor(DARK);p.setTextSize(42);c.drawText("Моя неделя учёбы",72,245,p);p.setTextSize(32);c.drawText("Фокус-время: "+minutes+" минут",72,360,p);c.drawText("Цель: "+goal+" минут",72,420,p);c.drawText("Выполнено: "+Math.min(100,minutes*100/Math.max(1,goal))+"%",72,480,p);p.setColor(PALE);c.drawRoundRect(72,540,1008,580,20,20,p);p.setColor(PINK);c.drawRoundRect(72,540,72+936*Math.min(1f,(float)minutes/Math.max(1,goal)),580,20,20,p);int y=700;Calendar day=Calendar.getInstance(AppTime.zone());for(int i=0;i<7;i++){String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(day.getTime());int m=prefs.getInt("focus_minutes_"+date,0);p.setColor(DARK);p.setTextSize(28);c.drawText(new SimpleDateFormat("EEE, d MMM",new Locale("ru")).format(day.getTime())+"  ·  "+m+" мин",90,y,p);y+=68;day.add(Calendar.DAY_OF_YEAR,-1);}p.setColor(PINK);p.setTextSize(25);c.drawText("Маленькие шаги складываются в большой результат ♡",72,1240,p);pendingWeeklyBitmap=bmp;Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/png");intent.putExtra(Intent.EXTRA_TITLE,"study_with_love_week.png");startActivityForResult(intent,WEEKLY_IMAGE_EXPORT);}
    private void exportProgressPdf(){Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,"study_with_love_report.pdf");startActivityForResult(intent,PDF_REPORT_EXPORT);}
    private String createDiaryExportText(){List<String> ids=new ArrayList<>(prefs.getStringSet("diary_ids",new TreeSet<String>()));java.util.Collections.sort(ids);StringBuilder out=new StringBuilder("ДНЕВНИК STUDY WITH LOVE\\n\\n");for(String id:ids){String entry=prefs.getString("diary_"+id,"");if(!entry.isEmpty())out.append(entry).append("\\n\\n————————————\\n\\n");}return out.toString();}
    private void writeDiaryPdf(android.net.Uri uri)throws Exception{android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();android.graphics.Paint paint=new android.graphics.Paint(3);paint.setTextSize(11);paint.setColor(Color.BLACK);String[] lines=createDiaryExportText().split("\\n");int pageNo=1,y=50;android.graphics.pdf.PdfDocument.Page page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNo).create());android.graphics.Canvas canvas=page.getCanvas();for(String line:lines){if(y>800){pdf.finishPage(page);page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=50;}String textLine=line;while(paint.measureText(textLine)>530&&textLine.length()>4){int cut=Math.max(4,(int)(textLine.length()*530/paint.measureText(textLine)));canvas.drawText(textLine.substring(0,cut),32,y,paint);textLine=textLine.substring(cut);y+=17;if(y>800){pdf.finishPage(page);page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,++pageNo).create());canvas=page.getCanvas();y=50;}}canvas.drawText(textLine,32,y,paint);y+=17;}pdf.finishPage(page);java.io.OutputStream out=getContentResolver().openOutputStream(uri);if(out==null)throw new Exception("No output stream");pdf.writeTo(out);out.close();pdf.close();}

    private void writeProgressPdf(android.net.Uri uri)throws Exception{
        android.graphics.pdf.PdfDocument doc=new android.graphics.pdf.PdfDocument();int pageNo=1;
        android.graphics.pdf.PdfDocument.Page page=doc.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNo).create());
        android.graphics.Canvas c=page.getCanvas();android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(0xFF3A3036);p.setTextSize(22);p.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.BOLD));c.drawText("Мой учебный отчёт",40,55,p);p.setTypeface(android.graphics.Typeface.DEFAULT);p.setTextSize(12);
        List<String> lines=new ArrayList<>();lines.add("Дата отчёта: "+todayKey());lines.add("Учебных дней: "+prefs.getStringSet("study_days",new TreeSet<String>()).size());lines.add("Всего минут фокуса: "+totalStudyMinutes());lines.add("За последние 7 дней: "+weeklyMinutesLast7Days()+" минут");lines.add("Недельная цель: "+prefs.getInt("weekly_goal_minutes",300)+" минут");lines.add("");lines.add("ЗАВЕРШЁННЫЕ ТЕМЫ:");
        for(Map.Entry<String,List<String>> e:chapters.entrySet())for(String topic:e.getValue())if(topicProgressForChapter(e.getKey(),topic)==100)lines.add("• "+topic);
        lines.add("");lines.add("ПОСЛЕДНИЕ РЕЗУЛЬТАТЫ ТЕСТОВ:");
        List<String> results=new ArrayList<>();for(String key:prefs.getAll().keySet())if(key.startsWith("test_result_"))results.add(key);java.util.Collections.sort(results,java.util.Collections.reverseOrder());int limit=Math.min(15,results.size());
        for(int i=0;i<limit;i++){try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(results.get(i),"{}"));lines.add(o.optString("date","")+" · "+o.optString("topic","Тест")+" — "+o.optInt("score",0)+"/"+o.optInt("total",0)+", ошибок: "+o.optInt("wrong",0));}catch(Exception ignored){}}
        int y=90;for(String line:lines){if(y>790){doc.finishPage(page);pageNo++;page=doc.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,pageNo).create());c=page.getCanvas();y=50;}c.drawText(line.length()>78?line.substring(0,77)+"…":line,40,y,p);y+=19;}
        doc.finishPage(page);java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt");if(out==null)throw new Exception("output");doc.writeTo(out);out.close();doc.close();
    }
    private void showDetailedStats() {
        currentScreen="stats";saveForegroundUsage();base("Подробная статистика","Посмотри, как растёт твой учебный путь");int completed=0,total=0;
        for(Map.Entry<String,List<String>> entry:chapters.entrySet())for(String topic:entry.getValue()){if(!isTopicInExamScope(entry.getKey(),topic))continue;total++;if(topicProgressForChapter(entry.getKey(),topic)==100)completed++;}LinearLayout summary=card();addText(summary,"📚  ИЗУЧЕНИЕ",12,PINK,true);addText(summary,completed+" из "+total+" тем завершено",22,DARK,true);
        addProgressLine(summary,"Общий прогресс",total==0?0:completed*100/total);Set<String> days=prefs.getStringSet("study_days",new TreeSet<String>());TextView studyDays=text("Учебных дней: 0",14,MUTED,false);summary.addView(studyDays);animateNumber(studyDays,days.size()," учебных дней");
        TextView totalTime=text("Всего времени: 0 мин",14,MUTED,false);summary.addView(totalTime);animateNumber(totalTime,totalStudyMinutes()," мин всего");addText(summary,"Завершённых занятий: "+(prefs.getBoolean("exam_topic_filter_enabled",false)?completed:prefs.getInt("done_count",0)),14,MUTED,false);root.addView(summary,lp(-1,-2,0,0,0,12));
        int tests=0,correctAnswers=0,totalAnswers=0;List<String> testKeys=new ArrayList<>();for(String key:prefs.getAll().keySet())if(key.startsWith("test_result_"))testKeys.add(key);java.util.Collections.sort(testKeys,java.util.Collections.reverseOrder());for(String key:testKeys){try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(key,"{}"));tests++;correctAnswers+=o.optInt("score",0);totalAnswers+=o.optInt("total",0);}catch(Exception ignored){}}
        LinearLayout testStats=card();addText(testStats,"📝  РЕЗУЛЬТАТЫ МИНИ-ТЕСТОВ",11,PINK,true);addText(testStats,"Пройдено: "+tests+" · Верных ответов: "+correctAnswers+" из "+totalAnswers,14,DARK,true);addProgressLine(testStats,"Точность ответов",totalAnswers==0?0:correctAnswers*100/totalAnswers);for(int i=0;i<Math.min(3,testKeys.size());i++){try{org.json.JSONObject o=new org.json.JSONObject(prefs.getString(testKeys.get(i),"{}"));addText(testStats,o.optString("topic","Тест")+" · "+o.optInt("score",0)+"/"+o.optInt("total",0),11,MUTED,false);}catch(Exception ignored){}}root.addView(testStats,lp(-1,-2,0,0,0,12));
        renderStudyHeatmapCalendar();
        Calendar c=Calendar.getInstance(AppTime.zone());LinearLayout week=card();addText(week,"ПОСЛЕДНИЕ 7 ДНЕЙ",12,PINK,true);
        for(int i=6;i>=0;i--){Calendar day=(Calendar)c.clone();day.add(Calendar.DAY_OF_YEAR,-i);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(day.getTime());int mins=prefs.getInt("focus_minutes_"+date,0);addProgressLine(week,new SimpleDateFormat("EEE, d MMM",new Locale("ru")).format(day.getTime())+" · "+mins+" мин",Math.min(100,mins*2));}
        root.addView(week,lp(-1,-2,0,0,0,12));
        int goal=prefs.getInt("weekly_goal_minutes",300),doneMinutes=weeklyMinutesLast7Days();LinearLayout target=card();addText(target,"🎯  НЕДЕЛЬНАЯ ЦЕЛЬ",11,PINK,true);addText(target,doneMinutes+" из "+goal+" минут",20,DARK,true);addProgressLine(target,"Выполнение цели",goal<=0?0:Math.min(100,doneMinutes*100/goal));root.addView(target,lp(-1,-2,0,0,0,10));Button image=button("🖼  Итоги недели картинкой",true);image.setOnClickListener(v->exportWeeklySummaryImage());root.addView(image,lp(-1,48,0,0,0,7));Button pdf=button("📄  Создать PDF-отчёт",false);pdf.setOnClickListener(v->exportProgressPdf());root.addView(pdf,lp(-1,48,0,0,0,7));backHome();
    }

    private String levelName(int level){if(level<3)return "Начинающий";if(level<6)return "Ученик";if(level<10)return "Знаток";if(level<15)return "Мастер";return "Легенда знаний";}
    private void renderStudyHeatmapCalendar(){
        Calendar now=Calendar.getInstance(AppTime.zone());LinearLayout box=card();addText(box,"КАЛЕНДАРЬ АКТИВНОСТИ",11,PINK,true);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView monthName=text(new SimpleDateFormat("LLLL yyyy",new Locale("ru")).format(now.getTime()),15,DARK,true);header.addView(monthName,new LinearLayout.LayoutParams(0,-2,1f));
        TextView active=text(prefs.getStringSet("study_days",new TreeSet<String>()).size()+" дней всего",10,MUTED,false);header.addView(active);box.addView(header,lp(-1,30,0,2,0,4));
        String[] weekdays={"ПН","ВТ","СР","ЧТ","ПТ","СБ","ВС"};android.widget.GridLayout grid=new android.widget.GridLayout(this);grid.setColumnCount(7);
        for(String day:weekdays){TextView label=text(day,9,MUTED,true);label.setGravity(Gravity.CENTER);android.widget.GridLayout.LayoutParams p=new android.widget.GridLayout.LayoutParams();p.width=0;p.height=dp(22);p.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);grid.addView(label,p);}
        Calendar first=(Calendar)now.clone();first.set(Calendar.DAY_OF_MONTH,1);int offset=(first.get(Calendar.DAY_OF_WEEK)+5)%7,count=now.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i=0;i<offset;i++){View blank=new View(this);android.widget.GridLayout.LayoutParams p=new android.widget.GridLayout.LayoutParams();p.width=0;p.height=dp(29);p.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);grid.addView(blank,p);}
        Set<String> days=prefs.getStringSet("study_days",new TreeSet<String>());
        for(int day=1;day<=count;day++){Calendar date=(Calendar)first.clone();date.set(Calendar.DAY_OF_MONTH,day);String key=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.getTime());int mins=prefs.getInt("focus_minutes_"+key,0);boolean studied=days.contains(key)||mins>0;
            TextView cell=text(String.valueOf(day),10,studied?0xFFFFFFFF:MUTED,true);cell.setGravity(Gravity.CENTER);cell.setBackground(shape(studied?PINK:PALE,8,0));android.widget.GridLayout.LayoutParams p=new android.widget.GridLayout.LayoutParams();p.width=0;p.height=dp(29);p.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);p.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(cell,p);cell.setOnClickListener(v->showAppMessage(new SimpleDateFormat("d MMMM",new Locale("ru")).format(date.getTime())+": "+mins+" мин учёбы"));}
        box.addView(grid,lp(-1,-2,0,3,0,0));root.addView(box,lp(-1,-2,0,0,0,10));
    }
    private void showFocusMode(){
        currentScreen="focus_mode";base("Режим концентрации","Подготовь спокойное место и начни короткий фокус-сеанс");
        LinearLayout hero=card();hero.setGravity(Gravity.CENTER);hero.setPadding(dp(18),dp(22),dp(18),dp(22));
        TextView icon=text("◉",48,PINK,true);icon.setGravity(Gravity.CENTER);hero.addView(icon,lp(-1,66,0,0,0,5));
        addText(hero,"ТВОЁ ВРЕМЯ ДЛЯ СЕБЯ",11,PINK,true);addText(hero,"Один экран. Одна задача. Один шаг.",18,DARK,true);
        addText(hero,"Включи таймер, убери лишние вкладки и выбери одну понятную цель на занятие.",12,MUTED,false);root.addView(hero,lp(-1,-2,0,0,0,10));
        String[] tips={"Отключи лишние уведомления на время занятия","Положи телефон экраном вниз, если он отвлекает","Открой только материалы по выбранному предмету","После занятия сделай короткий перерыв"};
        LinearLayout tipsCard=card();addText(tipsCard,"НЕБОЛЬШАЯ ПОДГОТОВКА",11,PINK,true);
        for(int i=0;i<tips.length;i++){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView num=text((i+1)+".",14,PINK,true);row.addView(num,lp(28,34,0,0,5,0));TextView label=text(tips[i],12,DARK,false);row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));tipsCard.addView(row,lp(-1,-2,0,4,0,4));}
        root.addView(tipsCard,lp(-1,-2,0,0,0,10));
        addText(root,"Автоматическая блокировка других приложений пока не включена: для неё нужны отдельные системные разрешения. Этот режим не ограничивает доступ к телефону.",10,MUTED,false);
        Button start=button("▶  Начать 25 минут фокуса",true);start.setOnClickListener(v->{selectedStudyMinutes=25;timerIsBreak=false;timerRemainingMs=25*60000L;showPomodoro();showStudyRitual();});root.addView(start,lp(-1,48,0,8,0,8));
        Button stats=button("Открыть календарь и статистику",false);stats.setOnClickListener(v->showDetailedStats());root.addView(stats,lp(-1,44,0,0,0,4));backHome();
    }
    private void startAmbience() {
        stopAmbience();
        String mode=prefs.getString("study_atmosphere","🌧️  Дождь за окном");
        ambienceAudioManager=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);
        if(Build.VERSION.SDK_INT>=26){android.media.AudioAttributes attrs=new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build();android.media.AudioFocusRequest request=new android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener(ambienceFocusListener).setWillPauseWhenDucked(true).build();ambienceFocusRequest=request;if(ambienceAudioManager.requestAudioFocus(request)!=android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED){showAppMessage("Звук сейчас недоступен: занят аудиофокус");return;}}else if(ambienceAudioManager.requestAudioFocus(ambienceFocusListener,android.media.AudioManager.STREAM_MUSIC,android.media.AudioManager.AUDIOFOCUS_GAIN)!=android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return;
        ambienceLoading=true;ambiencePlaying=false;
        String url=mode.contains("Дождь")?"https://upload.wikimedia.org/wikipedia/commons/3/3d/Rain.ogg":
                mode.contains("кафе")?"https://upload.wikimedia.org/wikipedia/commons/5/54/Cafe_ambiance.ogg":
                mode.contains("природы")?"https://upload.wikimedia.org/wikipedia/commons/3/38/Birds_forest.ogg":
                "https://upload.wikimedia.org/wikipedia/commons/f/fb/20121112_TU_Delft_Library%2C_quiet_study_room_-_general_ambience_-_SoundCloud_-_el_mar.ogg";
        try{
            android.media.MediaPlayer player=new android.media.MediaPlayer();ambiencePlayer=player;
            float volume=prefs.getInt("ambience_volume",35)/100f;player.setVolume(volume,volume);player.setLooping(true);if(Build.VERSION.SDK_INT>=21)player.setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnPreparedListener(mp->{if(ambiencePlayer==mp){ambienceLoading=false;mp.start();ambiencePlaying=true;int auto=prefs.getInt("ambience_auto_stop",0);if(auto>0){if(ambienceAutoStopTick!=null)settingsHandler.removeCallbacks(ambienceAutoStopTick);ambienceAutoStopTick=()->{if(ambiencePlaying){stopAmbience();if("atmosphere".equals(currentScreen))showAtmosphere();showAppMessage("Звук атмосферы автоматически остановлен");}};settingsHandler.postDelayed(ambienceAutoStopTick,auto*60000L);}}});
            player.setOnErrorListener((mp,what,extra)->{if(ambiencePlayer==mp)stopAmbience();runOnUiThread(()->showAppMessage("Не удалось загрузить звук. Проверь интернет."));return true;});
            java.util.Map<String,String> headers=new java.util.HashMap<>();headers.put("User-Agent","StudyWithLove/4.1 (Android; ambience player)");player.setDataSource(this,android.net.Uri.parse(url),headers);player.prepareAsync();
        }catch(Exception e){stopAmbience();showAppMessage("Не удалось загрузить звук атмосферы");}
    }
    private void stopAmbience(){if(ambienceAutoStopTick!=null){settingsHandler.removeCallbacks(ambienceAutoStopTick);ambienceAutoStopTick=null;}ambiencePlaying=false;ambienceLoading=false;android.media.MediaPlayer player=ambiencePlayer;ambiencePlayer=null;if(player!=null){try{if(player.isPlaying())player.stop();}catch(Exception ignored){}try{player.release();}catch(Exception ignored){}}if(ambienceAudioManager!=null){try{if(Build.VERSION.SDK_INT>=26&&ambienceFocusRequest!=null)ambienceAudioManager.abandonAudioFocusRequest((android.media.AudioFocusRequest)ambienceFocusRequest);else ambienceAudioManager.abandonAudioFocus(ambienceFocusListener);}catch(Exception ignored){}}}
    @Override protected void onDestroy(){if(flowerBreatheAnimator!=null){flowerBreatheAnimator.cancel();flowerBreatheAnimator=null;}if(timerPulseAnimator!=null){timerPulseAnimator.cancel();timerPulseAnimator=null;}stopAmbience();super.onDestroy();}

    private void showAtmosphere() {
        currentScreen = "atmosphere";
        if(prefs.getBoolean("ambience_autostart",false)&&!ambiencePlaying&&!ambienceLoading)startAmbience();
        base("Атмосфера учёбы", "Нажми на звук, чтобы включить или остановить");
        String[] names={"🌧️  Дождь за окном","☕  Уютное кафе","🌿  Звуки природы","📚  Тихая библиотека"};
        String[] descriptions={"Мягкий дождливый настрой","Тёплая атмосфера кофейни","Спокойствие и свежесть","Минимум отвлечений"};
        String chosen=prefs.getString("study_atmosphere",names[0]);
        for(int i=0;i<names.length;i++){
            final String n=names[i];
            final boolean selected=chosen.equals(n);
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4),dp(9),dp(4),dp(9));
            row.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            LinearLayout words=new LinearLayout(this);
            words.setOrientation(LinearLayout.VERTICAL);
            TextView title=text(n,18,selected?PINK:DARK,selected);
            words.addView(title,lp(-1,-2,0,0,0,3));
            words.addView(text(descriptions[i],12,MUTED,false),lp(-1,-2,0,0,0,0));
            row.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
            TextView state=text(selected&&(ambiencePlaying||ambienceLoading)?"Ⅱ":"▶",21,selected?PINK:MUTED,true);
            state.setGravity(Gravity.CENTER);state.setContentDescription(selected&&(ambiencePlaying||ambienceLoading)?"Остановить звук":"Включить звук");
            row.addView(state,lp(36,36,4,0,0,0));
            row.setOnClickListener(v->{
                boolean same=prefs.getString("study_atmosphere",names[0]).equals(n);
                if(same&&ambiencePlaying){stopAmbience();}
                else{
                    if(ambiencePlaying||ambienceLoading)stopAmbience();
                    prefs.edit().putString("study_atmosphere",n).apply();
                    startAmbience();
                }
                showAtmosphere();
            });
            root.addView(row,lp(-1,-2,0,0,0,2));
        }
        android.widget.SeekBar volume=new android.widget.SeekBar(this);
        volume.setMax(100);
        volume.setProgress(prefs.getInt("ambience_volume",35));
        if(Build.VERSION.SDK_INT>=21)volume.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
        addText(root,"ГРОМКОСТЬ",11,PINK,true);
        root.addView(volume,lp(-1,34,0,4,0,4));
        volume.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(android.widget.SeekBar b,int value,boolean user){if(user){prefs.edit().putInt("ambience_volume",value).apply();if(ambiencePlayer!=null)ambiencePlayer.setVolume(value/100f,value/100f);}}
            public void onStartTrackingTouch(android.widget.SeekBar b){}
            public void onStopTrackingTouch(android.widget.SeekBar b){}
        });
        backHome();
    }
    private void showAudioCredits(){new android.app.AlertDialog.Builder(this).setTitle("Источники звуков").setMessage("Дождь — ジダネ, Wikimedia Commons, общественное достояние.\n\nКафе — Marble Toast, Wikimedia Commons, CC0 1.0.\n\nПрирода — Barracuda1983, Wikimedia Commons, общественное достояние.\n\nТихая библиотека — “Quiet study room ambience”, el_mar, Wikimedia Commons, CC BY 3.0. Запись используется без изменений.\n\nПодробности: AUDIO_CREDITS.md в репозитории проекта.").setPositiveButton("Понятно",null).show();}

    private int countStudyDaysThisMonth(Set<String> days, int year, int month) {
        int count = 0;
        String prefix = String.format(Locale.US, "%04d-%02d-", year, month + 1);
        for (String day : days) if (day.startsWith(prefix)) count++;
        return count;
    }

    private void handleBackNavigation() {
        if ("exam_topics".equals(currentScreen)) { showExam(); return; }
        if ("chapters".equals(currentScreen) && lessonsSearchOpen) {
            lessonsSearchOpen=false;
            android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if(imm!=null)imm.hideSoftInputFromWindow(root.getWindowToken(),0);
            showChapters();
            return;
        }
        if ("exam_goal_form".equals(currentScreen)) { showExam(); return; }
        if (!screenHistory.isEmpty()) {
            String target = screenHistory.remove(screenHistory.size() - 1);
            navigatingBack = true;
            if (currentScreen.startsWith("math_quiz")) { quizActive = false; stopQuizTimer(); }
            showScreenByKey(target);
            return;
        }
        if (!"home".equals(currentScreen)) { showHome(); return; }
        long now=SystemClock.elapsedRealtime();
        if(now-lastBackPressedAt<2000L){finish();return;}
        lastBackPressedAt=now;
        showAppMessage("Нажми ещё раз, чтобы выйти");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() { handleBackNavigation(); }

    private void showScreenByKey(String key) {
        switch (key) {
            case "home": showHome(); break;
            case "chapters": showChapters(); break;
            case "math_chapters": showMathChapters(); break;
            case "topics": if(selectedChapter!=null)showTopics(selectedChapter);else showChapters(); break;
            case "topic": if(selectedTopic!=null)showTopic(selectedTopic);else if(selectedChapter!=null)showTopics(selectedChapter);else showChapters(); break;
            case "settings": currentScreen="settings"; showSettings(); break;
            case "settings_appearance": case "settings_reminders": case "settings_goals": case "settings_data": case "settings_about": currentScreen=key; showSettings(); break;
            case "progress": showProgress(); break;
            case "pomodoro": showPomodoro(); break;
            case "focus_mode": showFocusMode(); break;
            case "exam": showExam(); break;
            case "diary": showDiary(); break;
            case "flower": showFlower(); break;
            case "garden": showKnowledgeGarden(); break;
            case "postcards": showPostcards(); break;
            case "achievements": showAchievements(); break;
            case "letters": showFutureLetters(); break;
            case "planner": showPlanner(); break;
            case "stats": showDetailedStats(); break;
            case "atmosphere": showAtmosphere(); break;
            case "reminders": showReminderSettings(); break;
            case "message": showMessage(); break;
            case "math_quiz": showMathQuiz(); break;
            case "math_quiz_result": showMathQuiz(); break;
            case "math_quiz_awards": showMathQuizAchievements(); break;
            default: showHome(); break;
        }
    }

    private int countCompleted(List<String> topics){return countCompleted(selectedChapter,topics);}
    private int countCompleted(String chapter,List<String> topics){int count=0;for(String topic:topics)if(topicProgressForChapter(chapter,topic)==100)count++;return count;}

    private void addProgressLine(LinearLayout parent, String label, int percent) {
        boolean sliderMode=prefs!=null&&"slider".equals(prefs.getString("progress_display","percent"));
        TextView caption = text(sliderMode?label:label+"  ·  "+Math.max(0,Math.min(100,percent))+"%", 12, MUTED, false);
        parent.addView(caption, lp(-1, -2, 0, 5, 0, 3));
        if(!sliderMode)return;
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(Math.max(0, Math.min(100, percent)));
        if (Build.VERSION.SDK_INT >= 21) {
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
            bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFE2EA));
        }
        parent.addView(bar, lp(-1, 8, 0, 0, 0, 5));
    }

    private boolean isYesterday(String date) {
        if (date == null || date.isEmpty()) return false;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(AppTime.zone());
            java.util.Date d = f.parse(date);
            Calendar c = Calendar.getInstance(AppTime.zone());
            c.add(Calendar.DAY_OF_YEAR, -1);
            return d != null && f.format(c.getTime()).equals(f.format(d));
        } catch (Exception e) { return false; }
    }

    private int currentStreak() {
        String last = prefs.getString("last_study_date", "");
        if (todayKey().equals(last) || isYesterday(last) || (isOneDayGap(last)&&!weekKey().equals(prefs.getString("streak_freeze_week","")))) return prefs.getInt("current_streak", 0);
        return 0;
    }

    private boolean isOneDayGap(String date){try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(AppTime.zone());Date d=f.parse(date);Calendar c=Calendar.getInstance(AppTime.zone());c.setTime(d);c.add(Calendar.DAY_OF_YEAR,2);return todayKey().equals(f.format(c.getTime()));}catch(Exception e){return false;}}
    private String weekKey(){Calendar c=Calendar.getInstance(AppTime.zone());c.setFirstDayOfWeek(Calendar.MONDAY);c.setMinimalDaysInFirstWeek(4);int week=c.get(Calendar.WEEK_OF_YEAR),year=c.get(Calendar.YEAR),month=c.get(Calendar.MONTH);if(week==1&&month==Calendar.DECEMBER)year++;else if(week>=52&&month==Calendar.JANUARY)year--;return year+"-"+week;}
    private String dayWord(int n) {
        int x = Math.abs(n) % 10, y = Math.abs(n) % 100;
        if (x == 1 && y != 11) return "день";
        if (x >= 2 && x <= 4 && (y < 12 || y > 14)) return "дня";
        return "дней";
    }

    private Set<String> selectedDays() {
        Set<String> saved = prefs.getStringSet("reminder_days", null);
        if (saved != null) return new TreeSet<>(saved);
        Set<String> result = new TreeSet<>();
        for (int d = 1; d <= 7; d++) if (prefs.getBoolean("day_" + d, true)) result.add(String.valueOf(d));
        return result;
    }

    private List<String> getSavedTimes() {
        Set<String> saved = prefs.getStringSet("reminder_times", null);
        Set<String> values = saved == null ? new TreeSet<>(java.util.Arrays.asList("10:00", "15:00", "19:00", "21:00")) : new TreeSet<>(saved);
        return new ArrayList<>(values);
    }

    private String reminderSummary() {
        Set<String> days = selectedDays();
        List<String> times = getSavedTimes();
        if (times.isEmpty() || days.isEmpty()) return "Напоминания выключены. Открой настройки, чтобы включить.";
        List<String> labels = new ArrayList<>();
        for (String day : days) { int index = Integer.parseInt(day) - 1; labels.add(new String[]{"Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"}[index]); }
        return android.text.TextUtils.join(", ", labels) + "  ·  " + android.text.TextUtils.join(", ", times) + " (местное время)";
    }

    private void showReminderSettings() {
        currentScreen = "reminders";
        base("Напоминания 🔔", "Выбери дни и время. Часовой пояс устройства.");
        addText(root, "ДНИ НЕДЕЛИ", 12, PINK, true);
        dayChecks.clear();
        Set<String> savedDays = selectedDays();
        LinearLayout daysRow = new LinearLayout(this); daysRow.setOrientation(LinearLayout.HORIZONTAL); daysRow.setGravity(Gravity.CENTER);
        String[] dayShort = {"Пн","Вт","Ср","Чт","Пт","Сб","Вс"};
        int[] calendarDays = {2,3,4,5,6,7,1};
        for (int d = 0; d < 7; d++) {
            CheckBox cb = new CheckBox(this); cb.setButtonTintList(android.content.res.ColorStateList.valueOf(PINK)); cb.setText(dayShort[d]); cb.setSingleLine(true); cb.setMinWidth(0); cb.setTextSize(9); cb.setTypeface(Typeface.DEFAULT,Typeface.BOLD); cb.setTextColor(DARK); cb.setGravity(Gravity.CENTER); cb.setPadding(dp(2),0,dp(2),0); cb.setBackground(shape(PALE,14,0xFFF0DCE4));
            cb.setChecked(savedDays.contains(String.valueOf(calendarDays[d])));
            cb.setBackground(shape(cb.isChecked()?0xFFF7DCE6:PALE,14,cb.isChecked()?PINK:0xFFF0DCE4));
            cb.setOnCheckedChangeListener((button,checked)->cb.setBackground(shape(checked?0xFFF7DCE6:PALE,14,checked?PINK:0xFFF0DCE4)));
            dayChecks.add(cb); LinearLayout.LayoutParams dayLp=new LinearLayout.LayoutParams(0,dp(36),1f);dayLp.setMargins(dp(2),0,dp(2),0);daysRow.addView(cb,dayLp);
        }
        LinearLayout daysCard = card(); daysCard.addView(daysRow, lp(-1, 38, 0, 0, 0, 0)); root.addView(daysCard, lp(-1, -2, 0, 2, 0, 6));
        LinearLayout presets=new LinearLayout(this);presets.setGravity(Gravity.CENTER_VERTICAL);
        String[] presetNames={"Каждый день","Будни","Выходные"};
        for(String preset:presetNames){TextView chip=text(preset,10,PINK,true);chip.setGravity(Gravity.CENTER);chip.setPadding(dp(10),dp(7),dp(10),dp(7));chip.setBackground(shape(0xFFFFF0F4,15,0));presets.addView(chip,lp(-2,48,0,0,5,0));chip.setOnClickListener(v->{for(int k=0;k<dayChecks.size();k++){boolean checked=preset.equals("Каждый день")||(preset.equals("Будни")&&k<5)||(preset.equals("Выходные")&&k>=5);dayChecks.get(k).setChecked(checked);}});}
        root.addView(presets,lp(-1,36,0,0,0,8));
        addText(root, "ВРЕМЯ НАПОМИНАНИЙ", 12, PINK, true);
        reminderTimes.clear(); reminderTimes.addAll(getSavedTimes());
        LinearLayout timeList = new LinearLayout(this); timeList.setOrientation(LinearLayout.VERTICAL); root.addView(timeList, lp(-1, -2, 0, 4, 0, 8));
        final Runnable[] renderTimes = new Runnable[1];
        renderTimes[0] = () -> {
            timeList.removeAllViews();
            if (reminderTimes.isEmpty()) addText(timeList, "Время не выбрано — напоминания выключены", 14, MUTED, false);
            for (String time : new ArrayList<>(reminderTimes)) {
                LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setBackground(shape(PALE, 16, 0)); row.setPadding(dp(12),0,dp(6),0);
                LinearLayout timeInfo=new LinearLayout(this);timeInfo.setOrientation(LinearLayout.VERTICAL);timeInfo.setGravity(Gravity.CENTER_VERTICAL);
                TextView label = text("◷  " + time, 16, DARK, true);timeInfo.addView(label,lp(-1,24,0,0,0,0)); label.setOnClickListener(v->editReminderTime(time,renderTimes[0]));
                String slot=time.startsWith("10:")?"Утро":time.startsWith("15:")?"День":time.startsWith("19:")?"Вечер":time.startsWith("21:")?"Перед сном":"Напоминание";
                TextView sub=text(slot,10,MUTED,false);timeInfo.addView(sub,lp(-1,16,0,0,0,0));row.addView(timeInfo,new LinearLayout.LayoutParams(0,dp(46),1));
                Button edit = button("✎", false); edit.setContentDescription(translate("Изменить время напоминания")); edit.setTextSize(14); edit.setMinWidth(dp(48)); edit.setPadding(0,0,0,0); edit.setOnClickListener(v -> editReminderTime(time,renderTimes[0]));
                Button remove = button("×", false); remove.setContentDescription(translate("Удалить время напоминания")); remove.setTextSize(16); remove.setMinWidth(dp(48)); remove.setPadding(0,0,0,0); remove.setOnClickListener(v -> { reminderTimes.remove(time); renderTimes[0].run(); });
                row.addView(edit,lp(48,48,2,0,2,0)); row.addView(remove,lp(48,48,2,0,0,0)); timeList.addView(row, lp(-1, 56, 0, 3, 0, 3));
            }
        };
        renderTimes[0].run();
        Button addTime = button("＋  Добавить время", false);
        addTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance(AppTime.zone());
            new TimePickerDialog(this, (TimePicker picker, int hour, int minute) -> {
                String value = String.format(Locale.US, "%02d:%02d", hour, minute);
                if (!reminderTimes.contains(value)) { reminderTimes.add(value); java.util.Collections.sort(reminderTimes); renderTimes[0].run(); }
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        });
        root.addView(addTime, lp(-1, 48, 0, 4, 0, 14));
        TextView summary=text(selectedDays().size()+" дней · "+reminderTimes.size()+" напоминания в день",11,MUTED,true);summary.setGravity(Gravity.CENTER);root.addView(summary,lp(-1,28,0,2,0,2));
        Runnable updateReminderSummary=()->{int count=0;for(CheckBox cb:dayChecks)if(cb.isChecked())count++;summary.setText(count+" "+dayWord(count)+" · "+reminderTimes.size()+" напоминаний в день");};
        for(CheckBox cb:dayChecks)cb.setOnCheckedChangeListener((button,checked)->{cb.setBackground(shape(checked?0xFFF7DCE6:PALE,14,checked?PINK:0xFFF0DCE4));updateReminderSummary.run();});
        Button save = button("💗  Сохранить расписание", true);
        save.setOnClickListener(v -> {
            Set<String> days = new TreeSet<>();
            for (int i = 0; i < dayChecks.size(); i++) if (dayChecks.get(i).isChecked()) days.add(String.valueOf(i == 6 ? 1 : i + 2));
            Set<String> times = new TreeSet<>(reminderTimes);
            SharedPreferences.Editor e = prefs.edit().putStringSet("reminder_days", days).putStringSet("reminder_times", times);
            for (int d = 1; d <= 7; d++) e.putBoolean("day_" + d, days.contains(String.valueOf(d)));
            e.apply(); scheduleReminders(this);
            if (!days.isEmpty() && !times.isEmpty() && Build.VERSION.SDK_INT >= 31) {
                AlarmManager alarms=(AlarmManager)getSystemService(ALARM_SERVICE);
                if (!alarms.canScheduleExactAlarms()) {
                    try { startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:"+getPackageName()))); }
                    catch (Exception ignored) { }
                }
            }
            showAppMessage(days.isEmpty() || times.isEmpty() ? "Расписание очищено. Напоминания выключены." : "Готово! Расписание сохранено по местному времени. Разреши точные будильники и уведомления, чтобы получать напоминания без системной задержки.");
            showHome();
        });
        root.addView(save, lp(-1, 54, 0, 4, 0, 8));

    }
    private void editReminderTime(String oldTime,Runnable refresh){
        try{String[] p=oldTime.split(":");int h=Integer.parseInt(p[0]),m=Integer.parseInt(p[1]);
            new TimePickerDialog(this,(picker,hour,minute)->{String value=String.format(Locale.US,"%02d:%02d",hour,minute);
                if(!oldTime.equals(value)&&reminderTimes.contains(value)){showAppMessage("Такое время уже добавлено");return;}
                int index=reminderTimes.indexOf(oldTime);if(index>=0){reminderTimes.set(index,value);java.util.Collections.sort(reminderTimes);refresh.run();}
            },h,m,true).show();
        }catch(Exception ignored){showAppMessage("Не удалось изменить время");}
    }

    private void backHome() { /* Нижние кнопки возврата убраны по запросу. */ }
    private void ritualTile(LinearLayout row, String icon, String title, String subtitle, OnAction run) {
            LinearLayout tile = card();
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_VERTICAL);
            tile.setPadding(dp(9), dp(8), dp(8), dp(7));
            TextView symbol = text(icon, 24, PINK, true);
            symbol.setGravity(Gravity.CENTER);
            symbol.setIncludeFontPadding(false);
            symbol.setBackground(shape(PALE, 15, 0));
            tile.addView(symbol, lp(32, 32, 0, 0, 0, 5));
            TextView heading = text(title, 15, DARK, true);
            heading.setMaxLines(2);
            tile.addView(heading, lp(-1, -2, 0, 0, 0, 4));
            TextView detail = text(subtitle, 12, MUTED, false);
            detail.setMaxLines(2);
            tile.addView(detail, lp(-1, -2, 0, 0, 0, 0));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(106), 1f);
            p.setMargins(dp(3), 0, dp(3), 0);
            row.addView(tile, p);
            tile.setContentDescription(title + ". " + subtitle);
            tile.setFocusable(true);
            tile.setOnClickListener(v -> {
                symbol.animate().scaleX(1.08f).scaleY(1.08f).setDuration(140)
                    .withEndAction(() -> symbol.animate().scaleX(1f).scaleY(1f).setDuration(140).start()).start();
                tile.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.88f).setDuration(110)
                    .withEndAction(() -> { tile.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).start(); run.go(); }).start();
            });
        }

    private void animateLessonTap(View view,Runnable action) {
        if(prefs!=null&&prefs.getBoolean("reduce_motion",false)){action.run();return;}
        view.animate().cancel();
        view.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        view.animate().scaleX(0.965f).scaleY(0.965f).alpha(0.72f).setDuration(90).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(()->
            view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(150).setInterpolator(new android.view.animation.OvershootInterpolator(1.15f)).withEndAction(()->{view.setLayerType(View.LAYER_TYPE_NONE,null);action.run();}).start()
        ).start();
    }
    private void actionRegular(String title, String sub, OnAction run) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.HORIZONTAL); c.setGravity(Gravity.CENTER_VERTICAL); c.setPadding(dp(16), dp(14), dp(14), dp(14));
        c.setBackground(shape(0xFFFFFFFF, 24, 0xFFF3E9EC));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL);
        TextView heading = text(title, 17, DARK, false);
        if (title.startsWith("РАЗДЕЛ ")) {
            android.text.SpannableString styled = new android.text.SpannableString(title);
            styled.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), 0, 6, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            heading.setText(styled);
        }
        words.addView(heading, lp(-1, -2, 0, 0, 0, 4));
        TextView detail = text(sub, 13, MUTED, false); words.addView(detail, lp(-1, -2, 0, 0, 0, 0));
        c.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = text("›", 28, PINK, false); arrow.setGravity(Gravity.CENTER); c.addView(arrow, lp(24, 34, 8, 0, 0, 0));
        c.setOnClickListener(v -> animateLessonTap(c,run::go));
        root.addView(c, lp(-1, -2, 0, 0, 0, 9));
    }

    private LinearLayout action(String title, String sub, OnAction run) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.HORIZONTAL); c.setGravity(Gravity.CENTER_VERTICAL); c.setPadding(dp(16), dp(14), dp(14), dp(14));
        c.setBackground(shape(0xFFFFFFFF, 24, 0xFFF3E9EC)); c.setElevation(dp(2));
        LinearLayout words = new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 17, DARK, true); heading.setMaxLines(2); words.addView(heading, lp(-1, -2, 0, 0, 0, 4));
        TextView detail = text(sub, 13, MUTED, false); detail.setMaxLines(2); words.addView(detail, lp(-1, -2, 0, 0, 0, 0));
        c.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = text("›", 28, PINK, false); arrow.setGravity(Gravity.CENTER); c.addView(arrow, lp(24, 34, 8, 0, 0, 0));
        c.setOnClickListener(v -> { c.animate().scaleX(0.985f).scaleY(0.985f).setDuration(100).withEndAction(() -> { c.animate().scaleX(1f).scaleY(1f).setDuration(150).start(); run.go(); }).start(); });
        c.setContentDescription(title + ". " + sub);
        c.setFocusable(true);
        c.setMinimumHeight(dp(48));
        root.addView(c, lp(-1, -2, 0, 0, 0, 9));
        return c;
    }
    private String selectedFontFamily(){
        String key=prefs==null?"sans":prefs.getString("app_font","sans");
        if("rounded".equals(key))return "sans-serif-rounded";
        if("serif".equals(key))return "serif";
        if("mono".equals(key))return "monospace";
        if("condensed".equals(key))return "sans-serif-condensed";
        if("medium".equals(key))return "sans-serif-medium";
        return "sans-serif";
    }
    private void installThemeFontListener(android.view.ViewGroup group){
        group.setOnHierarchyChangeListener(new android.view.ViewGroup.OnHierarchyChangeListener(){
            public void onChildViewAdded(android.view.View parent,android.view.View child){applyThemeFont(child);}
            public void onChildViewRemoved(android.view.View parent,android.view.View child){}
        });
        for(int i=0;i<group.getChildCount();i++)applyThemeFont(group.getChildAt(i));
    }
    private void applyThemeFont(View view){
        if(view instanceof TextView){
            TextView t=(TextView)view;boolean bold=t.getTypeface()!=null&&t.getTypeface().isBold();
            t.setTypeface(Typeface.create(selectedFontFamily(),bold?Typeface.BOLD:Typeface.NORMAL));
            int c=t.getCurrentTextColor();
            if(isThemeAccentColor(c))t.setTextColor(PINK);
            else if(isThemeTextColor(c))t.setTextColor(DARK);
            else if(isThemeMutedColor(c))t.setTextColor(MUTED);
        }
        if(view instanceof android.widget.ProgressBar&&Build.VERSION.SDK_INT>=21){
            android.widget.ProgressBar bar=(android.widget.ProgressBar)view;
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PINK));
            bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(isDarkAppearance()?0xFF49404B:PALE));
            if(view instanceof android.widget.SeekBar)((android.widget.SeekBar)view).setThumbTintList(android.content.res.ColorStateList.valueOf(PINK));
        }
        if(view instanceof CheckBox&&Build.VERSION.SDK_INT>=21&&!("round_checkbox".equals(view.getTag())))((CheckBox)view).setButtonTintList(android.content.res.ColorStateList.valueOf(PINK));
        if(view instanceof android.view.ViewGroup)installThemeFontListener((android.view.ViewGroup)view);
    }
    private boolean isThemeAccentColor(int c){
        return c==0xFFC97891||c==0xFFD96C91||c==0xFF397DB5||c==0xFF245B91||
                c==0xFF8979B8||c==0xFF659B86||c==0xFF5D91B5||c==0xFFD88968||
                c==0xFFC49318||c==0xFF8B6046||c==0xFFD6B36A||c==0xFFB32635||
                c==0xFF76502F||c==0xFFD9C19A||c==0xFFE39AB5;
    }
    private boolean isThemeTextColor(int c){
        return c==0xFF3F3439||c==0xFF3A3036||c==0xFF382F34||c==0xFF49382E||
                c==0xFF303844||c==0xFF34443D||c==0xFF393448||c==0xFF304656||
                c==0xFF513D34||c==0xFF4B2026||c==0xFF3B281C||c==0xFF49340B;
    }
    private boolean isThemeMutedColor(int c){
        return c==0xFF75666D||c==0xFF6F6684||c==0xFF796657||c==0xFF87919E||
                c==0xFF84958C||c==0xFF8E879E||c==0xFF8199A8||c==0xFFA88D80||
                c==0xFF9D777B||c==0xFF60786B||c==0xFF5E788A||c==0xFF856C60||
                c==0xFF81595E||c==0xFF76604A||c==0xFF795B1E;
    }
    private boolean isDarkAppearance(){String t=prefs==null?"pink":prefs.getString("app_theme","pink");return "night".equals(t)||"luxury".equals(t)||("system".equals(t)&&(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES);}
    private LinearLayout card() {
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20),dp(18),dp(20),dp(18));
        String theme=prefs==null?"pink":prefs.getString("app_theme","pink");
        boolean night=isDarkAppearance();
        int surface,border;
        switch(theme){
            case "night": surface=0xFF25212B;border=0xFF403541;break;
            case "luxury": surface=0xFF30251F;border=0xFF78613D;break;
            case "lavender": surface=night?0xFF25212B:0xFFFFFFFF;border=night?0xFF403541:0xFFE5E1EF;break;
            case "mint": surface=night?0xFF252B28:0xFFFEFFFE;border=night?0xFF3C5147:0xFFDCEBE2;break;
            case "white": surface=night?0xFF252A32:0xFFFFFFFF;border=night?0xFF414B58:0xFFDCE1E8;break;
            case "brown": surface=night?0xFF302720:0xFFFFFCF7;border=night?0xFF594535:0xFFE1D0BE;break;
            case "gold": surface=night?0xFF302A1D:0xFFFFFBEA;border=night?0xFF665329:0xFFD9B64B;break;
            case "sky": surface=night?0xFF202B33:0xFFFFFFFF;border=night?0xFF3A5262:0xFFD4E5F0;break;
            case "peach": surface=night?0xFF30251F:0xFFFFFCF9;border=night?0xFF654638:0xFFF0D2C4;break;
            case "red": surface=night?0xFF302124:0xFFFFFCFC;border=night?0xFF5A343A:0xFFECC9CC;break;
            case "lv": surface=night?0xFF30281F:0xFFFFF9EF;border=night?0xFF68553B:0xFFC6A779;break;
            default: surface=night?0xFF30232A:0xFFFFFFFF;border=night?0xFF60404E:0xFFF4DCE5;break;
        }
        android.graphics.drawable.Drawable bg=shape(surface,26,border);
        if(Build.VERSION.SDK_INT>=21)l.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22000000|(PINK&0x00FFFFFF)),bg,shape(surface,24,0)));
        else l.setBackground(bg);
        l.setElevation(dp(0));
        return l;
    }
    private void addText(LinearLayout parent, String value, int size, int color, boolean bold) { TextView t = text(value, size, color, bold); parent.addView(t, lp(-1, -2, 0, 0, 0, 5)); }
    private TextView text(String value, int size, int color, boolean bold) {
        if(isThemeAccentColor(color))color=PINK;
        else if(isThemeTextColor(color))color=DARK;
        else if(isThemeMutedColor(color))color=MUTED;
        value=translate(value); TextView t = new TextView(this); t.setText(value); float scale=prefs==null?1f:prefs.getFloat("app_text_scale",1f); t.setTextSize(Math.max(12, Math.round(size*scale))); t.setTextColor(color); t.setTypeface(Typeface.create(selectedFontFamily(),bold?Typeface.BOLD:Typeface.NORMAL)); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private void applyPressFeedback(View view) {
        if(prefs!=null&&prefs.getBoolean("reduce_motion",false))return;
        view.setOnTouchListener((v,event)->{
            if(event.getAction()==android.view.MotionEvent.ACTION_DOWN){
                if(prefs==null||prefs.getBoolean("haptics_enabled",true))v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                v.animate().scaleX(.97f).scaleY(.97f).setDuration(85).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            }else if(event.getAction()==android.view.MotionEvent.ACTION_UP||event.getAction()==android.view.MotionEvent.ACTION_CANCEL){
                v.animate().scaleX(1f).scaleY(1f).setDuration(145).setInterpolator(new android.view.animation.OvershootInterpolator(1.15f)).start();
            }
            return false;
        });
    }
    private void animateNumber(TextView view,int target,String suffix) {
        if(prefs.getBoolean("reduce_motion",false)){view.setText(Math.max(0,target)+suffix);return;}
        android.animation.ValueAnimator animator=android.animation.ValueAnimator.ofInt(0,Math.max(0,target));
        animator.setDuration(720);animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(a->view.setText(a.getAnimatedValue()+suffix));animator.start();
    }
    private void animateProgress(ProgressBar bar,int target) {
        if(prefs.getBoolean("reduce_motion",false)){bar.setProgress(Math.max(0,target));return;}
        bar.setProgress(0);android.animation.ObjectAnimator.ofInt(bar,"progress",0,Math.max(0,target)).setDuration(850).start();
    }
    private void animateHomeCascade() {
        if(root==null||prefs.getBoolean("reduce_motion",false))return;
        for(int i=0;i<root.getChildCount();i++){View child=root.getChildAt(i);child.setAlpha(0f);child.setTranslationY(dp(12));child.animate().alpha(1f).translationY(0f).setStartDelay(i*40L).setDuration(300).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
    }
    private void haptic(View view, boolean success) {
        if(view!=null && prefs!=null && prefs.getBoolean("haptics_enabled",true)) view.performHapticFeedback(success?android.view.HapticFeedbackConstants.CONFIRM:android.view.HapticFeedbackConstants.REJECT);
    }
    private void celebrate() {
        if(root==null||prefs.getBoolean("reduce_motion",false)||!prefs.getBoolean("celebrations_enabled",true))return;
        final CelebrationView fx=new CelebrationView(this);
        root.getOverlay().add(fx);fx.layout(0,0,root.getWidth(),root.getHeight());
        android.animation.ValueAnimator animator=android.animation.ValueAnimator.ofFloat(0f,1f);
        animator.setDuration(1250);
        animator.addUpdateListener(a->{fx.progress=(float)a.getAnimatedValue();fx.invalidate();});
        animator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){if(root!=null)root.getOverlay().remove(fx);}});
        animator.start();
    }
    private class CelebrationView extends View {
        float progress=0f; final android.graphics.Paint paint=new android.graphics.Paint(3);
        final float[] x=new float[34],y=new float[34],speed=new float[34],size=new float[34],rotation=new float[34];
        final int[] colors={0xFFFF8FB1,0xFFFFC66D,0xFFB7DCA5,0xFFC7A4E8,0xFFFFD9E4};
        CelebrationView(android.content.Context c){super(c);java.util.Random r=new java.util.Random();for(int i=0;i<x.length;i++){x[i]=r.nextFloat();y[i]=-.12f-r.nextFloat()*.7f;speed[i]=.55f+r.nextFloat()*.9f;size[i]=dp(3+r.nextInt(4));rotation[i]=r.nextFloat()*360f;}}
        @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);float w=getWidth(),h=getHeight();for(int i=0;i<x.length;i++){float yy=(y[i]+progress*speed[i]*1.5f)*h;if(yy<0||yy>h)continue;canvas.save();canvas.rotate(rotation[i]+progress*360f,x[i]*w,yy);paint.setColor(colors[i%colors.length]);paint.setStyle(android.graphics.Paint.Style.FILL);canvas.drawOval(x[i]*w-size[i],yy-size[i]*1.5f,x[i]*w+size[i],yy+size[i]*1.5f,paint);canvas.restore();}}
    }
    private TextView textFixed(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(translate(value));t.setTextSize(size);if(isThemeAccentColor(color))color=PINK;else if(isThemeTextColor(color))color=DARK;else if(isThemeMutedColor(color))color=MUTED;t.setTextColor(color);t.setTypeface(Typeface.create(selectedFontFamily(),bold?Typeface.BOLD:Typeface.NORMAL));t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private Button button(String label, boolean primary) {
        label=translate(label);
        Button b = new Button(this);
        float scale=prefs==null?1f:prefs.getFloat("app_text_scale",1f);
        b.setText(label);
        b.setTextSize(13.5f*scale);
        b.setTypeface(Typeface.create(selectedFontFamily(), Typeface.BOLD));
        b.setAllCaps(false);
        b.setLetterSpacing(0.005f);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(dp(42));
        b.setPadding(dp(8), dp(5), dp(8), dp(5));
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setMaxLines(2);
        b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (Build.VERSION.SDK_INT >= 26) {
            float minSize = Math.max(10f, 10.5f*scale);
            float maxSize = Math.max(minSize, 14f*scale);
            b.setAutoSizeTextTypeUniformWithConfiguration(
                    Math.round(minSize), Math.round(maxSize), 1,
                    android.util.TypedValue.COMPLEX_UNIT_SP);
        }
        int accent=PINK;
        int accentDeep=adjustColor(accent,0.78f);
        int outline=blendColors(accent,BG,isDarkAppearance()?0.38f:0.62f);
        b.setElevation(primary ? dp(2) : 0);
        b.setTextColor(primary ? onAccentTextColor() : (isDarkAppearance()?DARK:accentDeep));
        android.graphics.drawable.Drawable bg;
        if(primary){
            GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{adjustColor(accent,0.92f),accentDeep});
            gradient.setCornerRadius(dp(18));
            gradient.setStroke(dp(0),accentDeep);
            bg=gradient;
        }else{
            GradientDrawable inactive=new GradientDrawable();
            inactive.setColor(isDarkAppearance()?blendColors(PALE,BG,0.22f):0xBFFFFFFF);
            inactive.setCornerRadius(dp(18));
            inactive.setStroke(dp(1),isDarkAppearance()?0xFF4B4558:0xFFE7E3F0);
            bg=inactive;
        }
        if(Build.VERSION.SDK_INT>=21){
            GradientDrawable mask=shape(0xFFFFFFFF,14,0);
            b.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf((0x33000000 | (PINK & 0x00FFFFFF))),bg,mask));
        } else b.setBackground(bg);
        applyPressFeedback(b);
        return b;
    }
    private int onAccentTextColor(){return 0xFFFFFFFF;}
    private int onAccentTextColor(int background){return 0xFFFFFFFF;}
    private double contrastRatio(int first,int second){
        double a=relativeLuminance(first),b=relativeLuminance(second);
        return (Math.max(a,b)+0.05)/(Math.min(a,b)+0.05);
    }
    private double relativeLuminance(int color){
        double r=Color.red(color)/255.0,g=Color.green(color)/255.0,b=Color.blue(color)/255.0;
        r=r<=0.04045?r/12.92:Math.pow((r+0.055)/1.055,2.4);
        g=g<=0.04045?g/12.92:Math.pow((g+0.055)/1.055,2.4);
        b=b<=0.04045?b/12.92:Math.pow((b+0.055)/1.055,2.4);
        return 0.2126*r+0.7152*g+0.0722*b;
    }
    private int adjustColor(int color,float factor){
        return Color.rgb(Math.min(255,Math.round(Color.red(color)*factor)),
                Math.min(255,Math.round(Color.green(color)*factor)),
                Math.min(255,Math.round(Color.blue(color)*factor)));
    }
    private int blendColors(int foreground,int background,float backgroundWeight){
        float w=Math.max(0f,Math.min(1f,backgroundWeight));
        return Color.rgb(Math.round(Color.red(foreground)*(1-w)+Color.red(background)*w),
                Math.round(Color.green(foreground)*(1-w)+Color.green(background)*w),
                Math.round(Color.blue(foreground)*(1-w)+Color.blue(background)*w));
    }
    private android.graphics.drawable.Drawable settingsIcon(int type) {
        final int color=PINK;
        return new android.graphics.drawable.Drawable() {
            private final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(android.graphics.Canvas canvas) {
                android.graphics.Rect b=getBounds();
                canvas.save();
                canvas.translate(b.left,b.top);
                float sx=b.width()/24f, sy=b.height()/24f;
                canvas.scale(sx,sy);
                paint.setColor(color);paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(1.8f);paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
                android.graphics.Path path=new android.graphics.Path();
                if(type==0){
                    path.moveTo(12,3.2f);path.cubicTo(6.8f,3.2f,3,7.1f,3,12);
                    path.cubicTo(3,17.2f,6.8f,21,12,21);
                    path.cubicTo(14.1f,21,15.2f,19.4f,14.1f,17.8f);
                    path.cubicTo(13.1f,16.3f,14.2f,14.8f,16,14.8f);
                    path.lineTo(18.1f,14.8f);path.cubicTo(20,14.8f,21,13.5f,21,11.7f);
                    path.cubicTo(21,6.8f,17,3.2f,12,3.2f);canvas.drawPath(path,paint);
                    paint.setStyle(android.graphics.Paint.Style.FILL);
                    canvas.drawCircle(8,9,1.15f,paint);canvas.drawCircle(12,7.2f,1.15f,paint);
                    canvas.drawCircle(16,9,1.15f,paint);
                }else if(type==1){
                    path.moveTo(3,5);path.lineTo(21,5);path.lineTo(21,19);path.lineTo(3,19);path.close();
                    canvas.drawPath(path,paint);path.reset();path.moveTo(7,15);path.lineTo(10,11.5f);path.lineTo(13,14);path.lineTo(15,12);path.lineTo(18,15);canvas.drawPath(path,paint);
                    path.reset();path.moveTo(17,2);path.lineTo(17,8);path.moveTo(14,5);path.lineTo(20,5);canvas.drawPath(path,paint);
                }else{
                    path.moveTo(4,6);path.lineTo(20,6);path.moveTo(9,6);path.lineTo(9,3.5f);path.lineTo(15,3.5f);path.lineTo(15,6);
                    path.moveTo(6.5f,6);path.lineTo(7.5f,20);path.lineTo(16.5f,20);path.lineTo(17.5f,6);
                    path.moveTo(10,9);path.lineTo(10.5f,17);path.moveTo(14,9);path.lineTo(13.5f,17);canvas.drawPath(path,paint);
                }
                canvas.restore();
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(android.graphics.ColorFilter filter) {}
            @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
        };
    }

    private String applyAddressStyle(String value){if(value==null||prefs==null||!"formal".equals(prefs.getString("address_style","you")))return value;String[][] pairs={{"Ты справляешься","Вы справляетесь"},{"ты справляешься","вы справляетесь"},{"Продолжай","Продолжайте"},{"продолжай","продолжайте"},{"Начни","Начните"},{"начни","начните"},{"Выбери","Выберите"},{"выбери","выберите"},{"Нажми","Нажмите"},{"нажми","нажмите"},{"Сохрани","Сохраните"},{"сохрани","сохраните"},{"Попробуй","Попробуйте"},{"попробуй","попробуйте"},{"Открой","Откройте"},{"открой","откройте"},{"Хочешь","Хотите"},{"хочешь","хотите"},{"Можешь","Можете"},{"можешь","можете"},{"Тебе","Вам"},{"тебе","вам"},{"Тебя","Вас"},{"тебя","вас"},{"Твой","Ваш"},{"твой","ваш"},{"Твоя","Ваша"},{"твоя","ваша"},{"Твоё","Ваше"},{"твоё","ваше"},{"Твои","Ваши"},{"твои","ваши"},{"готова","готовы"},{"готов","готовы"},{"рада","рады"},{"позанималась","позанимались"},{"позанимался","позанимались"}};for(String[] pair:pairs)value=value.replace(pair[0],pair[1]);value=value.replaceAll("(?<![\\p{L}])Ты(?![\\p{L}])","Вы").replaceAll("(?<![\\p{L}])ты(?![\\p{L}])","вы");return value;}
    private String translate(String value){
        String lang=prefs==null?"ru":prefs.getString("app_language","ru");if(value==null)return null;if("ru".equals(lang))return applyAddressStyle(value);
        String[][] pairs={
            {"ОБНОВЛЕНИЕ ПРИЛОЖЕНИЯ","APP UPDATES","YENİLƏNMƏLƏR"},
            {"Установленная версия: ","Installed version: ","Quraşdırılmış versiya: "},
            {"Проверяй наличие новых возможностей и исправлений.","Check for new features and fixes.","Yeni funksiyaları və düzəlişləri yoxla."},
            {"Проверить обновления","Check for updates","Yeniləmələri yoxla"},
            {"Сортировка предметов","Sort subjects","Fənləri çeşidlə"},
            {"Предыдущий месяц","Previous month","Əvvəlki ay"},
            {"Следующий месяц","Next month","Növbəti ay"},
            {"Уменьшить время учёбы","Decrease study time","Dərs vaxtını azalt"},
            {"Увеличить время учёбы","Increase study time","Dərs vaxtını artır"},
            {"Уменьшить время перерыва","Decrease break time","Fasilə vaxtını azalt"},
            {"Увеличить время перерыва","Increase break time","Fasilə vaxtını artır"},
            {"Очистить поиск","Clear search","Axtarışı təmizlə"},
            {"Добавить эмодзи к теме","Add an emoji to the topic","Mövzuya emoji əlavə et"},
            {"Изменить время напоминания","Edit reminder time","Xatırlatma vaxtını dəyiş"},
            {"Удалить время напоминания","Remove reminder time","Xatırlatma vaxtını sil"},
            {"Настройки ⚙","Settings ⚙","Parametrlər ⚙"},{"Настрой приложение под себя","Make the app yours","Tətbiqi özünə uyğunlaşdır"},
            {"Главная","Home","Ana səhifə"},{"Уроки","Lessons","Dərslər"},{"Прогресс","Progress","İrəliləyiş"},{"Экзамен","Exam","İmtahan"},{"Дневник","Diary","Gündəlik"},{"Таймер","Timer","Taymer"},
            {"Таймер фокуса","Focus timer","Fokus taymeri"},{"Настрой время и занимайся в своём ритме","Set your time and study at your own pace","Vaxtı seç və öz ritmində oxu"},
            {"ВРЕМЯ УЧЁБЫ","STUDY TIME","DƏRS VAXTI"},{"ВРЕМЯ ПЕРЕРЫВА","BREAK TIME","FASİLƏ VAXTI"},{"минут","minutes","dəqiqə"},{"Начать","Start","Başla"},{"Продолжить","Resume","Davam et"},{"Пауза","Pause","Fasilə"},{"Сброс","Reset","Sıfırla"},
            {"Мой цветочек","My flower","Mənim çiçəyim"},{"Мой экзамен ✨","My exam ✨","Mənim imtahanım ✨"},{"Главная цель","Main goal","Əsas məqsəd"},{"ТВОЯ ЦЕЛЬ","YOUR GOAL","SƏNİN MƏQSƏDİN"},{"ГЛАВНАЯ ЦЕЛЬ","MAIN GOAL","ƏSAS MƏQSƏD"},{"ДНЕЙ ОСТАЛОСЬ","DAYS LEFT","QALAN GÜN"},{"СЕГОДНЯ","TODAY","BU GÜN"},{"Добавить цель","Add goal","Məqsəd əlavə et"},{"Изменить","Edit","Dəyiş"},{"Удалить","Delete","Sil"},{"Сохранить изменения","Save changes","Dəyişiklikləri saxla"},
            {"Добавить свой урок","Add a custom lesson","Öz dərsini əlavə et"},{"Добавить тему","Add topic","Mövzu əlavə et"},{"Новая тема","New topic","Yeni mövzu"},{"Название темы","Topic title","Mövzunun adı"},{"Создать урок","Create lesson","Dərs yarat"},{"Отмена","Cancel","Ləğv et"},{"Сохранить","Save","Yadda saxla"},
            {"Напоминания 🔔","Reminders 🔔","Xatırlatmalar 🔔"},{"Сохранить расписание","Save schedule","Cədvəli saxla"},{"Настроить дни и время","Set days and time","Günləri və vaxtı seç"},{"УВЕДОМЛЕНИЯ","NOTIFICATIONS","BİLDİRİŞLƏR"},
            {"Дневник успехов ♡","Success diary ♡","Uğur gündəliyi ♡"},{"Сохранить запись","Save entry","Qeydi saxla"},{"Сохранить имя","Save name","Adı yadda saxla"},{"ТЕМА ОФОРМЛЕНИЯ","APP THEME","TƏRTİBAT MÖVZUSU"},{"ЯЗЫК ПРИЛОЖЕНИЯ","APP LANGUAGE","TƏTBİQİN DİLİ"},
            {"Русский","Russian","Rus dili"},{"Выбери тему, которую хочешь изучить:","Choose a topic to study:","Öyrənmək istədiyin mövzunu seç:"},{"← К урокам","← Lessons","← Dərslərə"},{"← К разделам","← Sections","← Bölmələr"},{"← Главное меню","← Home","← Ana səhifə"},
            {"Полить цветочек","Water the flower","Çiçəyi sulamaq"},{"Уже полит сегодня ✓","Watered today ✓","Bu gün sulanıb ✓"},{"Сменить вид цветка","Change flower","Çiçəyi dəyiş"},{"УХОД ЗА ЦВЕТОЧКОМ","FLOWER CARE","ÇİÇƏYƏ QULLUQ"},
            {"СВОЙ ФОН","CUSTOM BACKGROUND","ÖZ FONUN"},{"Выбрать своё фото","Choose a photo","Şəkil seç"},{"Убрать своё фото","Remove photo","Şəkli sil"},{"ТВОЁ ИМЯ","YOUR NAME","SƏNİN ADIN"},
            {"Выбери настроение приложения","Choose your app mood","Tətbiqin görünüşünü seç"},
            {"КАК К ТЕБЕ ОБРАЩАТЬСЯ?","WHAT SHOULD WE CALL YOU?","SƏNƏ NECƏ MÜRACİƏT EDƏK?"},{"Выбери обращение","Choose how to address you","Müraciət formasını seç"},{"Установи любимое фото как нежный фон приложения","Set your favorite photo as the app background","Sevdiyin şəkli tətbiqin fonu et"},{"Выбери язык интерфейса","Choose interface language","İnterfeys dilini seç"},
            {"НАПОМИНАНИЯ","REMINDERS","XATIRLATMALAR"},{"ОТМЕТИТЬ ПРОГРЕСС","TRACK PROGRESS","İRƏLİLƏYİŞİ QEYD ET"},{"ГЛАВНЫЙ ЭКЗАМЕН","MAIN EXAM","ƏSAS İMTAHAN"},{"НАЗВАНИЕ ЦЕЛИ","GOAL NAME","MƏQSƏDİN ADI"},{"ДАТА","DATE","TARİX"},{"Пока нет целей","No goals yet","Hələ məqsəd yoxdur"},{"Добавь первую дату экзамена или важную цель.","Add your first exam date or goal.","İlk imtahan tarixini və ya məqsədini əlavə et."},
            {"МАЛЕНЬКАЯ ПОБЕДА СЕГОДНЯ","TODAY'S LITTLE WIN","BU GÜNÜN KİÇİK QƏLƏBƏSİ"},{"Здесь появятся твои маленькие победы","Your little wins will appear here","Kiçik qələbələrin burada görünəcək"},{"Сохрани первую запись — тебе будет приятно перечитать её позже.","Save your first entry — you'll enjoy reading it later.","İlk qeydi saxla — sonra oxumaq xoş olacaq."},
            {"Поливай цветочек раз в день. Вода появится только после нажатия.","Water your flower once a day. Water appears when you tap.","Çiçəyini gündə bir dəfə sulа. Su yalnız toxunduqda görünəcək."},{"Твой цветочек растёт в своём горшочке — так же, как твои маленькие победы ♡","Your flower grows in its pot, just like your little wins ♡","Çiçəyin dibçəkdə böyüyür, kiçik uğurların kimi ♡"},
            {"ДНИ НЕДЕЛИ","DAYS OF THE WEEK","HƏFTƏNİN GÜNLƏRİ"},{"ВРЕМЯ НАПОМИНАНИЙ","REMINDER TIMES","XATIRLATMA VAXTLARI"},{"Время не выбрано — напоминания выключены","No times selected — reminders are off","Vaxt seçilməyib — xatırlatmalar sönülüdür"},
            {"Сохраняй моменты, которыми ты гордишься","Save the moments you're proud of","Qürur duyduğun anları saxla"},{"Выбери главную цель и следи за оставшимися днями","Choose your main goal and track the days remaining","Əsas məqsədini seç və qalan günləri izlə"},{"Он растёт вместе с твоими учебными победами","It grows with your study achievements","O, dərs uğurlarınla birlikdə böyüyür"},
            {"Привет, ","Hello, ","Salam, "},{"Сегодня — хороший день, чтобы сделать маленький шаг к своей цели.","Today is a good day to take a small step toward your goal.","Bu gün məqsədinə doğru kiçik addım atmaq üçün yaxşı gündür."},
            {"ТВОЙ ПРОГРЕСС","YOUR PROGRESS","SƏNİN İRƏLİLƏYİŞİN"},{"Ты справляешься.\nПродолжай в своём темпе.","You're doing great.\nKeep going at your own pace.","Sən bacarırsan.\nÖz tempində davam et."},{"Каждое занятие — это забота о себе будущей.","Every study session is care for your future self.","Hər dərs gələcək özünə qayğıdır."},
            {"Выбери дни и время. Часовой пояс устройства.","Choose days and times. Time zone — Baku.","Günləri və vaxtı seç. Saat qurşağı — Bakı."},{"Сохраняй моменты, которыми ты гордишься","Save moments you're proud of","Qürur duyduğun anları saxla"},
            {"Как к тебе обращаться?","What should we call you?","Sənə necə müraciət edək?"},{"Например: Английский, История, Биология","For example: English, History, Biology","Məsələn: İngilis dili, Tarix, Biologiya"},{"Название урока","Lesson name","Dərsin adı"},{"Название темы","Topic name","Mövzunun adı"},{"Сегодня у меня получилось...","Today I managed to...","Bu gün bacardım ki..."},{"Например, экзамен по математике","For example, math exam","Məsələn, riyaziyyat imtahanı"},
            {"Всё нужное — в одном месте","Everything you need in one place","Lazım olan hər şey bir yerdə"},
            {"ПРОФИЛЬ","PROFILE","PROFİL"},{"ВНЕШНИЙ ВИД","APPEARANCE","GÖRÜNÜŞ"},{"Тема приложения","App theme","Tətbiqin mövzusu"},{"Нажми на палитру, чтобы сразу применить оформление.","Tap a palette to apply a look instantly.","Görünüşü tətbiq etmək üçün palitraya toxun."},
            {"Язык приложения","App language","Tətbiqin dili"},{"После выбора язык применяется ко всем экранам приложения.","The selected language is used throughout the app.","Seçilmiş dil tətbiqin bütün ekranlarında istifadə olunur."},
            {"ОТОБРАЖЕНИЕ ПРОГРЕССА","PROGRESS DISPLAY","İRƏLİLƏYİŞİN GÖRÜNÜŞÜ"},{"Выбери удобный формат отображения учебных результатов.","Choose how your study results are displayed.","Nəticələrin göstərilmə formasını seç."},{"С процентами","With percentages","Faizlərlə"},{"Полосы прогресса","Progress bars","İrəliləyiş zolaqları"},
            {"НАПОМИНАНИЯ","REMINDERS","XATIRLATMALAR"},{"Включить напоминания о занятиях","Enable study reminders","Dərs xatırlatmalarını aktiv et"},{"РЕЖИМ ФОКУСА","FOCUS MODE","FOKUS REJİMİ"},{"Временно отключает учебные напоминания.","Temporarily disables study reminders.","Dərs xatırlatmalarını müvəqqəti söndürür."},
            {"РЕЗЕРВНАЯ КОПИЯ","BACKUP","EHTİYAT NÜSXƏSİ"},{"Сохрани уроки, цели, дневник и прогресс в один файл.","Save lessons, goals, diary and progress in one file.","Dərsləri, məqsədləri, gündəliyi və irəliləyişi bir faylda saxla."},
            {"Атмосфера учёбы","Study atmosphere","Dərs atmosferi"},{"Выбери настроение — звук включится сразу","Choose a mood — sound starts immediately","Əhval seç — səs dərhal başlayacaq"},{"Мягкий дождливый настрой","Soft rainy ambience","Yumşaq yağış atmosferi"},{"Тёплая атмосфера кофейни","Cozy café ambience","Rahat kafe atmosferi"},{"Спокойствие и свежесть","Calm and freshness","Sakitlik və təravət"},{"Минимум отвлечений","Fewer distractions","Daha az diqqət yayındıran amil"},{"Нажми, чтобы включить","Tap to play","Başlatmaq üçün toxun"},{"Играет сейчас  •  ✓","Playing now  •  ✓","İndi səslənir  •  ✓"},{"Остановить звук","Stop sound","Səsi dayandır"},
            {"ДНИ НЕДЕЛИ","DAYS OF THE WEEK","HƏFTƏNİN GÜNLƏRİ"},{"ВРЕМЯ НАПОМИНАНИЙ","REMINDER TIMES","XATIRLATMA VAXTLARI"},{"Время не выбрано — напоминания выключены","No time selected — reminders are off","Vaxt seçilməyib — xatırlatmalar sönülüdür"},{"Сохранить расписание","Save schedule","Cədvəli yadda saxla"},
            {"Поиск уроков и тем","Search lessons and topics","Dərs və mövzu axtarışı"},
            {"Математика 📐","Mathematics 📐","Riyaziyyat 📐"},{"Все разделы и темы по математике","All math sections and topics","Riyaziyyatın bütün bölmələri və mövzuları"},
            {"Добавить свой урок","Add your own lesson","Öz dərsini əlavə et"},{"Создать урок","Create lesson","Dərs yarat"},{"Выбери предмет или создай свой урок","Choose a subject or create your own lesson","Fənn seç və ya öz dərsini yarat"}
        };
        for(String[] p:pairs){String from=p[0],to="en".equals(lang)?p[1]:p[2];if(value.equals(from))return to;if(value.startsWith(from+" "))return to+value.substring(from.length());}
        return value;
    }

    private interface OnAction { void go(); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        if(prefs!=null){String corner=prefs.getString("corner_style","round");if("sharp".equals(corner))radius=Math.min(radius,8);else if("normal".equals(corner))radius=Math.max(10,Math.min(radius,16));else radius=Math.max(radius,24);}
        String theme = prefs == null ? "pink" : prefs.getString("app_theme", "pink");
        if(isThemeAccentColor(color))color=PINK;
        if(isThemeAccentColor(stroke))stroke=PINK;
        boolean white = color == 0xFFFFFFFF || color == 0xFFFFF9F7 || color == 0xFFFFFBFC || color == 0xFFFFF5F8 || color == 0xFFFFF4F7 || color == 0xFFFFF5F7 || color == 0xFFFFF8F6 || color == 0xFFF7F4F2 || color == 0xFFFFFCF7 || color == 0xFFFFFCFC || color == 0xFFFFF9EF;
        boolean soft = color == 0xFFFCEEF2 || color == 0xFFFFF0F4 || color == 0xFFFFF0F5 || color == 0xFFFFEEF3 || color == 0xFFFFF1F5 || color == 0xFFF8E8EE || color == 0xFFFFF4F7 || color == 0xFFFFF5F8 || color == 0xFFFFF5F7 || color == 0xFFFFF5F4;
        boolean pale = color == 0xFFF3F0FF || color == 0xFFF1F0FF || color == 0xFFEFF7F1 || color == 0xFFF5F2F1 || color == 0xFFF0E4E8 || color == 0xFFF1F0F2 || color == 0xFFF7F4F2 || color == 0xFFF7F4F5 || color == 0xFFEAF2F9 || color == 0xFFEAF1F7 || color == 0xFFD8E4ED;
        boolean border = stroke == 0xFFF3E9EC || stroke == 0xFFF0E4E8 || stroke == 0xFFEADDE2 || stroke == 0xFFFFD5E1 || stroke == 0xFFF4DCE5 || stroke == 0xFFF2DDE4 || stroke == 0xFFEEDFE4 || stroke == 0xFFD8E4ED || stroke == 0xFFE0D8DF;
        if ("system".equals(theme)) {
            if(white)color=color==0xFFFFF9F7?BG:(isDarkAppearance()?PALE:0xFFFFFFFF);
            else if(soft||pale)color=PALE;
            if(border)stroke=isDarkAppearance()?0xFF49404B:0xFFE0D8DF;
        } else if ("night".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFF17151D : 0xFF25212B;
            else if (soft) color = 0xFF302735; else if (pale) color = 0xFF342E39;
            if (border) stroke = 0xFF403541;
        } else if ("lavender".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF8F7FC : 0xFFFFFFFF;
            else if (soft || pale) color = 0xFFF0EDFA;
            if (border) stroke = 0xFFE5E1EF;
        } else if ("pink".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFFFF9F7 : 0xFFFFFFFF;
            else if (soft || pale) color = 0xFFFCEEF2;
            if (border) stroke = 0xFFF4DCE5;
        } else if ("mint".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF5FAF7 : 0xFFFEFFFE;
            else if (soft || pale) color = 0xFFEAF5EF;
            if (border) stroke = 0xFFDCEBE2;
        } else if ("white".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF7F8FA : 0xFFFFFFFF; else if (soft || pale) color = 0xFFE9EDF2;
            if (border) stroke = 0xFFDCE1E8;
        } else if ("brown".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF7F1EA : 0xFFFFFCF7; else if (soft || pale) color = 0xFFEDE0D2;
            if (border) stroke = 0xFFE1D0BE;
        } else if ("gold".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFFFF5D0 : 0xFFFFFBEA; else if (soft || pale) color = 0xFFF5E2A6;
            if (border) stroke = 0xFFD9B64B;
        } else if ("luxury".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFF211B18 : 0xFF30251F; else if (soft || pale) color = 0xFF3A3028;
            if (border) stroke = 0xFF78613D;
        } else if ("sky".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFF1F8FC : 0xFFFFFFFF; else if (soft || pale) color = 0xFFE1EFF8;
            if (border) stroke = 0xFFD4E5F0;
        } else if ("red".equals(theme)) { if (white) color = color == 0xFFFFF9F7 ? 0xFFFFF5F4 : 0xFFFFFCFC; else if (soft || pale) color = 0xFFF8E2E3; if (border) stroke = 0xFFECC9CC;
        } else if ("lv".equals(theme)) { if (white) color = color == 0xFFFFF9F7 ? 0xFFF4E8D5 : 0xFFFFF9EF; else if (soft || pale) color = 0xFFE7D2B3; if (border) stroke = 0xFFC6A779;
        } else if ("peach".equals(theme)) {
            if (white) color = color == 0xFFFFF9F7 ? 0xFFFFF5EF : 0xFFFFFCF9; else if (soft || pale) color = 0xFFFBE5D9;
            if (border) stroke = 0xFFF0D2C4;
        }
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); p.setMargins(dp(l), dp(t), dp(r), dp(b)); return p; }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }
}
