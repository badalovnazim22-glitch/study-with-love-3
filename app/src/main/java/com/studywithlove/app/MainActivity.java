package com.studywithlove.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int BG=0xFFF6F7FC, WHITE=0xFFFFFFFF, INK=0xFF171A2E;
    private static final int MUTED=0xFF85899D, PURPLE=0xFF7065E8, LILAC=0xFFECEAFF;
    private static final int GREEN=0xFF24A783, LINE=0xFFE9EAF2;
    private static final String PREFS="math_academy_v2";
    private SharedPreferences prefs;
    private LinearLayout root;
    private String selectedChapter="";
    private String selectedTopic="";
    private int questionAnswer=0, questionCount=0, correctCount=0;
    private boolean answered=false;
    private final Random random=new Random();
    private final List<Chapter> chapters=new ArrayList<>();

    private static class Chapter {
        String title,subtitle,icon;
        List<String> topics;
        Chapter(String t,String s,String i,String... p){title=t;subtitle=s;icon=i;topics=Arrays.asList(p);}
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(WHITE);
        if(android.os.Build.VERSION.SDK_INT>=23)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        // Remove all data from the previous multi-purpose app. Only the new math profile remains.
        getSharedPreferences("study_progress",MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences("study_with_love",MODE_PRIVATE).edit().clear().apply();
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        loadCurriculum();
        showHome();
    }

    private void loadCurriculum(){
        chapters.add(new Chapter("Числа и действия","Основа математики","01",
            "Натуральные числа","Сравнение и упорядочивание","Округление натуральных чисел",
            "Сложение и вычитание","Квадрат и куб числа","Умножение и деление",
            "Числовые выражения","Делители и кратные","Простые и составные числа","Признаки делимости"));
        chapters.add(new Chapter("Обыкновенные дроби","Части целого и действия с ними","02",
            "Правильные и неправильные дроби","Сравнение дробей","Сложение дробей",
            "Вычитание дробей","Смешанные числа","Умножение дробей","Деление дробей",
            "Часть числа и число по его части","Сокращение дробей","Общий знаменатель"));
        chapters.add(new Chapter("Десятичные дроби","Вычисления и преобразования","03",
            "Десятичные дроби","Сравнение десятичных дробей","Округление десятичных дробей",
            "Перевод дробей","Сложение и вычитание десятичных дробей","Умножение десятичных дробей",
            "Деление десятичных дробей","Среднее арифметическое","Задачи на десятичные дроби"));
        chapters.add(new Chapter("Проценты и пропорции","Математика в повседневной жизни","04",
            "Понятие процента","Процент от числа","Нахождение числа по проценту",
            "Увеличение и уменьшение на процент","Отношение и пропорция","Задачи на проценты"));
    }

    private void showHome(){
        page("Математика","Твой понятный путь к уверенным знаниям");
        LinearLayout hero=panel(0xFF22233F,0);
        hero.setPadding(dp(22),dp(22),dp(22),dp(22));
        TextView over=label("ТВОЯ МАТЕМАТИЧЕСКАЯ СРЕДА",10,0xFFBEB9FF,true);
        hero.addView(over);
        TextView h=label("Думай ясно.\nРешай уверенно.",29,WHITE,true);
        h.setLineSpacing(dp(3),1f);hero.addView(h,margin(-1,-2,0,12,0,8));
        TextView desc=label("Короткие темы, понятные примеры и практика — шаг за шагом.",13,0xFFD0D0E2,false);
        desc.setLineSpacing(dp(3),1f);hero.addView(desc,margin(-1,-2,0,0,0,18));
        LinearLayout stats=new LinearLayout(this);stats.setGravity(Gravity.CENTER_VERTICAL);
        stat(stats,String.valueOf(completedTopics()),"тем изучено");
        View sep=new View(this);sep.setBackgroundColor(0x446F70A0);stats.addView(sep,margin(1,34,15,0,15,0));
        stat(stats,String.valueOf(totalTopics()),"в программе");
        hero.addView(stats);
        root.addView(hero,margin(-1,-2,0,2,0,20));
        section("ТВОЯ ПРОГРАММА","Выбери раздел");
        for(int i=0;i<chapters.size();i++)chapterCard(chapters.get(i),i);
        LinearLayout quick=panel(WHITE,LINE);
        TextView q=label("Быстрая практика",18,INK,true);quick.addView(q);
        quick.addView(label("Проверь себя на коротких математических заданиях.",13,MUTED,false),margin(-1,-2,0,5,0,14));
        Button practice=primary("Начать тренировку");quick.addView(practice,margin(-1,50,0,0,0,0));
        practice.setOnClickListener(v->{selectedTopic="Смешанная практика";startPractice();});
        root.addView(quick,margin(-1,-2,0,18,0,20));
        footer();
    }

    private void chapterCard(Chapter c,int index){
        int done=0;for(String t:c.topics)if(progress(t)>=100)done++;
        LinearLayout card=panel(WHITE,LINE);card.setPadding(dp(17),dp(17),dp(17),dp(16));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView num=label(c.icon,12,PURPLE,true);num.setGravity(Gravity.CENTER);
        num.setBackground(round(LILAC,13,0));row.addView(num,margin(48,44,0,0,13,0));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);
        words.addView(label(c.title,17,INK,true));
        words.addView(label(c.subtitle,12,MUTED,false),margin(-1,-2,0,3,0,0));
        row.addView(words,new LinearLayout.LayoutParams(0,-2,1f));
        TextView arrow=label("↗",20,PURPLE,true);row.addView(arrow);
        card.addView(row);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(c.topics.size());bar.setProgress(done);
        if(android.os.Build.VERSION.SDK_INT>=21){bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF0F0F6));}
        card.addView(bar,margin(-1,5,0,14,0,8));
        LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.addView(label(done+" из "+c.topics.size()+" тем",11,MUTED,false),new LinearLayout.LayoutParams(0,-2,1f));
        bottom.addView(label("ОТКРЫТЬ РАЗДЕЛ  →",10,PURPLE,true));card.addView(bottom);
        card.setOnClickListener(v->{selectedChapter=c.title;showTopics(c);});
        root.addView(card,margin(-1,-2,0,0,0,11));
    }

    private void showTopics(Chapter c){
        selectedTopic="";
        page(c.title,"Раздел "+String.format(Locale.US,"%02d",chapters.indexOf(c)+1)+"  ·  "+c.topics.size()+" учебных тем");
        Button back=quiet("← Все разделы");back.setOnClickListener(v->showHome());root.addView(back,margin(-1,44,0,0,0,18));
        for(int i=0;i<c.topics.size();i++){
            final String topic=c.topics.get(i);final int n=i+1;int p=progress(topic);
            LinearLayout card=panel(WHITE,LINE);card.setPadding(dp(15),dp(14),dp(15),dp(14));
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView index=label(String.format(Locale.US,"%02d",n),12,PURPLE,true);index.setGravity(Gravity.CENTER);index.setBackground(round(LILAC,12,0));
            row.addView(index,margin(42,42,0,0,12,0));
            LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);
            col.addView(label(topic,15,INK,true));
            col.addView(label(p==100?"Тема завершена":"Прогресс · "+p+"%",11,MUTED,false),margin(-1,-2,0,4,0,0));
            row.addView(col,new LinearLayout.LayoutParams(0,-2,1f));
            row.addView(label(p==100?"✓":"›",22,p==100?GREEN:PURPLE,true));card.addView(row);
            card.setOnClickListener(v->{selectedTopic=topic;showLesson(topic);});
            root.addView(card,margin(-1,-2,0,0,0,9));
        }
        footer();
    }

    private void showLesson(String topic){
        page(topic,"Короткое объяснение · пример · закрепление");
        Button back=quiet("← К списку тем");back.setOnClickListener(v->showTopics(findChapter(selectedChapter)));root.addView(back,margin(-1,42,0,0,0,16));
        LinearLayout intro=panel(WHITE,LINE);
        intro.addView(label("КЛЮЧЕВАЯ ИДЕЯ",10,PURPLE,true),margin(-1,-2,0,0,0,9));
        intro.addView(label(explanation(topic),16,INK,true),margin(-1,-2,0,0,0,12));
        intro.addView(label(example(topic),14,MUTED,false));
        root.addView(intro,margin(-1,-2,0,0,0,13));
        LinearLayout goal=panel(0xFFF0EFFD,0);
        goal.addView(label("Закрепи материал",17,INK,true));
        goal.addView(label("Реши несколько заданий и отметь тему как изученную.",13,MUTED,false),margin(-1,-2,0,5,0,12));
        Button train=primary("Перейти к упражнениям");goal.addView(train,margin(-1,49,0,0,0,0));
        train.setOnClickListener(v->startPractice());
        root.addView(goal,margin(-1,-2,0,0,0,13));
        section("МОЙ РЕЗУЛЬТАТ","Отметь свой уровень");
        int p=progress(topic);
        TextView status=label("Текущий прогресс · "+p+"%",13,MUTED,true);root.addView(status,margin(-1,-2,0,0,0,8));
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(p);
        if(android.os.Build.VERSION.SDK_INT>=21){bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE9E8F2));}
        root.addView(bar,margin(-1,7,0,0,0,14));
        LinearLayout levels=new LinearLayout(this);
        int[] vals={25,50,75,100};
        for(int val:vals){Button b=quiet(val+"%");if(p>=val)b.setBackground(round(LILAC,13,0));levels.addView(b,new LinearLayout.LayoutParams(0,46,1f));b.setOnClickListener(v->{prefs.edit().putInt(key(topic),val).apply();showLesson(topic);if(val==100)Toast.makeText(this,"Тема отмечена как изученная",Toast.LENGTH_SHORT).show();});}
        root.addView(levels,margin(-1,-2,0,0,0,10));
        footer();
    }

    private void startPractice(){questionCount=0;correctCount=0;nextQuestion();}
    private void nextQuestion(){
        answered=false;questionCount++;
        page("Практика", "Задание "+questionCount+" · правильных ответов: "+correctCount);
        LinearLayout card=panel(WHITE,LINE);card.setPadding(dp(22),dp(24),dp(22),dp(22));
        card.addView(label(selectedTopic.toUpperCase(Locale.ROOT),10,PURPLE,true),margin(-1,-2,0,0,0,17));
        String[] q=makeQuestion();questionAnswer=Integer.parseInt(q[1]);
        TextView prompt=label(q[0],30,INK,true);prompt.setGravity(Gravity.CENTER);card.addView(prompt,margin(-1,90,0,0,0,20));
        root.addView(card,margin(-1,-2,0,0,0,14));
        section("ВЫБЕРИ ОТВЕТ","Не торопись — подумай");
        List<Integer> options=new ArrayList<>();options.add(questionAnswer);
        while(options.size()<4){int v=questionAnswer+random.nextInt(19)-9;if(v>=0&&!options.contains(v))options.add(v);}
        Collections.shuffle(options);
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<4;i+=2){LinearLayout line=new LinearLayout(this);for(int j=i;j<i+2;j++){final int value=options.get(j);Button b=quiet(String.valueOf(value));b.setTextSize(18);line.addView(b,new LinearLayout.LayoutParams(0,58,1f));LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)b.getLayoutParams();lp.setMargins(dp(4),dp(4),dp(4),dp(4));b.setLayoutParams(lp);b.setOnClickListener(v->{if(answered)return;answered=true;if(value==questionAnswer){correctCount++;b.setBackground(round(0xFFE2F6EF,15,0));b.setTextColor(GREEN);Toast.makeText(this,"Верно! Отличная работа",Toast.LENGTH_SHORT).show();}else{b.setBackground(round(0xFFFFE9E9,15,0));b.setTextColor(0xFFD45C65);Toast.makeText(this,"Правильный ответ: "+questionAnswer,Toast.LENGTH_SHORT).show();}for(int k=0;k<grid.getChildCount();k++){LinearLayout rr=(LinearLayout)grid.getChildAt(k);for(int x=0;x<rr.getChildCount();x++)rr.getChildAt(x).setEnabled(false);}Button next=primary("Следующее задание");root.addView(next,margin(-1,52,0,16,0,0));next.setOnClickListener(w->nextQuestion());});}grid.addView(line);}
        root.addView(grid,margin(-1,-2,0,0,0,10));
        Button finish=quiet("Завершить тренировку");finish.setOnClickListener(v->showPracticeResult());root.addView(finish,margin(-1,48,0,10,0,0));
    }
    private String[] makeQuestion(){
        int a,b,op=random.nextInt(4);String symbol;
        if(op==0){a=random.nextInt(40)+10;b=random.nextInt(30)+1;symbol="+";return new String[]{a+"  +  "+b,String.valueOf(a+b)};}
        if(op==1){a=random.nextInt(40)+20;b=random.nextInt(a)+1;symbol="−";return new String[]{a+"  −  "+b,String.valueOf(a-b)};}
        if(op==2){a=random.nextInt(12)+2;b=random.nextInt(9)+2;return new String[]{a+"  ×  "+b,String.valueOf(a*b)};}
        b=random.nextInt(9)+2;a=b*(random.nextInt(10)+2);return new String[]{a+"  ÷  "+b,String.valueOf(a/b)};
    }
    private void showPracticeResult(){
        page("Тренировка завершена","Хорошая работа — каждый пример приближает к результату");
        LinearLayout card=panel(0xFF22233F,0);card.setPadding(dp(22),dp(25),dp(22),dp(25));
        TextView score=label(correctCount+" / "+Math.max(1,questionCount),38,WHITE,true);score.setGravity(Gravity.CENTER);card.addView(score);
        TextView sub=label("правильных ответов",13,0xFFD0D0E2,false);sub.setGravity(Gravity.CENTER);card.addView(sub);
        root.addView(card,margin(-1,-2,0,0,0,16));
        Button again=primary("Ещё одна тренировка");again.setOnClickListener(v->startPractice());root.addView(again,margin(-1,52,0,0,0,9));
        Button home=quiet("К программе");home.setOnClickListener(v->showHome());root.addView(home,margin(-1,48,0,0,0,0));
    }

    private String explanation(String t){
        String s=t.toLowerCase(Locale.ROOT);
        if(s.contains("процент"))return "Процент — это одна сотая часть целого. Чтобы найти процент от числа, переведи процент в дробь и умножь на число.";
        if(s.contains("дроб"))return "Дробь показывает, на сколько равных частей разделили целое и сколько таких частей взяли. Знаменатель — снизу, числитель — сверху.";
        if(s.contains("округ"))return "При округлении посмотри на цифру справа от нужного разряда: 0–4 — оставляем, 5–9 — увеличиваем предыдущую цифру на единицу.";
        if(s.contains("сравнен"))return "Сравнивай числа по разрядам слева направо. Для дробей сначала приведи их к общему знаменателю или сравни десятичную запись.";
        if(s.contains("умнож"))return "Умножение — это краткая запись повторяющегося сложения. Для дробей перемножь числители и отдельно знаменатели.";
        if(s.contains("делен"))return "Деление показывает, сколько раз одно число содержится в другом. Деление на дробь заменяется умножением на обратную дробь.";
        if(s.contains("пропорц"))return "Пропорция — это равенство двух отношений. Основное свойство: произведение крайних членов равно произведению средних.";
        if(s.contains("среднее"))return "Чтобы найти среднее арифметическое, сложи все значения и раздели сумму на их количество.";
        if(s.contains("сложен")||s.contains("вычит"))return "Запиши числа так, чтобы соответствующие разряды стояли друг под другом. Выполняй действие справа налево, не забывая о переносе или заимствовании.";
        if(s.contains("делимост")||s.contains("делител")||s.contains("кратн"))return "Делитель делит число без остатка. Число кратно другому, если результат деления — целое число.";
        if(s.contains("натураль"))return "Натуральные числа используются при счёте предметов: 1, 2, 3 и так далее. Они помогают описывать количество и порядок.";
        return "Внимательно прочитай условие, выдели известные величины и выбери подходящее математическое действие. После вычисления обязательно проверь результат обратным действием.";
    }
    private String example(String t){
        String s=t.toLowerCase(Locale.ROOT);
        if(s.contains("процент"))return "Пример: 20% от 150 = 0,2 × 150 = 30.";
        if(s.contains("дроб"))return "Пример: 1/4 + 2/4 = 3/4. Знаменатель одинаковый — складываем числители.";
        if(s.contains("округ"))return "Пример: 3,768 до сотых = 3,77, потому что следующая цифра — 8.";
        if(s.contains("среднее"))return "Пример: (4 + 6 + 8) ÷ 3 = 6.";
        if(s.contains("пропорц"))return "Пример: 2/3 = 4/6, потому что 2 × 6 = 3 × 4.";
        return "Пример: 24 + 18 = 42. Проверка: 42 − 18 = 24.";
    }

    private void page(String title,String subtitle){
        getWindow().setNavigationBarColor(WHITE);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(18),dp(20),dp(30));root.setBackgroundColor(BG);
        scroll.addView(root);setContentView(scroll);
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark=label("∑",24,WHITE,true);mark.setGravity(Gravity.CENTER);mark.setBackground(round(PURPLE,15,0));brand.addView(mark,margin(43,43,0,0,11,0));
        LinearLayout brandText=new LinearLayout(this);brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.addView(label("STUDY WITH LOVE",10,INK,true));brandText.addView(label("MATH STUDIO",9,MUTED,true));
        brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1f));
        TextView badge=label("MATH  2.0",9,PURPLE,true);badge.setPadding(dp(11),dp(8),dp(11),dp(8));badge.setBackground(round(LILAC,11,0));brand.addView(badge);
        root.addView(brand,margin(-1,46,0,0,0,24));
        TextView heading=label(title,29,INK,true);heading.setLetterSpacing(-.025f);heading.setLineSpacing(dp(2),1f);root.addView(heading,margin(-1,-2,0,0,0,6));
        TextView sub=label(subtitle,13,MUTED,false);sub.setLineSpacing(dp(3),1f);root.addView(sub,margin(-1,-2,0,0,0,20));
        scroll.setAlpha(0f);scroll.setTranslationY(dp(9));scroll.animate().alpha(1f).translationY(0).setDuration(240).setInterpolator(new DecelerateInterpolator()).start();
    }
    private void section(String title,String sub){TextView t=label(title,10,PURPLE,true);t.setLetterSpacing(.12f);root.addView(t,margin(-1,-2,0,0,0,5));root.addView(label(sub,19,INK,true),margin(-1,-2,0,0,0,13));}
    private LinearLayout panel(int color,int stroke){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(17),dp(16),dp(17),dp(16));l.setBackground(round(color,21,stroke));l.setElevation(dp(1));return l;}
    private void stat(LinearLayout row,String value,String caption){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.addView(label(value,23,WHITE,true));c.addView(label(caption,10,0xFFC9C9DD,false));row.addView(c,new LinearLayout.LayoutParams(0,-2,1f));}
    private void footer(){TextView t=label("МАТЕМАТИКА · УЧИСЬ В СВОЁМ ТЕМПЕ",9,0xFFA0A3B4,true);t.setGravity(Gravity.CENTER);root.addView(t,margin(-1,44,0,18,0,0));}
    private Button primary(String s){Button b=button(s,true);return b;}
    private Button quiet(String s){return button(s,false);}
    private Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setAllCaps(false);b.setMinHeight(dp(45));b.setPadding(dp(13),dp(8),dp(13),dp(8));b.setTextColor(primary?WHITE:PURPLE);b.setBackground(round(primary?PURPLE:LILAC,15,0));b.setStateListAnimator(null);b.setOnTouchListener((v,e)->{if(e.getAction()==android.view.MotionEvent.ACTION_DOWN)v.animate().scaleX(.98f).scaleY(.98f).setDuration(75).start();else if(e.getAction()==android.view.MotionEvent.ACTION_UP||e.getAction()==android.view.MotionEvent.ACTION_CANCEL)v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();return false;});return b;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int top,int r,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));p.setMargins(dp(l),dp(top),dp(r),dp(bottom));return p;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String key(String topic){return "topic_"+topic.hashCode();}
    private int progress(String topic){return prefs.getInt(key(topic),0);}
    private int completedTopics(){int n=0;for(Chapter c:chapters)for(String t:c.topics)if(progress(t)>=100)n++;return n;}
    private int totalTopics(){int n=0;for(Chapter c:chapters)n+=c.topics.size();return n;}
    private Chapter findChapter(String title){for(Chapter c:chapters)if(c.title.equals(title))return c;return chapters.get(0);}
    @Override public void onBackPressed(){
        if(root==null){showHome();return;}
        if(!selectedChapter.isEmpty()&& !selectedTopic.isEmpty()){selectedTopic="";showTopics(findChapter(selectedChapter));}
        else if(!selectedChapter.isEmpty()){selectedChapter="";showHome();}
        else showHome();
    }
}