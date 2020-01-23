package com.jassani.sms_gateway;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import fi.iki.elonen.NanoHTTPD;

//import android.support.v4.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    public static TextView numTexto;
    public static TextView txtTexto;
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
                server = new MyHTTPD(port, numTexto, txtTexto); // cree le serveur NanoHTTPD si inexistant
            }
            server.start();             // lance ou relance le serveur
            startService(new Intent(this, YourBackgroundService.class));
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
////        if (server != null) {
//            server.stop();   // serveur non operationnel quand l'appli est en pause
//        }
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

        handler = new Handler();
    }

    /**
     * Methode appelee apres start.
     * Cree le socket d'ecoute et attend le client
     *
     * @param session
     * @return Response code de fin de connection 200 si ok
     */

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public Response serve(IHTTPSession session) {
        final Context context = App.getContext();
        final String SimID = session.getParameters().get("simID").get(0);
        final String NumTel = session.getParameters().get("number").get(0);
        final String Text = session.getParameters().get("text").get(0);

        if (session.getMethod() == Method.GET) {



//            numWidget.setText("Numéro : " + NumTel); // met a jour l'UI
//            txtWidget.setText("Message : " + Text);

            SimUtil.sendSMS(context, Integer.parseInt(SimID), NumTel, null, Text, null, null);
            MainActivity.numTexto.setText(SimID + " --> " + NumTel + " --> " + Text);
            return newFixedLengthResponse("ok");
        }


        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                "The requested resource does not exist");
    }



//    @Override
//    public Response serve(IHTTPSession session) {
//        final JSONdecodeur jsonDec;
//        byte[] messageDuClient = new byte[MAXCHAR];
//        int tailleMessageClient = 0;
//        try {  // conversion d'un tableau en String
//            tailleMessageClient = session.getInputStream().read(messageDuClient, 0, MAXCHAR);
//            String jsonMsg = new String(messageDuClient);
//            jsonMsg = jsonMsg.substring(0, tailleMessageClient);
//            jsonDec = new JSONdecodeur(jsonMsg);
//
//            final Context context = App.getContext();
//            handler.post(new Runnable() {
//                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
//                @Override
//                public void run() {   // affiche le numero et le texte du SMS envoye
//                    if (jsonDec.isParsed()) {  // seulement si JSON valide
//                        numWidget.setText("Numéro : " + jsonDec.getNumTel()); // met a jour l'UI
//                        txtWidget.setText("Message : " + jsonDec.getTxtSMS());
//
//                      //  SmsManager smsManager = SmsManager.getDefault();  // envoi du SMS
//                      //  smsManager.sendTextMessage(jsonDec.getNumTel(), null, jsonDec.getTxtSMS(),null, null);
//
//                        SimUtil.sendSMS(context, Integer.parseInt(jsonDec.getSimID()), jsonDec.getNumTel(), null, jsonDec.getTxtSMS(), null, null);
//                    } else {
//                        numWidget.setText("Format du JSON"); // met a jour l'UI
//                        txtWidget.setText("Revoir la syntaxe");
//                    }
//                }
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        // renvoi code de fin au client
//        return newFixedLengthResponse(java.net.HttpURLConnection.HTTP_OK + "\n");
//    }



}


class JSONdecodeur {
    private String numTel, txtSMS;
    private String simID;
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
                simID = (String) jsonSMS.get("simID");  // retourne la valeur du champ simID
                if (numTel != null && txtSMS != null && simID != null) {
                    isParsed = true;
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    String getSimID() {
        return simID;
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


    Boolean isParsed() {
        return isParsed;
    }
}


class SimUtil {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public static boolean sendSMS(Context ctx, int simID, String toNum, String centerNum, String smsText, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        String name;
//        int subscriptionId = 0;
//        SubscriptionManager subscriptionManager = SubscriptionManager.from(ctx);
//        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
//        for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
//            subscriptionId = subscriptionInfo.getSubscriptionId();
//            Log.d("apipas","subscriptionId:"+subscriptionId);
//        }


        try {
            if (simID == 1) {
                name = "isms";
                // for model : "Philips T939" name = "isms0"
            } else if (simID == 2) {
                name = "isms2";
            } else {
                throw new Exception("can not get service which for sim '" + simID + "', only 1,2 accepted as values");
            }
            Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
            method.setAccessible(true);
            Object param = method.invoke(null, name);

            method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
            method.setAccessible(true);
            Object stubObj = method.invoke(null, param);
            if (Build.VERSION.SDK_INT < 18) {
                method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                method.invoke(stubObj, toNum, centerNum, smsText, sentIntent, deliveryIntent);
            } else {
//                method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                SmsManager.getSmsManagerForSubscriptionId(simID).sendTextMessage(toNum, centerNum, smsText, sentIntent, deliveryIntent);
//                method.invoke(stubObj,ctx.getPackageName(),  toNum, centerNum, smsText, sentIntent, deliveryIntent);

            }
            return true;
        } catch (ClassNotFoundException e) {
            Log.e("apipas", "ClassNotFoundException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.e("apipas", "NoSuchMethodException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            Log.e("apipas", "InvocationTargetException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e("apipas", "IllegalAccessException:" + e.getMessage());
        } catch (Exception e) {
            Log.e("apipas", "Exception:" + e.getMessage());
        }
        return false;

    }

//    public static boolean sendMultipartTextSMS(Context ctx, int simID, String toNum, String centerNum, ArrayList<String> smsTextlist, ArrayList<PendingIntent> sentIntentList, ArrayList<PendingIntent> deliveryIntentList) {
//        String name;
//        try {
//            if (simID == 0) {
//                name = "isms";
//                // for model : "Philips T939" name = "isms0"
//            } else if (simID == 1) {
//                name = "isms2";
//            } else {
//                throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
//            }
//            Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
//            method.setAccessible(true);
//            Object param = method.invoke(null, name);
//
//            method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
//            method.setAccessible(true);
//            Object stubObj = method.invoke(null, param);
//            if (Build.VERSION.SDK_INT < 18) {
//                method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, List.class, List.class, List.class);
//                method.invoke(stubObj, toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList);
//            } else {
//                method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, String.class, List.class, List.class, List.class);
//                method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList);
//            }
//            return true;
//        } catch (ClassNotFoundException e) {
//            Log.e("apipas", "ClassNotFoundException:" + e.getMessage());
//        } catch (NoSuchMethodException e) {
//            Log.e("apipas", "NoSuchMethodException:" + e.getMessage());
//        } catch (InvocationTargetException e) {
//            Log.e("apipas", "InvocationTargetException:" + e.getMessage());
//        } catch (IllegalAccessException e) {
//            Log.e("apipas", "IllegalAccessException:" + e.getMessage());
//        } catch (Exception e) {
//            Log.e("apipas", "Exception:" + e.getMessage());
//        }
//        return false;
//    }
}


