package org.jlab.rec.ftof;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.root.func.F1D;
import org.root.group.TBrowser;
import org.root.group.TDirectory;
import org.root.histogram.H1D;
import org.root.pad.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FTOFCalibrationVersion1 {
	
	public static void execute() {
	
		String fileName = "/home/louise/orig_FTOF_calibration/cpp/trunk/data/usc_first_configuration.txt";
		String line = null;
		String paddleKey;
        
        HashMap<String, FTOFPaddleVersion1> paddles = new HashMap<String, FTOFPaddleVersion1>();
        
		try {
            // Open the file
            FileReader fileReader = 
                new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.s
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);
            
            // Read each line of the file up to a maximum count
            int maxLines=200000;
            int lineNum=0;
            line = bufferedReader.readLine();
            //while((line = bufferedReader.readLine()) != null) {
            while ((maxLines==0 || lineNum<maxLines) && (line != null)) {
                
                // Each line contains Adc L, Adc R, Tdc L, Tdc R x 6
                String[] lineValues;
                lineValues = line.split(" ");
                
                // 
                for (int lineIter=0; lineIter<6; lineIter++) {
                	
                	// Pretend everything is sector 1, panel 1a, for the moment. 
                	// Line has paddles 0 to 5
                	
                	// Does the paddle exist in the hashmap?
                	
                	paddleKey = "1 1a "+lineIter;
                	FTOFPaddleVersion1 currentPaddle = null;
                	
                	if (paddles.containsKey(paddleKey)) {
                		// paddle exists in hashmap
                		
                		// get the paddle
                		currentPaddle = paddles.get(paddleKey);
                		
                	} else {
                		// paddle doesn't exist in hashmap
                		
                		// create the paddle and put it in the hashmap
                		currentPaddle = new FTOFPaddleVersion1(1, "1a", lineIter);
                		paddles.put(paddleKey, currentPaddle);
                	}
             
                	// fill the geomean histogram with sqrt(adc left * adc right) for the current paddle
                	// fill the veff histogram with (tdc left / tdc right) / 2
                	double adcLeft = Double.parseDouble(lineValues[(lineIter*4)+0]);
                	double adcRight = Double.parseDouble(lineValues[(lineIter*4)+1]);
                	double tdcLeft = Double.parseDouble(lineValues[(lineIter*4)+2]);
                	double tdcRight = Double.parseDouble(lineValues[(lineIter*4)+3]);
                	
                	currentPaddle.fillHists(adcLeft, adcRight, tdcLeft, tdcRight);
                	
                	
                	
//                	System.out.println("lineNum "+lineNum);
//                	System.out.println("lineIter "+lineIter);
//                	System.out.println("paddleKey "+paddleKey);
//                	System.out.println("adcLeft "+adcLeft);
//                	System.out.println("adcRight "+adcRight);                	
                	
                }
                                
                //System.out.println(line);
                
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

		// Loop round the hashmap
		// Draw the histograms and output the calibration values to the screen
		
		TCanvas c1,c2,c3,c4;
		c1 = new TCanvas("Geometric Mean","Geometric Mean",1200,800,2,3);
		c2 = new TCanvas("Effective Velocity","Effective Velocity",1200,800,2,3);
		c3 = new TCanvas("Log Ratio","Log Ratio",1200,800,2,3); 
		
		// Try out attenuation plot
		//c4 = new TCanvas("Attenuation","Attenuation",1200,800,2,3);
		
		
		//c1.setLabelSize(18);
		
		Iterator<String> keySetIterator = paddles.keySet().iterator();
		while (keySetIterator.hasNext()) {
			String key = keySetIterator.next();
			FTOFPaddleVersion1 currentPaddle = paddles.get(key);

			// Geometric mean
			// do this later after get fitting to work
			currentPaddle.fitGain();
			

			
			
			double nEntries=currentPaddle.getEntries(currentPaddle.getGeometricMeanHist());
			//System.out.println("nEntries "+nEntries);
			
			int nRebin=(int) (50000/nEntries);           
			if (nRebin>5) {
				nRebin=5;               
			}
			
			H1D mGMHistRebin = currentPaddle.getGeometricMeanHist();
			//if(nRebin>0) {
				//mGMHistRebin = rebin(currentPaddle.getGeometricMeanHist(), nRebin);
			//}
			
			// Work out parameter values based on the maximum entries
			// Doing this for now until I know the best replacement for the TSpectrum code
			
			int maxBin = currentPaddle.getMaximumBin(currentPaddle.getGeometricMeanHist());
			double maxCounts = currentPaddle.getGeometricMeanHist().getBinContent(maxBin);
			double maxPos = currentPaddle.getGeometricMeanHist().getAxis().getBinCenter(maxBin);
			
			// the range for the fit is to be 0.5 to 1.6 * position of max
			// OR
			// if position of max is < 2000 then it's to be (0.25 * position of max) to (position of max + 1200)
			
			final double ALT_GM_FIT_LOW_FRACTION = 0.25;
			final double ALT_GM_FIT_HIGH_WIDTH = 1200;
			
			double lowFit, highFit;
			//if (maxPos < GM_FIT_ALT_CUT_OFF) {
			//	lowFit = maxPos * ALT_GM_FIT_LOW_FRACTION;
			//	highFit = maxPos + ALT_GM_FIT_HIGH_WIDTH;
			//}
			//else {
				lowFit = maxPos * 0.5;
				highFit = maxPos * 1.6;
			//}
						
			F1D func = new F1D("landau+exp",lowFit, highFit);
			//F1D func = new F1D("landau",lowFit, highFit);
			//F1D func = new F1D("landau+p1",100.0,2800.0); // Gagik's values
			
			// first draft of parameter setting
			// final method of determining parameters to be confirmed
			
			//func.setParameter(0, 800.0);
			func.setParameter(0, maxCounts);
			//func.setParameter(1, 1500.0);
			func.setParameter(1, maxPos);
			func.setParameter(2, 100.0);
			func.setParLimits(2, 0.0,400.0);
			func.setParameter(3, 20.0);
			func.setParameter(4, 0.0);
			
			// these are Gagik's values for the exp part
			// doesn't seem to recognise exp, so have changed it to p1 with free params
			//func.setParameter(3, 20.0);
			//func.setParameter(4, 0.0);
			
			// Draw the fits for checking
			
			//TCanvas c1 = new TCanvas("c1", "c1", 750,500,1,1);
			//func.show();
			//mGMHistRebin.fit(func);
			currentPaddle.getGeometricMeanHist().fit(func);
			
			//mGeometricMeanPeak = func.getParameters()[1];
			//System.out.println("GM peak position after fitting "+func.getParameters()[1]);
			
			
			//mGeometricMeanHist.setLineColor(4);
			func.show();
			
			//c1.setFontSize(14);
			//c1.cd(0);
			//c1.draw(mGeometricMeanHist);
			//c1.draw(func, "same");
			
			// Calculate and output the calibration values
			
			// Veff
			currentPaddle.fitPlateau();
			currentPaddle.calculateVeffEdges();
			
			F1D veffEdgesFunc = new F1D("p1",currentPaddle.getVeffLeftEdge(), currentPaddle.getVeffRightEdge());
			veffEdgesFunc.setParameter(1, 0.0);
			veffEdgesFunc.setParameter(0, 
					(currentPaddle.getVeffHist().getBinContent(currentPaddle.getVeffHist().getMaximumBin()))/2);
			

			
			// Log ratio
			currentPaddle.calculateLogRatio();
		
			
			System.out.println("Paddle "+key+"; Veff left edge = "+currentPaddle.getVeffLeftEdge()
					+"; Veff right edge = "+currentPaddle.getVeffRightEdge()
					+"; Veff width = "+(currentPaddle.getVeffRightEdge()-currentPaddle.getVeffLeftEdge())
					+"; Geometric Mean peak = "+currentPaddle.getGeometricMeanPeak()
					+"; Log Ratio peak = "+currentPaddle.getLogRatioPeak()
					+"; Log Ratio error = "+currentPaddle.getLogRatioError());			
			
			
			
			
			// Draw the histograms
			currentPaddle.getGeometricMeanHist().setLineColor(2);
			c1.cd(currentPaddle.getPaddleNumber());
			c1.draw(currentPaddle.getGeometricMeanHist(),"*");
			c1.draw(func, "same");
			
			currentPaddle.getVeffHist().setLineColor(2);
			c2.cd((currentPaddle.getPaddleNumber())); 
			c2.draw(currentPaddle.getVeffHist(),"*");
			c2.draw(veffEdgesFunc, "same");
			
			currentPaddle.getLogRatioHist().setLineColor(2);
			c3.cd((currentPaddle.getPaddleNumber()));
			c3.draw(currentPaddle.getLogRatioHist(),"*");
			
			//c4.cd(currentPaddle.getPaddleNumber());
			//c4.draw(currentPaddle.getAttenGraph(),"*");
			

		}
		
		// Output a sample histogram as a string
		FTOFPaddleVersion1 outputPaddle = paddles.get("1 1a 0");
		String outputString = outputPaddle.getGeometricMeanHist().toString();
		try {
			Files.write(Paths.get("geoMeanHist.txt"), outputString.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
				 
	}

	public static void main (String[] args) {
		
		FTOFCalibrationVersion1.execute();
		
		// Test that I can read in the evio file output with the directory contents
		TDirectory dirFile = new TDirectory();
		dirFile.readFile("FTOFCalibration.0.evio");
		TBrowser browser = new TBrowser(dirFile);
		
	
	}
}