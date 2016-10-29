package ultron.cashless;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class StatsFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    String id;
    View stats;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public StatsFragment() {
        // Required empty public constructor
    }

    public static StatsFragment newInstance(String param1, String param2) {
        StatsFragment fragment = new StatsFragment();
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
        String android_id = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
        HashMap<String,String> hm = new HashMap<>(1);
        hm.put("UID", android_id);
        CallAPI getId=new CallAPI("http://php-hometue.rhcloud.com/UID2ID.php",hm);
        getId.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View stats=inflater.inflate(R.layout.fragment_stats,container,false);

        this.stats=stats;
        return stats;
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
        void onFragmentInteraction(Uri uri);
    }

    void handleResponse(String url, String result){
        if (url.contains("UID2ID")){
            HashMap<String,String> hm = new HashMap<>(1);
            hm.put("id", result);
            CallAPI ana=new CallAPI("http://php-hometue.rhcloud.com/transact_ana.php",hm);
            ana.execute();
            hm.clear();
            hm.put("id", result);
            CallAPI budget=new CallAPI("http://php-hometue.rhcloud.com/week.php",hm);
            budget.execute();
        }
        else if (url.contains("transact_ana")){
            PieChart expenditure = (PieChart) stats.findViewById(R.id.chart);
            ArrayList<PieEntry> entries=new ArrayList<PieEntry>();
            ArrayList<String> categories=new ArrayList<String>();
            String[] cats=result.split(";");
            for (int i=0;i<cats.length;i++){
                Log.v("cats",cats[i] );
                String[] tmp=cats[i].split(":");
                entries.add(new PieEntry(Float.parseFloat(tmp[1]),tmp[0]));
                categories.add(tmp[0]);
            }
            PieDataSet dataset = new PieDataSet(entries, "Of Money");
            dataset.setColors(ColorTemplate.COLORFUL_COLORS);
            dataset.setValueTextSize(16f);
            PieData data = new PieData(dataset);
            expenditure.setData(data);
            expenditure.animateY(2000);
            expenditure.setDescription("");
            expenditure.setCenterText("Total Expenditure");
            expenditure.setCenterTextSize(20);
            expenditure.setDrawEntryLabels(false);
            expenditure.setTransparentCircleRadius(0f);
           // expenditure.setHoleRadius(0.1f);
            Legend l = expenditure.getLegend();
            l.setPosition(Legend.LegendPosition.LEFT_OF_CHART);
            l.setTextSize(12f);
            l.setMaxSizePercent(1.2f);
            l.setTextColor(Color.BLACK);

        }
        else if (url.contains("week")){
            ProgressBar budgetBar = (ProgressBar) stats.findViewById(R.id.progressBar);
            String cachePath = getActivity().getCacheDir().getAbsolutePath() + "budget";
            TextView budgetStatus=(TextView) stats.findViewById(R.id.budgetStatus);
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
                    Float r=Float.parseFloat(result);Float b=Float.parseFloat(budget);
                    if (r<b) {
                        budgetBar.setProgress(Math.round(r / b * 100));
                        budgetStatus.setText("You can spend $" + (b - r) + " more this week.");
                    }
                    else{
                        budgetBar.setProgress((100));
                        budgetStatus.setText("You exceeded your budget of $"+b+". Remember to watch what you spend on!");
                    }
                    fis.close();
                } catch (Exception e) {
                    Log.e("Reading budget", "Error reading ID: " + e);
                }
            }
            else{
                budgetStatus.setText("You can set your budget in Settings.");
            }
        }
    }

    private class CallAPI extends AsyncTask<String, String, String> {
        String url;
        HashMap<String,String> hm;
        TextView textView;

        public CallAPI(String url, HashMap<String,String> hm) {
            this.url=url;
            this.hm=hm;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected String doInBackground(String... params) {
            return performPostCall(url,hm);
        }

        public String performPostCall(String requestURL,
                                      HashMap<String, String> postDataParams) {

            URL url;
            String response = "";
            try {
                url = new URL(requestURL);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line = br.readLine()) != null) {
                        response += line;
                    }
                } else {
                    response = "";

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            return result.toString();
        }


        @Override
        protected void onPostExecute(String result) {
            handleResponse(url, result);
        }
    }
}
