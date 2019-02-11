package sky4s.garmin.hud;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/* Fragment used as page 2 */
public class Page2Fragment extends Fragment {
    private TextView textViewDebug;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page2, container, false);

        return rootView;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        TextView textView = (TextView) getView().findViewById(R.id.textViewDebug);
        ((MainActivity)getActivity()).textViewDebug = textView;

//        button.setOnClickListener(btnChangeFragment_Listener);
//        btnChangeFragment.setText("Change to ViewFragment1");
//        btnChangeFragment.setOnClickListener(btnChangeFragment_Listener);
    }
//    Button.OnClickListener btnChangeFragment_Listener = new View.OnClickListener()
//    {
//        @Override
//        public void onClick(View v)
//        {
//            ((MainActivity)getActivity()).buttonOnClicked(v);
//        }
//    };

}
