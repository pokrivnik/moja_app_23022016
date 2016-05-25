package com.example.hradi.moja_app_23022016;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.inputmethodservice.KeyboardView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.Objdetect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Message;
import java.net.UnknownHostException;

import java.net.Socket;


public class konecne extends AppCompatActivity {
    //---------------------------------------INFO------------------------------------------------------------------------------------------------
    //pridanie kniznice opencv do projektu
//http://blog.codeonion.com/2015/11/25/creating-a-new-opencv-project-in-android-studio/

    //---------------------------------------premenne------------------------------------------------------------------------------------------
    TextView zobraz_text_v_textviev;
    static final String LOG_TAG = "Start APP";
    private DatagramSocket ds;
    private byte[] lMsg;
    private DatagramPacket dp;
    private byte[] obrazok;
    private int a = 0;
    private int pom = 0;
    private String lText;
    private String text = "";
    private String tcp1;
    private String tcp2;
    private boolean pom_connect, pom_video = false, pom_video_start = false;
    private final Object lockObject = new Object();
    private String text_old, text_new;
    private final Object lockObject1 = new Object();


    //--------------------------------------------------------------------------------------------------------------------------------------
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(LOG_TAG, "OpenCV loaded successfully");


                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    //----------------------------------------------------------------------------------------------------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_konecne);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); //zabrani otoceniu obrazovky
        //pociatocna inicializacia prvkov ako textview
        zobraz_text_v_textviev = (TextView) findViewById(R.id.textView);
        //zobraz_text_v_textviev.setText("0");

        final Button tlacitko_connect = (Button) findViewById(R.id.connect);
        imgView = (ImageView) findViewById(R.id.imageView);
        //---------------------------------------------------tlacitka-------------------------------------------------------------------------
        final Button spusti_video = (Button) findViewById(R.id.spusti_video);

        // funkcia timer--------------------------------------------------------------------------------------------------------

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                TimerMethod();
            }
        }, 0, 1);

        // -----------------------funkcia pre kliknutie na tlacitko - prijate suboru-----------------------------

        tlacitko_connect.setOnClickListener(new View.OnClickListener() {
            //stlacenim tlacitka sa spusti pociatocna inicializacia. spusti sa vlakno s tcp komunikaciou na porte 1235, kde sa odosle
            //prikaz start, ktory bude cakat na odpoved, mozeme zacat...po stlaceni tlacitka sa zmeni nazov tlacitka na disconnect,
            //pri stlaceni tlacitka disconnect sa do raspberry odosle prikaz stop, ktory zastavy vsetku komunikaciu aj veskeru cinnost
            @Override
            public void onClick(View v) {
                if (pom_connect == true) {
                    pom_connect = false;//ukonci hlavny komunikacny thread
                    tlacitko_connect.setText("connect");
                    Log.i(LOG_TAG, "pom_connect=false");
                    spusti_video.setText("Spusti video");
                    pom_video = false;
                } else {//spusti hlavny komunikacny thread
                    pom_connect = true;
                    tlacitko_connect.setText("disconnect");
                    Log.i(LOG_TAG, "pom_connect=true");
                    TcpClient();

                }


            }
        });
//----------------------------------------------------------------------tlacitko spusti video---------------------------------------------------------------------------------
        spusti_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pom_video == false) {
                    pom_video = true;
                    spusti_video.setText("Video ON");
                }
                /*else{
                    pom_video = true;
                    spusti_video.setText("Spusti video");
                }*/
            }


        });

    }
    // ------------------------------------------------------------casovac---------------------------------------------------------------------
    private void TimerMethod() {
        this.runOnUiThread(Timer_Tick);
    }

    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            //text();
            zapis_text();
            if (a == 1) {
                a=0;
                obrazok();


            }
        }
    };
    private ImageView imgView;

    //------------------------------------------------------------------------------------------------------------------------------------------
    public void zapis_text(){
        if (text_old!=text_new){
            Log.d(LOG_TAG, "text_old = text_new");
            synchronized (lockObject1) {
                text_old = text_new;
            }
            zobraz_text_v_textviev.setText("\n" + text_old);//"" System.getProperty("line.separator"));//"\n");//System.getProperty("line.separator"
                    Log.d(LOG_TAG, "zapisujem");
        }

    }

    //--------------------------------------------------------------------funkcia UDP z UDP client------------------------------------------------------------------------
    public String prijem() {
        Log.d(LOG_TAG, "fce prijem");
        try {
            Log.d(LOG_TAG, "try");

        } finally {

        }

        Thread thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(LOG_TAG, "start recv thread, thread id: "
                        + Thread.currentThread().getId());
                // funkcia z ineho vlakna
                try {
                    ds = new DatagramSocket(1234, InetAddress.getByName("192.168.0.11"));
                    lMsg = new byte[64000];
                    Log.d(LOG_TAG, "vytvorenie ds");

                    while (true) {
                        Log.d(LOG_TAG, "while");
                        dp = new DatagramPacket(lMsg, lMsg.length);
                        Log.d(LOG_TAG, "packet");
                        ds.receive(dp);
                        //---------------------------------------------------

                        byte[] buff = dp.getData();

                        Log.d(LOG_TAG, "prijem");
                        obrazok = buff;
                        //Bitmap bmp = BitmapFactory.decodeByteArray(buff, 0, dp.getLength());
                        //bmp2 = bmp;
                        pom = dp.getLength();
                        a = 1;
                        //Thread.sleep(1);
                        // --------------------------------------------------------------------------------------------------------
                        if (pom_video_start = false){
                            Thread.sleep(100);
                            break;
                        }
                    }
                    ds.close();
                } catch (SocketException se) {
                    Log.e(LOG_TAG, "SocketException: " + se.toString());
                } catch (IOException ie) {
                    Log.e(LOG_TAG, "IOException" + ie.toString());
                } /*catch (InterruptedException e) {
                    e.printStackTrace();
                }*/ catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        });
        Log.d(LOG_TAG, "thread close");

        thrd.start();
        Log.d(LOG_TAG, "vrati text");
        return lText;

    }

    public void text(){
        tcp2=tcp1;
        zobraz_text_v_textviev.setText(tcp2);

    }
//--------------------------------------------------------------------------------------------------------------------------------------
    public void obrazok() {
        byte[] obraz=obrazok;

        int pom1 = pom;
        Bitmap bmp3 = BitmapFactory.decodeByteArray(obraz, 0, pom1);
        Log.d(LOG_TAG, "view");
        imgView.setImageBitmap(Bitmap.createScaledBitmap(bmp3, 640, 480, false));

    }

    //--------------------------------------------------------funkcia start prenosu port 1237---------------------------------------------------------------------------------------
    public void TcpClient(){
        try {

        }finally {

        }
        final Thread thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(LOG_TAG, "start recv thread, thread id: "
                        + Thread.currentThread().getId());
                String outMsg, inMsg;
                // tuna bude bezat hlavna komunikacia s raspberry, port 1235. v prvom rade bude poslany prikaz start. ten inicializuje raspberry. spusti sa komunikacia
                //a raspberry bude cakat co dalej. napriklad spustenie videa, zastavenie videa, zapnutie vysielacky, ovladanie z tabletu
                //spustenie videa bude znamenat spustenie dalsieho vlakna, ktore bude prijimat obrazky cez UDP
                try {
                    Socket s = new Socket("192.168.0.70", 1237);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    //send output msg

                    if (pom_connect){ //ak bolo stlacene tlacitko connect, vlakno je v nekonecnej smycke. ak sa da disconnect, vlakno sa ukonci.
                        out.write("connect");//odosle inicializacnu spravu do raspberry...
                        out.flush();
                        Log.d(LOG_TAG, "odoslana sprava connect");
                        synchronized (lockObject1) {
                            text_new="conn start";
                        }
                        //inMsg = in.readLine();
                        while (true) {
                            Thread.sleep(1000);
                            Log.d(LOG_TAG, "while");
                            out.write("stav");//odosle inicializacnu spravu do raspberry...
                            inMsg = in.readLine();
                            out.flush();
                            Thread.sleep(1000);
                            //out.write("arduino");
                            //out.flush();
                            Log.d(LOG_TAG, "sprava stav odoslana");


                            if(pom_video == true){
                                synchronized (lockObject1) {
                                    text_new="video start";
                                }

                                pom_video_start = true;
                                text = prijem();
                                Thread.sleep(500);
                                Log.d(LOG_TAG, "start videa");
                                out.write("video");
                                out.flush();
                                pom_video = false;
                            }

                            if (pom_connect == false){

                                Thread.sleep(10);
                                Log.d(LOG_TAG, "disconnect");
                                out.write("disconnect");
                                out.flush();
                                Thread.sleep(100);
                                pom_video_start = false;
                                break;
                            }
                        }
                        Log.d(LOG_TAG, "preskakujem while");
                    }
                    else {
                        out.write("disconnect");
                        Log.d(LOG_TAG, "posledna sprava");
                        inMsg = in.readLine();
                        out.flush();
                        s.close();
                        Log.d(LOG_TAG, "socket close");
                    }
                    s.close();
                    Log.d(LOG_TAG, "socket ukonceny");

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        Log.d(LOG_TAG, "thread tcp client close");

        thrd.start();

    }

    //------------------------------------------------------------------------koniec-------------------------------------------------------------------------------
}//--------------------------------------------------------------------class-------------------------------------------------


