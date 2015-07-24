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


}
