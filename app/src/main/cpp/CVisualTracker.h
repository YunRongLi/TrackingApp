//
// Created by GP62-7RE on 2018/1/3.
//

#include <jni.h>
#include <opencv2/opencv.hpp>

enum TempMatchParam {
    TM_SQDIFF = CV_TM_SQDIFF, TM_SQDIFF_NORMED, TM_CCORR, TM_CCORR_NORMED, TM_CCOEFF, TM_CCOEFF_NORMED
};

enum VT_Method_Type {
    TemplateMatch = 1, MeanShift, CAMShift
};

typedef struct {
    int vmin;
    int vmax;
}MeanShiftParam;

typedef struct {
    int vmin;
    int vmax;
    int smin;
}CamShiftParam;

typedef struct {
    TempMatchParam TM_Param;
    MeanShiftParam MS_Param;
    CamShiftParam  CS_Param;
}VT_Params;

class CVisualTracker {
private:
    bool TrackingByTempMatching(cv::Mat &Frame, cv::Rect &TrackRect);
    bool TrackingByMeanShift(cv::Mat &Frame, cv::Rect &TrackRect);
    bool TrackingByCamShift(cv::Mat &Frame, cv::Rect &TrackRect);

    void ImgHueExtraction(cv::Mat &Frame);
    void PrepareForBackProject(cv::Rect &selection);

    VT_Method_Type m_CurType;
    VT_Params m_CurParams;

    cv::Mat* m_pROI;

    cv::Mat m_hsv;
    cv::Mat m_hue;
    cv::Mat m_mask;
    cv::MatND m_hist;
    cv::Mat m_backproj;
    cv::Mat m_res;

    int m_hmin;
    int m_hmax;
    int m_vmin;
    int m_vmax;
    int m_smin;
    int m_hsize;
    float m_hranges[2] = {0 , 180};

    bool m_bROIchanged;
public:
    CVisualTracker();
    ~CVisualTracker();

    void SetMethodType(VT_Method_Type);
    VT_Method_Type GetMethodType() const;

    void SetVT_Params(VT_Method_Type Type, VT_Params Param);
    VT_Params GetVT_Params() const;

    void SetROI(cv::Mat* pROI);
    cv::Mat* GetROI() const;

    bool Tracking(cv::Mat &Frame, cv::Rect &TrackRect);
    void ShowResult(cv::Mat &Frame, cv::Rect &TrackRect);
};