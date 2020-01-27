package com.jassani.sms_gateway;

import android.app.PendingIntent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class SimUtil {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public static boolean sendSMS(int simID, String toNum, String centerNum, String smsText, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        String name;
        try {
            if (simID == 1) {
                name = "isms";
            } else if (simID == 2) {
                name = "isms2";
            } else {
                MainActivity.log(TypeLog.ERROR, "can not get service which for sim '" + simID + "', only 1,2 accepted as values");
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
                SmsManager.getSmsManagerForSubscriptionId(simID).sendTextMessage(toNum, centerNum, smsText, sentIntent, deliveryIntent);
            }
            return true;
        } catch (ClassNotFoundException e) {
            MainActivity.log(TypeLog.ERROR, "ClassNotFoundException:" + e.getMessage());
            Log.e("apipas", "ClassNotFoundException:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            MainActivity.log(TypeLog.ERROR, "NoSuchMethodException:" + e.getMessage());
            Log.e("apipas", "NoSuchMethodException:" + e.getMessage());
        } catch (InvocationTargetException e) {
            MainActivity.log(TypeLog.ERROR, "InvocationTargetException:" + e.getMessage());
            Log.e("apipas", "InvocationTargetException:" + e.getMessage());
        } catch (IllegalAccessException e) {
            MainActivity.log(TypeLog.ERROR, "IllegalAccessException:" + e.getMessage());
            Log.e("apipas", "IllegalAccessException:" + e.getMessage());
        } catch (Exception e) {
            MainActivity.log(TypeLog.ERROR, "Exception:" + e.getMessage());
            Log.e("apipas", "Exception:" + e.getMessage());
        }
        return false;

    }
}
