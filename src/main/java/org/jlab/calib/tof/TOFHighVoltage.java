/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.awt.EventQueue;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import org.jlab.clas.detector.*;
import org.jlab.evio.clas12.*;
import org.root.func.*;
import org.root.histogram.*;
import org.root.pad.*;


/**
 *
 * @author gavalian
 */
public class TOFHighVoltage {

	TreeMap<Integer,TOFH1D[]> container = new TreeMap<Integer,TOFH1D[]>();
	TreeMap<Integer,F1D[]> functions = new TreeMap<Integer,F1D[]>();
	
	private final int GEOMEAN = 0;
	private final int LOGRATIO = 1;
	
	private final double[]		GM_HIST_MAX = {5000.0,15000.0,3000.0};
	private final int[]			GM_HIST_BINS = {200, 300, 150};
	private final double 		LR_THRESHOLD_FRACTION = 0.2;
	private final int			GM_REBIN_THRESHOLD = 50000;
	
		
	
	public void processEvent(EvioDataEvent event){
		List<TOFPaddle> list = DataProvider.getPaddleList(event);        
		this.process(list);
	}

	public void process(List<TOFPaddle> paddleList){
		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
				
				//System.out.println(paddle.toString());
				
				// fill Geometric Mean
				this.container.get(paddle.getDescriptor().getHashCode())[GEOMEAN].fill(paddle.geometricMean());
				
				// fill Log Ratio
				if (paddle.isValidLogRatio()) {
					this.container.get(paddle.getDescriptor().getHashCode())[LOGRATIO].fill(paddle.logRatio());
				}
				
			} else {
				System.out.println("Cant find : " + paddle.getDescriptor().toString() );
			}
		}
	}
	
	public void init(){
		DetectorDescriptor desc = new DetectorDescriptor();
		for(int sector = 0; sector < 6; sector++){
			for (int layer = 0; layer < 3; layer++) {
				for(int paddle = 0; paddle < TOFCalibration.NUM_PADDLES[layer]; paddle++){

					desc.setSectorLayerComponent(sector, layer, paddle);
					TOFH1D hists[] = {new TOFH1D("Geometric Mean Paddle "+paddle,"Geometric Mean Paddle "+paddle, 
											GM_HIST_BINS[layer], 0.0, GM_HIST_MAX[layer]),
								   new TOFH1D("Log Ratio Paddle "+paddle,"Log Ratio Paddle "+paddle, 75,-3.0,3.0)};
					container.put(desc.getHashCode(), hists);
				}
			}
		}
	}

	public void analyze(){
		for(int sector = 0; sector < 6; sector++){
			for (int layer = 0; layer < 3; layer++) {
				for(int paddle = 0; paddle < TOFCalibration.NUM_PADDLES[layer]; paddle++){
					fitGeoMean(sector, layer, paddle, 0.0, 0.0);
					fitLogRatio(sector, layer, paddle, 0.0, 0.0);
				}
			}
		}
	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		int numHists = this.getH1D(sector, layer, paddle).length;
		canvas.divide(numHists, 1);
		
		for (int i=0; i<numHists; i++) {
			canvas.cd(i);
			canvas.draw(this.getH1D(sector, layer, paddle)[i],"");
			canvas.draw(this.getF1D(sector, layer, paddle)[i],"same");
		}
		
	}

	public void fitGeoMean(int sector, int layer, int paddle,
			double minRange, double maxRange){

		TOFH1D h = this.getH1D(sector, layer, paddle)[GEOMEAN];

		// First rebin depending on number of entries
		int nEntries = h.getEntries(); 
		if ((nEntries != 0) && (h.getAxis().getNBins() == GM_HIST_BINS[layer])) {
		//   not empty      &&   hasn't already been rebinned
			int nRebin=(int) (GM_REBIN_THRESHOLD/nEntries);            
			if (nRebin>5) {
				nRebin=5;               
			}

			if(nRebin>0) {
				h.rebin(nRebin);
			}		
		}		
		// Work out the range for the fit
		double maxChannel = h.getAxis().getBinCenter(h.getAxis().getNBins()-1);
		double startChannelForFit = 0.0;
		double endChannelForFit = 0.0;
		if (minRange==0.0) {
			// default value
			startChannelForFit = maxChannel * 0.1;
		}
		else {
			// custom value
			startChannelForFit = minRange;
		}
		if (maxRange==0.0) {
			//default value
			endChannelForFit = maxChannel * 0.9;
		}
		else {
			// custom value
			endChannelForFit = maxRange;
		}

		// find the maximum bin after the start channel for the fit
		int startBinForFit = h.getxAxis().getBin(startChannelForFit);
		int endBinForFit = h.getxAxis().getBin(endChannelForFit);

		double maxCounts = 0;
		int maxBin = 0;
		for (int i=startBinForFit; i<=endBinForFit; i++) {
			if (h.getBinContent(i) > maxCounts) {
				maxBin = i;
				maxCounts = h.getBinContent(i);
			};
		}

		double maxPos = h.getAxis().getBinCenter(maxBin);
		F1D gmFunc = new F1D("landau+exp",startChannelForFit, endChannelForFit);

		gmFunc.setParameter(0, maxCounts);
		gmFunc.setParameter(1, maxPos);
		gmFunc.setParameter(2, 100.0);
		gmFunc.setParLimits(2, 0.0,400.0);
		gmFunc.setParameter(3, 20.0);
		gmFunc.setParameter(4, 0.0);
		h.fit(gmFunc);		
		
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		
		// get the log ratio function so can update the functions treemap
		F1D lrFunc;
		try {
			lrFunc = functions.get(desc.getHashCode())[LOGRATIO];
		} catch (NullPointerException e) {
			lrFunc = null;
		}
		
		F1D[] funcs = {gmFunc,lrFunc};
		functions.put(desc.getHashCode(), funcs);

	}
	
	public void fitLogRatio(int sector, int layer, int paddle,
			double minRange, double maxRange){

		H1D h = this.getH1D(sector, layer, paddle)[LOGRATIO];		

		// calculate the mean value using portion of the histogram where counts are > 0.2 * max counts
		
		double sum =0;
		double sumWeight =0;
		double sumSquare =0;
		int maxBin = h.getMaximumBin();
		double maxCounts = h.getBinContent(maxBin);
		int nBins = h.getAxis().getNBins();
		int lowThresholdBin = 0;
		int highThresholdBin = nBins-1;
		
		// find the bin left of max bin where counts drop to 0.2 * max
		for (int i=maxBin; i>0; i--) {

			if (h.getBinContent(i) < LR_THRESHOLD_FRACTION*maxCounts) {
				lowThresholdBin = i;
				break;
			}
		}

		// find the bin right of max bin where counts drop to 0.2 * max
		for (int i=maxBin; i<nBins; i++) {

			if (h.getBinContent(i) < LR_THRESHOLD_FRACTION*maxCounts) {
				highThresholdBin = i;
				break;
			}
		}

		// include the values in the sum if we're within the thresholds
		for (int i=lowThresholdBin; i<=highThresholdBin; i++) {

			double value=h.getBinContent(i);
			double middle=h.getAxis().getBinCenter(i);

			sum+=value;
			sumWeight+=value*middle;
			sumSquare+=value*middle*middle;
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
		
		// store the function showing the width over which mean is calculated
		F1D lrFunc = new F1D("p1",h.getAxis().getBinCenter(lowThresholdBin), 
				h.getAxis().getBinCenter(highThresholdBin));
		lrFunc.setParameter(1, 0.0);
		lrFunc.setParameter(0, LR_THRESHOLD_FRACTION*maxCounts); // height to draw line at
		

		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		
		// get the geometric mean function so can update the functions treemap
		F1D gmFunc;
		try {
			gmFunc = functions.get(desc.getHashCode())[GEOMEAN];
		} catch (NullPointerException e) {
			gmFunc = null;
		}		
		
		F1D[] funcs = {gmFunc,lrFunc};
		functions.put(desc.getHashCode(), funcs);

	}	

	public void customFit(int sector, int layer, int paddle){

		H1D h = getH1D(sector, layer, paddle)[GEOMEAN];
		F1D f = getF1D(sector, layer, paddle)[GEOMEAN];        
		
		TOFCustomFitPanel panel = new TOFCustomFitPanel();

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = Double.parseDouble(panel.minRange.getText());
			double maxRange = Double.parseDouble(panel.maxRange.getText());

			fitGeoMean(sector, layer, paddle, minRange, maxRange);

		}	 
	}

	public TOFH1D[] getH1D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public F1D[] getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	
	public void fillTable(int sector, int layer, ConstantsTable table) {
		
		for (int paddle=0; paddle<TOFCalibration.NUM_PADDLES[layer]; paddle++) {
			F1D f = getF1D(sector, layer, paddle)[GEOMEAN];
			TOFH1D h = getH1D(sector, layer, paddle)[LOGRATIO];
			table.addEntry(sector, layer, paddle);
			table.getEntry(sector, layer, paddle).setData(0, Double.parseDouble(new DecimalFormat("0").format(f.getParameter(1))));
			table.getEntry(sector, layer, paddle).setData(1, Double.parseDouble(new DecimalFormat("0.000").format(h.getMean())));
		}
	}

	
	public TBookCanvas showFits(int sector, int layer) {
		TBookCanvas		book = new TBookCanvas(2,2);
		
		for (int paddle=0; paddle<TOFCalibration.NUM_PADDLES[layer]; paddle++){
			book.add(getH1D(sector, layer, paddle)[GEOMEAN], "");
			book.add(getF1D(sector, layer, paddle)[GEOMEAN], "same");
			
		}
		return book;
	}
	
	public void show(){
		for(Map.Entry<Integer,TOFH1D[]> item : this.container.entrySet()){
			System.out.println(item.getKey() + "  -->  " + item.getValue()[GEOMEAN].getMean());
		}
	}

}
