package com.handterminalprinternfc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

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
import android.os.Environment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.Gson;

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

    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@React METHOD
    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    @ReactMethod
    public void Test() {
        try {

            // String path = "/data/data/com.yascayalim/databases/";
            // Log.d("DBFILE", "Path: " + path);
            // File directory = new File(path);
            // File[] files = directory.listFiles();
            // Log.d("DBFILE", "Size: " + files.length);
            // for (int i = 0; i < files.length; i++) {
            //     Log.d("DBFILE", "FileName:" + files[i].getName());
            // }

            final String inFileName = reactContext.getDatabasePath("dataDB").getPath();
            Log.d("DBFILE", inFileName);
            File dbFile = new File(inFileName);
            FileInputStream fis = new FileInputStream(dbFile);

            String outFileName = Environment.getExternalStorageDirectory() + "/database_copy.db";
            Log.d("DBFILE", outFileName);

            // Open the empty db as the output stream
            OutputStream output = new FileOutputStream(outFileName);

            // Transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            // Close the streams
            output.flush();
            output.close();
            fis.close();
            Log.d("DBFILE", "BAŞARILI");

        } catch (Exception ex) {
            Log.d("DBFILE", "DOSYA HATA" + ex.toString());
        }

    }

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
    public void PrintMsg(String data, Boolean Tekrar) {
        PrintText(data, Tekrar);
    }

    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@React
    // METHOD@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    // @@@@@@@@@@ PRINTER @@@@@@@@@@@@@@@@@@@@@@

    // Kagit Kontrol
    // Yazma talebi gonderildiginde cihazda kagit algilanmaz ise
    // DeviceEmitter tarafinda PrinterPaperStatus bolumunu tetikler
    private final static String PRNT_ACTION = "android.prnt.message";

    public void PrinterInit() {
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

    public void PrintText(String data, Boolean Tekrar) {
        Gson gson = new Gson();
        PrinterData pData = gson.fromJson(data, PrinterData.class);

        PrinterManager printer = new PrinterManager();

        printer.setupPage(384, -1);
        String fisTekrarMsg = "";
        if (Tekrar) {
            fisTekrarMsg = "\t\t\t\t\tFiş Tekrarı\n";
        }
        int ret = printer.drawTextEx("\n\t\t\t\t\t\tOFÇAY\n", 5, 0, 450, -1, "arial", 26, 0, 0x0001, 0);
        ret += printer.drawTextEx("\t\t\t\t" + pData.FisBaslik + "\n", 5, ret - 1, 450, -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx(fisTekrarMsg, 5, ret - 1, 450, -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx(GenerateAliciBilgileri(pData), 5, ret - 1, 450, -1, "arial", 24, 0, 0, 0);
        for (Alim alim : pData.Alimlar) {
            ret += printer.drawTextEx(GenerateUreticiBilgileri(alim), 5, ret - 1, 450, -1, "arial", 24, 0, 0, 0);

        }
        ret += printer.drawTextEx(" İmza :" + "______________________" + "\n\n\n\n\n", 5, ret - 1, 450, -1, "arial", 24,
                0, 0, 0);

        ret = printer.printPage(0);

        Intent i = new Intent("android.prnt.message");
        i.putExtra("ret", ret);

        reactContext.sendBroadcast(i);

    }

    private String GenerateAliciBilgileri(PrinterData data) {
        String text = "------------------------------------------------\n" + "\n" + "  Eksper Adı:\t" + data.EksperAdi
                + "\n" + "  Alım Yeri:\t" + data.AlimYeriAdi + "\n" + "  Fabrika:\t" + data.FabrikaAdi + "\n";

        return text;
    }

    private String GenerateUreticiBilgileri(Alim data) {
        String NakitBilgiler = "";
        String OdemeYapildi = "";
        if (data.NakitAlim) {
            NakitBilgiler = " Tutar\t" + data.Tutar + "\n" + " Kesinti Tutar\t" + data.KesintiTutar + "\n"
                    + " Net Tutar\t" + data.NetTutar + " \n";
            OdemeYapildi = data.NetTutar + " Ödeme yapıldı\n";
        }
        String text = "*********************************************\n" + " Mustashil Adı:\t" + data.UreticiAdi
                + "\n" + " Tarih/Saat:\t" + data.Tarih + "\n" + " Ağırlık:\t" + data.Agirlik + "\n" + " Fire:\t"
                + data.Fire + "\n" + " Cüzdan No:\t" + data.CuzdanNo + " \n" + NakitBilgiler + "\n"
                + "------------------------------------------------\n" + " Net Ağırlık:\t" + data.NetAgirlik + "\n"
                + " Ödeme: " + data.Odeme + "\n" + OdemeYapildi + "\n";
        return text;
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