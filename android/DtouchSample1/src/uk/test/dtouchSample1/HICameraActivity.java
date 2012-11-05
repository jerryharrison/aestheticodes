package uk.test.dtouchSample1;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class HICameraActivity extends Activity{
    
    static final int DIALOG_MARKER_DETECTION_ID = 0;
    private static final String TAG = "Tableware::TablewareActivity";
            
    private HIMarkerSurfaceView mMarkerSurfaceView;
    
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
    	@Override
    	public void onManagerConnected(int status) {
    	   switch (status) {
    	       case LoaderCallbackInterface.SUCCESS:
    	       {
    	    	   Log.i(TAG, "OpenCV loaded successfully");
    	    	   // Create and set View
    	    	   setContentView(R.layout.markercamera);
    	    	   mMarkerSurfaceView = (HIMarkerSurfaceView) findViewById(R.id.MarkerSurfaceView);
    	    	   startMarkerDetectionProcess();
    	      } break;
    	       default:
    	       {
    	    	   super.onManagerConnected(status);
    	       } break;
    	   }
    	}
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    
    @Override 
    public void onResume(){
    	super.onResume();
    	Log.d(TAG, "On Resume");
    	//startMarkerDetectionProcess();
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack))
        {
        	Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.d(TAG, "On Resume");
    	stopMarkerDetectionProcess();
    }
    
    private void stopMarkerDetectionProcess(){
    	mMarkerSurfaceView.stopProcessing();
    }
    
    private void startMarkerDetectionProcess(){
    	mMarkerSurfaceView.startProcessing();
    }
    
}