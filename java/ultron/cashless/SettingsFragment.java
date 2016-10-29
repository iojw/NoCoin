package ultron.cashless;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;


public class settingsFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public settingsFragment() {
        // Required empty public constructor
    }

    public static settingsFragment newInstance(String param1, String param2) {
        settingsFragment fragment = new settingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View settings = inflater.inflate(R.layout.fragment_settings,container,false);
        SeekBar seekBar=(SeekBar) settings.findViewById(R.id.seekBar);
        seekBar.setMax(500);
        seekBar.setProgress(30);
        final TextView p = (TextView) settings.findViewById(R.id.progress);

        String cachePath = getActivity().getCacheDir().getAbsolutePath() + "budget";
        File cacheFile = new File(cachePath);
        if (cacheFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(cachePath);
                StringBuilder builder = new StringBuilder();
                int ch;
                while ((ch = fis.read()) != -1) {
                    builder.append((char) ch);
                }
                String budget=builder.toString();
                seekBar.setProgress(Integer.parseInt(budget));
                p.setText(budget);
                fis.close();
            } catch (Exception e) {
                Log.e("Reading budget in pref", "Error reading ID: " + e);
            }
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                p.setText("$"+Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                String cachePath = getActivity().getCacheDir().getAbsolutePath() + "budget";
                File cacheFile = new File(cachePath);
                Log.e("Writing budget",  "Normal");
                try {
                    FileOutputStream fos = new FileOutputStream(cachePath);
                    fos.write(Integer.toString(seekBar.getProgress()).getBytes());
                    fos.close();
                } catch (Exception e) {
                    Log.e("Writing budget", "Error writing: " + e);
                }
            }
        });
        return settings;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
