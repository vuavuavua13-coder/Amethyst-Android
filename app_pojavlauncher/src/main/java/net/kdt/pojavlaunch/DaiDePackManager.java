package net.kdt.pojavlaunch;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.kdt.pojavlaunch.utils.DownloadUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One-click client pack synchronizer for Đại Đế Tu Tiên Android.
 * Keeps the Android pack pinned to the same 1.20.1/Forge family as Launcher19.
 */
public final class DaiDePackManager {
    public static final String RSP_NAME = "DAIDE_RPG_RSP_BUILD29_1.20.1.zip";
    public static final String BRIDGE_NAME = "DaiDeRPG-ForgeBridge-BUILD29-CODEONLY.jar";
    public static final String WAIFU_NAME = "waifu_of_god-1.0.0-forge-1.20.1.jar";

    // Exact Modrinth version IDs. Do not silently upgrade these: the desktop pack is pinned.
    private static final String[][] MODRINTH = {
            {"curios", "nEba8UUT"},
            {"playeranimator", "xe2EVE6q"},
            {"cerbons-api", "XWZQbKsr"},
            {"bomd", "2Kw4xPhp"},
            {"geckolib", "aC5KMoNg"},
            {"cloth-config", "t8TXrZvZ"},
            {"born-in-chaos", "8wmEhUSt"},
            {"aquamirae", "72GwOBcB"},
            {"citadel", "lTAAe4sZ"},
            {"ice-and-fire", "EzN8KQYF"}
    };

    private static final CurseFile[] CURSE = {
            new CurseFile("fragmentum", "1123970", "8506891", "fragmentum-forge-1.20.1-1.5.2.jar"),
            new CurseFile("bosses-unleashed", "1392778", "7704181", "bossesunleashed-1.0.4-hotfix.jar"),
            new CurseFile("meet-your-fight", "409371", "8955468", "meetyourfight-1.20.1-1.6.2.jar"),
            new CurseFile("waifu", "1114161", "7434417", WAIFU_NAME)
    };

    private DaiDePackManager() {}

    public interface ProgressListener {
        void onProgress(String message);
    }

    private static final class CurseFile {
        final String label, projectId, fileId, fileName;
        CurseFile(String label, String projectId, String fileId, String fileName) {
            this.label = label;
            this.projectId = projectId;
            this.fileId = fileId;
            this.fileName = fileName;
        }
    }

    public static void sync(Context context, ProgressListener listener) throws Exception {
        File gameDir = new File(Tools.DIR_GAME_NEW);
        File modsDir = new File(gameDir, "mods");
        File rspDir = new File(gameDir, "resourcepacks");
        if (!modsDir.exists() && !modsDir.mkdirs()) throw new IOException("Không tạo được thư mục mods");
        if (!rspDir.exists() && !rspDir.mkdirs()) throw new IOException("Không tạo được thư mục resourcepacks");

        emit(listener, "DỌN MOD CŨ...");
        cleanupManagedDuplicates(modsDir);

        for (String[] spec : MODRINTH) {
            emit(listener, "TẢI " + spec[0].toUpperCase(Locale.ROOT) + "...");
            downloadModrinthVersion(spec[1], modsDir);
        }

        for (CurseFile spec : CURSE) {
            emit(listener, "TẢI " + spec.label.toUpperCase(Locale.ROOT) + "...");
            ensureCurseFile(spec, modsDir);
        }

        emit(listener, "ĐỒNG BỘ BRIDGE...");
        copyAssetIfPresent(context, "daide/" + BRIDGE_NAME, new File(modsDir, BRIDGE_NAME), true);

        // BUILD29 RSP is enabled automatically whenever it is present in the APK or already on disk.
        File rsp = new File(rspDir, RSP_NAME);
        if (!isGood(rsp, 1024 * 1024)) {
            copyAssetIfPresent(context, "daide/" + RSP_NAME, rsp, false);
        }
        if (isGood(rsp, 1024 * 1024)) enableResourcePack(gameDir, RSP_NAME);

        emit(listener, "PACK ĐÃ SẴN SÀNG");
    }

    private static void downloadModrinthVersion(String versionId, File modsDir) throws Exception {
        String json = DownloadUtils.downloadString("https://api.modrinth.com/v2/version/" + versionId);
        JsonObject root = Tools.GLOBAL_GSON.fromJson(json, JsonObject.class);
        JsonArray files = root == null ? null : root.getAsJsonArray("files");
        if (files == null || files.size() == 0) throw new IOException("Modrinth version không có file: " + versionId);

        JsonObject chosen = null;
        for (JsonElement e : files) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String name = string(o, "filename");
            if (!name.toLowerCase(Locale.ROOT).endsWith(".jar") || name.toLowerCase(Locale.ROOT).contains("sources")) continue;
            if (chosen == null) chosen = o;
            if (o.has("primary") && o.get("primary").getAsBoolean()) {
                chosen = o;
                break;
            }
        }
        if (chosen == null) throw new IOException("Không tìm thấy JAR Modrinth: " + versionId);
        String fileName = string(chosen, "filename");
        String url = string(chosen, "url");
        if (fileName.isEmpty() || url.isEmpty()) throw new IOException("Modrinth metadata thiếu filename/url: " + versionId);
        ensureDownloaded(url, new File(modsDir, fileName), 1024);
    }

    private static void ensureCurseFile(CurseFile spec, File modsDir) throws IOException {
        File out = new File(modsDir, spec.fileName);
        if (isGood(out, 1024)) return;
        String url = "https://www.curseforge.com/api/v1/mods/" + spec.projectId + "/files/" + spec.fileId + "/download";
        ensureDownloaded(url, out, 1024);
    }

    private static void ensureDownloaded(String url, File out, long minSize) throws IOException {
        if (isGood(out, minSize)) return;
        File part = new File(out.getParentFile(), out.getName() + ".part");
        if (part.exists() && !part.delete()) throw new IOException("Không xóa được file tải dở: " + part.getName());
        try {
            DownloadUtils.downloadFile(url, part);
            if (!isGood(part, minSize)) throw new IOException("File tải về quá nhỏ: " + out.getName());
            replaceFile(part, out);
        } finally {
            if (part.exists()) part.delete();
        }
    }

    private static void cleanupManagedDuplicates(File modsDir) {
        File[] jars = modsDir.listFiles(f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (jars == null) return;
        Set<String> exactPinned = new HashSet<>();
        for (CurseFile c : CURSE) exactPinned.add(c.fileName.toLowerCase(Locale.ROOT));
        exactPinned.add(BRIDGE_NAME.toLowerCase(Locale.ROOT));

        // Preserve arbitrary user mods; only stale partial downloads are removed automatically.
        File[] parts = modsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".part"));
        if (parts != null) for (File p : parts) p.delete();
    }

    private static void copyAssetIfPresent(Context context, String assetPath, File out, boolean required) throws IOException {
        try (InputStream in = context.getAssets().open(assetPath)) {
            File parent = out.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Không tạo được " + parent);
            File part = new File(parent, out.getName() + ".part");
            try (FileOutputStream fos = new FileOutputStream(part)) {
                byte[] buf = new byte[128 * 1024];
                int n;
                while ((n = in.read(buf)) >= 0) fos.write(buf, 0, n);
            }
            replaceFile(part, out);
        } catch (IOException e) {
            if (required) throw new IOException("APK thiếu asset bắt buộc: " + assetPath, e);
        }
    }

    private static void enableResourcePack(File gameDir, String rspName) throws IOException {
        File options = new File(gameDir, "options.txt");
        List<String> lines = new ArrayList<>();
        if (options.isFile()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(options), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) lines.add(line);
            }
        }
        String token = "file/" + rspName;
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("resourcePacks:")) continue;
            found = true;
            if (!line.contains("\"" + token + "\"")) {
                int rb = line.lastIndexOf(']');
                if (rb < 0) line = "resourcePacks:[\"vanilla\",\"" + token + "\"]";
                else line = line.substring(0, rb) + (line.substring(0, rb).trim().endsWith("[") ? "" : ",") + "\"" + token + "\"]";
                lines.set(i, line);
            }
            break;
        }
        if (!found) lines.add("resourcePacks:[\"vanilla\",\"" + token + "\"]");
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(options), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
        }
    }

    private static void replaceFile(File source, File target) throws IOException {
        if (target.exists() && !target.delete()) throw new IOException("Không thay được file: " + target.getName());
        if (!source.renameTo(target)) {
            try (InputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[128 * 1024];
                int n;
                while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            }
            if (!source.delete()) source.deleteOnExit();
        }
    }

    private static boolean isGood(File f, long minSize) {
        return f.isFile() && f.length() >= minSize;
    }

    private static String string(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static void emit(ProgressListener listener, String msg) {
        if (listener != null) listener.onProgress(msg);
    }
}
