#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

using namespace cv;

IplImage * change4channelTo3InIplImage(IplImage * src);

extern "C"{
    JNIEXPORT jintArray
    JNICALL
    Java_martianlee_trackingapplication_MainActivity_getCannyImg(
            JNIEnv *env, jobject obj, jintArray buf, int w, int h) {
        static  jboolean false_ = false;
        static  jboolean true_ = true;

        jint *cbuf;
        cbuf = env->GetIntArrayElements(buf, &false_);
        if(cbuf == NULL){
            return 0;
        }

        Mat myImg(h, w, CV_8UC4, (unsigned char *) cbuf);
        IplImage image = IplImage(myImg);
        IplImage *image3channel = change4channelTo3InIplImage(&image);

        IplImage *cannyImage = cvCreateImage(cvGetSize(image3channel), IPL_DEPTH_8U, 1);

        cvCanny(image3channel, cannyImage, 50, 150, 3);

        int *outImage = new int(w * h);
        for(int i = 0; i < w * h; i++){
            outImage[i] = (int) cannyImage->imageData[i];
        }

        int size = w * h;
        jintArray result = env->NewIntArray(size);
        env->SetIntArrayRegion(result, 0, size, outImage);
        env->ReleaseIntArrayElements(buf, cbuf, 0);
        return result;
    }
}

IplImage* change4channelTo3InIplImage(IplImage * src){
    if(src->nChannels != 4){
        return NULL;
    }

    IplImage * destImg = cvCreateImage(cvGetSize(src), IPL_DEPTH_8U, 3);
    for(int row = 0; row < src->height; row++){
        for(int col = 0; col < src->width; ++col){
            CvScalar s = cvGet2D(src, row, col);
            cvSet2D(destImg, row, col, s);
        }
    }
    return destImg;
}