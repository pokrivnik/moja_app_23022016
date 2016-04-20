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
import android.widget.ImageView;
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
    private boolean pom_dopredu, pom_dozadu, pom_dolava, pom_doprava, pom_connect;
    private final Object lockObject = new Object();


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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); //zabrani otoceniu obrazovky
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
        Button spusti_video = (Button)findViewById(R.id.spusti_video);



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
                text = prijem();
            }
        });
        tlacitko_connect.setOnClickListener(new View.OnClickListener() {
            //stlacenim tlacitka sa spusti pociatocna inicializacia. spusti sa vlakno s tcp komunikaciou na porte 1235, kde sa odosle
            //prikaz start, ktory bude cakat na odpoved, mozeme zacat...po stlaceni tlacitka sa zmeni nazov tlacitka na disconnect,
            //pri stlaceni tlacitka disconnect sa do raspberry odosle prikaz stop, ktory zastavy vsetku komunikaciu aj veskeru cinnost
            @Override
            public void onClick(View v) {
                if (pom_connect){
                    pom_connect = false;
                    tlacitko_connect.setText("connect");
                }
                else{
                    pom_connect = true;
                    tlacitko_connect.setText("disconnect");
                    TcpClient();
                }


            }
        });
        /*tlacitko_dopredu.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                pom_dopredu = true;
            }
        });*/
        tlacitko_dozadu.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                pom_dozadu = true;
            }

        });
        tlacitko_doprava.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v) {
                    pom_doprava = true;
            }


        });
        tlacitko_dolava.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    pom_dolava = true;
                }
        });

        tlacitko_dopredu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    pom_dopredu = true;
                }
                else if (event.getAction()==MotionEvent.ACTION_UP){
                    pom_dopredu = false;
                }
                return false;
            }
        });
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
            text();
            if (a == 1) {
                a=0;
                obrazok();


            }
        }
    };
    private ImageView imgView;


    //------------------------------------------------------------------------------------------------------------------------------------------




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

                        // --------------------------------------------------------------------------------------------------------
                    }
                } catch (SocketException se) {
                    Log.e(LOG_TAG, "SocketException: " + se.toString());
                } catch (IOException ie) {
                    Log.e(LOG_TAG, "IOException" + ie.toString());
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
        tcp2=tcp1;
        zobraz_text_v_textviev.setText(tcp2);
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
        imgView.setImageBitmap(bmp3);
        //a = 0;

    }
    //--------------------------------------------------------------------------------------------------------------------------------------------------------
    public void TcpClient(){
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
                        Socket s = new Socket("192.168.0.70", 1235);
                        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                        //send output msg
                        while (true) {


                           if (pom_connect){

                           }
                            else if (pom_dopredu){

                           }
                            String outMsg = "TCP connecting to ";// + System.getProperty("line.separator");

                            out.write(outMsg);
                            out.flush();
                            Log.i("TcpClient", "sent: " + outMsg);
                            //accept server response
                            String inMsg = in.readLine();// + System.getProperty("line.separator");
                            //String inMsg = in.
                            Log.i("TcpClient", "received: " + inMsg);
                            //zobraz_text_v_textviev.setText(inMsg);
                            tcp1 = inMsg;
                        }
                                //close connection
                       // s.close();
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
    //--------------------------------------------------------funkcia start prenosu---------------------------------------------------------------------------------------
    public void TcpClient1(){
        try {

        }finally {

        }
        Thread thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(LOG_TAG, "start recv thread, thread id: "
                        + Thread.currentThread().getId());
                // tuna bude bezat hlavna komunikacia s raspberry, port 1235. v prvom rade bude poslany prikaz start. ten inicializuje raspberry. spusti sa komunikacia
                //a raspberry bude cakat co dalej. napriklad spustenie videa, zastavenie videa, zapnutie vysielacky, ovladanie z tabletu
                //spustenie videa bude znamenat spustenie dalsieho vlakna, ktore bude prijimat obrazky cez UDP
                try {
                    Socket s = new Socket("192.168.0.70", 1235);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    //send output msg
                    if (pom_connect){ //ak bolo stlacene tlacitko connect, vlakno je v nekonecnej smycke. ak sa da disconnect, vlakno sa ukonci.
                        out.write("connect");//odosle inicializacnu spravu do raspberry...
                        String outMsg, inMsg;
                        while (true) {
                            inMsg = in.readLine();
                            Log.i("prichodzia sprava", inMsg);
                            if (inMsg=="ok"){
                                synchronized (lockObject){
                                    zobraz_text_v_textviev.setText(inMsg);
                                }

                            }


                            out.write(outMsg);
                            out.flush();
                            Log.i("TcpClient", "sent: " + outMsg);
                            //accept server response
                            String inMsg = in.readLine();// + System.getProperty("line.separator");
                            //String inMsg = in.
                            Log.i("TcpClient", "received: " + inMsg);
                            //zobraz_text_v_textviev.setText(inMsg);
                            tcp1 = inMsg;
                        }
                    }
                    //close connection
                    // s.close();
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

    //------------------------------------------------------------------------funkcia komunikacia TCP-----------------------------------------------------------------------------
    //tuto funkciu by som rad inicializoval po stlaceni tlacitka. TCP komunikacia by nastala ako prva. spustala by video, obrazky, detekciu prekazky,
    //opticky, ultrazvukovy senzor, akoo aj povolenie ovladania z vysielacky alebo tabletu
    /*class ClientThread implements Runnable {

        public static final String SERVER_IP = "192.168.0.11";
        public static final int SERVER_PORT = 1235;
        private String mServerMessage;
        private OnMessageReceived mMessageListener = null;
        private boolean mRun = false;
        private PrintWriter mBufferOut;
        private BufferedReader mBufferIn;

        @Override
        public TcpClient (OnMessageReceived listener){
            mMessageListener = listener;
        }
        @Override
        public void stopClient() {
            Log.i("Debug", "stopClient");

            // send mesage that we are closing the connection
            //sendMessage(Constants.CLOSED_CONNECTION + "Kazy");

            mRun = false;

            if (mBufferOut != null) {
                mBufferOut.flush();
                mBufferOut.close();
            }

            mMessageListener = null;
            mBufferIn = null;
            mBufferOut = null;
            mServerMessage = null;
        }

        @Override
        public void sendMessage(String message) {
            if (mBufferOut != null && !mBufferOut.checkError()) {
                mBufferOut.println(message);
                mBufferOut.flush();
            }
        }

        public void run(){
            mRun = true;
            try {

                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                Log.e("TCP Client", "C: Connecting...");
                Socket socket = new Socket(serverAddr, SERVER_PORT);
                try {
                    //here you must put your computer's IP address.
                    InetAddress serverAddr = InetAddress.getByName(SERVER_IP);

                    Log.e("TCP Client", "C: Connecting...");

                    //create a socket to make the connection with the server
                    Socket socket = new Socket(serverAddr, SERVER_PORT);

                    try {
                        Log.i("Debug", "inside try catch");
                        mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                        mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        while (mRun) {
                            mServerMessage = mBufferIn.readLine();
                            if (mServerMessage != null && mMessageListener != null) {
                                //call the method messageReceived from MyActivity class
                                mMessageListener.messageReceived(mServerMessage);
                            }

                        }
                        Log.e("RESPONSE FROM SERVER", "S: Received Message: '" + mServerMessage + "'");

                    } catch (Exception e) {

                        Log.e("TCP", "S: Error", e);

                    } finally {
                        socket.close();
                    }

                } catch (Exception e) {

                    Log.e("TCP", "C: Error", e);

                }


            }

    }}*/
    //------------------------------------------------------------------------koniec-------------------------------------------------------------------------------
}//--------------------------------------------------------------------class-------------------------------------------------


