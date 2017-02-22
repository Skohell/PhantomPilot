package com.dji.FPVDemo;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import dji.sdk.products.DJIAircraft;
import dji.sdk.base.DJIBaseProduct;

public class ConnectionActivity extends AppCompatActivity implements View.OnClickListener {

    /* ------------------------------ ELEMENTS GRAPHIQUES ------------------------------*/

    // Etat de la connection au drone.
    private TextView mTextConnectionStatus;

    // Nom du drone connecté.
    private TextView mTextProduct;

    // Boutton pour accéder à la vue de pilotage.
    private Button mBtnOpen;

    /* ---------------------------------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Le SDK peut nécessiter des permissions en plus selon certaines versions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_connection);

        // Initialisation de l'interface.
        initUI();

        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void onReturn(View view){
        this.finish();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /**
     * Méthode d'initialisation des éléments graphiques.
     */
    private void initUI() {

        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);

        mTextProduct = (TextView) findViewById(R.id.text_product_info);

        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBtnOpen.setOnClickListener(this);
        mBtnOpen.setEnabled(false); // Le boutton est désactivé tant qu'aucun porduit n'est connecté.

    }

    /**
     * Receiver permettant d'actualiser la vue quand un produit est connecté.
     */
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUI();
        }
    };


    /**
     * Méthode d'actualisation de l'affichage.
     */
    private void refreshUI() {

        DJIBaseProduct mProduct = FPVDemoApplication.getProductInstance();

        // Si on a bien un produit de connecté.
        if (null != mProduct && mProduct.isConnected()) {

            // On active le boutton
            mBtnOpen.setEnabled(true);

            // On met à jour les différents affichages.
            String str = mProduct instanceof DJIAircraft ? "DJIAircraft" : "DJIHandHeld";
            mTextConnectionStatus.setText("Status: " + str + " connecté");

            if (null != mProduct.getModel()) {
                mTextProduct.setText(mProduct.getModel().getDisplayName());
            } else {
                mTextProduct.setText(R.string.product_information);
            }

        } else {
            mBtnOpen.setEnabled(false);
            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
        }

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            // Clic sur le boutton "Ouvrir"
            case R.id.btn_open: {
                // On lance la main activity.
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            }
            default:
                break;
        }
    }

}
