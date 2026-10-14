package app.redmi.inspector;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private LinearLayout content;
    private String latestReport = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        runScan();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        page.setPadding(pad, dp(24), pad, dp(28));
        page.setBackgroundColor(0xFFF5F6F8);

        TextView title = new TextView(this);
        title.setText("Redmi System Inspector");
        title.setTextSize(25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFF172033);
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("فحص النظام دون روت — نسخة تشخيص أولية");
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFF536078);
        subtitle.setPadding(0, dp(8), 0, dp(18));
        page.addView(subtitle);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button scan = new Button(this);
        scan.setText("إعادة الفحص");
        scan.setOnClickListener(v -> runScan());
        buttons.addView(scan, new LinearLayout.LayoutParams(0, dp(52), 1f));
        Button share = new Button(this);
        share.setText("مشاركة التقرير");
        share.setOnClickListener(v -> shareReport());
        buttons.addView(share, new LinearLayout.LayoutParams(0, dp(52), 1f));
        page.addView(buttons);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(12), 0, 0);
        page.addView(content);

        TextView note = new TextView(this);
        note.setText("مهم: هذا التطبيق لا يثبت تحديثات ولا يغيّر ملفات النظام. لا يستطيع تطبيق عادي قراءة كل خصائص النظام المحمية أو تأكيد أصالة ROM وحده.");
        note.setTextSize(13);
        note.setTextColor(0xFF7A4D00);
        note.setPadding(dp(14), dp(14), dp(14), dp(14));
        note.setBackgroundColor(0xFFFFF0D6);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(16);
        page.addView(note, noteParams);

        scroll.addView(page);
        setContentView(scroll);
    }

    private void runScan() {
        content.removeAllViews();
        StringBuilder report = new StringBuilder();
        report.append("Redmi System Inspector\n");
        report.append("وقت الفحص: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n\n");

        addSection("معلومات الجهاز");
        addItem(report, "الشركة", Build.MANUFACTURER);
        addItem(report, "الطراز", Build.MODEL);
        addItem(report, "اسم الجهاز (device)", Build.DEVICE);
        addItem(report, "إصدار Android", Build.VERSION.RELEASE);
        addItem(report, "مستوى API", String.valueOf(Build.VERSION.SDK_INT));
        addItem(report, "تصحيح الأمان", safe(Build.VERSION.SECURITY_PATCH));
        addItem(report, "Build ID", safe(Build.ID));
        addItem(report, "Incremental", safe(Build.VERSION.INCREMENTAL));
        addItem(report, "بصمة البناء", safe(Build.FINGERPRINT));

        addSection("فحص اتساق معلومات البناء");
        String fp = safe(Build.FINGERPRINT);
        String inc = safe(Build.VERSION.INCREMENTAL);
        String finding;
        if (fp.equals("غير متاح") || inc.equals("غير متاح")) {
            finding = "تعذّر الحصول على جميع القيم العامة.";
        } else {
            String fpTail = fp.contains("/") ? fp.substring(fp.lastIndexOf('/') + 1) : fp;
            if (fpTail.equals(inc)) {
                finding = "رقم Incremental يطابق نهاية بصمة البناء العامة.";
            } else {
                finding = "القيمتان مختلفتان. هذا يستحق المراجعة لكنه لا يثبت وحده تعديل ROM؛ بعض الشركات تستخدم صيغًا مختلفة.";
            }
        }
        addItem(report, "النتيجة", finding);

        addSection("تطبيق تحديث النظام");
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo("com.android.updater", 0);
            ApplicationInfo ai = pm.getApplicationInfo("com.android.updater", 0);
            String version = pi.versionName == null ? "غير معروف" : pi.versionName;
            boolean enabled = pm.getApplicationEnabledSetting("com.android.updater") != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    && pm.getApplicationEnabledSetting("com.android.updater") != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    && ai.enabled;
            addItem(report, "الحالة", "مثبّت");
            addItem(report, "إصدار التطبيق", version);
            addItem(report, "مفعّل", enabled ? "نعم" : "لا");
            Intent launch = pm.getLaunchIntentForPackage("com.android.updater");
            addItem(report, "واجهة تشغيل معروفة", launch != null ? "نعم" : "لم يتم العثور على واجهة تشغيل عامة");
        } catch (PackageManager.NameNotFoundException e) {
            addItem(report, "الحالة", "لم يُعثر على com.android.updater");
        }

        addSection("حدود الفحص");
        addItem(report, "صلاحيات الروت", "غير مستخدمة");
        addItem(report, "IMEI / الرقم التسلسلي", "لم تتم قراءتهما");
        addItem(report, "حالة Verified Boot الدقيقة", "غير متاحة لتطبيق عادي دون صلاحيات إضافية");
        addItem(report, "خصائص system/vendor المحمية", "لا يمكن ضمان قراءتها من تطبيق عادي");

        latestReport = report.toString();
        Toast.makeText(this, "اكتمل الفحص", Toast.LENGTH_SHORT).show();
    }

    private void addSection(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setTextColor(0xFF174EA6);
        tv.setPadding(0, dp(18), 0, dp(8));
        content.addView(tv);
    }

    private void addItem(StringBuilder report, String label, String value) {
        report.append(label).append(": ").append(value).append("\n");
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(0xFFFFFFFF);
        TextView heading = new TextView(this);
        heading.setText(label);
        heading.setTextSize(13);
        heading.setTextColor(0xFF667085);
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(15);
        valueView.setTextColor(0xFF182230);
        valueView.setTextIsSelectable(true);
        valueView.setPadding(0, dp(4), 0, 0);
        card.addView(heading);
        card.addView(valueView);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(7);
        content.addView(card, params);
    }

    private void shareReport() {
        if (latestReport == null || latestReport.isEmpty()) runScan();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, "Redmi System Inspector report");
        send.putExtra(Intent.EXTRA_TEXT, latestReport);
        try {
            startActivity(Intent.createChooser(send, "مشاركة تقرير الفحص"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "لا يوجد تطبيق لمشاركة التقرير", Toast.LENGTH_LONG).show();
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "غير متاح" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
