package ir.payamban.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.IDN;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final int navy = Color.rgb(15, 31, 56);
    private final int teal = Color.rgb(0, 145, 143);
    private final int ink = Color.rgb(35, 48, 67);
    private final int muted = Color.rgb(105, 119, 139);
    private EditText input;
    private LinearLayout results;

    private static class Finding {
        final String title;
        final String detail;
        final int points;
        Finding(String title, String detail, int points) {
            this.title = title; this.detail = detail; this.points = points;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(244, 247, 251));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(20), dp(18), dp(28));
        scroll.addView(page);

        LinearLayout hero = card(navy, 24);
        hero.setPadding(dp(20), dp(22), dp(20), dp(22));
        addText(hero, "زنگ خطر", 29, Color.WHITE, true);
        addSpace(hero, 7);
        addText(hero, "هشدار پیامک و لینک • تحلیل کاملاً آفلاین", 14,
                Color.rgb(207, 224, 237), false);
        addSpace(hero, 13);
        TextView privacy = pill("کاملاً محلی • بدون ارسال اطلاعات", Color.rgb(31, 65, 83),
                Color.rgb(210, 241, 239));
        hero.addView(privacy, wrap());
        page.addView(hero, match());
        addSpace(page, 20);

        addText(page, "متن پیام یا لینک", 18, ink, true);
        addSpace(page, 8);
        input = new EditText(this);
        input.setTextSize(15);
        input.setTextColor(ink);
        input.setHintTextColor(Color.rgb(145, 156, 171));
        input.setHint("متن پیام مشکوک را اینجا وارد یا جای‌گذاری کن...");
        input.setGravity(Gravity.TOP | Gravity.RIGHT);
        input.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        input.setMinLines(6);
        input.setPadding(dp(15), dp(14), dp(15), dp(14));
        input.setBackground(shape(Color.WHITE, 18, Color.rgb(225, 231, 239)));
        page.addView(input, match());
        addSpace(page, 10);

        Button paste = button("جای‌گذاری از حافظه موقت", Color.rgb(229, 239, 244), navy);
        paste.setOnClickListener(v -> pasteText());
        page.addView(paste, match());
        addSpace(page, 9);
        Button scan = button("تحلیل امنیت پیام", teal, Color.WHITE);
        scan.setOnClickListener(v -> analyze());
        page.addView(scan, match());
        addSpace(page, 17);

        results = card(Color.WHITE, 20);
        results.setPadding(dp(17), dp(17), dp(17), dp(17));
        addText(results, "نتیجه تحلیل", 17, ink, true);
        addSpace(results, 8);
        addText(results, "پس از واردکردن متن، دکمه تحلیل را بزن.", 14, muted, false);
        page.addView(results, match());
        addSpace(page, 15);

        LinearLayout note = card(Color.rgb(255, 248, 235), 16);
        note.setPadding(dp(14), dp(13), dp(14), dp(13));
        addText(note, "نکته مهم", 14, Color.rgb(139, 87, 25), true);
        addSpace(note, 5);
        addText(note, "این ابزار فقط نشانه‌های ظاهری و الگوهای شناخته‌شده را بررسی می‌کند. نتیجه کم‌خطر، امن‌بودن پیام را ثابت نمی‌کند؛ لینک را باز نکن و رمز پویا، CVV2 یا کد پیامکی را در صفحه‌ای که از پیام دریافت کرده‌ای وارد نکن.", 12,
                Color.rgb(117, 83, 47), false);
        page.addView(note, match());
        addSpace(page, 12);
        addText(page, "حریم خصوصی: برنامه مجوز اینترنت، پیامک و مخاطبین ندارد. متن فقط روی همین گوشی تحلیل می‌شود؛ هیچ لینکی باز یا بررسی آنلاین نمی‌شود. استعلام بانکی در برنامه وجود ندارد.", 11, muted, false);
        setContentView(scroll);
    }

    private void pasteText() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return;
        ClipData clip = cm.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            CharSequence value = clip.getItemAt(0).coerceToText(this);
            if (value != null) input.setText(value);
        }
    }

    private void analyze() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            input.setError("ابتدا متن پیام یا لینک را وارد کن");
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);
        Map<String, Finding> findings = new LinkedHashMap<>();

        if (containsAny(lower, "فوری", "همین الان", "مسدود", "مسدودسازی", "قطع خواهد شد",
                "مهلت", "اقدام کنید", "ظرف ۲۴", "ظرف 24", "آخرین اخطار", "بلافاصله")) {
            put(findings, "urgency", "فشار زمانی یا تهدید",
                    "پیام با فوریت، مهلت کوتاه یا تهدید به قطع/مسدودی سعی در وادارکردن مخاطب به اقدام سریع دارد.", 2);
        }
        if (containsAny(lower, "سهام عدالت", "ابلاغیه", "ثنا", "یارانه", "سامانه قضایی",
                "ثبت احوال", "مالیات", "عدالت الکترونیک")) {
            put(findings, "public-service", "استفاده از عنوان دولتی یا قضایی",
                    "نام یک خدمت عمومی یا قضایی آمده است؛ این عنوان‌ها ممکن است برای جلب اعتماد یا ترساندن مخاطب جعل شوند.", 1);
        }
        if (containsAny(lower, "واریز سود", "برنده شدید", "جایزه", "هدیه", "دریافت وجه",
                "تکمیل اطلاعات", "ثبت نام", "سود تضمینی")) {
            put(findings, "reward", "وعده پول، جایزه یا تکمیل اطلاعات",
                    "وعده دریافت وجه یا درخواست تکمیل اطلاعات می‌تواند بخشی از سناریوی فریب باشد.", 2);
        }
        if (containsAny(lower, "رمز پویا", "رمز دوم", "cvv2", "کد تایید", "کد تأیید",
                "کد یکبار مصرف", "کد یک‌بار مصرف", "اطلاعات کارت", "شماره کارت",
                "رمز عبور", "کد پیامکی")) {
            put(findings, "credentials", "درخواست اطلاعات حساس",
                    "به رمز، کد تأیید یا اطلاعات بانکی اشاره شده است. این اطلاعات را در لینک پیامک وارد نکن.", 5);
        }
        if (containsAny(lower, "نصب برنامه", "نصب اپلیکیشن", "دانلود برنامه", ".apk", "فایل apk")) {
            put(findings, "apk", "درخواست نصب یا دریافت برنامه",
                    "نصب فایل یا برنامه‌ای که از پیام ناشناس دریافت شده خطرناک است؛ فایل APK را نصب نکن.", 5);
        }

        Pattern urlPattern = Pattern.compile("(?i)(https?://[^\\s<>]+|www\\.[^\\s<>]+)");
        Matcher matcher = urlPattern.matcher(text);
        int urlCount = 0;
        while (matcher.find()) {
            urlCount++;
            String raw = trimUrlPunctuation(matcher.group());
            inspectUrl(raw, findings, urlCount);
        }
        if (urlCount > 0) {
            put(findings, "local-url", "بررسی صرفاً ظاهری لینک",
                    "هیچ لینکی باز یا به اینترنت ارسال نشد؛ مقصد واقعی لینک و محتوای سایت بررسی نشده است.", 0);
        }

        int score = 0;
        for (Finding f : findings.values()) score += f.points;
        score = Math.min(10, score);
        String level = score >= 7 ? "ریسک بالا" : score >= 4 ? "نیازمند احتیاط" : score >= 1 ? "چند نشانه قابل بررسی" : "نشانه مشخصی پیدا نشد";
        int color = score >= 7 ? Color.rgb(180, 52, 59) : score >= 4 ? Color.rgb(185, 116, 25) : Color.rgb(25, 130, 104);

        results.removeAllViews();
        addText(results, "نتیجه تحلیل", 17, ink, true);
        addSpace(results, 10);
        LinearLayout scoreCard = card(Color.rgb(247, 249, 252), 16);
        scoreCard.setPadding(dp(14), dp(13), dp(14), dp(13));
        addText(scoreCard, level, 20, color, true);
        addSpace(scoreCard, 4);
        addText(scoreCard, "امتیاز نشانه‌ها: " + score + " از 10", 13, ink, true);
        addText(scoreCard, "این امتیاز احتمال قطعی کلاهبرداری نیست؛ جمع وزن نشانه‌های پیدا‌شده است.", 11, muted, false);
        results.addView(scoreCard, match());
        addSpace(results, 12);

        if (findings.isEmpty()) {
            addText(results, "در قواعد آفلاین این نسخه نشانه مشخصی پیدا نشد. این نتیجه به معنی امن‌بودن پیام نیست.", 13, muted, false);
        } else {
            addText(results, "موارد شناسایی‌شده", 14, ink, true);
            addSpace(results, 6);
            for (Finding f : findings.values()) {
                LinearLayout item = new LinearLayout(this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setPadding(dp(11), dp(10), dp(11), dp(10));
                item.setBackground(shape(Color.rgb(248, 250, 252), 13, Color.rgb(233, 237, 243)));
                addText(item, "• " + f.title, 13, ink, true);
                addSpace(item, 4);
                addText(item, f.detail, 12, muted, false);
                results.addView(item, match());
                addSpace(results, 7);
            }
        }
        addSpace(results, 5);
        addText(results, "محدودیت: تحلیل بر اساس قواعد محلی است؛ ممکن است پیام سالم را مشکوک بداند یا کلاهبرداری جدید را تشخیص ندهد. HTTPS نیز به‌تنهایی نشانه معتبر بودن سایت نیست.", 11, Color.rgb(139, 91, 43), false);
    }

    private void inspectUrl(String raw, Map<String, Finding> findings, int index) {
        try {
            String candidate = raw.startsWith("www.") ? "https://" + raw : raw;
            URI uri = new URI(candidate);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                put(findings, "url-invalid-" + index, "ساختار لینک نامشخص",
                        "دامنه لینک از متن قابل تشخیص نیست؛ روی آن کلیک نکن.", 2);
                return;
            }
            String asciiHost = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            String unicodeHost = host.toLowerCase(Locale.ROOT);

            if (asciiHost.contains("xn--")) {
                put(findings, "punycode-" + index, "دامنه با نویسه‌های کدگذاری‌شده",
                        "دامنه شامل بخش Punycode است؛ ممکن است برای شبیه‌سازی نام دامنه دیگری استفاده شود. این مورد به‌تنهایی اثبات تقلب نیست.", 3);
            }
            if (uri.getRawUserInfo() != null || raw.matches("(?i)^https?://[^/]*@.*")) {
                put(findings, "userinfo-" + index, "وجود بخش کاربری قبل از دامنه",
                        "ساختار لینک دارای user-info یا علامت @ است و می‌تواند دامنه واقعی را پنهان کند.", 4);
            }
            if (isIpv4(asciiHost)) {
                put(findings, "ip-host-" + index, "استفاده از نشانی IP به‌جای دامنه",
                        "مقصد لینک به‌صورت IP نوشته شده است؛ در پیام ناشناس با احتیاط بیشتری برخورد کن.", 3);
            }
            int port = uri.getPort();
            if (port != -1 && port != 80 && port != 443) {
                put(findings, "port-" + index, "درگاه غیرمعمول در لینک",
                        "لینک از شماره درگاهی غیر از 80 یا 443 استفاده می‌کند.", 2);
            }
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                put(findings, "http-" + index, "اتصال HTTP بدون رمزنگاری",
                        "ارتباط HTTP رمزنگاری‌شده نیست. این موضوع خطر را بیشتر می‌کند اما به‌تنهایی اثبات کلاهبرداری نیست.", 2);
            }
            if (isShortener(asciiHost)) {
                put(findings, "short-" + index, "لینک کوتاه‌شده",
                        "مقصد نهایی از متن پیام مشخص نیست و این برنامه لینک را باز یا گسترش نمی‌دهد.", 3);
            }
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
            String query = uri.getQuery() == null ? "" : uri.getQuery().toLowerCase(Locale.ROOT);
            if (path.endsWith(".apk") || query.contains(".apk")) {
                put(findings, "apk-url-" + index, "لینک مستقیم یا احتمالی فایل APK",
                        "دانلود و نصب فایل برنامه از لینک ناشناس می‌تواند باعث دسترسی غیرمجاز شود.", 5);
            }
            String hostWords = asciiHost.replace('.', ' ');
            if (containsAny(hostWords, "bank", "banka", "banc", "shaparak", "sep", "mellat", "melli", "saderat", "tejarat", "pasargad")) {
                put(findings, "brand-word-" + index, "وجود واژه بانکی در نام دامنه",
                        "نام یا واژه مرتبط با بانک در دامنه دیده می‌شود؛ این برنامه اصالت دامنه بانکی را تأیید نمی‌کند. آدرس را مستقل از پیام و از منبع رسمی بررسی کن.", 2);
            }
            if (asciiHost.matches(".*\\.(top|xyz|click|work|support)$")) {
                put(findings, "tld-" + index, "پسوند دامنه نیازمند بررسی بیشتر",
                        "پسوند دامنه به‌تنهایی معتبر یا جعلی بودن سایت را مشخص نمی‌کند.", 1);
            }
            if (unicodeHost.indexOf('\u200c') >= 0 || unicodeHost.indexOf('\u200d') >= 0) {
                put(findings, "invisible-" + index, "نویسه نامعمول در دامنه",
                        "دامنه دارای نویسه‌های کنترلی یا نامعمول است.", 3);
            }
        } catch (Exception e) {
            put(findings, "url-parse-" + index, "ساختار لینک قابل تحلیل نیست",
                    "ساختار لینک غیرمعمول است؛ آن را باز نکن و مقصد را از منبع رسمی بررسی کن.", 2);
        }
    }

    private boolean isIpv4(String host) {
        if (!host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return false;
        String[] parts = host.split("\\.");
        for (String part : parts) {
            try { int n = Integer.parseInt(part); if (n < 0 || n > 255) return false; }
            catch (NumberFormatException e) { return false; }
        }
        return true;
    }

    private boolean isShortener(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        return h.equals("bit.ly") || h.endsWith(".bit.ly") || h.equals("tinyurl.com") ||
                h.equals("t.co") || h.equals("is.gd") || h.equals("cutt.ly") ||
                h.equals("rb.gy") || h.equals("shorturl.at") || h.equals("tiny.cc");
    }

    private String trimUrlPunctuation(String value) {
        return value.replaceAll("[),،؛.!?؟]+$", "");
    }

    private void put(Map<String, Finding> map, String key, String title, String detail, int points) {
        map.put(key, new Finding(title, detail, points));
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private LinearLayout card(int color, int radius) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(shape(color, radius, Color.TRANSPARENT));
        return box;
    }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private TextView pill(String text, int bg, int fg) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(11); t.setTextColor(fg);
        t.setPadding(dp(11), dp(7), dp(11), dp(7));
        t.setBackground(shape(bg, 30, Color.TRANSPARENT));
        return t;
    }

    private TextView addText(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(t, match());
        return t;
    }

    private Button button(String value, int bg, int fg) {
        Button b = new Button(this);
        b.setText(value); b.setTextSize(14); b.setTextColor(fg);
        b.setAllCaps(false); b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setBackground(shape(bg, 15, Color.TRANSPARENT));
        return b;
    }

    private void addSpace(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout.LayoutParams match() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(-2, -2); }
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
