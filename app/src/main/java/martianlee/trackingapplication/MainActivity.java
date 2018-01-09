package martianlee.trackingapplication;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

enum TemplateMatchParamNum{
    TM
}

enum MeanShiftParamNum{
    hmin, hmax, vmin, vmax
}

enum CamShiftParamNum{
    hmin, hmax, vmin, vmax, smin
}

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    ImageButton m_ImageButton_SetTrackingMethod;
    ImageButton m_ImageButton_PauseCapture;
    ImageButton m_ImageButton_SelectMethodUp;
    ImageButton m_ImageButton_SelectMethodDown;
    Button m_Button_ImageSwitch;

    TextView m_TextView_Param;
    TextView m_TextView_Value;
    SeekBar m_SeekBar_Param;
    Spinner m_Spinner_SelectMethod;
    RelativeLayout m_Relative2;

    TemplateMatchParamNum TMParamNum = TemplateMatchParamNum.TM;
    MeanShiftParamNum MsParamNum = MeanShiftParamNum.hmin;
    CamShiftParamNum CsParamNum = CamShiftParamNum.hmin;

    Core.MinMaxLocResult minmaxlocresult;

    private VT_Params m_Param = new VT_Params();
    private CVisualTracker cvisualtracker = new CVisualTracker();

    boolean TrackingSetState;
    boolean m_SeekbarTounching = false;
    Mat mRgba;
    Mat mGray;

    private Rect TrackRect = new Rect();
    private Point m_Origin = new Point();
    private Point m_Current = new Point();

    int m_MotionEvent;

    private JavaCameraView javaCameraView;
    private static String TAG = "MainActivity";
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS:
                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        m_ImageButton_SetTrackingMethod = (ImageButton) findViewById(R.id.SetTrackingMethod);
        m_Button_ImageSwitch = (Button) findViewById(R.id.ImageSwitch);
        m_ImageButton_PauseCapture = (ImageButton) findViewById(R.id.PauseCapture);

        m_ImageButton_SelectMethodUp = (ImageButton) findViewById(R.id.ImageButtonSelectUp);
        m_ImageButton_SelectMethodDown = (ImageButton) findViewById(R.id.ImageButtonSelectDown);

        m_TextView_Param = (TextView) findViewById(R.id.TextviewParam);
        m_TextView_Value = (TextView) findViewById(R.id.TextviewValue);

        m_SeekBar_Param = (SeekBar) findViewById(R.id.SeekBarParam);

        m_Spinner_SelectMethod = (Spinner) findViewById(R.id.SpinnerSelectMethod);
        final String[] Method = {"TemplateMatch", "MeanShift", "CamShift"};
        ArrayAdapter<String> MethodList = new ArrayAdapter<String>(MainActivity.this, R.layout.method_textview, Method);
        m_Spinner_SelectMethod.setAdapter(MethodList);

        m_Relative2 = (RelativeLayout) findViewById(R.id.Relative2);

        javaCameraView = (JavaCameraView) findViewById(R.id.javaCameraView);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.setMaxFrameSize(640, 480);
    }

    @Override
    protected void onStart() {
        super.onStart();

        m_Relative2.setEnabled(false);
        m_ImageButton_SelectMethodUp.setEnabled(false);
        m_ImageButton_SelectMethodDown.setEnabled(false);
        m_TextView_Value.setEnabled(false);
        m_TextView_Param.setEnabled(false);
        m_SeekBar_Param.setEnabled(false);
        m_Spinner_SelectMethod.setEnabled(false);

        m_Spinner_SelectMethod.setAlpha(0f);
        m_ImageButton_SelectMethodUp.setAlpha(0f);
        m_ImageButton_SelectMethodDown.setAlpha(0f);
        m_TextView_Param.setAlpha(0f);
        m_TextView_Value.setAlpha(0f);
        m_SeekBar_Param.setAlpha(0f);
        m_Relative2.setAlpha(0f);

        TrackingSetState = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                m_MotionEvent = 0;
                m_Origin.x = event.getX() * 640 / 1920;
                m_Origin.y = event.getY() * 480 / 1080;
                break;
            case MotionEvent.ACTION_MOVE:
                m_MotionEvent = 1;
                m_Current.x = event.getX() * 640 / 1920;
                m_Current.y = event.getY() * 480 / 1080;
                break;
            case MotionEvent.ACTION_UP:
                m_MotionEvent = 2;

                TrackRect.x = (int) m_Origin.x;
                TrackRect.y = (int) m_Origin.y;
                TrackRect.width  = Math.abs((int) event.getX() * 640 / 1920 - (int) m_Origin.x);
                TrackRect.height = Math.abs((int) event.getY() * 480 / 1080 - (int) m_Origin.y);
                cvisualtracker.SetROI(mRgba, TrackRect);
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV load Success.");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.i(TAG, "OpenCV not load.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, mLoaderCallback);
        }

        m_ImageButton_SetTrackingMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!TrackingSetState){
                    m_ImageButton_SelectMethodUp.setEnabled(true);
                    m_ImageButton_SelectMethodDown.setEnabled(true);
                    m_TextView_Value.setEnabled(true);
                    m_TextView_Param.setEnabled(true);
                    m_SeekBar_Param.setEnabled(true);
                    m_Spinner_SelectMethod.setEnabled(true);
                    m_Relative2.setEnabled(true);

                    m_Spinner_SelectMethod.setAlpha(1f);
                    m_ImageButton_SelectMethodUp.setAlpha(1f);
                    m_ImageButton_SelectMethodDown.setAlpha(1f);
                    m_TextView_Param.setAlpha(1f);
                    m_TextView_Value.setAlpha(1f);
                    m_SeekBar_Param.setAlpha(1f);
                    m_Relative2.setAlpha(0.5f);

                    TrackingSetState = true;
                }
                else{
                    m_ImageButton_SelectMethodUp.setEnabled(false);
                    m_ImageButton_SelectMethodDown.setEnabled(false);
                    m_TextView_Value.setEnabled(false);
                    m_TextView_Param.setEnabled(false);
                    m_SeekBar_Param.setEnabled(false);
                    m_Spinner_SelectMethod.setEnabled(false);
                    m_Relative2.setEnabled(false);

                    m_ImageButton_SelectMethodUp.setAlpha(0f);
                    m_ImageButton_SelectMethodDown.setAlpha(0f);
                    m_TextView_Param.setAlpha(0f);
                    m_TextView_Value.setAlpha(0f);
                    m_SeekBar_Param.setAlpha(0f);
                    m_Relative2.setAlpha(0f);

                    TrackingSetState = false;
                }
            }
        });

        m_Button_ImageSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text;
                if(!cvisualtracker.GetImageSwitch()){
                    text = "RGB";
                    m_Button_ImageSwitch.setText(text);
                    cvisualtracker.SetImageBackProj();
                }
                else {
                    text = "反投影";
                    m_Button_ImageSwitch.setText(text);
                    cvisualtracker.SetImageRGB();
                }
            }
        });


        m_SeekBar_Param.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(m_SeekbarTounching){
                    switch (cvisualtracker.GetMethodType().ordinal()){
                        case 0:
                            m_TextView_Value.setText(String.valueOf(progress + 1));
                            switch (progress){
                                case 0:
                                    m_Param.TM_Param = TempMatchParam.TM_SQDIFF;
                                    break;
                                case 1:
                                    m_Param.TM_Param = TempMatchParam.TM_SQDIFF_NORMED;
                                    break;
                                case 2:
                                    m_Param.TM_Param = TempMatchParam.TM_CCORR;
                                    break;
                                case 3:
                                    m_Param.TM_Param = TempMatchParam.TM_CCORR_NORMED;
                                    break;
                                case 4:
                                    m_Param.TM_Param = TempMatchParam.TM_CCOEFF;
                                    break;
                                case 5:
                                    m_Param.TM_Param = TempMatchParam.TM_CCOEFF_NORMED;
                                    break;
                            }
                            cvisualtracker.SetVT_Params(VT_Method_Type.TemplateMatch, m_Param);
                            break;
                        case 1:
                            m_TextView_Value.setText(String.valueOf(progress));
                            switch (MsParamNum.ordinal()){
                                case 0:
                                    m_Param.MS_Param.hmin = progress;
                                    break;
                                case 1:
                                    m_Param.MS_Param.hmax = progress;
                                    break;
                                case 2:
                                    m_Param.MS_Param.vmin = progress;
                                    break;
                                case 3:
                                    m_Param.MS_Param.vmax = progress;
                                    break;
                                default:
                                    break;
                            }
                            cvisualtracker.SetVT_Params(VT_Method_Type.MeanShift, m_Param);
                            break;
                        case 2:
                            m_TextView_Value.setText(String.valueOf(progress));
                            switch (CsParamNum.ordinal()){
                                case 0:
                                    m_Param.CS_Param.hmin = progress;
                                    break;
                                case 1:
                                    m_Param.CS_Param.hmax = progress;
                                    break;
                                case 2:
                                    m_Param.CS_Param.vmin = progress;
                                    break;
                                case 3:
                                    m_Param.CS_Param.vmax = progress;
                                    break;
                                case 4:
                                    m_Param.CS_Param.smin = progress;
                                    break;
                                default:
                                    break;
                            }
                            cvisualtracker.SetVT_Params(VT_Method_Type.CamShift, m_Param);
                            break;
                        default:
                            break;
                    }
                }
                else {
                    if(cvisualtracker.GetMethodType().ordinal() == 0){
                        m_TextView_Value.setText(String.valueOf(progress + 1));
                    }
                    else{
                        m_TextView_Value.setText(String.valueOf(progress));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                m_SeekbarTounching = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                m_SeekbarTounching = false;
            }
        });

        m_Spinner_SelectMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String ParamText;
                switch(position){
                    case 0:
                        cvisualtracker.SetMethodType(VT_Method_Type.TemplateMatch);
                        ParamText = "TM";
                        m_TextView_Param.setText(ParamText);
                        m_SeekBar_Param.setMax(5);
                        int TMParam = cvisualtracker.GetVT_Params().TM_Param.ordinal();
                        m_SeekBar_Param.setProgress(TMParam);

                        break;
                    case 1:
                        cvisualtracker.SetMethodType(VT_Method_Type.MeanShift);
                        ParamText = "hmin";
                        m_TextView_Param.setText(ParamText);
                        m_SeekBar_Param.setMax(180);
                        int MsHminParam = cvisualtracker.GetVT_Params().MS_Param.hmin;
                        m_SeekBar_Param.setProgress(MsHminParam);

                        MsParamNum = MsParamNum.hmin;
                        break;
                    case 2:
                        cvisualtracker.SetMethodType(VT_Method_Type.CamShift);
                        ParamText = "hmin";
                        m_TextView_Param.setText(ParamText);
                        m_SeekBar_Param.setMax(180);
                        int CsHminParam = cvisualtracker.GetVT_Params().CS_Param.hmin;
                        m_SeekBar_Param.setProgress(CsHminParam);

                        CsParamNum = CsParamNum.hmin;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        m_ImageButton_SelectMethodDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ParamText;

                switch (cvisualtracker.GetMethodType().ordinal()){
                    case 0:
                        break;
                    case 1:
                        switch (MsParamNum.ordinal()){
                            case 0:
                                MsParamNum = MsParamNum.hmax;
                                ParamText = "hmax";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(180);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().MS_Param.hmax);

                                break;
                            case 1:
                                MsParamNum = MsParamNum.vmin;
                                ParamText = "vmin";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().MS_Param.vmin);

                                break;
                            case 2:
                                MsParamNum = MsParamNum.vmax;
                                ParamText = "vmax";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().MS_Param.vmax);
                                break;
                            default:
                                break;
                        }
                        break;
                    case 2:
                        switch (CsParamNum.ordinal()){
                            case 0:
                                CsParamNum = CsParamNum.hmax;
                                ParamText = "hmax";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(180);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.hmax);
                                break;
                            case 1:
                                CsParamNum = CsParamNum.vmin;
                                ParamText = "vmin";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.vmin);
                                break;
                            case 2:
                                CsParamNum = CsParamNum.vmax;
                                ParamText = "vmax";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.vmax);
                                break;
                            case 3:
                                CsParamNum = CsParamNum.smin;
                                ParamText = "smin";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.smin);
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        m_ImageButton_SelectMethodUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String ParamText;
                switch (cvisualtracker.GetMethodType().ordinal()){
                    case 0:
                        break;
                    case 1:
                        switch (MsParamNum.ordinal()){
                            case 1:
                                MsParamNum = MsParamNum.hmin;
                                ParamText = "hmin";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(180);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().MS_Param.hmin);
                                m_SeekBar_Param.refreshDrawableState();
                                break;
                            case 2:
                                MsParamNum = MsParamNum.hmax;
                                ParamText = "hmax";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(180);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().MS_Param.hmax);
                                m_SeekBar_Param.refreshDrawableState();
                                break;
                            case 3:
                                MsParamNum = MsParamNum.vmin;
                                ParamText = "vmin";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().MS_Param.vmin);
                                m_SeekBar_Param.refreshDrawableState();
                                break;
                            default:
                                break;
                        }
                        break;
                    case 2:
                        switch (CsParamNum.ordinal()){
                            case 1:
                                CsParamNum = CsParamNum.hmin;
                                ParamText = "hmin";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(180);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.hmin);
                                break;
                            case 2:
                                CsParamNum = CsParamNum.hmax;
                                ParamText = "hmax";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(180);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.hmax);
                                break;
                            case 3:
                                CsParamNum = CsParamNum.vmin;
                                ParamText = "vmin";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.vmin);
                                break;
                            case 4:
                                CsParamNum = CsParamNum.vmax;
                                ParamText = "vmax";
                                m_TextView_Param.setText(ParamText);
                                m_SeekBar_Param.setMax(255);
                                m_SeekBar_Param.setProgress(cvisualtracker.GetVT_Params().CS_Param.vmax);
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(javaCameraView != null){
            javaCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(width, height, CvType.CV_8UC4);
        mGray = new Mat(width, height, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba  = inputFrame.rgba();
        mGray  = inputFrame.gray();

        if(m_MotionEvent == 1){
            Imgproc.rectangle(mRgba, m_Origin, m_Current, new Scalar(255, 255, 255), 2);
        }
        if(m_MotionEvent == 2){
            if(cvisualtracker.Tracking(mRgba, TrackRect)){
                if(cvisualtracker.GetImageSwitch()){
                    return cvisualtracker.ShowBackproj();
                }
                else{
                    return cvisualtracker.ShowResult(mRgba, TrackRect);
                }
            }
        }

        return mRgba;
    }

    /*
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native int[] getCannyImg(int[] a, int b, int c);
    //public native Mat imgRotate(Mat img, int rotateFlags);
}
