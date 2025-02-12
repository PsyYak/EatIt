package com.example.iFood.Activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.iFood.R;
import com.example.iFood.Utils.ConnectionBCR;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * About screen and information on the app creator
 */
public class About extends AppCompatActivity {
    ConnectionBCR bcr = new ConnectionBCR();
    String formattedDate;
    String url = "paypal.me/Yakir262";
    private ImageView donate,sendEmail,sendWhatsApp;
    private TextView tvDate;


    @SuppressLint({"SetJavaScriptEnabled", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        initUiViews();
        initListeners();
        initCurrentYear();



    }

    private void initCurrentYear() {
        formattedDate = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText(formattedDate);
    }

    private void initListeners() {
        donate.setOnClickListener(v -> {
            Intent donate = new Intent();
            donate.setAction(Intent.ACTION_VIEW);
            donate.addCategory(Intent.CATEGORY_BROWSABLE);
            donate.setData(Uri.parse(url));
            startActivity(donate);

        });
        sendEmail.setOnClickListener(v -> new Thread(() -> {

            Intent i = new Intent(Intent.ACTION_SEND);

            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"ifoodspprt@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "");
            i.putExtra(Intent.EXTRA_TEXT   , "");
            i.setType("message/rfc822");
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(About.this, "There are no email application installed.", Toast.LENGTH_SHORT).show();
            }
        }).start());
        sendWhatsApp.setOnClickListener(v -> openWhatsApp());
    }


    private void initUiViews() {
        tvDate = findViewById(R.id.tvCreatedBy);
        sendEmail = findViewById(R.id.sendEmail);
        sendWhatsApp = findViewById(R.id.sendWhatsApp);
        donate = findViewById(R.id.donate);
    }

    /**
     * Register our Broadcast Receiver when opening the app.
     */
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(bcr,filter);
    }

    /**
     * Stop our Broadcast Receiver when the app is closed.
     */
    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(bcr);
    }

    /**
     * This function responsible for sending WhatsApp message
     */
    private void openWhatsApp() {
        String smsNumber = "972546613551";
        boolean isWhatsAppInstalled = whatsAppInstalledOrNot();
        if (isWhatsAppInstalled) {

            Intent sendIntent = new Intent("android.intent.action.MAIN");
            sendIntent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Conversation")); // to open the conversation with the number
            sendIntent.putExtra("jid", PhoneNumberUtils.stripSeparators(smsNumber) + "@s.whatsapp.net");//phone number without "+" prefix

            startActivity(sendIntent);
        } else {
            // if not found prompt user to download WhatsApp from PlayStore
            Uri uri = Uri.parse("market://details?id=com.whatsapp");
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            Toast.makeText(this, "WhatsApp not Installed",
                    Toast.LENGTH_SHORT).show();
            startActivity(goToMarket);
        }
    }

    /**
     * Functions check if WhatsApp installed or not.
     * @return true if WhatsApp installed, false if not
     */
    private boolean whatsAppInstalledOrNot() {
        PackageManager pm = getPackageManager();
        boolean app_installed = false;
        try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("TAG","Exception :"+e.getMessage());
        }
        return app_installed;
    }
}
