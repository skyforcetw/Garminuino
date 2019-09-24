package sky4s.garminhud.app;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetSequence;
import uk.co.samuelwall.materialtaptargetprompt.extras.backgrounds.RectanglePromptBackground;
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.RectanglePromptFocal;

/* Fragment used as page 1 */
public class Page1Fragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page1, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((MainActivity) getActivity()).switchHudConnected = (Switch) getView().findViewById(R.id.switchHudConnected);
        ((MainActivity) getActivity()).switchNotificationCaught = (Switch) getView().findViewById(R.id.switchNotificationCaught);
        ((MainActivity) getActivity()).switchGmapsNotificationCaught = (Switch) getView().findViewById(R.id.switchGmapsNotificationCaught);

        ((MainActivity) getActivity()).switchShowSpeed = (Switch) getView().findViewById(R.id.switchShowSpeed);
        ((MainActivity) getActivity()).switchAutoBrightness =  (Switch) getView().findViewById(R.id.switchAutoBrightness);;
        SeekBar seekBarBrightness= (SeekBar) getView().findViewById(R.id.seekBarBrightness);
        seekBarBrightness.setEnabled(false);
        ((MainActivity) getActivity()).seekBarBrightness = seekBarBrightness;

        ((MainActivity) getActivity()).switchShowETA = (Switch) getView().findViewById(R.id.switchShowETA);
        ((MainActivity) getActivity()).switchIdleShowCurrrentTime = (Switch) getView().findViewById(R.id.switchIdleShowCurrentTime);

        ((MainActivity) getActivity()).switchTrafficAndLane = (Switch) getView().findViewById(R.id.switchTrafficAndLane);
        ((MainActivity) getActivity()).switchAlertYellowTraffic = (Switch) getView().findViewById(R.id.switchAlertYellowTraffic);

        ((MainActivity) getActivity()).switchBtBindAddress = (Switch) getView().findViewById(R.id.switchBtBindAddress);

        SharedPreferences sharedPref = ((MainActivity) getActivity()).getPreferences(Context.MODE_PRIVATE);
        boolean showPrompt = sharedPref.getBoolean(getString(R.string.option_show_prompt), true);
        //first lesson for GarminHUD app
        if (showPrompt) {
            MaterialTapTargetSequence sequence = new MaterialTapTargetSequence();
            prompt(sequence, R.id.switchHudConnected, getString(R.string.prompt_switch_hud_connected));
            prompt(sequence, R.id.btnScanBT, getString(R.string.prompt_btn_scan_bt));
            prompt(sequence, R.id.switchNotificationCaught, getString(R.string.prompt_switch_notification_catched));
            prompt(sequence, R.id.switchGmapsNotificationCaught, getString(R.string.prompt_switch_gmaps_navigation_catched));
            prompt(sequence, R.id.layoutStatus, getString(R.string.prompt_layout_status));
            sequence.show();
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.option_show_prompt), false);
        editor.commit();

        ((MainActivity) getActivity()).loadOptions();
    }

    private MaterialTapTargetSequence prompt(MaterialTapTargetSequence sequence, final int target, String text) {
        sequence.addPrompt(new MaterialTapTargetPrompt.Builder(getActivity())
                .setTarget(target)
                .setPrimaryText(text)
                .setPromptBackground(new RectanglePromptBackground())
                .setPromptFocal(new RectanglePromptFocal())
                .create());
        return sequence;

    }


}