package uk.ac.horizon.tableware;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

class TWMarkerSurfaceView extends TWSurfaceViewBase {
    private static final int NO_OF_TILES = 2;
    private Mat mRgba;
    private Mat mGray;
    private ArrayList<Mat> mComponents;
    private Mat mHierarchy;
    private MarkerDetector markerDetector;
    private Mat mMarkerImage;
    private OnMarkerDetectedListener markerListener;
    private boolean mMarkerDetected;
    
    
    /*Define interface to call back when marker is detected:
     * 
     */
    public interface OnMarkerDetectedListener{
    		void onMarkerDetected(DtouchMarker marker);
    		void onMarkerScanned(DtouchMarker marker);
    }
    
    public TWMarkerSurfaceView(Context context) {
        super(context);
    }
    
    public TWMarkerSurfaceView(Context context, AttributeSet attrs){
    	super(context, attrs);
    }
    
    public void setOnMarkerDetectedListener(OnMarkerDetectedListener listener){
    	this.markerListener = listener;
    }

    @Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        super.surfaceChanged(_holder, format, width, height);
    }
    
    private Bitmap processFrameForMarker(VideoCapture capture, DtouchMarker marker) {
      	switch (TablewareActivity.viewMode) {
           case TablewareActivity.VIEW_MODE_MARKER:
	        	processFrameForMarkers(capture, marker);
	            break;
           case TablewareActivity.VIEW_MODE_MARKER_DEBUG:
        	   processFrameForMarkersDebug(capture);
	            break;
	       default:
	    	    break;
        }

        Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        if (Utils.matToBitmap(mRgba, bmp))
            return bmp;
        bmp.recycle();
        return null;
    }
    
    private Bitmap displayDetectedMarker(VideoCapture capture, Mat markerImage){
    	//Get original image.
    	capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
    	displayRectOnImageSegment(mRgba,true);
    	displayMarkerImage(mMarkerImage, mRgba);
    	
    	 Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
         if (Utils.matToBitmap(mRgba, bmp))
             return bmp;
         bmp.recycle();
         return null;
    }
    
    private void processFrameForMarkers(VideoCapture capture, DtouchMarker marker){
    	//Get original image.
    	capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        //Get gray scale image.
       	capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
       	//Get image segment to detect marker. 	
    	Mat imgSegmentMat = cloneMarkerImageSegment(mGray);
    	//apply threshold.
    	Mat thresholdedImgMat = new Mat(imgSegmentMat.size(), imgSegmentMat.type());
    	applyThresholdOnImage(imgSegmentMat,thresholdedImgMat);
    	imgSegmentMat.release();
    	//find markers.
    	boolean markerFound = findMarkers(thresholdedImgMat, marker);
    	thresholdedImgMat.release();
    	//Marker detected.
    	if (markerFound){
    		setMarkerDetected(true);
    		//if marker is found then copy the marker image segment.
    		mMarkerImage = cloneMarkerImageSegment(mRgba);
    		//display codes on the original image.
    		displayMarkerCodes(mRgba, marker);
    		//display rect with indication that a marker is identified.
    		displayRectOnImageSegment(mRgba,true);
    		//display marker image
    		displayMarkerImage(mMarkerImage, mRgba);
    	}else
    		displayRectOnImageSegment(mRgba,false);
    }
    
    private void processFrameForMarkersDebug(VideoCapture capture){
    	//Get original image.
    	capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        //Get gray scale image.
    	capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);
    	//Get image segment to detect marker.    	
    	Mat imgSegmentMat = cloneMarkerImageSegment(mGray);
    	//apply threshold.
    	Mat thresholdedImgMat = new Mat(imgSegmentMat.size(), imgSegmentMat.type());
    	ArrayList<Double> localThresholds = applyThresholdOnImage(imgSegmentMat,thresholdedImgMat);
    	imgSegmentMat.release();
    	
      	copyThresholdedImageToRgbImgMat(thresholdedImgMat, mRgba);
      	  	
    	Scalar contourColor = new Scalar(0, 0, 255);
    	Scalar codesColor = new Scalar(255,0,0,255);
    	displayMarkersDebug(thresholdedImgMat, contourColor, codesColor);
    	//displayThresholds(mRgba, codesColor, localThresholds);
    	thresholdedImgMat.release();
    }
    
    private Mat cloneMarkerImageSegment(Mat imgMat){
    	Rect rect = calculateImageSegmentArea(imgMat);
        Mat calculatedImg = imgMat.submat(rect.y, rect.y + rect.height,rect.x,rect.x + rect.width);
    	return calculatedImg.clone();
    }
        
    private Rect calculateImageSegmentArea(Mat imgMat){
       	int x = imgMat.cols() / 4;
        int y = imgMat.rows() / 4;

        int width = imgMat.cols() / 2 ;
        int height = imgMat.rows() / 2;
        
        return new Rect(x, y, width, height);
    }
    
    private void displayRectOnImageSegment(Mat imgMat, boolean markerFound){
    	Scalar color = null;
    	if (markerFound)
    		color = new Scalar(0,255,0,255);
    	else
    		color = new Scalar(255,0,0,255);
    	Rect rect = calculateImageSegmentArea(imgMat);
    	Core.rectangle(imgMat, rect.tl(), rect.br(), color, 3, Core.LINE_AA);
    }
    
    private void displayMarkerImage(Mat srcImgMat, Mat destImageMat){
    	//find location of image segment to be replaced in the destination image.
    	Rect rect = calculateImageSegmentArea(destImageMat);
    	Mat destSubmat = destImageMat.submat(rect.y,rect.y + rect.height, rect.x, rect.x + rect.width);
    	//copy image.
    	srcImgMat.copyTo(destSubmat);
    }
    
    private void copyThresholdedImageToRgbImgMat(Mat thresholdedImgMat, Mat dest){
    	//convert thresholded image segment to RGB. 
    	Mat smallRegionImg = new Mat();
    	Imgproc.cvtColor(thresholdedImgMat, smallRegionImg, Imgproc.COLOR_GRAY2BGRA, 4);
    	//find location of image segment to be replaced in the destination image.
    	Rect rect = calculateImageSegmentArea(dest);
    	Mat destSubmat = dest.submat(rect.y,rect.y + rect.height, rect.x, rect.x + rect.width);
    	//copy image.
    	smallRegionImg.copyTo(destSubmat);
    	smallRegionImg.release();
    }
    
    
    private ArrayList<Double> applyThresholdOnImage(Mat srcImgMat, Mat outputImgMat){
    	double localThreshold;
    	int startRow;
    	int endRow;
    	int startCol;
    	int endCol;
    	    	
    	ArrayList<Double> localThresholds = new ArrayList<Double>();
    	
    	int tileWidth = (int)srcImgMat.size().height / NO_OF_TILES;
    	int tileHeight = (int)srcImgMat.size().width / NO_OF_TILES;
    	
    	//Split image into tiles and apply threshold on each image tile separately.
    	
    	//process image tiles other than the last one.
    	for (int tileRowCount = 0; tileRowCount < NO_OF_TILES; tileRowCount++){
    		startRow = tileRowCount * tileWidth;
    		if (tileRowCount < NO_OF_TILES - 1)
    			endRow = (tileRowCount + 1) * tileWidth;
    		else
    			endRow = (int)srcImgMat.size().height;
    		
    		for (int tileColCount = 0; tileColCount < NO_OF_TILES; tileColCount++){
    			startCol = tileColCount * tileHeight;
    			if (tileColCount < NO_OF_TILES -1 )
    				endCol = (tileColCount + 1) * tileHeight;
    			else
    				endCol = (int)srcImgMat.size().width;
    			
    			Mat tileThreshold = new Mat();
    			Mat tileMat = srcImgMat.submat(startRow, endRow, startCol, endCol);
    			localThreshold = Imgproc.threshold(tileMat, tileThreshold, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
    			Mat copyMat = outputImgMat.submat(startRow, endRow, startCol, endCol);
    			tileThreshold.copyTo(copyMat);
    			tileThreshold.release();
    			localThresholds.add(localThreshold);
    		}
    	}
    	    	
    	return localThresholds;
    }
    
    private boolean findMarkers(Mat imgMat, DtouchMarker marker){
    	boolean markerFound = false;
    	Mat contourImg = imgMat.clone();
    	//Find blobs using connect component.
    	Imgproc.findContours(contourImg, mComponents, mHierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
    	//No need to use contourImg so release it.
    	contourImg.release();
    	
    	List<Integer> code = new ArrayList<Integer>();
    	    	
    	for (int i = 0; i < mComponents.size(); i++){
    		//clean this list.
    		code.clear();
    		if (markerDetector.verifyRoot(i, mComponents.get(i), mHierarchy,imgMat,code)){
    			//if marker found.
    			marker.setCode(code);
    			marker.setComponent(mComponents.get(i));
    			marker.setComponentIndex(i);
    			markerFound = true;
    			break;
    		}
		}
    	return markerFound;
    }
    
    private void displayMarkerCodes(Mat imgMat, DtouchMarker marker){
    	Scalar codesColor = new Scalar(255,0,0,255);
    	String code = codeArrayToString(marker.getCode());
		Point codeLocation = new Point(imgMat.cols() / 4, imgMat.rows()/8);
		Core.putText(imgMat, code, codeLocation, Core.FONT_HERSHEY_COMPLEX, 1, codesColor,3);
    	
    	/*for (DtouchMarker marker : markers){
    		String code = codeArrayToString(marker.getCode());
    		Point codeLocation = new Point(imgMat.cols() / 4, imgMat.rows()/8);
    		Core.putText(imgMat, code, codeLocation, Core.FONT_HERSHEY_COMPLEX, 1, codesColor,3);
    	}*/
    }
    
    private void displayMarkersDebug(Mat imgMat, Scalar contourColor, Scalar codesColor){
    	DtouchMarker marker = new DtouchMarker();
    	boolean markerFound = findMarkers(imgMat, marker);
    	if (markerFound){    	
    		String code = codeArrayToString(marker.getCode());
    		Point codeLocation = new Point(imgMat.cols() / 4, imgMat.rows()/8);
    		Core.putText(mRgba, code, codeLocation, Core.FONT_HERSHEY_COMPLEX, 1, codesColor,3);
    		Imgproc.drawContours(mRgba, mComponents, marker.getComponentIndex(), contourColor, 3, 8, mHierarchy, 0);
    	}
    	/*
    	for (DtouchMarker marker : markers){
    		String code = codeArrayToString(marker.getCode());
    		Point codeLocation = new Point(imgMat.cols() / 4, imgMat.rows()/8);
    		Core.putText(mRgba, code, codeLocation, Core.FONT_HERSHEY_COMPLEX, 1, codesColor,3);
    		Imgproc.drawContours(mRgba, mComponents, marker.getComponentIndex(), contourColor, 3, 8, mHierarchy, 0);
    	}*/
    }

    private void displayThresholds(Mat ImgMat, Scalar thresholdColor, ArrayList<Double> thresholds){
    	Point thresholdLocation = new Point(10.0,20.0);
    	int yOffset = 30;
    	int i = 1;
    	for (Double threshold : thresholds){
    		thresholdLocation.y = i * yOffset;    
    		String thresholdString = "(" + threshold.toString() + ")";
    		Core.putText(ImgMat, thresholdString, thresholdLocation, Core.FONT_HERSHEY_COMPLEX, 1, thresholdColor,3);
    		i++;
    	}
    }
    
    private String codeArrayToString(List<Integer> codes){
    	StringBuffer code = new StringBuffer();
    	for(int i = 0; i < codes.size(); i++){
    		if (i > 0)
    			code.append(":");
    		code.append(codes.get(i));
    	}
    	return code.toString();
    }
    
    public void run() {
        try
        {
        	initData();
        	while (Thread.currentThread() == mThread) {
        		Bitmap bmp = null;
        		DtouchMarker dtouchMarker = new DtouchMarker();
        		
        		synchronized (this) {
        			if (mCamera == null)
        				break;

        			if (!mCamera.grab()) {
        				break;
        			}
        			if (Thread.currentThread().isInterrupted()){
        				throw new InterruptedException("Thread interrupted.");
        			}
        			
        			if(!mMarkerDetected){
        				bmp = processFrameForMarker(mCamera, dtouchMarker);
        			}else{
        				bmp = displayDetectedMarker(mCamera,mMarkerImage); 
        			}
        		}
        		if (bmp != null && mCamera != null) {
        			Canvas canvas = mHolder.lockCanvas();
        			if (canvas != null) {
        				canvas.drawBitmap(bmp, (canvas.getWidth() - bmp.getWidth()) / 2, (canvas.getHeight() - bmp.getHeight()) / 2, null);
        				mHolder.unlockCanvasAndPost(canvas);
        			}
        			bmp.recycle();
        		}
        	  	
        		if (dtouchMarker.getCode() != null){
        			if (markerListener != null){
        				markerListener.onMarkerDetected(dtouchMarker);
            			//break;
            		}
        		}
        	}
        } catch(Throwable t){
        	Log.i(TAG, "Camera processing stopped due to interrupt");
        }
        
        Log.i(TAG, "Finishing processing thread");
        releaseData();
    }    
    
    @Override
    protected void initData(){
    	releaseData();
        
    	synchronized (this) {
            // initialise Mats before usage
            mGray = new Mat();
            mRgba = new Mat();
            mMarkerImage = new Mat();
            mComponents = new ArrayList<Mat>();
            mHierarchy = new Mat();
        }
    	markerDetector = new MarkerDetector(this.getContext());
    	mMarkerDetected = false;
    }
    
    @Override
    protected void releaseData(){
        synchronized (this) {
            // Explicitly deallocate Mats
            if (mRgba != null)
                mRgba.release();
            if (mMarkerImage != null)
            	mMarkerImage.release();
            if (mGray != null)
                mGray.release();
            if (mComponents != null)
            	mComponents.clear();
            if (mHierarchy != null)
            	mHierarchy.release();
            mRgba = null;
            mGray = null;
            mComponents = null;
            mHierarchy = null;
        }
        markerDetector = null;
    }
    
    private void setMarkerDetected(boolean detected){
    	mMarkerDetected = detected;
    }
 
}