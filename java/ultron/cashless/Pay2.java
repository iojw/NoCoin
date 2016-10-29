package ultron.cashless;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Pay2 extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback
{

    private NfcAdapter nfcAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay2);
        ImageView checkmark=(ImageView) findViewById(R.id.checkmark);
        checkmark.setVisibility(View.INVISIBLE);
        setTitle("Paying...");

        nfcAdapter=NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Snackbar nfcOff=Snackbar.make(findViewById(android.R.id.content), "Please turn on NFC.", Snackbar.LENGTH_SHORT);
            finish();
            return;
        }
        // Register callback
        nfcAdapter.setNdefPushMessageCallback(this, this);
        nfcAdapter.setOnNdefPushCompleteCallback(this,this);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = getIntent().getStringExtra("id");
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { NdefRecord.createMime(
                        "application/ultron.cashless", text.getBytes())
                       // ,NdefRecord.createApplicationRecord("ultron.cashless")
                });
        return msg;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event)
    {
        nfcAdapter.setNdefPushMessage(null, this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView checkmark = (ImageView) findViewById(R.id.checkmark);
                checkmark.setVisibility(View.VISIBLE);
                TextView view=(TextView) findViewById(R.id.prompt);
                view.setText("Communcation established.");
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });

        //finish();
    }

}
