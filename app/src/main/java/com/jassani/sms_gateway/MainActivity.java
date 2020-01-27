package com.jassani.sms_gateway;

import android.Manifest;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//import android.support.v4.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    public static TextView logInput;
    private TextView edPort;
    private MyHTTPD server;
    private TextView textIpaddr;

    /**
     * Lorsque l'appli est cree, on connecte les objets aux widgets de l'UI
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textIpaddr = findViewById(R.id.ipaddr);
        logInput = findViewById(R.id.logArea);
        edPort = (EditText) findViewById(R.id.port);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) { // test si WiFi actif
            Toast.makeText(this, "WiFi désactivé", Toast.LENGTH_LONG).show();
        }
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        if (ipAddress == 0) {  // test si @IP affectee
            Toast.makeText(this, "Pas d'adresse IP", Toast.LENGTH_LONG).show();
        }
        // formate l'@ en norme IPV4 en faisant confiance a l'utilisateur sur les données saisies
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        int port = Integer.parseInt(edPort.getText().toString()); // on recupere le port choisi par l'utilisateur
        // affiche le socket d'ecoute
        textIpaddr.setText("Accès : http://" + formatedIpAddress + ":" + port);
        try {
            if (server == null) {
                server = new MyHTTPD(port); // cree le serveur NanoHTTPD si inexistant
            }
            server.start();             // lance ou relance le serveur
            startService(new Intent(this, BackgroundService.class));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    /**
     * Lorsque l'appli est lancee ou re-activee,
     * le serveur est a l'ecoute
     * On recupere le numero du port sur l'UI et on affiche
     * le socket IP:port qui est actif
     * Lorsque le wifi n'est pas actif, .getIpAddress() retourne 0
     * pour une IP de 0.0.0.0
     */
    @Override
    protected void onResume() { // lance apres onCreate
        super.onResume();
    }

    /**
     * Si l'appli est mise en pause, le socket est libere
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    public static void log(TypeLog typeLog, String text) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd mm.ss : ");
        logInput.append('\n' + dateFormat.format(new Date()) + "[" + typeLog.toString() + "] : " + text);
    }
}


