package sky4s.garminhud.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetSequence;
import uk.co.samuelwall.materialtaptargetprompt.extras.backgrounds.RectanglePromptBackground;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

/* Fragment used as page 1 */
public class Page1Fragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_page1, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity) getActivity()).mHudConnectedSwitch = getView().findViewById(R.id.switchHudConnected);
        ((MainActivity) getActivity()).mNotificationCaughtSwitch = getView().findViewById(R.id.switchNotificationCaught);
        ((MainActivity) getActivity()).mGmapsNotificationCaughtSwitch = getView().findViewById(R.id.switchGmapsNotificationCaught);

        ((MainActivity) getActivity()).mShowSpeedSwitch = getView().findViewById(R.id.switchShowSpeed);
        ((MainActivity) getActivity()).mAutoBrightnessSwitch = getView().findViewById(R.id.switchAutoBrightness);
        SeekBar seekBarBrightness = getView().findViewById(R.id.seekBarBrightness);
        seekBarBrightness.setEnabled(false);
        ((MainActivity) getActivity()).mBrightnessSeekbar = seekBarBrightness;

        ((MainActivity) getActivity()).mShowETASwitch = getView().findViewById(R.id.switchShowETA);
        ((MainActivity) getActivity()).mIdleShowCurrentTimeSwitch = getView().findViewById(R.id.switchIdleShowCurrentTime);

        ((MainActivity) getActivity()).mTrafficAndLaneSwitch = getView().findViewById(R.id.switchTrafficAndLane);
//        ((MainActivity) getActivity()).switchAlertYellowTraffic = (Switch) getView().findViewById(R.id.switchAlertYellowTraffic);

//        ((MainActivity) getActivity()).switchBtBindAddress = (Switch) getView().findViewById(R.id.switchBtBindAddress);
//
//        ((MainActivity) getActivity()).switchDarkModeAuto = (Switch) getView().findViewById(R.id.switchDarkModeAuto);
//        ((MainActivity) getActivity()).switchDarkModeManual = (Switch) getView().findViewById(R.id.switchDarkModeMan);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        boolean showPrompt = sharedPref.getBoolean(getString(R.string.option_show_prompt), true);
        //first lesson for GarminHUD app
        if (showPrompt) {
            MaterialTapTargetSequence sequence = new MaterialTapTargetSequence();
            prompt(sequence, R.id.switchHudConnected, getString(R.string.prompt_switch_hud_connected));
            prompt(sequence, R.id.btnScanHUD, getString(R.string.prompt_btn_scan_bt));
            prompt(sequence, R.id.switchNotificationCaught, getString(R.string.prompt_switch_notification_catched));
            prompt(sequence, R.id.switchGmapsNotificationCaught, getString(R.string.prompt_switch_gmaps_navigation_catched));
            prompt(sequence, R.id.layoutStatus, getString(R.string.prompt_layout_status));
            sequence.show();
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.option_show_prompt), false);
        editor.commit();
    }

    private void prompt(MaterialTapTargetSequence sequence, final int target, String text) {
        FragmentActivity activity = getActivity();
        sequence.addPrompt(new MaterialTapTargetPrompt.Builder(activity)
                .setTarget(target)
                .setPrimaryText(text)
                .setPromptBackground(new RectanglePromptBackground())
                .setPromptFocal(new RectanglePromptFocal())
                .create());

    }
}
