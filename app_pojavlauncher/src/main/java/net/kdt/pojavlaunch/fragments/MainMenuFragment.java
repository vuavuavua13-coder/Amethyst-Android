package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.dialogOnUiThread;
import static net.kdt.pojavlaunch.Tools.hasMods;
import static net.kdt.pojavlaunch.Tools.hasNoOnlineProfileDialog;
import static net.kdt.pojavlaunch.Tools.hasOnlineProfile;
import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.runOnUiThread;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.modloaders.LWJGL3ifyUtils;
import net.kdt.pojavlaunch.modloaders.ForgeUtils;
import net.kdt.pojavlaunch.modloaders.ForgeDownloadTask;
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener;
import net.kdt.pojavlaunch.JavaGUILauncherActivity;
import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.DaiDePackManager;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.List;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;
    private Button mPlayButton;

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mDiscordButton = view.findViewById(R.id.discord_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);
        Button mInstallJarButton = view.findViewById(R.id.install_jar_button);
        Button mShareLogsButton = view.findViewById(R.id.share_logs_button);
        Button mOpenDirectoryButton = view.findViewById(R.id.open_files_button);

        ImageButton mEditProfileButton = view.findViewById(R.id.edit_profile_button);
        mPlayButton = view.findViewById(R.id.play_button);
        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);

        mNewsButton.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        mDiscordButton.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.discord_invite)));
        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        if (hasOnlineProfile()) {
            mInstallJarButton.setOnClickListener(v -> runInstallerWithConfirmation(false));
            mInstallJarButton.setOnLongClickListener(v -> {
                runInstallerWithConfirmation(true);
                return true;
            });
        } else mInstallJarButton.setOnClickListener(v -> hasNoOnlineProfileDialog(requireActivity()));
        mEditProfileButton.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        mPlayButton.setText("CHƠI");
        mPlayButton.setOnClickListener(v -> startDaiDeOneClick(mPlayButton));

        mShareLogsButton.setOnClickListener((v) -> shareLog(requireContext()));

        mOpenDirectoryButton.setOnClickListener((v)-> {
            if (Tools.isDemoProfile(v.getContext())){ // Say a different message when on demo profile since they might see the hidden demo folder
                hasNoOnlineProfileDialog(getActivity(), getString(R.string.demo_unsupported), getString(R.string.change_account));
            } else if (!hasOnlineProfile()) { // Otherwise display the generic pop-up to log in
                hasNoOnlineProfileDialog(requireActivity());
            } else openPath(v.getContext(), getCurrentProfileDirectory(), false);

        });


        mNewsButton.setOnLongClickListener((v)->{
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });
    }

    private static final String DAIDE_MC = "1.20.1";
    private static final String DAIDE_FORGE = "47.3.22";
    private static final String DAIDE_FORGE_ID = DAIDE_MC + "-forge-" + DAIDE_FORGE;

    private void startDaiDeOneClick(Button playButton) {
        playButton.setEnabled(false);
        playButton.setText("ĐANG KIỂM TRA...");
        LauncherProfiles.load();
        MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get("daide-tu-tien");
        if (profile != null) {
            File forgeJson = new File(Tools.DIR_HOME_VERSION + "/" + DAIDE_FORGE_ID + "/" + DAIDE_FORGE_ID + ".json");
            if (forgeJson.isFile() && forgeJson.length() > 100) {
                profile.lastVersionId = DAIDE_FORGE_ID;
                LauncherProfiles.write();
                syncPackAndLaunch(playButton);
                return;
            }
            profile.lastVersionId = DAIDE_MC;
            LauncherProfiles.write();
        }

        LauncherPreferences.DEFAULT_PREF.edit().putBoolean("daide_oneclick_pending", true).apply();
        Toast.makeText(requireContext(), "Lần đầu: đang tự chuẩn bị Forge 47.3.22...", Toast.LENGTH_LONG).show();
        ForgeDownloadTask task = new ForgeDownloadTask(new ModloaderDownloadListener() {
            @Override
            public void onDownloadFinished(File downloadedFile) {
                requireActivity().runOnUiThread(() -> {
                    Intent intent = new Intent(requireContext(), JavaGUILauncherActivity.class);
                    // The bundled Forge installer agent makes the normal installer run unattended.
                    ForgeUtils.addAutoInstallArgs(intent, downloadedFile, true);
                    startActivity(intent);
                    playButton.setEnabled(true);
                    playButton.setText("CHƠI");
                });
            }

            @Override
            public void onDataNotAvailable() {
                requireActivity().runOnUiThread(() -> {
                    playButton.setEnabled(true);
                    playButton.setText("CHƠI");
                    Toast.makeText(requireContext(), "Không tìm thấy Forge 47.3.22.", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onDownloadError(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    playButton.setEnabled(true);
                    playButton.setText("CHƠI");
                    Tools.showError(requireContext(), "Không tải được Forge 47.3.22", e);
                });
            }
        }, DAIDE_MC, DAIDE_FORGE);
        PojavApplication.sExecutorService.execute(task);
    }

    private void syncPackAndLaunch(Button playButton) {
        playButton.setEnabled(false);
        playButton.setText("ĐANG ĐỒNG BỘ MOD...");
        PojavApplication.sExecutorService.execute(() -> {
            try {
                DaiDePackManager.sync(requireContext(), message -> requireActivity().runOnUiThread(() -> playButton.setText(message)));
                LauncherProfiles.load();
                MinecraftProfile profile = LauncherProfiles.mainProfileJson.profiles.get("daide-tu-tien");
                if (profile == null) throw new IllegalStateException("Thiếu profile Đại Đế Tu Tiên");
                profile.lastVersionId = DAIDE_FORGE_ID;
                LauncherProfiles.write();
                LauncherPreferences.DEFAULT_PREF.edit().putBoolean("daide_oneclick_pending", false).apply();
                requireActivity().runOnUiThread(() -> {
                    playButton.setEnabled(true);
                    playButton.setText("CHƠI");
                    ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
                });
            } catch (Exception e) {
                LauncherPreferences.DEFAULT_PREF.edit().putBoolean("daide_oneclick_pending", false).apply();
                requireActivity().runOnUiThread(() -> {
                    playButton.setEnabled(true);
                    playButton.setText("CHƠI");
                    Tools.showError(requireContext(), "Không đồng bộ được bộ Đại Đế", e);
                });
            }
        });
    }

    private void continuePendingOneClick() {
        if (mPlayButton == null) return;
        if (!LauncherPreferences.DEFAULT_PREF.getBoolean("daide_oneclick_pending", false)) return;
        File forgeJson = new File(Tools.DIR_HOME_VERSION + "/" + DAIDE_FORGE_ID + "/" + DAIDE_FORGE_ID + ".json");
        if (!forgeJson.isFile() || forgeJson.length() <= 100) return;
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean("daide_oneclick_pending", false).apply();
        syncPackAndLaunch(mPlayButton);
    }

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if(!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if(profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionSpinner.reloadProfiles();
        continuePendingOneClick();
    }

    private void runInstallerWithConfirmation(boolean isCustomArgs) {
        if (ProgressKeeper.getTaskCount() == 0)
            Tools.installMod(requireActivity(), isCustomArgs);
        else
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }
}
