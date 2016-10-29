package ultron.cashless;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;
    private String id;
    View main;
    Activity activity;
    ArrayList<Transaction> tlist;

    private OnFragmentInteractionListener mListener;

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
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

        View main = inflater.inflate(R.layout.content_main,container,false);
        Button send = (Button) main.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(getActivity(), Pay2.class);
                sendIntent.putExtra("id", id);
                startActivity(sendIntent);
            }
        });

        Button receive = (Button) main.findViewById(R.id.receive);
        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent receiveIntent = new Intent(getActivity(), Receive.class);
                receiveIntent.putExtra("ID", id);
                startActivity(receiveIntent);
            }
        });
        this.main=main;

        refresh();

        return main;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity=(Activity) context;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh){
            refresh();
        }

        return super.onOptionsItemSelected(item);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public class Transaction{
        public String message;
        public String desc;
        public String time;
        public String cat;
        public String amt;

        public Transaction(String message,String desc,String time,String cat,String amt){
            this.message=message;
            this.desc=desc;
            this.time=time;
            this.cat=cat;
            this.amt="$"+amt;
        }
    }

    public class TAdapter extends ArrayAdapter<Transaction>{
        public TAdapter(Context context,ArrayList<Transaction>trans){
            super(context,0,trans);
        }

        @Override
            public View getView(int position,View convertView,ViewGroup parent){
            Transaction t=getItem(position);
            if(convertView==null)
                convertView=getActivity().getLayoutInflater().inflate(R.layout.item_trans, parent, false);
            TextView amt=(TextView)convertView.findViewById(R.id.amount);
            TextView msg=(TextView)convertView.findViewById(R.id.msg);
            TextView time=(TextView)convertView.findViewById(R.id.time);
            TextView desc=(TextView)convertView.findViewById(R.id.desc);
            amt.setText(t.amt);
            msg.setText(t.message);
            time.setText(t.time);
            desc.setText(t.desc);
            return convertView;
        }

    }


    void refresh(){
        String android_id = Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
        HashMap<String,String> hm = new HashMap<>(1);
        hm.put("UID", android_id);
        CallAPI getId=new CallAPI("http://php-hometue.rhcloud.com/UID2ID.php",hm);
        getId.execute();
    }

    void handlePostResponse(String result, String url){
        if (url.contains("UID2ID")){
            Log.v("uid2id", result);
            id=result;
            String cachePath = getActivity().getCacheDir().getAbsolutePath() + "id";
            File cacheFile = new File(cachePath);
            try {
                FileOutputStream fos = new FileOutputStream(cachePath);
                fos.write(id.getBytes());
                fos.close();
            }
            catch (Exception e){
                Log.e("VoiceR","Error saving ID: "+e);
            }

            HashMap<String, String> hm = new HashMap<>(1);
            hm.put("uid", id);
            CallAPI namebal=new CallAPI("http://php-hometue.rhcloud.com/namebal.php",hm);
            namebal.execute();
            CallAPI tranhist=new CallAPI("http://php-hometue.rhcloud.com/transact_hist.php",hm);
            tranhist.execute();
        }
        else if (url.contains("namebal")){
            Log.v("Namebal",result);
            TextView name=(TextView) main.findViewById(R.id.name);
            TextView menu_name=(TextView)  getActivity().findViewById(R.id.menu_name);
            TextView balance=(TextView) main.findViewById(R.id.balance);
            TextView menu_balance=(TextView) getActivity().findViewById(R.id.menu_balance);
            String values[]=result.split(";");
            name.setText(values[0]);
            menu_name.setText(values[0]);
            if (values.length>1) {
                balance.setText("$" + values[1]);
                menu_balance.setText("$" + values[1]);
            }
        }
        else if(url.contains("transact_hist")){
            final ListView listview = (ListView) getView().findViewById(R.id.recent);
            Log.v("transact_hist",result);
            String[] array = result.split("/",-1);
            //final ArrayList<String> list = new ArrayList<String>();
            tlist=new ArrayList<Transaction>();
            for (int i = 0; i < array.length; i++) {
                if(array[i]!="") {
                    String[] array2 = array[i].split(";",-1);
                    if (array2.length==5) {
                        Transaction t = new Transaction(array2[0], array2[2], array2[4], array2[3], array2[1]);
                        tlist.add(t);
                    }
                    //array2[1]="$"+array2[1];
                    //String joined = TextUtils.join(" ", array2);
                    //list.add(joined); //slow code, left out for now.
                    //list.add(array[i]);
                }
            }
            Collections.reverse(tlist);
            //list.remove(0);
            //final StableArrayAdapter adapter = new StableArrayAdapter(getActivity(),
             //       android.R.layout.simple_list_item_1, list);
            TAdapter tadapter = new TAdapter(getActivity(),tlist);
            listview.setAdapter(tadapter);
        }
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
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
            //Update the UI
            handlePostResponse(result, url);
        }
    }
}
