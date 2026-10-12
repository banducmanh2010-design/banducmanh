package app.proxy.lab;

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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String PROXY_LIST_URL =
            "https://cdn.jsdelivr.net/gh/proxyscrape/free-proxy-list@main/proxies/protocols/http/data.txt";
    private static final String IP_CHECK_URL = "https://api.ipify.org";
    private static final int MAX_PROXIES = 200;
    private static final int MAX_TRIES_PER_CYCLE = 6;
    private static final long ROTATION_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final List<InetSocketAddress> proxies = new ArrayList<>();

    private TextView statusText;
    private TextView countText;
    private TextView proxyText;
    private TextView ipText;
    private Button loadButton;
    private Button startButton;
    private Button stopButton;
    private Button testButton;
    private int currentIndex = -1;
    private boolean rotating = false;

    private final Runnable rotationTask = new Runnable() {
        @Override
        public void run() {
            if (!rotating) return;
            rotateAndTestNext();
            handler.postDelayed(this, ROTATION_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(9, 15, 29));
        scrollView.addView(root);

        TextView title = text("PROXY LAB", 27, Color.rgb(74, 222, 187), true);
        root.addView(title);
        TextView subtitle = text("HTTP proxy checker · Chu kỳ 60 giây", 14,
                Color.rgb(174, 188, 207), false);
        LinearLayout.LayoutParams subtitleParams = params(-1, -2);
        subtitleParams.topMargin = dp(5);
        root.addView(subtitle, subtitleParams);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable panelBg = new GradientDrawable();
        panelBg.setColor(Color.rgb(19, 29, 48));
        panelBg.setCornerRadius(dp(16));
        panel.setBackground(panelBg);
        LinearLayout.LayoutParams panelParams = params(-1, -2);
        panelParams.topMargin = dp(22);
        root.addView(panel, panelParams);

        statusText = text("Trạng thái: chưa tải danh sách", 16, Color.WHITE, true);
        panel.addView(statusText);
        countText = text("Proxy đã tải: 0", 14, Color.rgb(174, 188, 207), false);
        addTop(panel, countText, 12);
        proxyText = text("Proxy hiện tại: —", 14, Color.rgb(220, 230, 242), false);
        addTop(panel, proxyText, 12);
        ipText = text("IP đầu ra: chưa kiểm tra", 18, Color.rgb(74, 222, 187), true);
        addTop(panel, ipText, 12);

        loadButton = button("1. Tải danh sách proxy", Color.rgb(42, 90, 145));
        startButton = button("2. Bật xoay mỗi 60 giây", Color.rgb(15, 140, 112));
        testButton = button("Kiểm tra proxy kế tiếp", Color.rgb(63, 74, 98));
        stopButton = button("Dừng xoay", Color.rgb(142, 57, 69));
        addTop(root, loadButton, 20);
        addTop(root, startButton, 10);
        addTop(root, testButton, 10);
        addTop(root, stopButton, 10);

        TextView notice = text(
                "LƯU Ý AN TOÀN\nProxy miễn phí không đáng tin cậy và có thể ngừng hoạt động. " +
                "Không dùng để đăng nhập, gửi mật khẩu/cookie hoặc dữ liệu riêng tư. " +
                "Ứng dụng chỉ dùng proxy cho yêu cầu kiểm tra IP của chính nó, không đổi IP " +
                "cho Chrome hay các ứng dụng khác. Xoay tự động chỉ chạy khi ứng dụng đang mở.",
                13, Color.rgb(174, 188, 207), false);
        addTop(root, notice, 22);

        loadButton.setOnClickListener(v -> loadProxyList());
        startButton.setOnClickListener(v -> startRotation());
        testButton.setOnClickListener(v -> rotateAndTestNext());
        stopButton.setOnClickListener(v -> stopRotation());

        setContentView(scrollView);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(12));
        button.setBackground(bg);
        button.setPadding(dp(12), dp(7), dp(12), dp(7));
        return button;
    }

    private void addTop(LinearLayout parent, View child, int marginDp) {
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(marginDp);
        parent.addView(child, lp);
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void loadProxyList() {
        setStatus("Đang tải danh sách proxy công khai…");
        loadButton.setEnabled(false);
        networkExecutor.execute(() -> {
            List<InetSocketAddress> downloaded = new ArrayList<>();
            HttpURLConnection connection = null;
            String failure = null;
            try {
                connection = (HttpURLConnection) new URL(PROXY_LIST_URL).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "ProxyLab-Android/1.0");
                connection.setRequestProperty("Accept", "text/plain");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && downloaded.size() < MAX_PROXIES) {
                        InetSocketAddress address = parseProxy(line.trim());
                        if (address != null) downloaded.add(address);
                    }
                }
                if (downloaded.isEmpty()) failure = "Danh sách tải về trống hoặc không đúng định dạng.";
            } catch (Exception e) {
                failure = "Không tải được danh sách. Kiểm tra Internet rồi thử lại.";
            } finally {
                if (connection != null) connection.disconnect();
            }

            final String finalFailure = failure;
            final List<InetSocketAddress> finalList = downloaded;
            handler.post(() -> {
                loadButton.setEnabled(true);
                if (finalFailure != null) {
                    setStatus(finalFailure);
                    return;
                }
                proxies.clear();
                proxies.addAll(finalList);
                currentIndex = -1;
                countText.setText("Proxy đã tải: " + proxies.size() + " (giới hạn 200)");
                proxyText.setText("Proxy hiện tại: —");
                ipText.setText("IP đầu ra: chưa kiểm tra");
                setStatus("Đã tải danh sách. Bấm kiểm tra hoặc bật xoay.");
            });
        });
    }

    private InetSocketAddress parseProxy(String line) {
        if (line == null) return null;
        line = line.trim();
        // ProxyScrape's text list uses "http://IP:port".
        if (line.startsWith("http://")) {
            line = line.substring("http://".length());
        } else if (line.contains("://")) {
            return null;
        }
        if (!line.matches("\\d{1,3}(\\.\\d{1,3}){3}:\\d{1,5}")) {
            return null;
        }
        String[] parts = line.split(":");
        String[] octets = parts[0].split("\\.");
        for (String octet : octets) {
            int value;
            try {
                value = Integer.parseInt(octet);
            } catch (NumberFormatException e) {
                return null;
            }
            if (value < 0 || value > 255) return null;
        }
        try {
            int port = Integer.parseInt(parts[1]);
            if (port < 1 || port > 65535) return null;
            return new InetSocketAddress(parts[0], port);
        } catch (Exception e) {
            return null;
        }
    }

    private void startRotation() {
        if (proxies.isEmpty()) {
            setStatus("Hãy tải danh sách proxy trước.");
            return;
        }
        rotating = true;
        handler.removeCallbacks(rotationTask);
        setStatus("Đã bật xoay. Mỗi 60 giây sẽ chọn proxy kế tiếp.");
        rotationTask.run();
    }

    private void stopRotation() {
        rotating = false;
        handler.removeCallbacks(rotationTask);
        setStatus("Đã dừng xoay proxy.");
    }

    private void rotateAndTestNext() {
        if (proxies.isEmpty()) {
            setStatus("Hãy tải danh sách proxy trước.");
            return;
        }
        networkExecutor.execute(() -> {
            String foundIp = null;
            String selected = null;
            int attempted = 0;
            int tries = Math.min(MAX_TRIES_PER_CYCLE, proxies.size());

            while (attempted < tries) {
                currentIndex = (currentIndex + 1) % proxies.size();
                InetSocketAddress address = proxies.get(currentIndex);
                selected = address.getHostString() + ":" + address.getPort();
                final String selectedForUi = selected;
                final int number = attempted + 1;
                handler.post(() -> {
                    proxyText.setText("Đang thử: " + selectedForUi);
                    setStatus("Đang kiểm tra proxy " + number + "/" + tries + "…");
                });
                foundIp = checkIpThrough(address);
                attempted++;
                if (foundIp != null) break;
            }

            final String ipResult = foundIp;
            final String selectedResult = selected;
            final int total = attempted;
            handler.post(() -> {
                if (ipResult != null) {
                    proxyText.setText("Proxy hiện tại: " + selectedResult);
                    ipText.setText("IP đầu ra: " + ipResult);
                    setStatus("Proxy trả lời thành công.");
                } else {
                    proxyText.setText("Proxy gần nhất: " +
                            (selectedResult == null ? "—" : selectedResult));
                    ipText.setText("IP đầu ra: không xác định");
                    setStatus("Không tìm được proxy hoạt động trong " + total +
                            " lần thử. Thử lại sau hoặc tải danh sách mới.");
                }
            });
        });
    }

    private String checkIpThrough(InetSocketAddress address) {
        HttpURLConnection connection = null;
        try {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
            connection = (HttpURLConnection) new URL(IP_CHECK_URL).openConnection(proxy);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent", "ProxyLab-Android/1.0");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String ip = reader.readLine();
                if (ip == null) return null;
                ip = ip.trim();
                if (ip.length() > 64 || !ip.matches("[0-9a-fA-F:.]+")) return null;
                return ip;
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void setStatus(String value) {
        statusText.setText("Trạng thái: " + value);
    }

    @Override
    protected void onDestroy() {
        rotating = false;
        handler.removeCallbacks(rotationTask);
        networkExecutor.shutdownNow();
        super.onDestroy();
    }
}
