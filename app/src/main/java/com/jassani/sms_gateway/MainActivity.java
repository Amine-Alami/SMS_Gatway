package com.jassani.sms_gateway;

/*
http://x.x.x.x:8765
        {"SMS": {
        "number": "06XXXXXXXX",
        "text": "mon message SMS"
        }}
*/

import android.Manifest;
import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends Activity {
    private TextView numTexto;
    private TextView txtTexto;
    private TextView edPort;
    private MyHTTPD server;
    private TextView textIpaddr;
    private static final int PERMISSION_REQUEST_CODE = 1;

    /**
     * Lorsque l'appli est cree, on connecte les objets aux widgets de l'UI
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textIpaddr = (TextView) findViewById(R.id.ipaddr);
        numTexto = (TextView) findViewById(R.id.numSMS);
        txtTexto = (TextView) findViewById(R.id.txtSMS);
        edPort = (EditText) findViewById(R.id.port);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
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
                server = new MyHTTPD(port, numTexto, txtTexto); // cree le serveur NanoHTTPD si inexistant
            }
            server.start();             // lance ou relance le serveur
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Si l'appli est mise en pause, le socket est libere
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (server != null) {
            server.stop();   // serveur non operationnel quand l'appli est en pause
        }
    }
}

class MyHTTPD extends NanoHTTPD {
    private Handler handler; // necessaire pour actualiser l'UI
    private TextView numWidget;
    private TextView txtWidget;
    final int MAXCHAR = 200;  // taille maximale du JSON recu
    /**
     * Constructeur qui initialise les variables
     * et cree un handler pour se connecter a l'UI
     */
    public MyHTTPD(int port, TextView numWid, TextView textWid) throws IOException {
        super(port);
        this.numWidget = numWid;
        this.txtWidget = textWid;
        handler = new Handler();
    }
    /**
     * Methode appelee apres start.
     * Cree le socket d'ecoute et attend le client
     *
     * @param session
     * @return Response code de fin de connection 200 si ok
     */
    @Override
    public Response serve(IHTTPSession session) {
        final JSONdecodeur jsonDec;
        byte[] messageDuClient = new byte[MAXCHAR];
        int tailleMessageClient = 0;
        try {  // conversion d'un tableau en String
            tailleMessageClient = session.getInputStream().read(messageDuClient,0,MAXCHAR);
            String jsonMsg = new String(messageDuClient);
            jsonMsg = jsonMsg.substring(0, tailleMessageClient);
            jsonDec = new JSONdecodeur(jsonMsg);

                handler.post(new Runnable() {
                    @Override
                    public void run() {   // affiche le numero et le texte du SMS envoye
                        if (jsonDec.isParsed()) {  // seulement si JSON valide
                            numWidget.setText("Numéro : " + jsonDec.getNumTel()); // met a jour l'UI
                            txtWidget.setText("Message : " + jsonDec.getTxtSMS());
                            SmsManager smsManager = SmsManager.getDefault();  // envoi du SMS
                            smsManager.sendTextMessage(jsonDec.getNumTel(), null, jsonDec.getTxtSMS(),
                                    null, null);
                        }
                        else {
                            numWidget.setText("Format du JSON"); // met a jour l'UI
                            txtWidget.setText("Revoir la syntaxe");
                        }
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
        // renvoi code de fin au client
        return newFixedLengthResponse(java.net.HttpURLConnection.HTTP_OK + "\n");
    }
}

class JSONdecodeur {
    private String numTel, txtSMS;
    private boolean isParsed = false;
    /**
     * Methode d'analyse d'une String au format JSON
     * et qui extrait les valeurs des champs "number" et "text"
     *
     * @param jsonString
     */
    JSONdecodeur(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(jsonString);
            // retourne un JSONObject qui est la valeur du champ SMS
            JSONObject jsonSMS = (JSONObject) jsonObject.get("SMS");
            if (jsonSMS != null) {
                numTel = (String) jsonSMS.get("number");  // retourne la valeur du champ number
                txtSMS = (String) jsonSMS.get("text");    // retourne la valeur du champ text
                if (numTel != null && txtSMS != null) {
                    isParsed = true;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retourne le numero du SMS à envoyer, extrait du code JSON
     *
     * @return String numero du SMS
     */
    String getNumTel() {
        return numTel;
    }

    /**
     * Retourne le texte du SMS à envoyer, extrait du code JSON
     *
     * @return String message du SMS
     */
    String getTxtSMS() {
        return txtSMS;
    }
    /**
     * Retourne true si le traitement est correct et exploitable
     *
     * @return Boolean
     */
    Boolean isParsed() { return isParsed;}
}