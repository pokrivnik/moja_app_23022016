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
    private boolean pom_dopredu, pom_dozadu, pom_dolava, pom_doprava, pom_connect, pom_prepinac, pom_video = false, pom_video_start = false;
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
        zobraz_text_v_textviev = (TextView)findViewById(R.id.textView);
        //zobraz_text_v_textviev.setText("0");
        Button btnSend = (Button)findViewById(R.id.angry_btn);
        final Button tlacitko_connect = (Button)findViewById(R.id.connect);
        imgView = (ImageView) findViewById(R.id.imageView);
        //---------------------------------------------------tlacitka-------------------------------------------------------------------------
        Button tlacitko_dopredu = (Button)findViewById(R.id.dopredu);
        Button tlacitko_dozadu = (Button)findViewById(R.id.dozadu);
        Button tlacitko_dolava = (Button)findViewById(R.id.dolava);
        Button tlacitko_doprava = (Button)findViewById(R.id.doprava);
        final Button spusti_video = (Button)findViewById(R.id.spusti_video);
        Switch prepinac = (Switch)findViewById(R.id.switch1);



        // funkcia timer--------------------------------------------------------------------------------------------------------

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            public void run() {
                TimerMethod();
            }
        }, 0, 1);

        // -----------------------funkcia pre kliknutie na tlacitko - prijate suboru-----------------------------
        btnSend.setOnClickListener(new View.OnClickListener() {
            // @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "btnRecv clicked");
                // funkcia pre prijatie
                //text = prijem();
            }
        });
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
                }
                else {//spusti hlavny komunikacny thread
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
                if (pom_video == false){
                    pom_video = true;
                    spusti_video.setText("Video ON");
                }
                /*else{
                    pom_video = true;
                    spusti_video.setText("Spusti video");
                }*/
            }


        });
//-------------------------------------------------------------------tlacitka ovladanie auta-------------------------------------------------------------
        tlacitko_dopredu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    pom_dopredu = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    pom_dopredu = false;
                }
                return false;
            }
        });
        tlacitko_dozadu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    pom_dozadu = true;
                }
                else if (event.getAction()==MotionEvent.ACTION_UP){
                    pom_dozadu = false;
                }
                return false;
            }
        });
        tlacitko_doprava.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    pom_doprava = true;
                }
                else if (event.getAction()==MotionEvent.ACTION_UP){
                    pom_doprava = false;
                }
                return false;
            }
        });
        tlacitko_dolava.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    pom_dolava = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    pom_dolava = false;
                }
                return false;
            }
        });

//---------------------------------------------------switch vysielacka/tablet---------------------------------------------------------------------------------
        prepinac.setChecked(false);
        prepinac.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //funkcia, prepinac je on
                    pom_prepinac = true;
                    Tcp_ovladanie();
                }
                else{
                    //prepinac je off
                    pom_prepinac = false;
                }

            }
        });
//-------------------------------------------------------------------------------------------------------------------------------------
        //tlacitko_connect.onHoverChanged();//zmena stavu???
        //tlacitko_connect.removeOnAttachStateChangeListener();
        //tlacitko_connect.hasOnClickListeners()//ak je stlacene, vrati 1
        //stlacime connect, a ak vyberieme ovladanie tabletu, vo while bude case ktore bude cekovat tlacitka ci su aktivne alebo neaktivne
        //a podla toho bude posielat ci ma ist auto vpred, vzad a pod...
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
  /*  @Override  //ak bude tlacitko stlacene, vykona sa funkcia, ak nebude, zavedie sa defaultna hodnota...
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.mybutton:
            {
                DoSomething();
                break;
            }

            case R.id.mybutton2:
            {
                DoSomething();
                break;
            }
        }
    }*/


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
    // ----------------------------------------funkcia na prevod string do image-------------------------------------------------------------
    public static byte[] dekodovanie(String imageDataString) {
        Log.d(LOG_TAG, "dekodovanie");
        return Base64.decode(imageDataString, 0);
    }

    //------------------------------------------------------prevod byte to jpg-------------------------------------
    public void text(){
        tcp2=tcp1;
        zobraz_text_v_textviev.setText(tcp2);

    }
//--------------------------------------------------------------------------------------------------------------------------------------
    public void obrazok() {
        //tcp2=tcp1;
        //zobraz_text_v_textviev.setText(tcp2);
        byte[] obraz=obrazok;
        //byte rgb[] = Base64.decode(obraz, 0);
        //Bitmap bmp3 = bmp2;
        int pom1 = pom;
        Bitmap bmp3 = BitmapFactory.decodeByteArray(obraz, 0, pom1);


        Log.d(LOG_TAG, "view");

        ///Mat mat = new Mat(200,200,CvType.CV_8UC4);
        ///mat.put(200, 200, obraz);
        //---------------------------------------------
        //Log.d(LOG_TAG, "vytvorenie bitmap");
        ///Bitmap bmp1 = Bitmap.createBitmap(200, 200,Config.RGB_565);
        //Log.d(LOG_TAG, "prevod do bitmap");
        //Imgproc.resize(obraz2, obraz2, );

        ///Utils.matToBitmap(mat, bmp1);
        //}
        //------------------------------------------------
        //Bitmap bm = BitmapFactory.decodeByteArray(imagePol, 0, imagePol.length);
        //Mat image = mat;
        //Highgui.imencode(ext, img, buf)
        //Bitmap bm = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        //bmp1.copyPixelsToBuffer(image.getByteBuffer());
        imgView.setImageBitmap(Bitmap.createScaledBitmap(bmp3, 640, 480, false));
        //imgView.setImageBitmap(bmp3);
        //a = 0;

    }
    //--------------------------------------------------------------------TCP ovladanie auta port 1236------------------------------------------------------------------------------------
    public void Tcp_ovladanie(){
        try {

        }finally {

        }
            Thread thrd = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.e(LOG_TAG, "start recv thread, thread id: "
                            + Thread.currentThread().getId());
                    // funkcia z ineho vlakna
                    try {
                        Socket s = new Socket("192.168.0.70", 1236);
                        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                        //send output msg
                        if (pom_prepinac){//ak bude zvolene ovladanie z tabletu
                        while (true) {
                            if (pom_dopredu){
                                out.write("dopredu");
                                out.flush();
                            }
                            else{
                                out.write("stop");
                                out.flush();
                            }
                            if (pom_dozadu){
                                out.write("dozadu");
                                out.flush();
                            }
                            else{
                                out.write("stop");
                                out.flush();
                            }
                            if (pom_doprava){
                                out.write("doprava");
                                out.flush();
                            }
                            else{
                                out.write("stred");
                                out.flush();
                            }
                            if (pom_dolava){
                                out.write("dolava");
                                out.flush();
                            }
                            else{
                                out.write("stred");
                                out.flush();
                            }
                            if (pom_prepinac == false){
                                break;
                            }

                        }}
                        else{
                            s.close();
                            out.write("stred");
                            out.flush();
                            out.write("stop");
                            out.flush();
                            out.write("disconnect");
                            out.flush();
                        }
                        s.close();
                        out.write("stred");
                        out.flush();
                        out.write("stop");
                        out.flush();
                                //close connection

                    } catch (UnknownHostException e) {
                     e.printStackTrace();
                    } catch (IOException e) {
                    e.printStackTrace();
                    }
                }

            });
            Log.d(LOG_TAG, "thread close");

            thrd.start();

    }


    //--------------------------------------------------------funkcia start prenosu port 1235---------------------------------------------------------------------------------------
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

                            //vsetky spravy by sa mohli zobrazovat pre istotu aj vo vedlajsom textview

                            /*if(pom_connect){//povolenie odosielania obrazu, prikaz na inicializaciu UDP prenosu. stlacenie tlacitka aktivuje UDP na strane talbetu
                                //a odoslanie prikazu aktivuje UDP prenos na strane raspberry
                            }*/
                            if(pom_video == true){//odoslanie informacie ci bude auto ovladane vysielackou alebo tabletom, defaultne vysielacka
                                synchronized (lockObject1) {
                                    text_new="video start";
                                }

                                pom_video_start = true;
                                text = prijem();
                                Thread.sleep(500);
                                Log.d(LOG_TAG, "start videa");
                                out.write("video");//povolenie ovladania tabletom, inicializacia tcp komunikacia pre prenos
                                out.flush();
                                pom_video = false;

                            }


                            if (pom_connect == false){
                                //Thread.sleep(100);
                                //out.write("nonvideo");
                                //out.flush();
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

    //------------------------------------------------------------------------funkcia komunikacia TCP-----------------------------------------------------------------------------
    //tuto funkciu by som rad inicializoval po stlaceni tlacitka. TCP komunikacia by nastala ako prva. spustala by video, obrazky, detekciu prekazky,
    //opticky, ultrazvukovy senzor, akoo aj povolenie ovladania z vysielacky alebo tabletu

    //------------------------------------------------------------------------koniec-------------------------------------------------------------------------------
}//--------------------------------------------------------------------class-------------------------------------------------


