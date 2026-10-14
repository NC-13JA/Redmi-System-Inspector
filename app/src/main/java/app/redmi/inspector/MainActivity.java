package app.redmi.inspector;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private LinearLayout content;
    private LinearLayout romResults;
    private AutoCompleteTextView codenameInput;
    private EditText versionFilterInput;
    private TextView romStatus;
    private String latestReport = "";
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private static final String TRACKER_API =
            "https://api.github.com/repos/XiaomiFirmwareUpdater/miui-updates-tracker/contents/rss";
    private static final String TRACKER_RSS =
            "https://raw.githubusercontent.com/XiaomiFirmwareUpdater/miui-updates-tracker/master/rss/";
    private static final String OFFICIAL_PORTAL =
            "https://new.c.mi.com/global/miuidownload/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        loadDeviceCodenames();
        runScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdownNow();
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
        subtitle.setText("تشخيص النظام والبحث عن رومات شاومي المنشورة");
        subtitle.setTextSize(15);
        subtitle.setTextColor(0xFF536078);
        subtitle.setPadding(0, dp(8), 0, dp(18));
        page.addView(subtitle);

        TextView romTitle = new TextView(this);
        romTitle.setText("البحث في سجل الرومات");
        romTitle.setTextSize(19);
        romTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        romTitle.setTextColor(0xFF174EA6);
        romTitle.setPadding(0, dp(8), 0, dp(8));
        page.addView(romTitle);

        TextView romHelp = new TextView(this);
        romHelp.setText("ابحث باستخدام رمز الجهاز (codename) لأي جهاز شاومي، مثل zircon. يمكنك إضافة رمز إصدار للعثور على حزمة محددة. المصدر فهرس مجتمعي وليس خادم شاومي الرسمي.");
        romHelp.setTextSize(13);
        romHelp.setTextColor(0xFF536078);
        page.addView(romHelp);

        codenameInput = new AutoCompleteTextView(this);
        codenameInput.setSingleLine(true);
        codenameInput.setText(Build.DEVICE == null ? "" : Build.DEVICE);
        codenameInput.setHint("رمز الجهاز، مثل zircon");
        codenameInput.setThreshold(1);
        codenameInput.setTextSize(16);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(-1, dp(52));
        fieldParams.topMargin = dp(10);
        page.addView(codenameInput, fieldParams);

        versionFilterInput = new EditText(this);
        versionFilterInput.setSingleLine(true);
        versionFilterInput.setHint("فلترة اختيارية: رقم الإصدار أو رمز البناء");
        versionFilterInput.setTextSize(14);
        LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-1, dp(50));
        filterParams.topMargin = dp(6);
        page.addView(versionFilterInput, filterParams);

        LinearLayout searchButtons = new LinearLayout(this);
        searchButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button search = new Button(this);
        search.setText("بحث عن الرومات");
        search.setOnClickListener(v -> searchRoms());
        searchButtons.addView(search, new LinearLayout.LayoutParams(0, dp(50), 1f));
        Button thisPhone = new Button(this);
        thisPhone.setText("جهازي");
        thisPhone.setOnClickListener(v -> {
            codenameInput.setText(Build.DEVICE == null ? "" : Build.DEVICE);
            versionFilterInput.setText("");
            searchRoms();
        });
        searchButtons.addView(thisPhone, new LinearLayout.LayoutParams(0, dp(50), 1f));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, -2);
        searchParams.topMargin = dp(4);
        page.addView(searchButtons, searchParams);

        Button officialPortal = new Button(this);
        officialPortal.setText("فتح بوابة رومات شاومي الرسمية");
        officialPortal.setOnClickListener(v -> openUrl(OFFICIAL_PORTAL));
        page.addView(officialPortal, new LinearLayout.LayoutParams(-1, dp(48)));

        romStatus = new TextView(this);
        romStatus.setText("جارٍ تحميل قائمة رموز الأجهزة…");
        romStatus.setTextSize(13);
        romStatus.setTextColor(0xFF536078);
        romStatus.setPadding(0, dp(8), 0, dp(8));
        page.addView(romStatus);

        romResults = new LinearLayout(this);
        romResults.setOrientation(LinearLayout.VERTICAL);
        page.addView(romResults);

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
        LinearLayout.LayoutParams diagnosticButtonsParams = new LinearLayout.LayoutParams(-1, -2);
        diagnosticButtonsParams.topMargin = dp(12);
        page.addView(buttons, diagnosticButtonsParams);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(12), 0, 0);
        page.addView(content);

        TextView note = new TextView(this);
        note.setText("مهم: التطبيق يبحث ويقارن فقط، ولا يثبت الرومات أو يتجاوز حماية التوقيع. وجود حزمة في سجل لا يعني أنها متوافقة مع جهازك أو منطقتك. لا تثبّت حزمة قبل التحقق من الطراز والمنطقة ونوع الحزمة.");
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

    private void loadDeviceCodenames() {
        networkExecutor.execute(() -> {
            try {
                ArrayList<String> codes = new ArrayList<>();
                for (int page = 1; page <= 20; page++) {
                    String jsonText = fetchText(TRACKER_API + "?per_page=100&page=" + page);
                    JSONArray arr = new JSONArray(jsonText);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.optJSONObject(i);
                        if (item == null) continue;
                        String name = item.optString("name", "");
                        if (name.toLowerCase(Locale.ROOT).endsWith(".xml")) {
                            codes.add(name.substring(0, name.length() - 4));
                        }
                    }
                    if (arr.length() < 100) break;
                }
                codes.sort(String.CASE_INSENSITIVE_ORDER);
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this, android.R.layout.simple_dropdown_item_1line, codes);
                    codenameInput.setAdapter(adapter);
                    romStatus.setText("تم تحميل " + codes.size() + " رمز جهاز. اكتب الرمز واختره من الاقتراحات، أو ابحث مباشرة.");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing()) romStatus.setText("تعذّر تحميل قائمة الأجهزة. يمكنك إدخال codename يدويًا أو فتح بوابة شاومي الرسمية.");
                });
            }
        });
    }

    private void searchRoms() {
        String code = codenameInput.getText().toString().trim();
        String filter = versionFilterInput.getText().toString().trim();
        if (!code.matches("[A-Za-z0-9_-]{2,80}")) {
            Toast.makeText(this, "أدخل رمز جهاز صالحًا مثل zircon", Toast.LENGTH_LONG).show();
            return;
        }
        romResults.removeAllViews();
        romStatus.setText("جارٍ البحث في RSS وأرشيف MIUI وHyperOS للجهاز " + code + "…");
        networkExecutor.execute(() -> {
            ArrayList<RomEntry> all = new ArrayList<>();
            ArrayList<String> sourceErrors = new ArrayList<>();
            int successfulSources = 0;

            try {
                ArrayList<RomEntry> rss = parseRss(fetchText(TRACKER_RSS + code + ".xml"));
                for (RomEntry item : rss) {
                    item.sourceName = "RSS مجتمعي";
                    all.add(item);
                }
                successfulSources++;
            } catch (Exception e) {
                sourceErrors.add("RSS: " + conciseError(e));
            }

            String[] archiveUrls = {
                    "https://xmfirmwareupdater.com/archive/hyperos/" + code.toLowerCase(Locale.ROOT) + "/",
                    "https://xmfirmwareupdater.com/archive/miui/" + code.toLowerCase(Locale.ROOT) + "/"
            };
            String[] archiveNames = {"أرشيف HyperOS", "أرشيف MIUI"};
            for (int i = 0; i < archiveUrls.length; i++) {
                try {
                    String html = fetchText(archiveUrls[i]);
                    ArrayList<RomEntry> archived = parseArchiveHtml(html, archiveUrls[i], archiveNames[i]);
                    all.addAll(archived);
                    successfulSources++;
                } catch (Exception e) {
                    sourceErrors.add(archiveNames[i] + ": " + conciseError(e));
                }
            }

            ArrayList<RomEntry> unique = deduplicateRoms(all);
            ArrayList<RomEntry> found = new ArrayList<>();
            for (RomEntry rom : unique) {
                String haystack = (rom.title + "\n" + rom.description + "\n" + rom.date).toLowerCase(Locale.ROOT);
                if (filter.isEmpty() || haystack.contains(filter.toLowerCase(Locale.ROOT))) {
                    found.add(rom);
                }
            }
            found.sort((a, b) -> {
                int dateCompare = safe(b.date).compareToIgnoreCase(safe(a.date));
                return dateCompare != 0 ? dateCompare : a.title.compareToIgnoreCase(b.title);
            });

            final int rawCount = all.size();
            final int uniqueCount = unique.size();
            final int sourceCount = successfulSources;
            final String errors = sourceErrors.isEmpty() ? "" : "\nتعذّر الوصول إلى بعض المصادر: " + String.join("؛ ", sourceErrors);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                romResults.removeAllViews();
                if (found.isEmpty()) {
                    String message;
                    if (uniqueCount == 0) {
                        message = "لم تُسترجع نتائج من المصادر الثلاثة. تحقق من الإنترنت ورمز الجهاز. لا يعني هذا أن الجهاز لا يملك رومات منشورة.";
                    } else {
                        message = "لم تطابق النتائج الفلتر الحالي. امسح حقل الإصدار لعرض كل النتائج.";
                    }
                    romStatus.setText(message + "\nالمصادر التي استجابت: " + sourceCount + "/3" + errors);
                    return;
                }

                java.util.LinkedHashMap<String, Integer> regionCounts = new java.util.LinkedHashMap<>();
                for (RomEntry item : found) {
                    String region = classifyRegion(item.title + " " + item.description);
                    regionCounts.put(region, regionCounts.containsKey(region) ? regionCounts.get(region) + 1 : 1);
                }
                StringBuilder summary = new StringBuilder();
                summary.append("النتائج المطابقة: ").append(found.size())
                        .append(" | الفريدة قبل الفلترة: ").append(uniqueCount)
                        .append(" | المصادر المستجيبة: ").append(sourceCount).append("/3")
                        .append("\nنسخة الجهاز المثبتة: ").append(safe(Build.VERSION.INCREMENTAL))
                        .append("\nمنطقة بصمة النظام: ").append(classifyRegion(safe(Build.FINGERPRINT)))
                        .append("\nتنبيه: اختلاف المنطقة ليس حكمًا على التوافق أو توفر OTA.");
                if (!filter.isEmpty()) summary.append("\nفلتر الإصدار: ").append(filter);
                summary.append("\nالتوزيع حسب المنطقة: ");
                boolean first = true;
                for (java.util.Map.Entry<String, Integer> entry : regionCounts.entrySet()) {
                    if (!first) summary.append("، ");
                    summary.append(entry.getKey()).append(" ").append(entry.getValue());
                    first = false;
                }
                summary.append(errors);
                romStatus.setText(summary.toString());
                for (RomEntry rom : found) addRomCard(rom, filter);
            });
        });
    }

    private ArrayList<RomEntry> parseArchiveHtml(String html, String archiveUrl, String sourceName) {
        ArrayList<RomEntry> entries = new ArrayList<>();
        if (html == null || html.isEmpty()) return entries;
        Matcher rowMatcher = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(html);
        Pattern cellPattern = Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>");
        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            Matcher cellMatcher = cellPattern.matcher(row);
            ArrayList<String> cells = new ArrayList<>();
            while (cellMatcher.find()) {
                String cellText = stripHtml(cellMatcher.group(1));
                if (!cellText.isEmpty()) cells.add(cellText);
            }
            if (cells.size() < 4) continue;
            String joined = String.join(" | ", cells);
            if (!extractVersion(joined).isEmpty()) {
                RomEntry entry = new RomEntry();
                entry.title = joined;
                entry.link = archiveUrl;
                entry.description = row;
                entry.sourceName = sourceName;
                Matcher dateMatcher = Pattern.compile("\\b20\\d{2}-\\d{2}-\\d{2}\\b").matcher(joined);
                if (dateMatcher.find()) entry.date = dateMatcher.group();
                entries.add(entry);
            }
        }
        return entries;
    }

    private ArrayList<RomEntry> deduplicateRoms(ArrayList<RomEntry> input) {
        java.util.LinkedHashMap<String, RomEntry> map = new java.util.LinkedHashMap<>();
        for (RomEntry item : input) {
            String version = extractVersion(item.title + " " + item.description);
            String type = classifyPackageType(item.title + " " + item.description);
            String region = classifyRegion(item.title + " " + item.description);
            String key = !version.isEmpty()
                    ? version.toUpperCase(Locale.ROOT) + "|" + type + "|" + region
                    : item.title.trim().toLowerCase(Locale.ROOT);
            RomEntry old = map.get(key);
            if (old == null) {
                map.put(key, item);
            } else if ((old.description == null || old.description.isEmpty())
                    && item.description != null && !item.description.isEmpty()) {
                map.put(key, item);
            }
        }
        return new ArrayList<>(map.values());
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("(?is)<script\\b.*?</script>", " ")
                .replaceAll("(?is)<style\\b.*?</style>", " ")
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("(?i)</(td|th|p|div|li)>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ").replace("&#39;", "'")
                .replace("&amp;", "&").replace("&quot;", "\"")
                .replaceAll("\\s+", " ").trim();
    }

    private String extractVersion(String text) {
        if (text == null) return "";
        Matcher matcher = Pattern.compile("(?i)(?:OS|V)\\d[0-9A-Z.]*?(?:EUXM|MIXM|CNXM|INXM|RUXM|TRXM|TWXM|IDXM|JPXM|KRXM|TWM|WXM|XM)").matcher(text);
        return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : "";
    }

    private String classifyRegion(String text) {
        String upper = text == null ? "" : text.toUpperCase(Locale.ROOT);
        String version = extractVersion(upper);
        if (!version.isEmpty()) upper = version;
        if (upper.matches("(?s).*EUXM.*")) return "EEA/أوروبا";
        if (upper.matches("(?s).*MIXM.*")) return "Global/عالمي";
        if (upper.matches("(?s).*CNXM.*")) return "China/الصين";
        if (upper.matches("(?s).*INXM.*")) return "India/الهند";
        if (upper.matches("(?s).*RUXM.*")) return "Russia/روسيا";
        if (upper.matches("(?s).*TRXM.*")) return "Turkey/تركيا";
        if (upper.matches("(?s).*TWXM.*")) return "Taiwan/تايوان";
        if (upper.matches("(?s).*IDXM.*")) return "Indonesia/إندونيسيا";
        if (upper.matches("(?s).*JPXM.*")) return "Japan/اليابان";
        if (upper.matches("(?s).*KRXM.*")) return "Korea/كوريا";
        return "غير محددة";
    }

    private String classifyPackageType(String text) {
        String upper = text == null ? "" : text.toUpperCase(Locale.ROOT);
        if (upper.contains("FASTBOOT")) return "Fastboot";
        if (upper.contains("RECOVERY")) return "Recovery";
        if (upper.contains("OTA") || upper.contains("INCREMENTAL")) return "OTA/تحديث جزئي";
        return "غير محدد";
    }

    private String conciseError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) return error.getClass().getSimpleName();
        return message.length() > 90 ? message.substring(0, 90) : message;
    }

    private ArrayList<RomEntry> parseRss(String xml) throws Exception {
        ArrayList<RomEntry> entries = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml));
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                RomEntry entry = new RomEntry();
                boolean inside = true;
                while (inside) {
                    event = parser.next();
                    if (event == XmlPullParser.END_TAG && "item".equals(parser.getName())) {
                        inside = false;
                    } else if (event == XmlPullParser.START_TAG) {
                        String tag = parser.getName();
                        if ("title".equals(tag)) entry.title = readTagText(parser);
                        else if ("link".equals(tag)) entry.link = readTagText(parser);
                        else if ("pubDate".equals(tag)) entry.date = readTagText(parser);
                        else if ("description".equals(tag)) entry.description = readTagText(parser);
                    } else if (event == XmlPullParser.END_DOCUMENT) {
                        inside = false;
                    }
                }
                if (entry.title != null && !entry.title.trim().isEmpty()) entries.add(entry);
            }
            event = parser.next();
        }
        return entries;
    }

    private String readTagText(XmlPullParser parser) {
        try {
            return parser.nextText();
        } catch (Exception e) {
            return "";
        }
    }

    private void addRomCard(RomEntry rom, String filter) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.bottomMargin = dp(8);

        TextView heading = new TextView(this);
        heading.setText(rom.title);
        heading.setTextSize(15);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(0xFF182230);
        heading.setTextIsSelectable(true);
        card.addView(heading);

        TextView metadata = new TextView(this);
        String romRegion = classifyRegion(rom.title + " " + rom.description);
        String installedRegion = classifyRegion(safe(Build.FINGERPRINT));
        String regionComparison = "غير محدد";
        if (!"غير محددة".equals(romRegion) && !"غير محددة".equals(installedRegion)) {
            regionComparison = romRegion.equals(installedRegion)
                    ? "رمز المنطقة مماثل لبصمة الجهاز"
                    : "رمز المنطقة مختلف عن بصمة الجهاز";
        }
        metadata.setText("المنطقة: " + romRegion
                + " | النوع: " + classifyPackageType(rom.title + " " + rom.description)
                + " | " + regionComparison
                + " | المصدر: " + safe(rom.sourceName));
        metadata.setTextSize(12);
        metadata.setTextColor(0xFF2456A6);
        metadata.setPadding(0, dp(5), 0, dp(3));
        card.addView(metadata);

        String version = extractVersion(rom.title + " " + rom.description);
        if (!version.isEmpty()) {
            TextView versionView = new TextView(this);
            versionView.setText("رمز الإصدار: " + version);
            versionView.setTextSize(12);
            versionView.setTextColor(0xFF344054);
            versionView.setTextIsSelectable(true);
            card.addView(versionView);
        }

        if (rom.date != null && !rom.date.isEmpty()) {
            TextView date = new TextView(this);
            date.setText("تاريخ النشر: " + rom.date);
            date.setTextSize(12);
            date.setTextColor(0xFF667085);
            date.setPadding(0, dp(5), 0, dp(3));
            card.addView(date);
        }

        if (!filter.isEmpty()) {
            TextView match = new TextView(this);
            match.setText("تطابق مع الفلتر: " + filter);
            match.setTextSize(12);
            match.setTextColor(0xFF16794B);
            card.addView(match);
        }

        ArrayList<String> urls = extractOfficialFirmwareUrls(rom.description);
        for (String url : urls) {
            Button download = new Button(this);
            download.setText("فتح رابط حزمة شاومي الرسمية");
            download.setOnClickListener(v -> openUrl(url));
            card.addView(download, new LinearLayout.LayoutParams(-1, dp(44)));
        }

        if (rom.link != null && rom.link.startsWith("https://")) {
            Button details = new Button(this);
            details.setText("تفاصيل الإصدار في سجل المصدر");
            details.setOnClickListener(v -> openUrl(rom.link));
            card.addView(details, new LinearLayout.LayoutParams(-1, dp(44)));
        }

        if (urls.isEmpty()) {
            TextView noDirect = new TextView(this);
            noDirect.setText("لم أستخرج رابط تنزيل مباشر من هذا السجل. افتح تفاصيل الإصدار أو استخدم بوابة شاومي الرسمية.");
            noDirect.setTextSize(12);
            noDirect.setTextColor(0xFF7A4D00);
            noDirect.setPadding(0, dp(6), 0, dp(4));
            card.addView(noDirect);
        }
        romResults.addView(card, cardParams);
    }

    private ArrayList<String> extractOfficialFirmwareUrls(String html) {
        ArrayList<String> result = new ArrayList<>();
        if (html == null || html.isEmpty()) return result;
        Pattern pattern = Pattern.compile("(?i)href\\s*=\\s*[\"'](https?://[^\"']+)[\"']");
        Matcher matcher = pattern.matcher(html);
        Set<String> unique = new HashSet<>();
        while (matcher.find()) {
            String url = matcher.group(1).replace("&amp;", "&").trim();
            try {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();
                String path = uri.getPath();
                if (host == null || path == null) continue;
                host = host.toLowerCase(Locale.ROOT);
                boolean officialHost = host.equals("mi.com") || host.endsWith(".mi.com") || host.endsWith(".miui.com");
                boolean zipFile = path.toLowerCase(Locale.ROOT).contains(".zip");
                if (officialHost && zipFile && unique.add(url)) result.add(url);
            } catch (Exception ignored) { }
        }
        return result;
    }

    private String fetchText(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Redmi-System-Inspector/0.2");
        connection.setRequestProperty("Accept", "application/json, application/xml, text/xml, */*");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new java.io.IOException("HTTP " + status);
            try (InputStream input = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) text.append(line).append('\n');
                return text.toString();
            }
        } finally {
            connection.disconnect();
        }
    }

    private void openUrl(String address) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "لا يوجد تطبيق لفتح الرابط", Toast.LENGTH_LONG).show();
        }
    }

    private static class RomEntry {
        String title = "";
        String link = "";
        String date = "";
        String description = "";
        String sourceName = "سجل مجتمعي";
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

        java.util.Map<String, String> props = readSystemProperties();
        addSection("خصائص النظام منخفضة المستوى (قراءة فقط)");
        String systemFp = propertyOrUnavailable(props, "ro.system.build.fingerprint");
        String vendorFp = propertyOrUnavailable(props, "ro.vendor.build.fingerprint");
        addItem(report, "بصمة system", systemFp);
        addItem(report, "بصمة vendor", vendorFp);
        addItem(report, "اسم المنتج", propertyOrUnavailable(props, "ro.product.name"));
        addItem(report, "طراز المنتج", propertyOrUnavailable(props, "ro.product.model"));
        addItem(report, "جهاز المنتج", propertyOrUnavailable(props, "ro.product.device"));
        addItem(report, "جهاز vendor", propertyOrUnavailable(props, "ro.product.vendor.device"));
        addItem(report, "منطقة MIUI/HyperOS", propertyOrUnavailable(props, "ro.miui.region"));
        addItem(report, "منطقة المنتج", propertyOrUnavailable(props, "ro.product.locale.region"));
        addItem(report, "حالة Verified Boot", propertyOrUnavailable(props, "ro.boot.verifiedbootstate"));
        addItem(report, "حالة vbmeta", propertyOrUnavailable(props, "ro.boot.vbmeta.device_state"));
        addItem(report, "قفل التفليش", propertyOrUnavailable(props, "ro.boot.flash.locked"));
        addItem(report, "وضع dm-verity", propertyOrUnavailable(props, "ro.boot.veritymode"));
        addItem(report, "نوع البناء", propertyOrUnavailable(props, "ro.build.type"));
        addItem(report, "وسوم البناء", propertyOrUnavailable(props, "ro.build.tags"));
        addItem(report, "معرّف تعديل المنطقة", propertyOrUnavailable(props, "ro.product.mod_device"));

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
        String systemDeviceFromFp = fingerprintDevice(systemFp);
        String vendorDeviceFromFp = fingerprintDevice(vendorFp);
        String publicDevice = safe(Build.DEVICE);
        addItem(report, "مقارنة device مع بصمة system",
                compareDeviceNames(publicDevice, systemDeviceFromFp));
        addItem(report, "مقارنة device مع بصمة vendor",
                compareDeviceNames(publicDevice, vendorDeviceFromFp));
        String productDevice = props.get("ro.product.device");
        String vendorProductDevice = props.get("ro.product.vendor.device");
        if (productDevice != null && !productDevice.trim().isEmpty()
                && vendorProductDevice != null && !vendorProductDevice.trim().isEmpty()) {
            addItem(report, "مقارنة product.device مع vendor.device",
                    compareDeviceNames(productDevice, vendorProductDevice));
        } else {
            addItem(report, "مقارنة product.device مع vendor.device", "لا تتوفر القيمتان للمقارنة.");
        }
        addItem(report, "تفسير اختلافات البناء",
                "الاختلافات مؤشرات تحتاج إلى مقارنة مع سجل رسمي للطراز والمنطقة؛ لا تثبت وحدها أن النظام معدل أو غير أصلي.");


        addSection("تطبيق تحديث النظام");
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo("com.android.updater", 0);
            ApplicationInfo ai = pm.getApplicationInfo("com.android.updater", 0);
            String version = pi.versionName == null ? "غير معروف" : pi.versionName;
            int enabledState = pm.getApplicationEnabledSetting("com.android.updater");
            boolean enabled = enabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    && enabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
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



    private String fingerprintDevice(String fingerprint) {
        if (fingerprint == null || fingerprint.trim().isEmpty() || "غير متاح".equals(fingerprint)
                || "غير متاح للتطبيق".equals(fingerprint)) return "";
        int colon = fingerprint.indexOf(':');
        String head = colon >= 0 ? fingerprint.substring(0, colon) : fingerprint;
        String[] parts = head.split("/");
        return parts.length >= 3 ? parts[2] : "";
    }

    private String compareDeviceNames(String first, String second) {
        if (first == null || second == null || first.trim().isEmpty() || second.trim().isEmpty()
                || "غير متاح".equals(first) || "غير متاح للتطبيق".equals(first)
                || "غير متاح".equals(second) || "غير متاح للتطبيق".equals(second)) {
            return "غير كافٍ للمقارنة؛ إحدى القيم غير متاحة.";
        }
        if (first.trim().equalsIgnoreCase(second.trim())) return "متطابقان: " + first;
        return "اختلاف: " + first + " مقابل " + second
                + " — يحتاج إلى تحقق إضافي ولا يثبت وحده تعديل النظام.";
    }

    private java.util.Map<String, String> readSystemProperties() {
        java.util.Map<String, String> values = new java.util.HashMap<>();
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/getprop").redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                Pattern pattern = Pattern.compile("^\\[([^\\]]+)\\]:\\s*\\[(.*)\\]$");
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line.trim());
                    if (matcher.matches()) values.put(matcher.group(1), matcher.group(2));
                }
            }
            process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Some Android builds restrict process execution; all fields remain safely unavailable.
        } finally {
            if (process != null) process.destroy();
        }
        return values;
    }

    private String propertyOrUnavailable(java.util.Map<String, String> properties, String key) {
        String value = properties.get(key);
        return value == null || value.trim().isEmpty() ? "غير متاح للتطبيق" : value;
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
