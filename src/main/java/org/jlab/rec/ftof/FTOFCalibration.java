package org.jlab.rec.ftof;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.jlab.clasrec.main.DetectorMonitoring;
import org.jlab.clasrec.utils.ServiceConfiguration;
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioDataSync;
import org.jlab.evio.clas12.EvioFactory;
import org.root.group.TDirectory;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.pad.*;
import org.root.group.*;
import org.root.histogram.*;
import org.root.data.DataSetXY;
import org.root.func.*;


public class FTOFCalibration extends DetectorMonitoring {
	
	public FTOFCalibration() {
		super("FTOF", "0.1", "Louise Clark");
	}

	// Constants

	final int PEDESTAL_CUT_OFF = 300;
	final double LOG_RATIO_X_THRESHOLD = 500;
	final double LOG_RATIO_THRESHOLD_FRACTION = 0.2;
	final double GM_FIT_LOW_FRACTION = 0.5;
	final double GM_FIT_HIGH_FRACTION = 1.6;
	final double ALT_GM_FIT_LOW_FRACTION = 0.25;
	final double ALT_GM_FIT_HIGH_WIDTH = 1200;
	final double GM_FIT_ALT_CUT_OFF = 2000;
	
	private H1D rebin(H1D h1, int nBinsCombine) {
		
		int nBinsOrig = h1.getAxis().getNBins();
		
//		System.out.println("min "+ (int) h1.getAxis().min());
//		System.out.println("max "+ h1.getAxis().max());
//		System.out.println("nBinsOrig "+ nBinsOrig);
//		System.out.println("nBinsCombine "+ nBinsCombine);
//		System.out.println("division "+ nBinsOrig/nBinsCombine);
		
		H1D h1Rebinned = new H1D("Rebinned", nBinsOrig/nBinsCombine, h1.getAxis().min(), h1.getAxis().max());
		
		int newBin = 0;
		
		for (int origBin=0; origBin<=nBinsOrig;) {
			
			double newBinCounts = 0;
			for (int i=0; i<nBinsCombine; i++) {
				newBinCounts = newBinCounts + h1.getBinContent(origBin);
				origBin++;				
			}
			h1Rebinned.setBinContent(newBin, newBinCounts);
			newBin++;
			
		}
		
		return h1Rebinned;
		
	}

	private void fitGain(String directory, int sectorNum, int paddleNum, FTOFPaddle currentPaddle) {

		// Get the geometric mean histogram
		String paddleText = String.format("%02d", paddleNum);
		H1D geoMeanHist = (H1D) getDir().getDirectory("calibration/"+directory+"/geomean").getObject("GEOMEAN_S"+sectorNum+"_P"+paddleText);
		
		double nEntries=geoMeanHist.getEntries();
		
		// Check for empty histogram
		if (nEntries==0) {
			currentPaddle.setGeometricMeanPeak(0.0);
			currentPaddle.setGeometricMeanError(0.0);
			return;
		}
		
		// Fit geometric mean

		// First does rebin depending on number of entries
//		int nRebin=(int) (50000/nEntries);           
//		if (nRebin>5) {
//			nRebin=5;               
//		}
		
//		if(nRebin>0) {
//			geoMeanHist = rebin(geoMeanHist, nRebin);
//		}

		// Work out parameter values based on the maximum entries
		// Doing this for now until I know the best replacement for the TSpectrum code

		int maxBin = geoMeanHist.getMaximumBin();  
		double maxCounts = geoMeanHist.getBinContent(maxBin);
		double maxPos = geoMeanHist.getAxis().getBinCenter(maxBin);

		// the range for the fit is to be 0.5 to 1.6 * position of max
		// OR
		// if position of max is < 2000 then it's to be (0.25 * position of max) to (position of max + 1200)


		double lowFit, highFit;
		//if (maxPos < GM_FIT_ALT_CUT_OFF) {
		//	lowFit = maxPos * ALT_GM_FIT_LOW_FRACTION;
		//	highFit = maxPos + ALT_GM_FIT_HIGH_WIDTH;
		//}
		//else {
		lowFit = maxPos * GM_FIT_LOW_FRACTION;
		highFit = maxPos * GM_FIT_HIGH_FRACTION;
		//}

		F1D func = new F1D("landau+exp",lowFit, highFit);
		func.setName("GEOMEAN_FUNC_S"+sectorNum+"_P"+paddleText);
				
		// first draft of parameter setting
		// final method of determining parameters to be confirmed

		func.setParameter(0, maxCounts);
		func.setParameter(1, maxPos);
		func.setParameter(2, 100.0);
		func.setParLimits(2, 0.0,400.0);
		func.setParameter(3, 20.0);
		func.setParameter(4, 0.0);
		func.show();
		geoMeanHist.fit(func);
		
		//DataSetXY funcData = func.getDataSet();
		func.setName("GEOMEAN_FUNC_S"+sectorNum+"_P"+paddleText);
		
		getDir().getDirectory("calibration/"+directory+"/geomean").add(func);

		
		// Return the calibration constants (the position and error of the peak)
		currentPaddle.setGeometricMeanPeak(func.getParameter(1));
		currentPaddle.setGeometricMeanError(func.getParameter(2)); // this should be the error - TO DO
		
		// Plot the values in the summary plot
		H1D geoMeanPeakHist = (H1D) getDir().getDirectory("calibration/"+directory+"/geomean").getObject("GEOMEAN_PEAK_ALL");
		
		int numPaddles =0;
		if (directory=="ftof1a") numPaddles=23;
		if (directory=="ftof1b") numPaddles=62;
		if (directory=="ftof2b") numPaddles=5;
		
		if ((sectorNum==0)&&(paddleNum==0)) {
			System.out.println("Setting Bin content");
			System.out.println("Bin number: "+(sectorNum*numPaddles)+paddleNum);
			System.out.println("Bin content: "+func.getParameter(1));
		}
		geoMeanPeakHist.setBinContent((sectorNum*numPaddles)+paddleNum, func.getParameter(1));
		
		//for (int i=0; i<func.getParameter(1); i++) {
		//	geoMeanPeakHist.fill((sectorNum*numPaddles)+paddleNum);
		//}
	}
	
	private void calculateLogRatio(String directory, int sectorNum, int paddleNum, FTOFPaddle currentPaddle) {
		
		// Get the effective velocity histogram
		String paddleText = String.format("%02d", paddleNum);
		H1D logRatioHist = (H1D) getDir().getDirectory("calibration/"+directory+"/logratio").getObject("LOGRATIO_S"+sectorNum+"_P"+paddleText);

		// calculate the mean value using portion of the histogram where counts are > 0.2 * max counts
		
		double sum =0;
		double sumWeight =0;
		double sumSquare =0;
		int maxBin = logRatioHist.getMaximumBin();
		double maxCounts = logRatioHist.getBinContent(maxBin);
		int nBins = logRatioHist.getAxis().getNBins();
		boolean lowThresholdReached = false;
		boolean highThresholdExceeded = false;
		
		for (int i=1; i<=nBins; i++) {
			
			// check if we're within the thresholds
			if (!lowThresholdReached) {
				
				if (logRatioHist.getBinContent(i) > (LOG_RATIO_THRESHOLD_FRACTION*maxCounts) && i<maxBin) {
					lowThresholdReached = true;
				}				
			}
			
			if (lowThresholdReached && !highThresholdExceeded) {
				
				if (logRatioHist.getBinContent(i) < (LOG_RATIO_THRESHOLD_FRACTION*maxCounts) && i>maxBin) {
					highThresholdExceeded = true;
				}				
			}
			
			// include the values in the sum if we're within the thresholds
			if (lowThresholdReached && !highThresholdExceeded) {
				
				double value=logRatioHist.getBinContent(i);
				double middle=logRatioHist.getAxis().getBinCenter(i);
				
				sum+=value;
				sumWeight+=value*middle;
				sumSquare+=value*middle*middle;
			}			
		}
		
		double logRatioPeak = 0.0;
		double logRatioError = 0.0;
				
		if (sum>0) {
			logRatioPeak=sumWeight/sum;
			logRatioError=Math.sqrt((sumSquare/sum)-logRatioPeak*logRatioPeak);
		}
		else {
			logRatioPeak=0.0;
			logRatioError=0.0;
		}
		
		// Return the calibration constants (the left and right edges of half max width)
		currentPaddle.setLogRatioPeak(logRatioPeak);
		currentPaddle.setLogRatioError(logRatioError);
		
	}
	
	private void calculateVeffEdges(String directory, int sectorNum, int paddleNum, FTOFPaddle currentPaddle) {
		
		// Get the effective velocity histogram
		String paddleText = String.format("%02d", paddleNum);
		H1D veffHist = (H1D) getDir().getDirectory("calibration/"+directory+"/veff").getObject("VEFF_S"+sectorNum+"_P"+paddleText);
		
    	// get the bins left and right of the max bin with contents closest to half maximum		

		int maxBin = veffHist.getMaximumBin();
		double halfMax = veffHist.getBinContent(maxBin) / 2;
		double minDifference = halfMax;
		double binDifference;
		int leftHalfMaxBin = -1;
		
		for (int i=1; i<maxBin; i++) {
			
			binDifference = Math.abs(veffHist.getBinContent(i) - halfMax);
			if (binDifference < minDifference) {
				minDifference = binDifference;
				leftHalfMaxBin = i;
			}
		}
		
		minDifference = halfMax;
		int rightHalfMaxBin = -1;
	
		for (int i=maxBin+1; i<= veffHist.getAxis().getNBins(); i++) {
			
			binDifference = Math.abs(veffHist.getBinContent(i) - halfMax);
			if (binDifference < minDifference) {
				minDifference = binDifference;
				rightHalfMaxBin = i;
			}
		}
		
		F1D veffEdgesFunc = new F1D("p1",veffHist.getAxis().getBinCenter(leftHalfMaxBin), 
										 veffHist.getAxis().getBinCenter(rightHalfMaxBin));
    	veffEdgesFunc.setParameter(1, 0.0);
    	veffEdgesFunc.setParameter(0, 
    			(veffHist.getBinContent(veffHist.getMaximumBin()))/2);
    	
    	veffEdgesFunc.setName("VEFF_FUNC_S"+sectorNum+"_P"+paddleText);
		
		getDir().getDirectory("calibration/"+directory+"/veff").add(veffEdgesFunc);


		// Return the calibration constants (the left and right edges of half max width)
		currentPaddle.setVeffLeftEdge(veffHist.getAxis().getBinCenter(leftHalfMaxBin));
		currentPaddle.setVeffRightEdge(veffHist.getAxis().getBinCenter(rightHalfMaxBin));
				
	}		 
	
	private void fitAttenSlope(String directory, int sectorNum, int paddleNum, FTOFPaddle currentPaddle) {
		
		// Get the attenuation 2D histogram
		String paddleText = String.format("%02d", paddleNum);
		H2D attenHist = (H2D) getDir().getDirectory("calibration/"+directory+"/atten").getObject("ATTEN_S"+sectorNum+"_P"+paddleText);

		// Populate values for a graph of
		// x = position = ((timeLeft - timeRight) / 2) * effective velocity
		//   = x axis bin of 2D histogram
		// y = ratio = log(energyLeft) / log(energyRight)
		//   = mean of projection on to y axis of 2D histogram
		
		int nBinX = attenHist.getXAxis().getNBins();
		int nBinY = attenHist.getYAxis().getNBins();
		
		double[] x = new double[nBinX];
		double[] y = new double[nBinY];
		double[] errorX = new double[nBinX];
		double[] errorY = new double[nBinY];
		
		int iBin=0;
		for (int i=0; i<= nBinX; i++) {
			
			H1D yProj = attenHist.sliceY(i+1);
			
			if (yProj.getEntries()>0) {
				y[iBin] = yProj.getMean();
				x[iBin] = attenHist.getXAxis().getBinCenter(i+1);
				errorX[iBin] = attenHist.getXAxis().getBinWidth(i+1)/2.0;
				//errorY[iBin] = yProj.getAxis().getMeanError(); // getMeanError doesn't exist in coatjava
				iBin++;
			}
		}
		
		if (iBin>3) {
			
			GraphErrors attenGraph = new GraphErrors(x,y);
			F1D attenFunc = new F1D("p1");
			attenGraph.fit(attenFunc);
			
			// Test - draw the graph
			
			TCanvas c1 = new TCanvas("Attenuation","Attenuation",1200,800,1,1);
			
			attenGraph.setLineColor(2);
			c1.draw(attenGraph,"*");
			c1.draw(attenFunc, "same");
			
		}
		
		// Test
		// Draw out a few of the projections
		if ((directory=="ftof1a") && (sectorNum == 0) && (paddleNum == 4)) {
			TCanvas c1 = new TCanvas("Y Projection", "Y Projection", 1200,800,2,2);
			c1.cd(0);
			c1.draw(attenHist.sliceY(attenHist.getXAxis().getBin(2.0)));
			c1.cd(1);
			c1.draw(attenHist.sliceY(attenHist.getXAxis().getBin(2.5)));
			c1.cd(2);
			c1.draw(attenHist.sliceY(attenHist.getXAxis().getBin(5.5)));
			c1.cd(3);
			c1.draw(attenHist.sliceY(attenHist.getXAxis().getBin(9.0)));
			
		}
		
		
		
		// Return the calibration constants (the left and right edges of half max width)
		//currentPaddle.setVeffLeftEdge(veffHist.getAxis().getBinCenter(leftHalfMaxBin));
		//currentPaddle.setVeffRightEdge(veffHist.getAxis().getBinCenter(rightHalfMaxBin));
		
	}	
	
	
	private void writeConstants(String directory, int sectorNum, String panel, int paddleNum, 
								BufferedWriter veffBw,
								BufferedWriter geoMeanBw,
								BufferedWriter logRatioBw) throws IOException {
		
		// Geometric Mean
		FTOFPaddle currentPaddle = new FTOFPaddle();
		fitGain(directory, sectorNum, paddleNum, currentPaddle);
		geoMeanBw.write(sectorNum+" "+panel+" "+paddleNum+" "
				+currentPaddle.getGeometricMeanPeak()+" "+currentPaddle.getGeometricMeanError());
		geoMeanBw.newLine();	
		
		// Log Ratio
		calculateLogRatio(directory, sectorNum, paddleNum, currentPaddle);
		logRatioBw.write(sectorNum+" "+panel+" "+paddleNum+" "
				+currentPaddle.getLogRatioPeak()+" "+currentPaddle.getLogRatioError());
		logRatioBw.newLine();
		
		// Effective Velocity
		calculateVeffEdges(directory, sectorNum, paddleNum, currentPaddle);
		veffBw.write(sectorNum+" "+panel+" "+paddleNum+" "
				+currentPaddle.getVeffLeftEdge()+" "+currentPaddle.getVeffRightEdge());
		veffBw.newLine();
		
		// Attenuation Length
		//fitAttenSlope(directory, sectorNum, paddleNum, currentPaddle);
		
		
	}
	
	@Override
	public Color getColor(int sector, int layer, int component){
		
		System.out.println("LOUISE getColor");
		
		int r = 0;
		int g = layer*10;
		int b = component*10;
		//return new Color(0,200,0);
		return new Color(r,g,b);
		
	}
		
	@Override
	public void analyze() {
		
		System.out.println("LOUISE analyze");
		
/*		TCanvas c1 = new TCanvas("Attenuation","Attenuation",1200,800,2,3);
		H2D attenHist = (H2D) getDir().getDirectory("calibration/ftof1a/atten").getObject("ATTEN_S0_P00");
		attenHist.setTitle("Paddle 0 - Attenuation");
		c1.cd(0);
		c1.draw(attenHist);
		
		attenHist = (H2D) getDir().getDirectory("calibration/ftof1a/atten").getObject("ATTEN_S0_P01");
		attenHist.setTitle("Paddle 1 - Attenuation");
		c1.cd(1);
		c1.draw(attenHist);
		
		attenHist = (H2D) getDir().getDirectory("calibration/ftof1a/atten").getObject("ATTEN_S0_P02");
		attenHist.setTitle("Paddle 2 - Attenuation");
		c1.cd(2);
		c1.draw(attenHist);
		
		attenHist = (H2D) getDir().getDirectory("calibration/ftof1a/atten").getObject("ATTEN_S0_P03");
		attenHist.setTitle("Paddle 3 - Attenuation");
		c1.cd(3);
		c1.draw(attenHist);
		
		attenHist = (H2D) getDir().getDirectory("calibration/ftof1a/atten").getObject("ATTEN_S0_P04");
		attenHist.setTitle("Paddle 4 - Attenuation");
		c1.cd(4);
		c1.draw(attenHist);
		
		attenHist = (H2D) getDir().getDirectory("calibration/ftof1a/atten").getObject("ATTEN_S0_P05");
		attenHist.setTitle("Paddle 5 - Attenuation");
		c1.cd(5);
		c1.draw(attenHist);
*/		

		// Create text files for each of effective velocity, geometric mean, and log ratio
		try {

			File veffFile = new File("FTOF_VEFF.txt");
			File geoMeanFile = new File("FTOF_GEOMEAN.txt");
			File logRatioFile = new File("FTOF_LOGRATIO.txt");


			// if files don't exist, then create them
			if (!veffFile.exists()) {
				veffFile.createNewFile();
			}
			if (!geoMeanFile.exists()) {
				geoMeanFile.createNewFile();
			}
			if (!logRatioFile.exists()) {
				logRatioFile.createNewFile();
			}

			FileWriter veffFw = new FileWriter(veffFile.getAbsoluteFile());
			FileWriter geoMeanFw = new FileWriter(geoMeanFile.getAbsoluteFile());
			FileWriter logRatioFw = new FileWriter(logRatioFile.getAbsoluteFile());

			BufferedWriter veffBw = new BufferedWriter(veffFw);
			BufferedWriter geoMeanBw = new BufferedWriter(geoMeanFw);
			BufferedWriter logRatioBw = new BufferedWriter(logRatioFw);

						
			// run through all the histograms, one per sector, per paddle
			// calculate the calibration constants and write to file
			for (int sectorNum=0; sectorNum < 6; sectorNum++){

				// Panel 1a
				for (int paddleNum=0; paddleNum < 23; paddleNum++) {

					writeConstants("ftof1a", sectorNum, "Panel-1a", paddleNum, veffBw, geoMeanBw, logRatioBw);

				}

				// Panel 1b
				for (int paddleNum=0; paddleNum < 62; paddleNum++) {

					writeConstants("ftof1b", sectorNum, "Panel-1b", paddleNum, veffBw, geoMeanBw, logRatioBw);

					
//					FTOFPaddle currentPaddle = new FTOFPaddle();
//					fitGain("ftof1b", sectorNum, paddleNum, currentPaddle);
//					geoMeanBw.write(sectorNum+" Panel-1b "+paddleNum+" "
//							+currentPaddle.getGeometricMeanPeak()+" "+currentPaddle.getGeometricMeanError());
//					geoMeanBw.newLine();

				}

				// Panel 2
				for (int paddleNum=0; paddleNum < 5; paddleNum++) {

					writeConstants("ftof2b", sectorNum, "Panel-2b", paddleNum, veffBw, geoMeanBw, logRatioBw);
//
//					FTOFPaddle currentPaddle = new FTOFPaddle();
//					fitGain("ftof2b", sectorNum, paddleNum, currentPaddle);
//					geoMeanBw.write(sectorNum+" Panel-2b "+paddleNum+" "
//							+currentPaddle.getGeometricMeanPeak()+" "+currentPaddle.getGeometricMeanError());
//					geoMeanBw.newLine();

				}

			}

			// Close the files
			veffBw.close();
			geoMeanBw.close();
			logRatioBw.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public void configure(ServiceConfiguration arg0) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void init() {
		
		// create the directory structure
		System.out.println("LOUISE init");
		
		TDirectory veffDir1a = new TDirectory("calibration/ftof1a/veff");
		TDirectory veffDir1b = new TDirectory("calibration/ftof1b/veff");
		TDirectory veffDir2b = new TDirectory("calibration/ftof2b/veff");
		TDirectory geoMeanDir1a = new TDirectory("calibration/ftof1a/geomean");
		TDirectory geoMeanDir1b = new TDirectory("calibration/ftof1b/geomean");
		TDirectory geoMeanDir2b = new TDirectory("calibration/ftof2b/geomean");
		TDirectory logRatioDir1a = new TDirectory("calibration/ftof1a/logratio");
		TDirectory logRatioDir1b = new TDirectory("calibration/ftof1b/logratio");
		TDirectory logRatioDir2b = new TDirectory("calibration/ftof2b/logratio");
//		TDirectory attenDir1a = new TDirectory("calibration/ftof1a/atten");
//		TDirectory attenDir1b = new TDirectory("calibration/ftof1b/atten");
//		TDirectory attenDir2b = new TDirectory("calibration/ftof2b/atten");
		
		getDir().addDirectory(veffDir1a);
		getDir().addDirectory(veffDir1b);
		getDir().addDirectory(veffDir2b);
		getDir().addDirectory(geoMeanDir1a);
		getDir().addDirectory(geoMeanDir1b);
		getDir().addDirectory(geoMeanDir2b);
		getDir().addDirectory(logRatioDir1a);
		getDir().addDirectory(logRatioDir1b);
		getDir().addDirectory(logRatioDir2b);
//		getDir().addDirectory(attenDir1a);
//		getDir().addDirectory(attenDir1b);
//		getDir().addDirectory(attenDir2b);

		// summary plots for all paddles
		geoMeanDir1a.add(new H1D("GEOMEAN_PEAK_ALL", 139, -0.5, 138.5));
		geoMeanDir1b.add(new H1D("GEOMEAN_PEAK_ALL", 373, -0.5, 372.5));
		geoMeanDir2b.add(new H1D("GEOMEAN_PEAK_ALL", 31, 0.5, 30.5));
		veffDir1a.add(new H1D("VEFF_WIDTH_ALL", 139, -0.5, 138.5));
		veffDir1b.add(new H1D("VEFF_WIDTH_ALL", 373, -0.5, 372.5));
		veffDir2b.add(new H1D("VEFF_WIDTH_ALL", 31, 0.5, 30.5));
		logRatioDir1a.add(new H1D("LOG_RATIO_ALL", 139, -0.5, 138.5));
		logRatioDir1b.add(new H1D("LOG_RATIO_ALL", 373, -0.5, 372.5));
		logRatioDir2b.add(new H1D("LOG_RATIO_ALL", 31, 0.5, 30.5));
		
		// create the histograms, one per sector, per paddle
		for (int sectorNum=0; sectorNum < 6; sectorNum++){
			
			// Panel 1a 
			for (int paddleNum=0; paddleNum < 23; paddleNum++) {
				
				String paddleText = String.format("%02d", paddleNum);
				
				// Should the ranges be more dynamically worked out?
				
				veffDir1a.add(new H1D("VEFF_S"+sectorNum+"_P"+paddleText,1500,-1500.0,1500.0));
				geoMeanDir1a.add(new H1D("GEOMEAN_S"+sectorNum+"_P"+paddleText, 1000, 0.0, 3000.0));
				logRatioDir1a.add(new H1D("LOGRATIO_S"+sectorNum+"_P"+paddleText, 500, -3.0, 3.0));
				
				// N.B. not sure these ranges are correct
//				attenDir1a.add(new H2D("ATTEN_S"+sectorNum+"_P"+paddleText, 300, -5.0, 20.0, 300, 0.0, 1.8)); 
				
			}

			// Panel 1b
			for (int paddleNum=0; paddleNum < 62; paddleNum++) {
				
				String paddleText = String.format("%02d", paddleNum);
				
				veffDir1b.add(new H1D("VEFF_S"+sectorNum+"_P"+paddleText,1500,-1500.0,1500.0));
				geoMeanDir1b.add(new H1D("GEOMEAN_S"+sectorNum+"_P"+paddleText, 1000, 0.0, 3000.0));
				logRatioDir1b.add(new H1D("LOGRATIO_S"+sectorNum+"_P"+paddleText, 500, -3.0, 3.0));
				
				// N.B. not sure these ranges are correct
//				attenDir1b.add(new H2D("ATTEN_S"+sectorNum+"_P"+paddleText, 500, 0.0, 6.0, 500, 0.0, 6.0)); 
				
			}

			// Panel 2
			for (int paddleNum=0; paddleNum < 5; paddleNum++) {
				
				String paddleText = String.format("%02d", paddleNum);
				
				veffDir2b.add(new H1D("VEFF_S"+sectorNum+"_P"+paddleText,1500,-1500.0,1500.0));
				geoMeanDir2b.add(new H1D("GEOMEAN_S"+sectorNum+"_P"+paddleText, 1000, 0.0, 3000.0));
				logRatioDir2b.add(new H1D("LOGRATIO_S"+sectorNum+"_P"+paddleText, 500, -3.0, 3.0));
				
				
				// N.B. not sure these ranges are correct
//				attenDir2b.add(new H2D("ATTEN_S"+sectorNum+"_P"+paddleText, 500, 0.0, 30.0, 500, 0.0, 6.0)); 
				
			}
				
		}
		
	}
	
	private void processBank(EvioDataEvent event, String bankDescriptor, String subdirectory) {

		EvioDataBank bank = (EvioDataBank) event.getBank(bankDescriptor);
		for(int i = 0; i < bank.rows(); i++){

			// get values for filling the histograms
			double adcLeft = (double)bank.getInt("ADCL",i);
			double adcRight = (double)bank.getInt("ADCR",i);
			double tdcLeft = (double)bank.getInt("TDCL",i);
			double tdcRight = (double)bank.getInt("TDCR",i);
			int sector = bank.getInt("sector",i); 
			int paddle = bank.getInt("paddle",i);
			String paddleText = String.format("%02d", bank.getInt("paddle",i));

			// fill the geomean histogram with sqrt(adc left * adc right) for the current paddle
			// fill the veff histogram with (tdc left / tdc right) / 2
			// fill the log ratio histogram with log (adc right / adc left)

			// Effective velocity
			H1D veffHist = (H1D) getDir().getDirectory("calibration/"+subdirectory+"/veff").getObject("VEFF_S"+sector+"_P"+paddleText);
			veffHist.fill((tdcLeft-tdcRight)/2);			
			
			// The following functionality is only in testGeometricMean, so I'm not sure if this should be included
			// Subtract 300 from the ADC values before adding to histogram
			// Do not include any where either ADC value is < 300
			// Basically throws away the part of the histogram containing the pedestal, and shifts everything left by 300

			if (adcLeft>PEDESTAL_CUT_OFF && adcRight>PEDESTAL_CUT_OFF) {

				adcLeft = adcLeft-PEDESTAL_CUT_OFF;
				adcRight = adcRight-PEDESTAL_CUT_OFF;

				double geoMean = Math.sqrt((adcLeft) * (adcRight));
				H1D geoMeanHist = (H1D) getDir().getDirectory("calibration/"+subdirectory+"/geomean").getObject("GEOMEAN_S"+sector+"_P"+paddleText);
				geoMeanHist.fill(geoMean);

				if (geoMean > LOG_RATIO_X_THRESHOLD) {

					H1D logRatioHist = (H1D) getDir().getDirectory("calibration/"+subdirectory+"/logratio").getObject("LOGRATIO_S"+sector+"_P"+paddleText);
					logRatioHist.fill(Math.log(adcRight/adcLeft));

				}	
			}
			
			// Atten
//			H2D attenHist = (H2D) getDir().getDirectory("calibration/"+subdirectory+"/atten").getObject("ATTEN_S"+sector+"_P"+paddleText);
			
			// current c++ generates test data for this I think
			double vEff = 16; // default effective velocity to 16cm/ns
			
			if(tdcLeft>1 && tdcRight>1){
			      int indexLeft=paddle/100*1000+paddle%100;
			      int indexRight=indexLeft+100;
			      double energyLeft=adcToEnergy(indexLeft,adcLeft);
			      double energyRight=adcToEnergy(indexRight,adcRight);
			      double timeLeft=tdcToTime(indexLeft,tdcLeft);
			      double timeRight=tdcToTime(indexRight,tdcRight);
			      
			      if(energyLeft<0) break;
			      if(energyRight<0) break;
			      if(Math.log(energyRight)<1e-6) break;
			
//			      attenHist.fill( ((timeLeft-timeRight)*vEff)/2, Math.log(energyLeft)/Math.log(energyRight));
			}
			
		}
	}

	@Override
	public void processEvent(EvioDataEvent event) {

	try {
		if (event.hasBank("FTOF1A::dgtz")==true){
			processBank(event, "FTOF1A::dgtz", "ftof1a");
		}
		
		if (event.hasBank("FTOF1B::dgtz")==true){
			processBank(event, "FTOF1B::dgtz", "ftof1b");
		}
		
		if (event.hasBank("FTOF2B::dgtz")==true){
			processBank(event, "FTOF2B::dgtz", "ftof2b");
		}
	} catch (Exception e) {
		e.printStackTrace();
	}

	}
	
	// Tried this to see if something was drawn when layer was changed but doesn't seem to get executed
	// maybe drawLayer just draws the graphic
//	public void drawLayer(int sector, int layer, int component, EmbeddedCanvas canvas){
//		
//		H1D h1 = new H1D("drawLayer","drawLayer", layer, 0.0, 500.0);
//		h1.setTitle("drawLayer");
//		canvas.draw(h1);
//		
//	}
		
	
	public void drawComponent(int sector, int layer, int component, EmbeddedCanvas canvas){
	    //System.out.println("Implements Detector Component Draw : " + this.FTOF1A_ADCL.size()
	    //+ " " + this.FTOF1A_TDCL.size());
		
		if ((sector==0)&&(component==0)) {
			analyze();
		}
		
	    String titleString = "SECTOR " + sector + " LAYER " + layer + " PADDLE " + component;
	    canvas.divide(3,2);
	    
	    String panelName ="";
	    if (layer==0) { 
	    	panelName = "ftof1a";
	    } else if (layer==1) {
	    	panelName = "ftof1b";
	    } else if (layer==2) {
	    	panelName = "ftof2b";
	    }
	    //    if(this.FTOF1A_ADCL.containsKey(sector)==true){
	    //H1D hissstADCL = this.FTOF1A_ADCL.get(sector).sliceX(component);

	    try {

	    	H1D geoMeanSummHist = (H1D) this.getDir().getDirectory("calibration/"+panelName+"/geomean").getObject("GEOMEAN_PEAK_ALL");
	    	geoMeanSummHist.setTitle("Geometric Mean Summary");
	    	geoMeanSummHist.setXTitle("Paddle Number");
	    	geoMeanSummHist.setYTitle("Peak Position");

	    	canvas.cd(0);
	    	canvas.draw(geoMeanSummHist);

	    	String paddleText = String.format("%02d", component);
	    	H1D geoMeanHist = (H1D) this.getDir().getDirectory("calibration/"+panelName+"/geomean").getObject("GEOMEAN_S"+sector+"_P"+paddleText);
	    	geoMeanHist.setTitle(titleString + ": Geometric Mean");
	    		    	
	    	F1D geoMeanFunc = (F1D) this.getDir().getDirectory("calibration/"+panelName+"/geomean").getObject("GEOMEAN_FUNC_S"+sector+"_P"+paddleText);
	    	DataSetXY funcData = geoMeanFunc.getDataSet();
	    	//DataSetXY funcData = (DataSetXY) this.getDir().getDirectory("calibration/"+panelName+"/geomean").getObject("GEOMEAN_FUNC_S"+sector+"_P"+paddleText);
	    	
	    	canvas.cd(1);
	    	geoMeanHist.setLineColor(2);
	    	canvas.draw(geoMeanHist);
	    	funcData.setMarkerColor(1);
	    	funcData.setMarkerStyle(2);
	    	funcData.setMarkerSize(3);
	    	canvas.draw(funcData);

	    		    	
	    	H1D veffSummHist = (H1D) this.getDir().getDirectory("calibration/"+panelName+"/veff").getObject("VEFF_WIDTH_ALL");
	    	veffSummHist.setTitle("Effective Velocity Summary");
	    	veffSummHist.setXTitle("Paddle Number");
	    	veffSummHist.setYTitle("Width");
	    	
	    	canvas.cd(2);
	    	canvas.draw(veffSummHist);

	    	H1D veffHist = (H1D) this.getDir().getDirectory("calibration/"+panelName+"/veff").getObject("VEFF_S"+sector+"_P"+paddleText);
	    	veffHist.setTitle(titleString + ": Effective Velocity");

	    	F1D veffFunc = (F1D) this.getDir().getDirectory("calibration/"+panelName+"/veff").getObject("VEFF_FUNC_S"+sector+"_P"+paddleText);
	    	DataSetXY veffFuncData = veffFunc.getDataSet();
	    	
	    	canvas.cd(3);
	    	veffHist.setLineColor(2);
	    	canvas.draw(veffHist);
	    	veffFuncData.setMarkerColor(1);
	    	veffFuncData.setMarkerStyle(2);
	    	veffFuncData.setMarkerSize(3);
	    	canvas.draw(veffFuncData);
	    	
	    	H1D logSummHist = (H1D) this.getDir().getDirectory("calibration/"+panelName+"/logratio").getObject("LOG_RATIO_ALL");
	    	logSummHist.setTitle("Log Ratio Summary");
	    	logSummHist.setXTitle("Paddle Number");
	    	logSummHist.setYTitle("Log Ratio");

	    		    	
	    	H1D logRatioHist = (H1D) this.getDir().getDirectory("calibration/"+panelName+"/logratio").getObject("LOGRATIO_S"+sector+"_P"+paddleText);
	    	logRatioHist.setTitle(titleString + ": Log ratio");


	    	canvas.cd(3);
	    	veffHist.setLineColor(2);
	    	canvas.draw(veffHist);
	    	veffFuncData.setMarkerColor(1);
	    	veffFuncData.setMarkerStyle(2);
	    	veffFuncData.setMarkerSize(3);
	    	canvas.draw(veffFuncData);

	    	canvas.cd(4);
	    	canvas.draw(logSummHist);

	    	canvas.cd(5);
	    	canvas.draw(logRatioHist);



	    } catch (Exception e) {
	    	e.printStackTrace();
	    }

	    //	            H1D histADCL = new H1D();
	    //	            histADCL.setTitle(titleString);
	    //	            histADCL.setXTitle("ADC Left");
	    //	            canvas.cd(0);
	    //	            histADCL.setFillColor(6);
	    //	            canvas.draw(histADCL);

	    /*
	            H1D histADCR = this.FTOF1A_ADCR.get(sector).sliceX(component);
	            histADCR.setTitle(titleString);
	            histADCR.setXTitle("ADC Right");
	            canvas.cd(1);
	            histADCR.setFillColor(6);
	            canvas.draw(histADCR);

	            H1D histTDCL = this.FTOF1A_TDCL.get(sector).sliceX(component);
	            histTDCL.setTitle(titleString);
	            histTDCL.setXTitle("TDC Left");
	            canvas.cd(2);
	            histTDCL.setFillColor(5);
	            canvas.draw(histTDCL);

	            H1D histTDCR = this.FTOF1A_TDCR.get(sector).sliceX(component);
	            histTDCR.setTitle(titleString);
	            histTDCR.setXTitle("TDC Right");
	            canvas.cd(3);
	            histTDCL.setFillColor(5);
	            canvas.draw(histTDCL);
	        }*/

	}	


	public static void main(String[] args) {
	
		FTOFCalibration calib = new FTOFCalibration();
		calib.init();
		
		String fileName = "/home/louise/FTOF_Calibration_040615/trunk/data/usc_first_configuration.txt";
		//String fileName = "/home/louise/workspace/usc_first_configuration.txt";
		String line = null;

		
		boolean success = (new File
		         ("FtofInputFile.0.evio")).delete();
		
		EvioDataSync writer = new EvioDataSync();
		writer.open("FtofInputFile.evio");
		
        int maxLines=5000;        	
		
		try { 
			
            // Open the file
            FileReader fileReader = 
                new FileReader(fileName);

            // Always wrap FileReader in BufferedReader
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);            

            // Read each line of the file up to a maximum count
            int lineNum=0;
            line = bufferedReader.readLine();
            while ((maxLines==0 || lineNum<maxLines) && (line != null)) {
                
                // Each line contains Adc L, Adc R, Tdc L, Tdc R x 6
                String[] lineValues;
                lineValues = line.split(" ");
                
                for (int sectorIter=0; sectorIter<6; sectorIter++) {

                	// Create event data
                	int numRows = 6;
                	EvioDataEvent  event = EvioFactory.createEvioEvent();
                	EvioDataBank   bankFTOF1A = EvioFactory.createEvioBank("FTOF1A::dgtz", 23); //numRows);
                	EvioDataBank   bankFTOF1B = EvioFactory.createEvioBank("FTOF1B::dgtz", 62); //numRows);
                	EvioDataBank   bankFTOF2B = EvioFactory.createEvioBank("FTOF2B::dgtz", 5);


                	// simplest method 
                	// put the 6 paddles data into panel 1A sector 0 paddles 0 to 5 
                	//                for (int lineIter=0; lineIter<numRows; lineIter++) {
                	//                	
                	//                	
                	//                	int adcLeft = Integer.parseInt(lineValues[(lineIter*4)+0]);
                	//                	int adcRight = Integer.parseInt(lineValues[(lineIter*4)+1]);
                	//                	int tdcLeft = Integer.parseInt(lineValues[(lineIter*4)+2]);
                	//                	int tdcRight = Integer.parseInt(lineValues[(lineIter*4)+3]);
                	//                	
                	//                	bankFTOF1A.setInt("sector", lineIter, 0);
                	//                	bankFTOF1A.setInt("paddle", lineIter, lineIter);
                	//                	bankFTOF1A.setInt("ADCL",   lineIter, adcLeft);
                	//                	bankFTOF1A.setInt("ADCR",   lineIter, adcRight);
                	//                	bankFTOF1A.setInt("TDCL",	lineIter, tdcLeft);
                	//                	bankFTOF1A.setInt("TDCR",   lineIter, tdcRight);
                	//                	
                	//                }

                	// method 2
                	// fill up all the paddles using my 6 paddles worth of data

                	int lineIter;
                	// panel 1A
                	for (int paddleIter=0; paddleIter<23; paddleIter++) {

                		lineIter = paddleIter%6;


                		int adcLeft = Integer.parseInt(lineValues[(lineIter*4)+0]);
                		int adcRight = Integer.parseInt(lineValues[(lineIter*4)+1]);
                		int tdcLeft = Integer.parseInt(lineValues[(lineIter*4)+2]);
                		int tdcRight = Integer.parseInt(lineValues[(lineIter*4)+3]);

                		bankFTOF1A.setInt("sector", paddleIter, sectorIter);
                		bankFTOF1A.setInt("paddle", paddleIter, paddleIter);
                		bankFTOF1A.setInt("ADCL",   paddleIter, adcLeft);
                		bankFTOF1A.setInt("ADCR",   paddleIter, adcRight);
                		bankFTOF1A.setInt("TDCL",	paddleIter, tdcLeft);
                		bankFTOF1A.setInt("TDCR",   paddleIter, tdcRight);

                	}
                	
                	// panel 1B
                	for (int paddleIter=0; paddleIter<62; paddleIter++) {

                		lineIter = paddleIter%6;


                		int adcLeft = Integer.parseInt(lineValues[(lineIter*4)+0]);
                		int adcRight = Integer.parseInt(lineValues[(lineIter*4)+1]);
                		int tdcLeft = Integer.parseInt(lineValues[(lineIter*4)+2]);
                		int tdcRight = Integer.parseInt(lineValues[(lineIter*4)+3]);

                		bankFTOF1B.setInt("sector", paddleIter, sectorIter);
                		bankFTOF1B.setInt("paddle", paddleIter, paddleIter);
                		bankFTOF1B.setInt("ADCL",   paddleIter, adcLeft);
                		bankFTOF1B.setInt("ADCR",   paddleIter, adcRight);
                		bankFTOF1B.setInt("TDCL",	paddleIter, tdcLeft);
                		bankFTOF1B.setInt("TDCR",   paddleIter, tdcRight);

                	}
                	
                	// panel 2B
                	for (int paddleIter=0; paddleIter<5; paddleIter++) {

                		lineIter = paddleIter%6;


                		int adcLeft = Integer.parseInt(lineValues[(lineIter*4)+0]);
                		int adcRight = Integer.parseInt(lineValues[(lineIter*4)+1]);
                		int tdcLeft = Integer.parseInt(lineValues[(lineIter*4)+2]);
                		int tdcRight = Integer.parseInt(lineValues[(lineIter*4)+3]);

                		bankFTOF2B.setInt("sector", paddleIter, sectorIter);
                		bankFTOF2B.setInt("paddle", paddleIter, paddleIter);
                		bankFTOF2B.setInt("ADCL",   paddleIter, adcLeft);
                		bankFTOF2B.setInt("ADCR",   paddleIter, adcRight);
                		bankFTOF2B.setInt("TDCL",	paddleIter, tdcLeft);
                		bankFTOF2B.setInt("TDCR",   paddleIter, tdcRight);

                	}


                	event.appendBanks(bankFTOF1A);
                	event.appendBanks(bankFTOF1B);
                	event.appendBanks(bankFTOF2B);
                	calib.processEvent(event);
                	

                	writer.writeEvent(event);
                }

                line = bufferedReader.readLine();
                lineNum++;
            }    
            

            bufferedReader.close();            
        }
		catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                fileName + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + fileName + "'");                   
            // Or we could just do this: 
            // ex.printStackTrace();
        }		
		
		calib.analyze();
		TBrowser browser = new TBrowser(calib.getDir());
		calib.getDir().write("FTOFCalibration.evio");
		
		writer.close();

	}
	
	// temporary code to generate energy and time data
	
	double tdcToTime(int index,double value){
		  double c1=0.0009811;//average value from CLAS
		  double c0=0;
		  return c0+c1*value;
		}	
	
	double adcToEnergy(int index,double value){
		  double mipLeft[]={
		    1050.02,
		    1027.19,
		    866.931,
		    928.128,
		    923.925,
		    964.359,
		    902.833,
		    877.427,
		    861.224,
		    851.656,
		    832.891,
		    1012.45,
		    983.369,
		    970.77,
		    1219.69,
		    830.52,
		    802.691,
		    795.23,
		    854.2,
		    608.616,
		    642.272,
		    974.723,
		    1392.5,
		    5670.46,
		    5990.23,
		    6573.4,
		    8015.28,
		    5148.67,
		    7432.46,
		    3986.41,
		    3767.46,
		    3522.67,
		    3847.07,
		    3932.82,
		    4082.46,
		    3755.44,
		    4793.57,
		    4562.73,
		    3812.8,
		    3395.22,
		    9820.73,
		    7432.8,
		    4425,
		    4272.28,
		    4475.65,
		    5312.64,
		    5315.76,
		    4129.4,
		    4742.06,
		    2674,
		    3060.27,
		    9476.6,
		    4477.81,
		    6601.19,
		    4353.28,
		    3654.95,
		    5694.41,
		    5949.56,
		    7628.13,
		    4902.38,
		    3604.08,
		    3012.74,
		    3242.17,
		    6932.08,
		    6529.5,
		    7031.3,
		    3705.3,
		    8162.33,
		    3472.42,
		    7604.71,
		    3808.29,
		    4587.19,
		    6637.47,
		    4908.16,
		    4503.47,
		    5148.73,
		    5157.63,
		    6437.7,
		    6715.97,
		    5781.03,
		    5197.86,
		    6405.39,
		    6478.03,
		    4845.13,
		    5367.12};
		  double mipRight[]={
		    1068.33,
		    1058.25,
		    1030.87,
		    957.097,
		    955.719,
		    1147.97,
		    932.314,
		    919.977,
		    934.816,
		    1031.34,
		    1044.24,
		    1003.75,
		    1051.7,
		    1067.82,
		    1106.01,
		    1090.91,
		    1174.11,
		    1063.76,
		    1062.11,
		    1307.56,
		    1124.84,
		    917.307,
		    1535.53,
		    7330.67,
		    5325.28,
		    6700.57,
		    2560.6,
		    3491.19,
		    3205.51,
		    4445.36,
		    7591.92,
		    8266.01,
		    4710.47,
		    5490.5,
		    3489.43,
		    4089.97,
		    4877.81,
		    4093.33,
		    5832.72,
		    7584.43,
		    11300.6,
		    6382.92,
		    8375.21,
		    4251.3,
		    3977.76,
		    2784.57,
		    3115.04,
		    4812.09,
		    7475.22,
		    5777.01,
		    2191.59,
		    8375.29,
		    6109.31,
		    7158.65,
		    4948.4,
		    7502.27,
		    7233.84,
		    10040.4,
		    9004.64,
		    8627.12,
		    7437.34,
		    5126.87,
		    3400.43,
		    5199.36,
		    9111.46,
		    12495.4,
		    8818.28,
		    11661.5,
		    4542.49,
		    8390.72,
		    4876.91,
		    7477.81,
		    4618.73,
		    5623.6,
		    6447.78,
		    4899.74,
		    5113.27,
		    6458.34,
		    5657.29,
		    8543,
		    6950.17,
		    4834.28,
		    8337.44,
		    4856.88,
		    6289.15};
		  if(index/1000==6){
		    if(index/100%10==0){//left
		      return value/mipLeft[index%100]*10;
		    }
		    else{
		      return value/mipRight[index%100]*10;
		    }
		  }
		  else{
		    if(index%100<23){//1A
		      return value/800*10;
		    }
		    else if(index%100<85){//1B
		      return value/2000*10;
		    }
		    else
		      return value/800*10;
		  }


		}	

}
