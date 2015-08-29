package org.jlab.rec.ftof;

public class FTOFPaddle {

	// Key fields
	private int mSector;
	private String mPanel;
	private int mPaddleNumber;

	// Calculated calibration values
	private double mVeffLeftEdge;
	private double mVeffRightEdge;
	private double mLogRatioPeak;
	private double mLogRatioError;
	private double mGeometricMeanPeak;
	private double mGeometricMeanError;
	private double mLRLeftEdge;
	private double mLRRightEdge;
	private double mLRLeftEdgeError;
	private double mLRRightEdgeError;

	
	// Constructor
	FTOFPaddle() {		
		mGeometricMeanPeak = 0.0;
		mGeometricMeanError = 0.0;
	}

	// Getters	
	public int getPaddleNumber() {
		return mPaddleNumber;
	}

	public double getVeffLeftEdge() {
		return mVeffLeftEdge;
	}

	public double getVeffRightEdge() {
		return mVeffRightEdge;
	}

	public double getLogRatioPeak() {
		return mLogRatioPeak;
	}

	public double getLogRatioError() {
		return mLogRatioError;
	}

	public double getGeometricMeanPeak() {
		return mGeometricMeanPeak;
	}
	
	public double getGeometricMeanError() {
		return mGeometricMeanError;
	}
	
	public double getLRLeftEdge() {
		return mLRLeftEdge;
	}

	public double getLRRightEdge() {
		return mLRRightEdge;
	}	

	public double getLRLeftEdgeError() {
		return mLRLeftEdgeError;
	}

	public double getLRRightEdgeError() {
		return mLRRightEdgeError;
	}		
	
	// Setters	
	public void setVeffLeftEdge(double veffLeftEdge) {
		mVeffLeftEdge = veffLeftEdge;
	}

	public void setVeffRightEdge(double veffRightEdge) {
		mVeffRightEdge = veffRightEdge;
	}
	
	public void setLogRatioPeak(double logRatioPeak) {
		mLogRatioPeak = logRatioPeak;
	}

	public void setLogRatioError(double logRatioError) {
		mLogRatioError = logRatioError;
	}

	public void setGeometricMeanPeak(double geometricMeanPeak) {
		mGeometricMeanPeak = geometricMeanPeak;
	}
	
	public void setGeometricMeanError(double geometricMeanError) {
		mGeometricMeanError = geometricMeanError;
	}
	
	public void setLRLeftEdge(double lRLeftEdge) {
		mLRLeftEdge = lRLeftEdge;
	}

	public void setLRRightEdge(double lRRightEdge) {
		mLRRightEdge = lRRightEdge;
	}	

	public void setLRLeftEdgeError(double lRLeftEdgeError) {
		mLRLeftEdgeError = lRLeftEdgeError;
	}

	public void setLRRightEdgeError(double lRRightEdgeError) {
		mLRRightEdgeError = lRRightEdgeError;
	}	
}
