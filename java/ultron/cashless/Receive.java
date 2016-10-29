package ultron.cashless;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcelable;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

public class Receive extends AppCompatActivity {
    String TAG="VoiceR";
    TextView speechResults;
    SpeechRecognizer sr;
    String amount="-1";
    String ID;
    Activity activity;
    Intent recog;
    boolean success;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity=this;
        if (savedInstanceState!=null)
            amount=savedInstanceState.getString("amount");
        setTitle("Receiving...");
        setContentView(R.layout.activity_receive);
        ID=getIntent().getStringExtra("ID");

        ImageView checkmark=(ImageView) findViewById(R.id.checkmark);
        checkmark.setVisibility(View.INVISIBLE);
        ImageView cross=(ImageView) findViewById(R.id.cross);
        cross.setVisibility(View.INVISIBLE);

        speechResults=(TextView) findViewById(R.id.results);
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(new Listener());
        recog = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        ImageView startListening=(ImageView) findViewById(R.id.startListening);
        startListening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recog.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recog.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "voice.recognition.test");
                recog.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
                recog.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                sr.startListening(recog);
            }
        });

        progressBar=(ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                TextView amountText = (TextView) findViewById(R.id.amount);
                amountText.setInputType(InputType.TYPE_NULL);
                amount = amountText.getText().toString();
                Log.v(TAG, "set amount: " + amount);
                String cachePath = activity.getCacheDir().getAbsolutePath() + "amount";
                File cacheFile = new File(cachePath);
                try {
                    FileOutputStream fos = new FileOutputStream(cachePath);
                    fos.write(amount.getBytes());
                    fos.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error writing: " + e);
                }
            }
        });

       // sr.startListening(recog);
    }


    @Override
    public void onResume() {
        super.onResume();
        NfcAdapter nfcAdapter=NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.setNdefPushMessage(null, this);

        String cachePath = activity.getCacheDir().getAbsolutePath() + "amount";
        File cacheFile = new File(cachePath);
        if (cacheFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(cachePath);
                StringBuilder builder = new StringBuilder();
                int ch;
                while ((ch = fis.read()) != -1) {
                    builder.append((char) ch);
                }
                amount=builder.toString();
                fis.close();
            } catch (Exception e) {
                Log.e(TAG, "Error reading: " + e);
            }
        }
        cachePath = activity.getCacheDir().getAbsolutePath() + "id";
        cacheFile = new File(cachePath);
        if (cacheFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(cachePath);
                StringBuilder builder = new StringBuilder();
                int ch;
                while ((ch = fis.read()) != -1) {
                    builder.append((char) ch);
                }
                ID=builder.toString();
                fis.close();
            } catch (Exception e) {
                Log.e(TAG, "Error reading ID: " + e);
            }
        }

        Log.v("VoiceR", amount);
        // Check to see that  Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Intent i=getIntent();
            Parcelable[] rawMsgs = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (rawMsgs!=null) {
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                String sendID=new String(msg.getRecords()[0].getPayload());
                if (amount!="-1"){
                    HashMap<String, String> hm = new HashMap<>(5);
                    hm.put("sendID", sendID);
                    hm.put("receiveID", ID);
                    hm.put("amt",amount);
                    hm.put("desc","Ramen");
                    hm.put("cat", "Food");
                    Log.v("Transaction", "Sending request:" + sendID + ID + amount);
                    CallAPI transfer=new CallAPI("http://php-hometue.rhcloud.com/transact.php",hm);
                    transfer.execute();
                }
            }
        }
    }

    class Listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "onReadyForSpeech");
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
            Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error)
        {
            Log.d(TAG,  "error " +  error);
            speechResults.setText("error " + error);
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            for (int i = 0; i < data.size(); i++)
            {
                Log.d(TAG, "result " + data.get(i));
                str += data.get(i);
            }
            speechResults.setText(data.get(0)+","+data.get(1));
    }
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
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
            Log.v(TAG,"Transaction done! "+result);
            amount="-1";
            String cachePath = activity.getCacheDir().getAbsolutePath() + "id";
            File cacheFile = new File(cachePath);
            try {
                FileOutputStream fos = new FileOutputStream(cachePath);
                fos.write(amount.getBytes());
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "Error writing: " + e);
            }
            //cacheFile.delete();
            Log.e(TAG, "Response : " + result+" "+result.equals("SUCCESS"));
            if (result.equals("SUCCESS"))
                success=true;
            else
                success=false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (success) {
                        Log.v("sc","Success");
                        ImageView checkmark = (ImageView) findViewById(R.id.checkmark);
                        checkmark.setVisibility(View.VISIBLE);
                    }
                    else{
                        Log.v("sc","Fail");
                        ImageView cross = (ImageView) findViewById(R.id.cross);
                        cross.setVisibility(View.VISIBLE);
                    }
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });

            /*Intent intent = new Intent(activity,MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
        }
    }

}
