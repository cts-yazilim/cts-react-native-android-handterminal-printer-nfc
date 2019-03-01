package com.handterminalprinternfc;

import android.util.Log;
import android.device.PiccManager;
import android.device.PrinterManager;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class RNAndroidPrinterNFCModule extends ReactContextBaseJavaModule {

    public static ReactApplicationContext reactContext;

    int scan_card = -1;
    int SNLen = -1;

    public RNAndroidPrinterNFCModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAndroidPrinterNFC";
    }

    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@React METHOD @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    @ReactMethod
    public void NfcInit() {
        NfcReaderInit();
    }
    @ReactMethod
    public void NfcOpen() {
        OpenReader();
    }

    @ReactMethod
    public void NfcManagerOku() {
        CheckPicc();
    }

   
    @ReactMethod
    public void PrinterCreate() {
        PrinterInit();
    }
   

    @ReactMethod
    public void PrintMsg(String data) {
        PrintText(data);
    }

    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@React METHOD@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    // @@@@@@@@@@ PRINTER @@@@@@@@@@@@@@@@@@@@@@

    // Kagit Kontrol
    // Yazma talebi gonderildiginde cihazda kagit algilanmaz ise
    // DeviceEmitter tarafinda PrinterPaperStatus bolumunu tetikler
    private final static String PRNT_ACTION = "android.prnt.message";
    public void PrinterInit()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PRNT_ACTION);
        reactContext.registerReceiver(mPrtReceiver, filter);
    }

    private BroadcastReceiver mPrtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int ret = intent.getIntExtra("ret", 0);
            if (ret == -1) {
                WritableMap map = new WritableNativeMap();
                map.putString("Err", "True");
                sendEvent("PrinterPaperStatus", map);
            }
        }
    };

    public void PrintText(String msg) 
    {
        PrinterManager printer = new PrinterManager();
        
        printer.setupPage(384, -1);
        int ret =printer.drawTextEx(msg, 5, 0,300,-1, "arial",  24, 0, 0, 0);
        ret=printer.printPage(0);
        
        Intent i = new Intent("android.prnt.message");
        i.putExtra("ret", ret);

        reactContext.sendBroadcast(i);
       
    }

    // @@@@@@@@@@ PRINTER @@@@@@@@@@@@@@@@@@@@@@

    // @@@@@@@@@@ NFC READER @@@@@@@@@@@@@@@@@@@@@@

    private static final int MSG_READING_SUCCESS = 1;
    private static final int MSG_READING_FAIL = 2;

    private PiccManager piccReader;
    private Handler handler;
    private ExecutorService exec;
    private Callback returnCallback;

    // Start Reader
    public void OpenReader() {
        exec.execute(new Thread(new Runnable() {
            @Override
            public void run() {
                piccReader.open();
            }
        }, "picc open"));
    }

    // Read NfcCard
    /*
     * Nfc okutuldugunda DeviceEmitter tarafimna PiccManager altinda data olarak
     * gonderilir
     */
    /*
     * gonderilen datalar HasErr : True -False ErrMsg : String Tag : String
     */

    public void NfcReaderInit() {
        piccReader = new PiccManager();
        exec = Executors.newSingleThreadExecutor();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                WritableMap map = new WritableNativeMap();

                switch (msg.what) {
                case MSG_READING_SUCCESS:

                    String uid = (String) msg.obj;

                    map.putString("HasError", "False");
                    map.putString("ErrMsg", "");
                    map.putString("Tag", uid);

                    break;
                case MSG_READING_FAIL:

                    map.putString("HasError", "True");
                    map.putString("ErrMsg", "Kart okunamadı.");
                    map.putString("Tag", "-1");
                    break;
                default:
                    break;
                }

                sendEvent("PiccManager", map);
                super.handleMessage(msg);
            }
        };
    }

    public void CheckPicc() {
        exec.execute(new Thread(new Runnable() {

            @Override
            public void run() {

                byte CardType[] = new byte[2];
                byte Atq[] = new byte[14];
                char SAK = 1;
                byte sak[] = new byte[1];
                sak[0] = (byte) SAK;
                byte SN[] = new byte[10];
                scan_card = piccReader.request(CardType, Atq);
                Log.w("TEST", "OK");

                Log.w("TEST", Integer.toString(scan_card));
                if (scan_card > 0) {
                    SNLen = piccReader.antisel(SN, sak);
                    Message msg = handler.obtainMessage(MSG_READING_SUCCESS);
                    msg.obj = bytesToHexString(SN, SNLen);
                    handler.sendMessage(msg);
                }

            }
        }, "picc check"));
    }

    // @@@@@@@@@@ NFC READER @@@@@@@@@@@@@@@@@@@@@@

    // @@@@@@@@@@ FUNCTIONS @@@@@@@@@@@@@@@@@@@@@@
    private static String bytesToHexString(byte[] src, int len) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        if (len <= 0) {
            return "";
        }
        for (int i = 0; i < len; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    private void sendEvent(String eventName, WritableMap map) {
        try {

            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, map);
        } catch (Exception e) {
            Log.d("ReactNativeJS", "Exception in sendEvent in ReferrerBroadcastReceiver is:" + e.toString());
        }

    }
    // @@@@@@@@@@ FUNCTIONS @@@@@@@@@@@@@@@@@@@@@@

}