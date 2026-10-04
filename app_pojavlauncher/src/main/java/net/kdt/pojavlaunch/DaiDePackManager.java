package net.kdt.pojavlaunch;

import android.content.Context;
import android.util.Base64;

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
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

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

    private static final String BRIDGE_CLASS_B64 = "yv66vgAAAD0AwQoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2JqZWN0AQAGPGluaXQ+AQADKClWCQAIAAkHAAoMAAsADAEAKG5ldC9taW5lY3JhZnRmb3JnZS9jb21tb24vTWluZWNyYWZ0Rm9yZ2UBAAlFVkVOVF9CVVMBAC5MbmV0L21pbmVjcmFmdGZvcmdlL2NvbW1vbi9NaW5lY3JhZnRGb3JnZSRCdXM7CgAOAA8HABAMABEAEgEALG5ldC9taW5lY3JhZnRmb3JnZS9jb21tb24vTWluZWNyYWZ0Rm9yZ2UkQnVzAQAIcmVnaXN0ZXIBABUoTGphdmEvbGFuZy9PYmplY3Q7KVYKABQAFQcAFgwAFwAYAQA2bmV0L21pbmVjcmFmdGZvcmdlL2V2ZW50L2VudGl0eS9saXZpbmcvTGl2aW5nSHVydEV2ZW50AQAJZ2V0RW50aXR5AQArKClMbmV0L21pbmVjcmFmdC93b3JsZC9lbnRpdHkvTGl2aW5nRW50aXR5OwoAFAAaDAAbABwBAAlnZXRTb3VyY2UBADEoKUxuZXQvbWluZWNyYWZ0L3dvcmxkL2RhbWFnZXNvdXJjZS9EYW1hZ2VTb3VyY2U7CgAeAB8HACAMACEAIgEALHZuL2RhaWRlL3JwZy9mb3JnZWJyaWRnZS9EYWlEZVJQR0ZvcmdlQnJpZGdlAQANcmVzb2x2ZVBsYXllcgEAWihMbmV0L21pbmVjcmFmdC93b3JsZC9kYW1hZ2Vzb3VyY2UvRGFtYWdlU291cmNlOylMbmV0L21pbmVjcmFmdC9zZXJ2ZXIvbGV2ZWwvU2VydmVyUGxheWVyOwoAJAAlBwAmDAAnACgBACduZXQvbWluZWNyYWZ0L3NlcnZlci9sZXZlbC9TZXJ2ZXJQbGF5ZXIBAAtzZXJ2ZXJMZXZlbAEAKigpTG5ldC9taW5lY3JhZnQvc2VydmVyL2xldmVsL1NlcnZlckxldmVsOwoAKgArBwAsDAAtAC4BACZuZXQvbWluZWNyYWZ0L3NlcnZlci9sZXZlbC9TZXJ2ZXJMZXZlbAEACWdldFNlcnZlcgEAKCgpTG5ldC9taW5lY3JhZnQvc2VydmVyL01pbmVjcmFmdFNlcnZlcjsKABQAMAwAMQAyAQAJZ2V0QW1vdW50AQADKClGCgA0ADUHADYMADcAOAEADmphdmEvbGFuZy9NYXRoAQADbWF4AQAFKEZGKUYKAB4AOgwAOwA8AQAKc2FmZVNvdXJjZQEAQyhMbmV0L21pbmVjcmFmdC93b3JsZC9kYW1hZ2Vzb3VyY2UvRGFtYWdlU291cmNlOylMamF2YS9sYW5nL1N0cmluZzsKACQAPgwAPwBAAQAHZ2V0VVVJRAEAEigpTGphdmEvdXRpbC9VVUlEOwoAQgBDBwBEDABFAEYBABBqYXZhL2xhbmcvU3RyaW5nAQAHdmFsdWVPZgEAJihMamF2YS9sYW5nL09iamVjdDspTGphdmEvbGFuZy9TdHJpbmc7CgBIAD4HAEkBACFuZXQvbWluZWNyYWZ0L3dvcmxkL2VudGl0eS9FbnRpdHkSAAAASwwATABNAQAXbWFrZUNvbmNhdFdpdGhDb25zdGFudHMBAEsoTGphdmEvbGFuZy9TdHJpbmc7TGphdmEvbGFuZy9TdHJpbmc7RkxqYXZhL2xhbmcvU3RyaW5nOylMamF2YS9sYW5nL1N0cmluZzsKAE8AUAcAUQwAUgBTAQAkbmV0L21pbmVjcmFmdC9zZXJ2ZXIvTWluZWNyYWZ0U2VydmVyAQALZ2V0Q29tbWFuZHMBACMoKUxuZXQvbWluZWNyYWZ0L2NvbW1hbmRzL0NvbW1hbmRzOwoATwBVDABWAFcBABhjcmVhdGVDb21tYW5kU291cmNlU3RhY2sBAC0oKUxuZXQvbWluZWNyYWZ0L2NvbW1hbmRzL0NvbW1hbmRTb3VyY2VTdGFjazsKAFkAWgcAWwwAXABXAQApbmV0L21pbmVjcmFmdC9jb21tYW5kcy9Db21tYW5kU291cmNlU3RhY2sBABR3aXRoU3VwcHJlc3NlZE91dHB1dAoAWQBeDABfAGABAA53aXRoUGVybWlzc2lvbgEALihJKUxuZXQvbWluZWNyYWZ0L2NvbW1hbmRzL0NvbW1hbmRTb3VyY2VTdGFjazsKAGIAYwcAZAwAZQBmAQAfbmV0L21pbmVjcmFmdC9jb21tYW5kcy9Db21tYW5kcwEAFnBlcmZvcm1QcmVmaXhlZENvbW1hbmQBAEAoTG5ldC9taW5lY3JhZnQvY29tbWFuZHMvQ29tbWFuZFNvdXJjZVN0YWNrO0xqYXZhL2xhbmcvU3RyaW5nOylJCgBoAGkHAGoMABcAawEALW5ldC9taW5lY3JhZnQvd29ybGQvZGFtYWdlc291cmNlL0RhbWFnZVNvdXJjZQEAJSgpTG5ldC9taW5lY3JhZnQvd29ybGQvZW50aXR5L0VudGl0eTsKAGgAbQwAbgBrAQAPZ2V0RGlyZWN0RW50aXR5CgACAHAMAHEAcgEACGdldENsYXNzAQATKClMamF2YS9sYW5nL0NsYXNzOwgAdAEACGdldE93bmVyBwB2AQAPamF2YS9sYW5nL0NsYXNzCgB1AHgMAHkAegEACWdldE1ldGhvZAEAQChMamF2YS9sYW5nL1N0cmluZztbTGphdmEvbGFuZy9DbGFzczspTGphdmEvbGFuZy9yZWZsZWN0L01ldGhvZDsKAHwAfQcAfgwAfwCAAQAYamF2YS9sYW5nL3JlZmxlY3QvTWV0aG9kAQAGaW52b2tlAQA5KExqYXZhL2xhbmcvT2JqZWN0O1tMamF2YS9sYW5nL09iamVjdDspTGphdmEvbGFuZy9PYmplY3Q7BwCCAQATamF2YS9sYW5nL1Rocm93YWJsZQgAhAEACmdldFNob290ZXIKAGgAhgwAhwCIAQAIZ2V0TXNnSWQBABQoKUxqYXZhL2xhbmcvU3RyaW5nOwgAigEAB3Vua25vd24KAEIAjAwAjQCOAQAHaXNCbGFuawEAAygpWgkAkACRBwCSDACTAJQBABBqYXZhL3V0aWwvTG9jYWxlAQAEUk9PVAEAEkxqYXZhL3V0aWwvTG9jYWxlOwoAQgCWDACXAJgBAAt0b0xvd2VyQ2FzZQEAJihMamF2YS91dGlsL0xvY2FsZTspTGphdmEvbGFuZy9TdHJpbmc7CACaAQANW15hLXowLTlfLjotXQgAnAEAAV8KAEIAngwAnwCgAQAKcmVwbGFjZUFsbAEAOChMamF2YS9sYW5nL1N0cmluZztMamF2YS9sYW5nL1N0cmluZzspTGphdmEvbGFuZy9TdHJpbmc7AQAFTU9ESUQBABJMamF2YS9sYW5nL1N0cmluZzsBAA1Db25zdGFudFZhbHVlCAClAQAPZGFpZGVycGdfYnJpZGdlAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEADG9uTGl2aW5nSHVydAEAOyhMbmV0L21pbmVjcmFmdGZvcmdlL2V2ZW50L2VudGl0eS9saXZpbmcvTGl2aW5nSHVydEV2ZW50OylWAQANU3RhY2tNYXBUYWJsZQEAGVJ1bnRpbWVWaXNpYmxlQW5ub3RhdGlvbnMBADBMbmV0L21pbmVjcmFmdGZvcmdlL2V2ZW50YnVzL2FwaS9TdWJzY3JpYmVFdmVudDsBAApTb3VyY2VGaWxlAQAYRGFpRGVSUEdGb3JnZUJyaWRnZS5qYXZhAQAjTG5ldC9taW5lY3JhZnRmb3JnZS9mbWwvY29tbW9uL01vZDsBAAV2YWx1ZQEAEEJvb3RzdHJhcE1ldGhvZHMIALMBABdkZGJyaWRnZSBkYW1hZ2UgASABIAEgAQ8GALUKALYAtwcAuAwATAC5AQAkamF2YS9sYW5nL2ludm9rZS9TdHJpbmdDb25jYXRGYWN0b3J5AQCYKExqYXZhL2xhbmcvaW52b2tlL01ldGhvZEhhbmRsZXMkTG9va3VwO0xqYXZhL2xhbmcvU3RyaW5nO0xqYXZhL2xhbmcvaW52b2tlL01ldGhvZFR5cGU7TGphdmEvbGFuZy9TdHJpbmc7W0xqYXZhL2xhbmcvT2JqZWN0OylMamF2YS9sYW5nL2ludm9rZS9DYWxsU2l0ZTsBAAxJbm5lckNsYXNzZXMBAANCdXMHAL0BACVqYXZhL2xhbmcvaW52b2tlL01ldGhvZEhhbmRsZXMkTG9va3VwBwC/AQAeamF2YS9sYW5nL2ludm9rZS9NZXRob2RIYW5kbGVzAQAGTG9va3VwADEAHgACAAAAAQAZAKEAogABAKMAAAACAKQABAABAAUABgABAKYAAAAsAAIAAQAAAAwqtwABsgAHKrYADbEAAAABAKcAAAAOAAMAAAAYAAQAGQALABoAAQCoAKkAAgCmAAAA2QAEAAgAAAB4K8YAESu2ABPGAAortgAZxwAEsSu2ABm4AB1NLMcABLErtgATTiy2ACO2ACk6BBkExwAEsQsrtgAvuAAzOAUrtgAZuAA5OgYstgA9uABBLbYAR7gAQRcFGQa6AEoAADoHGQS2AE4ZBLYAVLYAWAe2AF0ZB7YAYVexAAAAAgCnAAAANgANAAAAHgATAB8AGwAgACAAIQAlACIALgAjADQAJAA+ACUARwAmAGAAJwBnACgAcwAnAHcAKQCqAAAAEwAEEgD8AAwHACT9ABMHAEgHAE8AqwAAAAYAAQCsAAAACgAhACIAAQCmAAABBgADAAYAAACKKrYAZ0wrwQAkmQAKK8AAJE0ssCq2AGxNLMEAJJkACizAACROLbAsxgBhLLYAbxJzA70AdbYAd04tLAO9AAK2AHs6BBkEwQAkmQANGQTAACQ6BRkFsKcABE4stgBvEoMDvQB1tgB3Ti0sA70AArYAezoEGQTBACSZAA0ZBMAAJDoFGQWwpwAETgGwAAIAKgBUAFgAgQBZAIMAhwCBAAIApwAAADoADgAAACwABQAtABMALgAYAC8AJgAwACoAMgA4ADMAQwA0AFUANQBZADcAZwA4AHIAOQCEADoAiAA8AKoAAAAaAAj8ABMHAEj8ABIHAEguQgcAgQAqQgcAgQAACgA7ADwAAQCmAAAAZQADAAMAAAApKrYAhUynAAdNEolMK8YACiu2AIuZAAYSiUwrsgCPtgCVEpkSm7YAnbAAAQAAAAUACACBAAIApwAAAA4AAwAAAEEADABCABoAQwCqAAAADgAESAcAgfwAAwcAQgoCAAQArQAAAAIArgCrAAAACwABAK8AAQCwcwClALEAAAAIAAEAtAABALIAugAAABIAAgAOAAgAuwAZALwAvgDAABk=";
    private static final String BRIDGE_MODS_TOML =
            "modLoader=\"javafml\"\n" +
            "loaderVersion=\"[47,)\"\n" +
            "license=\"All Rights Reserved\"\n\n" +
            "[[mods]]\n" +
            "modId=\"daiderpg_bridge\"\n" +
            "version=\"0.29.0-BUILD29-RSP-LAUNCHER\"\n" +
            "displayName=\"DaiDeRPG Forge Bridge\"\n" +
            "authors=\"OpenAI\"\n" +
            "description='''DaiDeRPG BUILD29 code-only ModelVK + damage bridge; visual assets are installed by the BUILD29 RSP for Minecraft 1.20.1 / Forge 47.3.22 / Arclight.'''\n\n" +
            "[[dependencies.daiderpg_bridge]]\n" +
            "modId=\"forge\"\nmandatory=true\nversionRange=\"[47.3.22,)\"\nordering=\"NONE\"\nside=\"BOTH\"\n\n" +
            "[[dependencies.daiderpg_bridge]]\n" +
            "modId=\"minecraft\"\nmandatory=true\nversionRange=\"[1.20.1,1.20.2)\"\nordering=\"NONE\"\nside=\"BOTH\"\n";

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
        ensureEmbeddedBridge(new File(modsDir, BRIDGE_NAME));

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
        try {
            ensureDownloaded(url, out, 1024);
            return;
        } catch (IOException primary) {
            // CurseForge's mediafilez host occasionally answers 403 to launcher clients.
            // Try the alternate ForgeCDN edge host using the exact same pinned file id.
            long id;
            try { id = Long.parseLong(spec.fileId); }
            catch (NumberFormatException badId) { throw primary; }
            String group = Long.toString(id / 1000L);
            String tail = String.format(Locale.ROOT, "%03d", id % 1000L);
            String encodedName = spec.fileName
                    .replace(" ", "%20")
                    .replace("[", "%5B")
                    .replace("]", "%5D");
            String edge = "https://edge.forgecdn.net/files/" + group + "/" + tail + "/" + encodedName;
            try {
                ensureDownloaded(edge, out, 1024);
            } catch (IOException secondary) {
                secondary.addSuppressed(primary);
                throw secondary;
            }
        }
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

    private static void ensureEmbeddedBridge(File out) throws IOException {
        if (isGood(out, 1024)) return;
        File parent = out.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Không tạo được " + parent);
        File part = new File(parent, out.getName() + ".part");
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(part))) {
            putJarText(jar, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nCreated-By: Dai De Android\n\n");
            putJarText(jar, "META-INF/mods.toml", BRIDGE_MODS_TOML);
            JarEntry clazz = new JarEntry("vn/daide/rpg/forgebridge/DaiDeRPGForgeBridge.class");
            jar.putNextEntry(clazz);
            jar.write(Base64.decode(BRIDGE_CLASS_B64, Base64.DEFAULT));
            jar.closeEntry();
        }
        if (!isGood(part, 1024)) throw new IOException("Không dựng được ForgeBridge BUILD29");
        replaceFile(part, out);
    }

    private static void putJarText(JarOutputStream jar, String path, String text) throws IOException {
        JarEntry entry = new JarEntry(path);
        jar.putNextEntry(entry);
        jar.write(text.getBytes(StandardCharsets.UTF_8));
        jar.closeEntry();
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
