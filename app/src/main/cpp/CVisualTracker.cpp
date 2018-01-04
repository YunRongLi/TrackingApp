//
// Created by GP62-7RE on 2018/1/3.
//

#include <jni.h>
#include <opencv2/opencv.hpp>
#include "CVisualTracker.h"

CVisualTracker::CVisualTracker() {
    m_hsize = 256;
}

CVisualTracker::~CVisualTracker() {

}

bool CVisualTracker::TrackingByTempMatching(cv::Mat& frame, cv::Rect &TrackRect) {
    cv::Mat templ;
    cv::Point MaxPoint;
    cv::Point MinPoint;

    double MaxVal;
    double MinVal;

    if (GetROI() == NULL) {
        return false;
    }

    int result_cols = frame.cols - TrackRect.width + 1;
    int result_rows = frame.rows - TrackRect.height + 1;

    cv::Mat result;
    cv::Mat result_normalize;

    result.create(result_rows, result_cols,  frame.type());

    templ = *m_pROI;

    cv::matchTemplate(frame, templ, result, GetVT_Params().TM_Param);

    cv::minMaxLoc(result, &MinVal, &MaxVal, &MinPoint, &MaxPoint, cv::noArray());

    if (m_CurParams.TM_Param == cv::TM_SQDIFF || m_CurParams.TM_Param == cv::TM_SQDIFF_NORMED) {
        TrackRect.x = MinPoint.x;
        TrackRect.y = MinPoint.y;
    }
    else {
        TrackRect.x = MaxPoint.x;
        TrackRect.y = MaxPoint.y;
    }

    return true;
}

bool CVisualTracker::TrackingByMeanShift(cv::Mat &Frame, cv::Rect &TrackRect) {
    if (GetROI() == NULL) {
        return false;
    }

    m_vmin = m_CurParams.MS_Param.vmin;
    m_vmax = m_CurParams.MS_Param.vmax;
    m_smin = 30;

    ImgHueExtraction(Frame);

    if (m_bROIchanged) {
        PrepareForBackProject(TrackRect);
        m_bROIchanged = false;
    }

    const float* range[] = { m_hranges };

    cv::calcBackProject(&m_hue, 1, 0, m_hist, m_backproj, range);

    m_backproj &= m_mask;

    cv::TermCriteria term_crit(cv::TermCriteria::EPS, 10, 1.0);
    cv::meanShift(m_backproj, TrackRect, term_crit);

    return true;
}

bool CVisualTracker::TrackingByCamShift(cv::Mat &Frame, cv::Rect &TrackRect) {
    if (GetROI() == NULL) {
        return false;
    }

    m_vmin = m_CurParams.CS_Param.vmin;
    m_vmax = m_CurParams.CS_Param.vmax;
    m_smin = m_CurParams.CS_Param.smin;

    ImgHueExtraction(Frame);

    if (m_bROIchanged) {
        PrepareForBackProject(TrackRect);
        m_bROIchanged = false;
    }

    const float* range[] = { m_hranges };

    m_backproj.create(m_hsv.size(), m_hsv.depth());
    cv::calcBackProject(&m_hue, 1, 0, m_hist, m_backproj, range, 1.0, true);

    m_backproj &= m_mask;

    cv::TermCriteria term_crit(cv::TermCriteria::EPS, 20, 1.0);
    cv::RotatedRect trackRect = cv::CamShift(m_backproj, TrackRect, term_crit);

    cv::ellipse(Frame, trackRect, CV_RGB(0, 0, 255), 1);
}

void CVisualTracker::SetMethodType(VT_Method_Type Type) {
    m_CurType = Type;
}

VT_Method_Type CVisualTracker::GetMethodType() const {
    return m_CurType;
}

void CVisualTracker::SetVT_Params(VT_Method_Type Type, VT_Params Param) {
    switch (Type) {
        case VT_Method_Type::TemplateMatch:
            m_CurParams.TM_Param = Param.TM_Param;
            break;
        case VT_Method_Type::CAMShift:
            m_CurParams.CS_Param = Param.CS_Param;
            break;
        case VT_Method_Type::MeanShift:
            m_CurParams.MS_Param = Param.MS_Param;
            break;
        default:
            break;
    }
}

VT_Params CVisualTracker::GetVT_Params() const {
    return m_CurParams;
}

void CVisualTracker::SetROI(cv::Mat* pROI) {
    if(m_pROI != pROI){
        m_pROI = pROI;
        m_bROIchanged = true;
    }
}

cv::Mat* CVisualTracker::GetROI() const {
    return m_pROI;
}

bool CVisualTracker::Tracking(cv::Mat &Frame, cv::Rect &TrackRect) {
    bool state = false;

    switch (GetMethodType()) {
        case VT_Method_Type::TemplateMatch:
            state = TrackingByTempMatching(Frame, TrackRect);
            break;
        case VT_Method_Type::MeanShift:
            state = TrackingByMeanShift(Frame, TrackRect);
            break;
        case VT_Method_Type::CAMShift:
            state = TrackingByCamShift(Frame, TrackRect);
            break;
        default:
            break;
    }

    return state;
}

void CVisualTracker::ShowResult(cv::Mat &Frame, cv::Rect &TrackRect) {
    cv::rectangle(Frame, TrackRect, CV_RGB(255, 0, 0), 1, 8, 0);
    cv::imshow("MainWindow", Frame);
}

void CVisualTracker::ImgHueExtraction(cv::Mat &Frame) {
    //RGB image convert2 HSV image
    cv::cvtColor(Frame, m_hsv, CV_BGR2HSV);

    cv::inRange(m_hsv, cv::Scalar(m_hranges[0], m_smin, m_vmin), cv::Scalar(m_hranges[1], 255, m_vmax), m_mask);

    m_hue.create(m_hsv.size(), m_hsv.depth());

    int from_to[] = { 0,0 };

    cv::mixChannels(m_hsv, m_hue, from_to, 1);
}

void CVisualTracker::PrepareForBackProject(cv::Rect &selection) {
    //Generate ROI image for m_hue and m_mask
    cv::Mat m_hueROI = m_hue(selection);
    cv::Mat m_maskROI = m_mask(selection);

    //Use calcHist Function generate Histogram on m_hist
    const float* ranges[] = { m_hranges };

    cv::calcHist(&m_hueROI, 1, 0, m_maskROI, m_hist, 1, &m_hsize, ranges);

    //Nomalize m_hist
    cv::normalize(m_hist, m_hist, 0, 255, cv::NORM_MINMAX);
}

static CVisualTracker* cvisualtracker = NULL;

extern "C"{
    JNIEXPORT void JNICALL
    Java_martianlee_trackingapplication_CVisualTracker_SetMethodType(
            JNIEnv *env, jobject obj, VT_Method_Type Type) {
        cvisualtracker->SetMethodType(Type);
    }

    JNIEXPORT void JNICALL
    Java_martianlee_trackingapplication_CVisualTracker_GetMethodType(
            JNIEnv *env, jobject obj, VT_Method_Type Type) {
        cvisualtracker->GetMethodType();
    }

    JNIEXPORT void JNICALL
    Java_martianlee_trackingapplication_CVisualTracker_SetVT_Params(
            JNIEnv *env, jobject obj, VT_Method_Type Type, VT_Params Param) {
        cvisualtracker->SetVT_Params(Type, Param);
    }

    JNIEXPORT VT_Params JNICALL
    Java_martianlee_trackingapplication_CVisualTracker_GetVT_Params(
            JNIEnv *env, jobject obj) {
        cvisualtracker->GetVT_Params();
    }

    JNIEXPORT void JNICALL
    Java_martianlee_trackingapplication_CVisualTracker_SetROI(
            JNIEnv *env, jobject obj, cv::Mat* pROI) {
        cvisualtracker->SetROI(pROI);
    }

    JNIEXPORT jlong JNICALL
    Java_martianlee_trackingapplication_CVisualTracker_GetROI(
            JNIEnv *env, jobject obj) {
        cvisualtracker->GetROI();
    }
//JNIEXPORT jboolean JNICALL
//    Java_martianlee_trackingapplication_CVisualTracker_GetROI(
//            JNIEnv *env, jobject obj, VT_Method_Type Type) {
//        cvisualtracker->GetROI();
//    }

//
}





