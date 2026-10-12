package app.netcheck.lite;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final String IP_CHECK_URL = "https://api.ipify.org";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean checking = new AtomicBoolean(false);

    private TextView statusText;
    private TextView ipText;
    private TextView latencyText;
    private TextView checkedAtText;
    private Button checkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(9, 15, 29));
        scroll.addView(root);

        TextView title = text("NETCHECK", 29, Color.rgb(74, 222, 187), true);
        root.addView(title);

        TextView subtitle = text("LITE  ·  KIỂM TRA KẾT NỐI MẠNG", 13,
                Color.rgb(174, 188, 207), true);
        addTop(root, subtitle, 6);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.rgb(19, 29, 48));
        cardBg.setCornerRadius(dp(18));
        card.setBackground(cardBg);
        addTop(root, card, 24);

        statusText = text("Trạng thái\nChưa kiểm tra", 19, Color.WHITE, true);
        card.addView(statusText);

        addTop(card, text("IP CÔNG KHAI HIỆN TẠI", 12,
                Color.rgb(174, 188, 207), true), 22);
        ipText = text("—", 22, Color.rgb(74, 222, 187), true);
        addTop(card, ipText, 6);

        addTop(card, text("THỜI GIAN PHẢN HỒI HTTPS", 12,
                Color.rgb(174, 188, 207), true), 20);
        latencyText = text("—", 22, Color.WHITE, true);
        addTop(card, latencyText, 6);

        checkedAtText = text("Lần kiểm tra gần nhất: chưa có", 13,
                Color.rgb(174, 188, 207), false);
        addTop(card, checkedAtText, 18);

        checkButton = button("Kiểm tra kết nối", Color.rgb(15, 140, 112));
        addTop(root, checkButton, 20);
        checkButton.setOnClickListener(v -> checkConnection());

        TextView note = text(
                "CÁCH HOẠT ĐỘNG\nỨng dụng gửi một yêu cầu HTTPS đến dịch vụ kiểm tra IP, " +
                "rồi hiển thị IP mà dịch vụ nhìn thấy và thời gian phản hồi của yêu cầu đó. " +
                "Kết quả có thể thay đổi theo mạng đang dùng. Đây không phải bài đo tốc độ " +
                "download/upload. Ứng dụng không sử dụng proxy, không tạo VPN và không thay " +
                "đổi kết nối của trình duyệt hay ứng dụng khác.",
                13, Color.rgb(174, 188, 207), false);
        addTop(root, note, 24);

        TextView footer = text("NetCheck Lite · Không cần tài khoản", 12,
                Color.rgb(115, 132, 155), false);
        footer.setGravity(Gravity.CENTER);
        addTop(root, footer, 24);

        setContentView(scroll);
    }

    private void checkConnection() {
        if (!checking.compareAndSet(false, true)) return;

        checkButton.setEnabled(false);
        statusText.setText("Trạng thái\nĐang kiểm tra HTTPS…");
        ipText.setText("Đang lấy IP…");
        latencyText.setText("Đang đo…");

        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            String ip = null;
            long elapsedMs = -1L;
            String failure = null;
            long startNs = System.nanoTime();

            try {
                connection = (HttpURLConnection) new URL(IP_CHECK_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "text/plain");
                connection.setRequestProperty("User-Agent", "NetCheckLite/1.0");

                int responseCode = connection.getResponseCode();
                elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    failure = "Máy chủ trả về HTTP " + responseCode;
                } else {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line = reader.readLine();
                        if (line != null) {
                            line = line.trim();
                            if (line.length() <= 64 && line.matches("[0-9a-fA-F:.]+")) {
                                ip = line;
                            }
                        }
                    }
                    if (ip == null) failure = "Phản hồi IP không hợp lệ.";
                }
            } catch (Exception e) {
                elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                failure = "Không thể kết nối. Hãy kiểm tra Wi-Fi/4G rồi thử lại.";
            } finally {
                if (connection != null) connection.disconnect();
            }

            final String resultIp = ip;
            final long resultMs = elapsedMs;
            final String resultFailure = failure;
            final String time = java.text.DateFormat.getTimeInstance(
                    java.text.DateFormat.SHORT).format(new java.util.Date());

            handler.post(() -> {
                checking.set(false);
                checkButton.setEnabled(true);
                checkedAtText.setText("Lần kiểm tra gần nhất: " + time);

                if (resultIp != null) {
                    statusText.setText("Trạng thái\nKết nối HTTPS thành công");
                    ipText.setText(resultIp);
                    latencyText.setText(resultMs + " ms");
                } else {
                    statusText.setText("Trạng thái\nChưa kết nối được");
                    ipText.setText("Không xác định");
                    latencyText.setText(resultMs >= 0 ? resultMs + " ms (thất bại)" : "—");
                    android.widget.Toast.makeText(MainActivity.this, resultFailure,
                            android.widget.Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(13));
        b.setBackground(bg);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        return b;
    }

    private void addTop(LinearLayout parent, View child, int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(marginDp);
        parent.addView(child, lp);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        checking.set(false);
        networkExecutor.shutdownNow();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
