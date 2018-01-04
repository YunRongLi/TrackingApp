package martianlee.trackingapplication;

import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.utils.Converters;

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
        this.hmax = 0;
        this.vmin = 0;
        this.vmax = 0;
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
        this.hmax = 0;
        this.vmin = 0;
        this.vmax = 0;
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
    private VT_Method_Type m_CurType;
    private VT_Params m_CurParams;

    private Mat m_pROI;
    private Mat m_hsv;
    private Mat m_hue;
    private Mat m_mask;
    private Mat m_hist;
    private Mat m_backproj;
    private Mat m_res;

    private int m_hmin;
    private int m_hmax;
    private int m_vmin;
    private int m_vmax;
    private int m_smin;
    private MatOfInt m_hsize;

    private boolean m_bROIchanged;

    public CVisualTracker(){
        this.m_CurParams = new VT_Params();
        this.m_CurType = VT_Method_Type.TemplateMatch;

        this.m_pROI = null;
        this.m_hsv = null;
        this.m_hue = null;
        this.m_mask = null;
        this.m_hist = null;
        this.m_backproj = null;
        this.m_res = null;

        this.m_hmin = 0;
        this.m_hmax = 0;
        this.m_vmin = 0;
        this.m_vmax = 0;
        this.m_smin = 0;
        this.m_hsize = new MatOfInt(256);

        this.m_bROIchanged = false;
    }

    private boolean TrackingByTempMatching(Mat Frame, Rect TrackRect){
        MinMaxLocResult minmaxlocresult;

//        if(GetROI() == 0){
//            return false;
//        }

        int result_cols = Frame.cols() - TrackRect.width + 1;
        int result_rows = Frame.rows() - TrackRect.height + 1;

        Mat result = null;
        result.create(result_rows, result_cols, Frame.type());

        Imgproc.matchTemplate(Frame, m_pROI, result, GetVT_Params().TM_Param.ordinal());

        minmaxlocresult = Core.minMaxLoc(result);

        if(m_CurParams.TM_Param.ordinal() == 0 || m_CurParams.TM_Param.ordinal() == 1){
            TrackRect.x = (int) minmaxlocresult.minLoc.x;
        }

        return true;
    }

////    boolean TrackingByMeanShift(Mat Frame, Rect  TrackRect);
////    boolean TrackingByCamShift(Mat Frame, Rect  TrackRect);
////
    void ImgHueExtraction(Mat Frame){
        Imgproc.cvtColor(Frame, this.m_hsv, Imgproc.COLOR_BGR2HSV);

        Core.inRange(this.m_hsv, new Scalar(this.m_hmin, this.m_smin, this.m_vmin), new Scalar(this.m_hmax, 255, this.m_vmax), this.m_mask);

        this.m_hue.create(this.m_hsv.size(), this.m_hsv.depth());

        MatOfInt from_to = new MatOfInt(0,0);

        List<Mat> m_hsv_list = new ArrayList<>();
        List<Mat> m_hue_list = new ArrayList<>();
        m_hsv_list.add(this.m_hsv);
        m_hue_list.add(this.m_hue);

        Core.mixChannels(m_hsv_list, m_hue_list, from_to);

        this.m_hue = Converters.vector_Mat_to_Mat(m_hue_list);
    }

    void PrepareForBackProject(Rect selection){
        Mat m_hueROI = this.m_hue.submat(selection);
        Mat m_maskROI = this.m_mask.submat(selection);

        MatOfInt channel = new MatOfInt(0);
        List<Mat> m_hueROIList = new ArrayList<>();
        m_hueROIList.add(m_hueROI);

        MatOfFloat range = new MatOfFloat(this.m_hmin, this.m_hmax);

        Imgproc.calcHist(m_hueROIList, channel, m_maskROI, this.m_hist, this.m_hsize, range);
    }

//    /////////////////////////////////
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
//
////    public void SetROI(Mat pROI);
//      public long GetROI() {
//          return m_pROI.getNativeObjAddr();
//      }

//    public boolean Tracking(Mat Frame, Rect TrackRect);
//    public void ShowResult(Mat Frame, Rect TrackRect)
}
