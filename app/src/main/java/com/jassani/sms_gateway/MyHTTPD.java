package com.jassani.sms_gateway;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

class MyHTTPD extends NanoHTTPD {

    /**
     * Constructeur qui initialise les variables
     * et cree un handler pour se connecter a l'UI
     */
    public MyHTTPD(int port) throws IOException {
        super(port);
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
        Map<String, List<String>> parameters = session.getParameters();
        if (session.getMethod() == Method.GET && parameters != null && parameters.size() > 0) {

            // simID
            List<String> simIDList = parameters.get("simID");
            if (simIDList == null || simIDList.size() == 0) {
                MainActivity.log(TypeLog.INFO, "The request is malformed: simID is not defined.");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "The request is malformed: simID is not defined.");
            }
            String simID = simIDList.get(0);
            if (!simID.matches("[12]")) {
                MainActivity.log(TypeLog.INFO, "The request is malformed: simID is not a valid number.");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "The request is malformed: simID is not a valid number.");
            }
            // number
            List<String> numberList = parameters.get("number");
            if (numberList == null || numberList.size() == 0) {
                MainActivity.log(TypeLog.INFO, "The request is malformed: number is not defined.");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "The request is malformed: field 'number' is not defined.");
            }
            String numeroDestinataire = numberList.get(0);

            // text
            List<String> textList = parameters.get("text");
            if (textList == null || textList.size() == 0) {
                MainActivity.log(TypeLog.INFO, "The request is malformed: text is not defined.");
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "The request is malformed: field 'text' is not defined.");
            }
            String message = textList.get(0);
            SimUtil.sendSMS(Integer.parseInt(simID), numeroDestinataire, null, message, null, null);
            MainActivity.log(TypeLog.INFO, simID + " --> " + numeroDestinataire + " --> " + message);
            return newFixedLengthResponse("ok");
        } else {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "The request is malformed");
        }
    }
}
