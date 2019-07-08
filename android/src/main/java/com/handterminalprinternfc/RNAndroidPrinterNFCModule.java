package com.handterminalprinternfc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

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
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

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
    public void Backup() {
        try {

            // String path = "/data/data/com.yascayalim/databases/";
            // Log.d("DBFILE", "Path: " + path);
            // File directory = new File(path);
            // File[] files = directory.listFiles();
            // Log.d("DBFILE", "Size: " + files.length);
            // for (int i = 0; i < files.length; i++) {
            // Log.d("DBFILE", "FileName:" + files[i].getName());
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
    public void BackupMail(String DeviceInfo) {
        Log.d("DBFILE", "BAŞLADI");
        Backup();
        Log.d("DBFILE", "BİTTİ");
        Log.d("DBFILE", "COMPRESS");

        final String deviceInfo = DeviceInfo;
        final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        final Date date = new Date(System.currentTimeMillis());
        final String dbFile = Environment.getExternalStorageDirectory() + "/database_copy.db";
        final String ZipFile = Environment.getExternalStorageDirectory() + "/" + DeviceInfo + "backup.zip";
        CompressFile(dbFile, ZipFile);
        Log.d("DBFILE", "COMPRESSFIN");

        new Thread(new Runnable() {
            @Override

            public void run() {
                try {

                    Log.d("DBFILE", "MAIL SENDING");

                    GMailSender sender = new GMailSender("desk@cts.com.tr", "Desk12?");
                    sender.sendMail("ÇAY ALIM " + deviceInfo + " Kodlu Cihaz BACKUP",
                            "Cay alim uygulamasinda " + deviceInfo + " kodlu cihazin" + formatter.format(date)
                                    + " tarihli backup dosyasidir ",
                            ZipFile, "desk@cts.com.tr",
                            "selcuk.aksar@cts.com.tr,cem.elma@cts.com.tr,kadir.avci@cts.com.tr");
                } catch (Exception e) {
                    Log.e("SendMail", e.getMessage(), e);
                }

            }
        }).start();

    }

    static final int BUFFER = 2048;

    public void CompressFile(String file, String zipFileName) {
        try {
            BufferedInputStream origin = null;
            FileOutputStream dest = new FileOutputStream(zipFileName);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte data[] = new byte[BUFFER];

            Log.v("Compress", "Adding: " + file);
            FileInputStream fi = new FileInputStream(file);
            origin = new BufferedInputStream(fi, BUFFER);

            ZipEntry entry = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
            out.putNextEntry(entry);
            int count;

            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
            origin.close();

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
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
    public void PrintFis(String data, Boolean Tekrar) {
        FisYazdir(data, Tekrar);
    }

    @ReactMethod
    public void PrintGunSonuFis(String data) {
        GunSonuFisYazdir(data);
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

    public void FisYazdir(String data, Boolean Tekrar) {
        Gson gson = new Gson();
        PrinterData pData = gson.fromJson(data, PrinterData.class);
        Alim alim = pData.Alim;
        PrinterManager printer = new PrinterManager();

        printer.setupPage(384, -1);
        int ret = 0;
        String fisTekrarMsg = "";

        // Baslik
        if (Tekrar) {
            fisTekrarMsg = "\t\t\t\t\tFiş Tekrarı\n";
        }
        ret += printer.drawTextEx("\n\t\t\t\t\t\tOFÇAY\n", 5, 0, 384, -1, "arial", 26, 0, 0x0001, 0);
        ret += printer.drawTextEx("\t\t\t\t" + pData.FisBaslik + "\n", 5, ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx(fisTekrarMsg, 5, ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx(GenerateAliciBilgileri(pData), 5, ret - 1, 384, -1, "arial", 24, 0, 0, 0);
        // Baslik Bitis

        // Uretici Alim
        String OdemeYapildi = "";
        if (alim.NakitAlim) {
            OdemeYapildi = alim.NetTutar + " Ödeme yapıldı\n";
        }

        ret += printer.drawTextEx(GenerateUreticiBilgileri(alim), 5, ret - 1, 384, -1, "arial", 24, 0, 0, 0);
        ret += printer.drawTextEx(" Net Ağırlık:\t" + alim.NetAgirlik + "\n", 5, ret - 1, 384, -1, "arial", 24, 0,
                0x0001, 0);

        String OdemeText = "";
        String[] arrText = alim.Odeme.split(" ");
        int i = 0;
        String msg = "";
        for (i = 0; i < arrText.length; i++) {
            if (msg.length() + arrText[i].length() <= 22) {
                OdemeText += " " + arrText[i];
                msg += " " + arrText[i];
            } else {
                OdemeText += "\n" + arrText[i];
                msg = arrText[i];
            }
        }
        ret += printer.drawTextEx(" Ödeme: " + OdemeText + "\n" + OdemeYapildi + "\n", 5, ret - 1, 384, -1, "arial", 24,
                0, 0x0001, 0);

        ret += printer.drawTextEx(" İmza :" + "______________________" + "\n\n\n\n\n", 5, ret - 1, 384, -1, "arial", 24,
                0, 0, 0);
        // Uretici Alim Bitis
        ret = printer.printPage(0);

        // if (pData.GunSonuMu) {
        // ret += printer.drawTextEx("**********************************\n", 5, ret - 1,
        // 384, -1, "arial", 25, 0,
        // 0x0001, 0);
        // ret += printer.drawTextEx("\t\t " + pData.VadeTanim + " Ağırlık Özeti \n", 5,
        // ret - 1, 384, -1, "arial", 25,
        // 0, 0x0001, 0);
        // ret += printer.drawTextEx("Toplam Brüt Ağırlık : " +
        // pData.ToplamAlimKg.toString() + " KG", 5, ret - 1, 384,
        // -1, "arial", 25, 0, 0x0001, 0);
        // ret += printer.drawTextEx("Toplam Fire Ağırlık : " +
        // pData.ToplamKesintiKg.toString() + " KG", 5, ret - 1,
        // 384, -1, "arial", 25, 0, 0x0001, 0);
        // ret += printer.drawTextEx("Toplam Net Ağırlık : " +
        // pData.ToplamNetKg.toString() + " KG" + "\n\n", 5,
        // ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
        // }

        Intent myIntent = new Intent("android.prnt.message");
        myIntent.putExtra("ret", ret);

        reactContext.sendBroadcast(myIntent);

    }

    public void GunSonuFisYazdir(String data) {
        Gson gson = new Gson();
        PrinterData pData = gson.fromJson(data, PrinterData.class);
        PrinterManager printer = new PrinterManager();

        printer.setupPage(384, -1);
        int ret = 0;

        // Baslik

        ret += printer.drawTextEx("\n\t\t\t\t\t\tOFÇAY\n", 5, 0, 384, -1, "arial", 26, 0, 0x0001, 0);
        ret += printer.drawTextEx("\t\t\t\t" + pData.FisBaslik + "\n", 5, ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx(GenerateAliciBilgileri(pData), 5, ret - 1, 384, -1, "arial", 24, 0, 0, 0);
        // Baslik Bitis

        // Uretici Alim

        // Vade grubu ile donme
        for (VadeAlim vadeAlim : pData.Vadeler) {

            /* Vade Baslik Ayarlanmasi */
            String OdemeText = "";
            String[] arrText = vadeAlim.VadeFiyatAciklama.split(" ");
            int i = 0;
            String msg = "";
            for (i = 0; i < arrText.length; i++) {
               
                if (msg.length() + arrText[i].length() <= 24) {
                    OdemeText += " " + arrText[i].replaceAll("\\W", ""); ;
                    msg += " " + arrText[i].replaceAll("\\W", ""); ;
                } else {
                    OdemeText += "\n" + arrText[i].replaceAll("\\W", ""); ;
                    msg = arrText[i].replaceAll("\\W", ""); ;
                }
            }
            Log.w("ODEMETEXT", OdemeText);

            ret += printer.drawTextEx("** "+ OdemeText +" **\n", 5, ret - 1, 384, -1, "arial", 24, 0, 0x0001, 0);

            /* Vade Baslik Ayarlanmasi */

            /* Ayni Vadedeki Alimlarin Yazilmasi */
            for (Alim alim : vadeAlim.Alimlar) {

                ret += printer.drawTextEx(GenerateUreticiBilgileri(alim), 5, ret - 1, 384, -1, "arial", 24, 0, 0, 0);
                ret += printer.drawTextEx(" Net Ağırlık:\t" + alim.NetAgirlik + "\n", 5, ret - 1, 384, -1, "arial", 24,
                        0, 0x0001, 0);

            }
            /* Ayni Vadedeki Alimlarin Yazilmasi */

            /* Vade Toplam Ozetleri */
            ret += printer.drawTextEx("**********************************\n", 5, ret - 1, 384, -1, "arial", 25, 0,
                    0x0001, 0);
            ret += printer.drawTextEx("\t\t " + vadeAlim.VadeTanim + " Ağırlık Özeti \n", 5, ret - 1, 384, -1, "arial",
                    25, 0, 0x0001, 0);
            ret += printer.drawTextEx("Toplam Brüt Ağırlık : " + vadeAlim.ToplamAlimKg.toString() + " KG", 5, ret - 1,
                    384, -1, "arial", 25, 0, 0x0001, 0);
            ret += printer.drawTextEx("Toplam Fire Ağırlık : " + vadeAlim.ToplamKesintiKg.toString() + " KG", 5,
                    ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
            ret += printer.drawTextEx("Toplam Net Ağırlık : " + vadeAlim.ToplamNetKg.toString() + " KG" + "\n\n", 5,
                    ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
            ret += printer.drawTextEx("-----------------------------------------\n", 5,
                    ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
            /* Vade Toplam Ozetleri */

        }

        /* Genel Toplam Ozetleri */
        ret += printer.drawTextEx("#######################\n", 5, ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx("\t TOPLAM AĞIRLIK ÖZETİ \n", 5, ret - 1, 384, -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx("Toplam Brüt Ağırlık : " + pData.ToplamAlimKg.toString() + " KG", 5, ret - 1, 384, -1,
                "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx("Toplam Fire Ağırlık : " + pData.ToplamKesintiKg.toString() + " KG", 5, ret - 1, 384,
                -1, "arial", 25, 0, 0x0001, 0);
        ret += printer.drawTextEx("Toplam Net Ağırlık : " + pData.ToplamNetKg.toString() + " KG" + "\n\n", 5, ret - 1,
                384, -1, "arial", 25, 0, 0x0001, 0);

        /* Genel Toplam Ozetleri */
        ret += printer.drawTextEx(" İmza :" + "______________________" + "\n\n\n\n\n", 5, ret - 1, 384, -1, "arial", 24,
                0, 0, 0);
        // Uretici Alim Bitis
        ret = printer.printPage(0);

        Intent myIntent = new Intent("android.prnt.message");
        myIntent.putExtra("ret", ret);

        reactContext.sendBroadcast(myIntent);

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
            NakitBilgiler = " Tutar:\t" + data.Tutar + "\n" + " Kesinti Tutar:\t" + data.KesintiTutar + "\n"
                    + " Net Tutar:\t" + data.NetTutar + " \n";
            OdemeYapildi = data.NetTutar + " Ödeme yapıldı\n";
        }
        String text = "**************************************\n" + " Mustahsil Adı:\t" + data.UreticiAdi + "\n"
                + " Cüzdan No:\t" + data.CuzdanNo + " \n" + " Tarih/Saat:\t" + data.Tarih + "\n" + " Brüt Ağırlık:\t"
                + data.Agirlik + "\n" + " Fire:\t" + data.Fire + "\n" + NakitBilgiler
                + "------------------------------------------------";
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