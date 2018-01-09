package martianlee.trackingapplication;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;

import java.util.ArrayList;
import java.util.List;

enum VT_Method_Type {
    TemplateMatch, MeanShift, CamShift
}

enum TempMatchParam{
    TM_SQDIFF, TM_SQDIFF_NORMED, TM_CCORR, TM_CCORR_NORMED, TM_CCOEFF, TM_CCOEFF_NORMED
}

class MeanShiftParam{
    int hmin;
    int hmax;
    int vmin;
    int vmax;

    public MeanShiftParam(){
        this.hmin = 0;
        this.hmax = 180;
        this.vmin = 0;
        this.vmax = 255;
    }
}

class CamShiftParam{
    int hmin;
    int hmax;
    int vmin;
    int vmax;
    int smin;

    public CamShiftParam(){
        this.hmin = 0;
        this.hmax = 180;
        this.vmin = 0;
        this.vmax = 255;
        this.smin = 0;
    }
}

class VT_Params {
    TempMatchParam TM_Param;
    MeanShiftParam MS_Param;
    CamShiftParam  CS_Param;

    public VT_Params(){
        this.TM_Param = TempMatchParam.TM_SQDIFF;
        this.MS_Param = new MeanShiftParam();
        this.CS_Param = new CamShiftParam();
    }
}

public class CVisualTracker {
    static {
        System.loadLibrary("CVisualTracker");
    }

    private VT_Method_Type m_CurType;
    private VT_Params m_CurParams;

    private Mat m_pROI;
    private Mat m_hsv;
    private Mat m_hue;
    private Mat m_mask;
    private Mat m_hist;
    private Mat m_backproj;
    private Mat m_TMresult;

    private int m_hmin;
    private int m_hmax;
    private int m_vmin;
    private int m_vmax;
    private int m_smin;

    private MatOfInt m_hsize;
    private MatOfInt m_Channel;
    private MatOfInt m_FromTo;
    private MatOfFloat m_range;

    private List<Mat> m_hueList;
    private List<Mat> m_hsvList;
    private List<Mat> m_hueROIList;

    private Point m_Origin;
    private Point m_End;

    private RotatedRect trackRect;

    private boolean m_bROIchanged;

    private boolean m_ImageSwitch;

    private Scalar m_ScalarR;
    private Scalar m_ScalarB;

    public CVisualTracker(){
        m_CurParams = new VT_Params();
        m_CurType = VT_Method_Type.TemplateMatch;

        m_pROI = new Mat();
        m_hsv = new Mat();
        m_hue = new Mat();
        m_mask = new Mat();
        m_hist = new Mat();
        m_backproj = new Mat();
        m_TMresult = new Mat();

        m_Origin = new Point();
        m_End = new Point();

        trackRect = new RotatedRect();

        m_hueList = new ArrayList<>();
        m_hsvList = new ArrayList<>();
        m_hueROIList = new ArrayList<>();

        m_hmin = 0;
        m_hmax = 180;
        m_vmin = 0;
        m_vmax = 255;
        m_smin = 0;
        m_hsize = new MatOfInt(256);
        m_Channel = new MatOfInt(0);
        m_FromTo = new MatOfInt(0,0);
        m_range = new MatOfFloat();

        m_bROIchanged = false;

        m_ImageSwitch = false;

        m_ScalarR = new Scalar(255, 0 ,0);
        m_ScalarB = new Scalar(0, 0, 255);
    }

    private boolean TrackingByTempMatching(Mat Frame, Rect TrackRect){
        if(GetROI() == 0){
            return false;
        }

        int result_cols = Frame.cols() - TrackRect.width + 1;
        int result_rows = Frame.rows() - TrackRect.height + 1;

        m_TMresult.create(result_rows, result_cols, CvType.CV_32FC1);

        int minmaxresult[] = doTemplateMatch(Frame.getNativeObjAddr(), m_pROI.getNativeObjAddr(), m_TMresult.getNativeObjAddr(), GetVT_Params().TM_Param.ordinal());

        if(m_CurParams.TM_Param.ordinal() == 0 || m_CurParams.TM_Param.ordinal() == 1){
            TrackRect.x = minmaxresult[0];
            TrackRect.y = minmaxresult[1];
        }
        else{
            TrackRect.x = minmaxresult[2];
            TrackRect.y = minmaxresult[3];
        }

        return true;
    }

    private boolean TrackingByMeanShift(Mat Frame, Rect  TrackRect){
        if(GetROI() == 0){
            return false;
        }

        m_hmin = m_CurParams.MS_Param.hmin;
        m_hmax = m_CurParams.MS_Param.hmax;
        m_vmin = m_CurParams.MS_Param.vmin;
        m_vmax = m_CurParams.MS_Param.vmax;
        m_smin = 30;

        ImgHueExtraction(Frame);

        if(m_bROIchanged){
            PrepareForBackProject(TrackRect);
            m_bROIchanged = false;
        }

        m_hueList.add(m_hue);

        m_range.fromArray(m_hmin, m_hmax);

        Imgproc.calcBackProject(m_hueList, m_Channel, m_hist, m_backproj, m_range, 1.0);

        Core.bitwise_and(m_backproj, m_mask, m_backproj);

        int result[] = doMeanShift(m_backproj.getNativeObjAddr(), TrackRect.x, TrackRect.y, TrackRect.width, TrackRect.height);

        TrackRect.x = result[0];
        TrackRect.y = result[1];

        return true;
    }

    boolean TrackingByCamShift(Mat Frame, Rect  TrackRect){
        if(GetROI() == 0){
            return false;
        }

        m_hmin = m_CurParams.CS_Param.hmin;
        m_hmax = m_CurParams.CS_Param.hmax;
        m_vmin = m_CurParams.CS_Param.vmin;
        m_vmax = m_CurParams.CS_Param.vmax;
        m_smin = m_CurParams.CS_Param.smin;

        ImgHueExtraction(Frame);

        if(m_bROIchanged){
            PrepareForBackProject(TrackRect);
            m_bROIchanged = false;
        }

        m_hueList.add(m_hue);

        m_range.fromArray(m_hmin, m_hmax);

        Imgproc.calcBackProject(m_hueList, m_Channel, m_hist, m_backproj, m_range, 1.0);

        Core.bitwise_and(m_backproj, m_mask, m_backproj);

        double result[] = doCamShift(m_backproj.getNativeObjAddr(), TrackRect.x , TrackRect.y, TrackRect.width, TrackRect.height);

        trackRect.angle = result[0];
        trackRect.center.x = result[1];
        trackRect.center.y = result[2];
        trackRect.size.width = result[3];
        trackRect.size.height = result[4];

        Imgproc.ellipse(Frame, trackRect, m_ScalarB, 2);
        return true;
    }

    void ImgHueExtraction(Mat Frame){
        Imgproc.cvtColor(Frame, this.m_hsv, Imgproc.COLOR_BGR2HSV);

        Core.inRange(this.m_hsv, new Scalar(m_hmin, m_smin, this.m_vmin), new Scalar(this.m_hmax, 255, this.m_vmax), this.m_mask);

        m_hue.create(m_hsv.size(), m_hsv.depth());

        m_hueList.add(m_hue);
        m_hsvList.add(m_hsv);

        Core.mixChannels(m_hsvList, m_hueList, m_FromTo);

        m_hue = m_hueList.get(0);
    }

    void PrepareForBackProject(Rect selection){
        Mat m_hueROI = m_hue.submat(selection);
        Mat m_maskROI = m_mask.submat(selection);

        m_hueROIList.add(m_hueROI);

        m_range.fromArray(m_hmin, m_hmax);

        Imgproc.calcHist(m_hueROIList, m_Channel, m_maskROI, this.m_hist, this.m_hsize, m_range);

    }

    public void SetImageBackProj(){
        m_ImageSwitch = true;
    }

    public void SetImageRGB(){
        m_ImageSwitch = false;
    }

    public boolean GetImageSwitch(){
        return m_ImageSwitch;
    }

    public void SetMethodType(VT_Method_Type Type){
        this.m_CurType = Type;
    }

    public VT_Method_Type GetMethodType(){
        return this.m_CurType;
    }

    public void SetVT_Params(VT_Method_Type Type,VT_Params Param){
        switch (Type){
            case TemplateMatch:
                this.m_CurParams.TM_Param = Param.TM_Param;
                break;
            case MeanShift:
                this.m_CurParams.MS_Param = Param.MS_Param;
                break;
            case CamShift:
                this.m_CurParams.CS_Param = Param.CS_Param;
                break;
            default:
                break;
        }
    }

    public VT_Params GetVT_Params(){
        return this.m_CurParams;
    }

    public void SetROI(Mat Frame, Rect TrackRect){
        m_pROI = Frame.submat(TrackRect);
        m_bROIchanged = true;
    }

    public long GetROI() {
          return m_pROI.getNativeObjAddr();
      }

    public boolean Tracking(Mat Frame, Rect TrackRect){
        boolean state = false;

        switch(GetMethodType().ordinal()){
            case 0:
                state = TrackingByTempMatching(Frame, TrackRect);
                break;
            case 1:
                state = TrackingByMeanShift(Frame, TrackRect);
                break;
            case 2:
                state = TrackingByCamShift(Frame, TrackRect);
                break;
            default:
                break;
        }
        return state;
    }

    public Mat ShowResult(Mat Frame, Rect TrackRect){
        m_Origin.x = TrackRect.x;
        m_Origin.y = TrackRect.y;
        m_End.x = TrackRect.x + TrackRect.width;
        m_End.y = TrackRect.y + TrackRect.height;

        if(this.GetMethodType().ordinal() != 2){
            Imgproc.rectangle(Frame, m_Origin, m_End, m_ScalarR, 2);
        }

        return Frame;
    }

    public Mat ShowBackproj(){
        return m_backproj;
    }

    private native int[] doTemplateMatch(long FrameAddr, long m_pROIAddr, long resultAddr, int Method);
    private native int[] doMeanShift(long m_backprojAddr, int x, int y, int width, int height);
    private native double[] doCamShift(long m_backprojAddr, int x, int y, int width, int height);
}
