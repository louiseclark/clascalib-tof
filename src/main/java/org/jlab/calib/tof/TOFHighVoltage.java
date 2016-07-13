/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.Document;

import org.jlab.clas.detector.*;
import org.jlab.clas12.calib.CalibrationPane;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.containers.HashTable;
import org.jlab.evio.clas12.*;
import org.root.func.*;
import org.root.histogram.*;
import org.root.pad.TCanvas;
import org.root.pad.TBookCanvas;
import org.root.basic.EmbeddedCanvas;

public class TOFHighVoltage  implements IDetectorListener,IConstantsTableListener,ActionListener {

	private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
	private CalibrationPane  		calibPane = new CalibrationPane();
	private ConstantsTable   			constantsTable = null;
	private ConstantsTablePanel			constantsTablePanel;
	
	
	TreeMap<Integer,TOFH1D[]> container = new TreeMap<Integer,TOFH1D[]>();
	TreeMap<Integer,F1D[]> functions = new TreeMap<Integer,F1D[]>();
	TreeMap<Integer,Double[]> constants = new TreeMap<Integer,Double[]>();
	
	// constants for indexing the histogram and constant arrays
	// hists
	public final int GEOMEAN = 0;
	public final int LOGRATIO = 1;
	
	// consts
	public final int LR_CENTROID = 0;
	public final int LR_ERROR = 1;
	public final int CURRENT_VOLTAGE_LEFT = 2;
	public final int CURRENT_VOLTAGE_RIGHT = 3;
	public final int GEOMEAN_OVERRIDE = 4;
	public final int GEOMEAN_UNC_OVERRIDE = 5;
	public final int LOGRATIO_OVERRIDE = 6;
	public final int LOGRATIO_UNC_OVERRIDE = 7;	
	
	private final double[]		GM_HIST_MAX = {4000.0,8000.0,3000.0};
	private final int[]			GM_HIST_BINS = {200, 300, 150};
	private final double 		LR_THRESHOLD_FRACTION = 0.2;
	private final int			GM_REBIN_THRESHOLD = 50000;

    public final int[]		EXPECTED_MIP_CHANNEL = {800, 2000, 800};
    public final int		ALLOWED_MIP_DIFF = 25;
    public final double[]	ALPHA = {13.4, 4.7, 8.6};
    public final double[]	MAX_VOLTAGE = {2500.0, 2000.0, 2500.0};
    public final double		MAX_DELTA_V = 250;
    public final double		MIN_STATS = 100;
	
	public CalibrationPane getView() {
		return calibPane;
	}
    
	public void process(List<TOFPaddle> paddleList){
		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
				
				// fill Geometric Mean
				//if (paddle.isValidGeoMean()) {
					this.container.get(paddle.getDescriptor().getHashCode())[GEOMEAN].fill(paddle.geometricMean());
				//}
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
		
        this.calibPane.getCanvasPane().add(canvas);
        
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"Geometric Mean Peak","Uncertainty", "Log Ratio Centroid", "Uncertainty"});

        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);
        
        JButton buttonFit = new JButton("Adjust Fit / Override");
        buttonFit.addActionListener(this);
        
        JButton buttonViewAll = new JButton("View all");
        buttonViewAll.addActionListener(this);
        
        JButton buttonAdjust = new JButton("Adjust HV");
        buttonAdjust.addActionListener(this);
        
        JButton buttonExport = new JButton("Export plots");
        buttonExport.addActionListener(this);

        JButton buttonWrite = new JButton("Write to file");
        buttonWrite.addActionListener(this);
        
        this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonViewAll);
        this.calibPane.getBottonPane().add(buttonAdjust);
        this.calibPane.getBottonPane().add(buttonExport);
        this.calibPane.getBottonPane().add(buttonWrite);
        
		DetectorDescriptor desc = new DetectorDescriptor();
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){

					desc.setSectorLayerComponent(sector, layer, paddle);
									
					TOFH1D hists[] = {new TOFH1D("Geometric Mean Sector "+sector+" Paddle "+paddle,"Geometric Mean Sector "+sector+" Paddle "+paddle, 
											GM_HIST_BINS[layer_index], 0.0, GM_HIST_MAX[layer_index]),
								   new TOFH1D("Log Ratio Sector "+sector+" Paddle "+paddle,"Log Ratio Sector "+sector+" Paddle "+paddle, 75,-6.0,6.0)};
					container.put(desc.getHashCode(), hists);
					
					// initialize the treemap of constants array
					Double[] consts = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
					constants.put(desc.getHashCode(), consts);
				}
			}
		}
	}
		
	public void initDisplay(){
		
        // Display sector 1 initially
        drawComponent(2, 1, 1, canvas);
        
        // Until I can delete rows from the table will just add all sectors
        for (int sector=1; sector<=6; sector++) {
        	for (int layer=1; layer<=3; layer++) {
        		fillTable(sector, layer, constantsTable);
        	}
        }
	}
	
    public void update(DetectorShape2D dsd) {
    	// check any constraints

    	if (dsd.getDescriptor().getComponent()==0) return;
    	
    	double mipChannel = getMipChannel(dsd.getDescriptor().getSector(), 
				   dsd.getDescriptor().getLayer(), 
				   dsd.getDescriptor().getComponent());
    	int layer_index = dsd.getDescriptor().getLayer()-1;
    	double expectedMipChannel = EXPECTED_MIP_CHANNEL[layer_index];
    	
        if (mipChannel < expectedMipChannel - ALLOWED_MIP_DIFF ||
        	mipChannel > expectedMipChannel + ALLOWED_MIP_DIFF) {
        	
        	dsd.setColor(255, 153, 51); // amber
        	
        }
        else if (dsd.getDescriptor().getComponent()%2==0) {
            dsd.setColor(180, 255,180);
        } else {
            dsd.setColor(180, 180, 255);
        }
    	
    }
    
    public void entrySelected(int sector, int layer, int paddle) {

    	drawComponent(sector, layer, paddle, canvas);
        
    }

    public void actionPerformed(ActionEvent e) {
        
    	if(e.getActionCommand().compareTo("Adjust Fit / Override")==0){
        	
        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();
        	int paddle = constantsTablePanel.getSelected().getComponent();
	    	
        	customFit(sector, layer, paddle);
	    	
	    	constantsTable.getEntry(sector, layer, paddle).setData(0, 
	    			Math.round(getMipChannel(sector, layer, paddle)));
	    	constantsTable.getEntry(sector, layer, paddle).setData(1, 
	    			toDouble(new DecimalFormat("0.0").format(getMipChannelUnc(sector, layer, paddle))));
	    	constantsTable.getEntry(sector, layer, paddle).setData(2, 
					toDouble(new DecimalFormat("0.000").format(getLogRatio(sector, layer, paddle))));
	    	constantsTable.getEntry(sector, layer, paddle).setData(3, 
					toDouble(new DecimalFormat("0.000").format(getLogRatioUnc(sector, layer, paddle))));
	    	
			//constantsTable.fireTableDataChanged();
			
			drawComponent(sector, layer, paddle, canvas);
			calibPane.repaint();
			
        }
        else if (e.getActionCommand().compareTo("View all")==0){

        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();

        	JFrame viewAllFrame = new JFrame();
        	viewAllFrame.add(showFits(sector, layer));
        	viewAllFrame.pack();
        	viewAllFrame.setVisible(true);
        	viewAllFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        	
        	viewFits(sector, layer, GEOMEAN);
        	viewFits(sector, layer, LOGRATIO);
        	
        }
        else if(e.getActionCommand().compareTo("Adjust HV")==0){
        	
        	JFrame hvFrame = new JFrame("Adjust HV");
        	hvFrame.add(new TOFHVAdjustPanel(this));
        	hvFrame.pack();
        	hvFrame.setVisible(true);
        	hvFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        	
        }
        else if (e.getActionCommand().compareTo("Export plots")==0){

        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();
        	
        }
        else if (e.getActionCommand().compareTo("Write to file")==0) {
        	
        	String outputFileName = writeTable(constantsTable);
			JOptionPane.showMessageDialog(new JPanel(),"Calibration values written to "+outputFileName);
        }
    }


	public void analyze(){
		for(int sector = 1; sector <= 6; sector++){
		//for(int sector = 2; sector <= 2; sector++){
			for (int layer = 1; layer <= 3; layer++) {
			//for (int layer = 1; layer <= 2; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
					fitGeoMean(sector, layer, paddle, 0.0, 0.0);
					fitLogRatio(sector, layer, paddle, 0.0, 0.0);
				}
			}
		}

	}
	
    /**
     * This method comes from detector listener interface.
     * @param dd 
     */
    public void detectorSelected(DetectorDescriptor dd) {
        
        int sector = dd.getSector();
        int layer =  dd.getLayer();
        int paddle = dd.getComponent();

        drawComponent(sector, layer, paddle, canvas);
        
        // If the sector or layer has changed then redraw the table
//        if (sector != Integer.parseInt((String)constantsTable.getValueAt(0, 1)) ||
//        	layer != Integer.parseInt((String)constantsTable.getValueAt(0, 2))) {
//        	System.out.println("Refilling table with sector " + sector + " layer " + layer );
//        	hv.fillTable(sector, layer, constantsTable);
//        	constantsTable.fireTableDataChanged();
//        }
        
    }
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		int numHists = this.getH1D(sector, layer, paddle).length;
		canvas.divide(numHists, 1);
		
		for (int i=0; i<numHists; i++) {			

			canvas.cd(i);

			Font font = new Font("Verdana", Font.PLAIN, 7);		
			canvas.getPad().setFont(font);
			
			H1D hist = getH1D(sector, layer, paddle)[i];
			hist.setLineColor(1);
			
			canvas.draw(hist,"");
			canvas.draw(this.getF1D(sector, layer, paddle)[i],"same");
		}
		
	}

	public void fitGeoMean(int sector, int layer, int paddle,
			double minRange, double maxRange){

		int layer_index = layer-1;

		TOFH1D h = this.getH1D(sector, layer, paddle)[GEOMEAN];

		// First rebin depending on number of entries
		int nEntries = h.getEntries(); 
		if ((nEntries != 0) && (h.getAxis().getNBins() == GM_HIST_BINS[layer_index])) {
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
		
		// adjust the range now that the max has been found
		// unless it's been set to custom value
		if (minRange == 0.0) {
			startChannelForFit = maxPos*0.5;
		}
		if (maxRange == 0.0) {
			endChannelForFit = maxPos+GM_HIST_MAX[layer_index]*0.4;
			if (endChannelForFit > 0.9*GM_HIST_MAX[layer_index]) {
				endChannelForFit = 0.9*GM_HIST_MAX[layer_index];
			}	
		}
		
		F1D gmFunc = new F1D("landau+exp",startChannelForFit, endChannelForFit);
		
		gmFunc.setParameter(0, maxCounts*0.5);
		gmFunc.setParameter(1, maxPos);
		gmFunc.setParameter(2, 200.0);
		gmFunc.setParLimits(2, 0.0,400.0);
		gmFunc.setParameter(3, maxCounts*0.5);
		gmFunc.setParameter(4, -0.001);
		
		try {	
			h.fit(gmFunc, "RNQ");	
		} catch (Exception e) {
			System.out.println("Fit error with sector "+sector+" layer "+layer+" paddle "+paddle);
			e.printStackTrace();
		}
		
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
		
		double sum =0.0;
		double sumWeight =0.0;
		double sumSquare =0.0;
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

		double logRatioMean = 0.0;
		double logRatioError = 0.0;

		if (sum>0) {
			logRatioMean=sumWeight/sum;
			logRatioError=(1/Math.sqrt(sum))*Math.sqrt((sumSquare/sum)-(logRatioMean*logRatioMean));
		}
		else {
			logRatioMean=0.0;
			logRatioError=0.0;
		}
		
//		if (sector==2&&paddle<6) {
//			System.out.println("Paddle "+paddle);
//			System.out.println("sum = "+sum);
//			System.out.println("sumWeight = "+sumWeight);
//			System.out.println("sumSquare = "+sumSquare);
//			System.out.println("logRatioMean = "+logRatioMean);
//			System.out.println("sumSquare/sum = "+(sumSquare/sum));
//			System.out.println("mean^2 = "+(logRatioMean*logRatioMean));
//			System.out.println("error = "+logRatioError);
//			
//		}
		
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
		
		// put the constants in the treemap
		Double[] consts = getConst(sector, layer, paddle);
		consts[LR_CENTROID] = logRatioMean;
		consts[LR_ERROR] = logRatioError;
		constants.put(desc.getHashCode(), consts);

	}
	
	public double newHV(int sector, int layer, int paddle, double origVoltage, String pmt) {
		
		int layer_index = layer-1;
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		
		double gainIn = getF1D(sector, layer, paddle)[GEOMEAN].getParameter(1);
		double centroid = getConst(sector, layer, paddle)[LR_CENTROID];
		
		double gainLR = 0.0;
		if (pmt == "LEFT") {
			gainLR = gainIn / (Math.sqrt(Math.exp(centroid)));
			
			// put the constants in the treemap
			Double[] consts = getConst(sector, layer, paddle);
			consts[CURRENT_VOLTAGE_LEFT] = origVoltage;
			constants.put(desc.getHashCode(), consts);
		}
		else {
			gainLR = gainIn * (Math.sqrt(Math.exp(centroid)));

			// put the constants in the treemap
			Double[] consts = getConst(sector, layer, paddle);
			consts[CURRENT_VOLTAGE_RIGHT] = origVoltage;
			constants.put(desc.getHashCode(), consts);
		}
		
		double deltaGain = EXPECTED_MIP_CHANNEL[layer_index] - gainLR;

		System.out.println(sector+" "+layer+" "+paddle+" "+pmt);

		// Stop changing voltage if close enough
		if (Math.abs(deltaGain) < ALLOWED_MIP_DIFF) {
			System.out.println("deltaGain within allowed channel diff");
			deltaGain = 0;
		}
		double deltaV = (origVoltage * deltaGain) / (gainLR * ALPHA[layer_index]);
		
		// Safety check - don't exceed maximum voltage change
		if (deltaV > MAX_DELTA_V) {
			System.out.println("Max deltaV exceeded");

			deltaV = MAX_DELTA_V;
		} else if (deltaV < -MAX_DELTA_V) {
			System.out.println("Max deltaV exceeded");
			deltaV = -MAX_DELTA_V;
		}
		
		// Don't change voltage if stats are low
		if (getH1D(sector, layer, paddle)[GEOMEAN].getEntries() < MIN_STATS) {
			System.out.println("Low stats, deltaV set to zero");
			deltaV = 0.0;
		};
		
		double newVoltage = origVoltage + deltaV;
		
		// Safety check - don't exceed maximum voltage
		if (newVoltage > MAX_VOLTAGE[layer_index]) {
			System.out.println("Max V exceeded");
			newVoltage = MAX_VOLTAGE[layer_index];
		} else if (newVoltage < -MAX_VOLTAGE[layer_index]) {
			System.out.println("Max V exceeded");
			newVoltage = -MAX_VOLTAGE[layer_index];
		}			

		System.out.println("origVoltage = "+origVoltage);
		System.out.println("gainIn = "+gainIn);
		System.out.println("centroid = "+centroid);
		System.out.println("gainLR = "+gainLR);
		System.out.println("deltaGain = "+deltaGain);
		System.out.println("deltaV = "+deltaV);
		System.out.println("return = "+newVoltage);		
		
		return newVoltage;
		
	}
	
	private double toDouble(String stringVal) {
		
		double doubleVal;
		try {
			doubleVal = Double.parseDouble(stringVal);
		}
		catch (NumberFormatException e) {
			doubleVal = 0.0;
		}
		return doubleVal;
	}
	
	public void customFit(int sector, int layer, int paddle){

		String[] fields = {"Min range for geometric mean fit:", "Max range for geometric mean fit:", "SPACE",
						   "Override MIP channel:", "Override MIP channel uncertainty:","SPACE",
						   "Override Log ratio:", "Override Log ratio uncertainty:"};
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = toDouble(panel.textFields[0].getText());
			double maxRange = toDouble(panel.textFields[1].getText());
			double overrideGM = toDouble(panel.textFields[2].getText());
			double overrideGMUnc = toDouble(panel.textFields[3].getText());			
			double overrideLR = toDouble(panel.textFields[4].getText());
			double overrideLRUnc = toDouble(panel.textFields[5].getText());			
			
			
			// put the constants in the treemap
			Double[] consts = getConst(sector, layer, paddle);
			consts[GEOMEAN_OVERRIDE] = overrideGM;
			consts[GEOMEAN_UNC_OVERRIDE] = overrideGMUnc;
			consts[LOGRATIO_OVERRIDE] = overrideLR;
			consts[LOGRATIO_UNC_OVERRIDE] = overrideLRUnc;

			DetectorDescriptor desc = new DetectorDescriptor();
			desc.setSectorLayerComponent(sector, layer, paddle);
			constants.put(desc.getHashCode(), consts);

			fitGeoMean(sector, layer, paddle, minRange, maxRange);

		}	 
	}
	
	public TOFH1D[] getH1D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public F1D[] getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public Double[] getConst(int sector, int layer, int paddle){
		return this.constants.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}	
	
	public double getMipChannel(int sector, int layer, int paddle) {
		
		double mipChannel = 0.0;
		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(sector, layer, paddle))[GEOMEAN_OVERRIDE];
		
		if (overrideVal != 0.0) {
			mipChannel = overrideVal;
		}
		else {
			F1D f = getF1D(sector, layer, paddle)[GEOMEAN];
			mipChannel = f.getParameter(1);
		}
				
		return mipChannel;
	}
	
	public double getMipChannelUnc(int sector, int layer, int paddle) {
		
		double mipChannelUnc = 0.0;
		// has the value been overridden?
		double overrideUnc = constants.get(DetectorDescriptor.generateHashCode(sector, layer, paddle))[GEOMEAN_UNC_OVERRIDE];
		
		if (overrideUnc != 0.0) {
			mipChannelUnc = overrideUnc;
		}
		else {
			F1D f = getF1D(sector, layer, paddle)[GEOMEAN];
			mipChannelUnc = f.parameter(1).error();
			if (Double.isInfinite(mipChannelUnc)){
				mipChannelUnc = 9999.0;
			}
		}
				
		return mipChannelUnc;
	}	
	
	public double getLogRatio(int sector, int layer, int paddle) {
		
		double logRatio = 0.0;
		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(sector, layer, paddle))[LOGRATIO_OVERRIDE];
		
		if (overrideVal != 0.0) {
			logRatio = overrideVal;
		}
		else {
			logRatio = getConst(sector, layer, paddle)[LR_CENTROID];
		}
				
		return logRatio;

	}
	
	public double getLogRatioUnc(int sector, int layer, int paddle) {
		
		double logRatioUnc = 0.0;
		// has the value been overridden?
		double overrideUnc = constants.get(DetectorDescriptor.generateHashCode(sector, layer, paddle))[LOGRATIO_UNC_OVERRIDE];
		
		if (overrideUnc != 0.0) {
			logRatioUnc = overrideUnc;
		}
		else {
			logRatioUnc = getConst(sector, layer, paddle)[LR_ERROR];
		}
				
		return logRatioUnc;
	}	
	
	public void fillTable(int sector, int layer, final ConstantsTable table) {
		
		
		int layer_index = layer-1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++) {
			
			table.addEntry(sector, layer, paddle);
			table.getEntry(sector, layer, paddle).setData(0, 
					toDouble(new DecimalFormat("0.0").format(getMipChannel(sector, layer, paddle))));
			table.getEntry(sector, layer, paddle).setData(1, 
					toDouble(new DecimalFormat("0.0").format(getMipChannelUnc(sector, layer, paddle))));
			table.getEntry(sector, layer, paddle).setData(2, 
					toDouble(new DecimalFormat("0.000").format(getLogRatio(sector, layer, paddle))));
			table.getEntry(sector, layer, paddle).setData(3, 
					toDouble(new DecimalFormat("0.000").format(getLogRatioUnc(sector, layer, paddle))));

		}
	}
	
	public String writeTable(ConstantsTable table) {
		
		String outputFileName = nextFileName();
		
		try { 
			
			// Open the output file
			File outputFile = new File(outputFileName);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);
			
			
			for (int i=0; i< table.getRowCount(); i++) {
                
				for (int j=0; j<table.getColumnCount(); j++) {
					outputBw.write(table.getValueAt(i,j)+" ");
				}
				outputBw.newLine();
			}
		
    		outputBw.close();
        }
        catch(IOException ex) {
            ex.printStackTrace();
		}
		
		return outputFileName;
		
	}
	
	public String nextFileName() {
		
		// Get the next file name
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String todayString = dateFormat.format(today);
		String filePrefix = "FTOF_CALIB_HV_"+todayString;
		int newFileNum = 0;

		File dir = new File(".");
		File[] filesList = dir.listFiles();

		for (File file : filesList) {
			if (file.isFile()) {
				String fileName = file.getName();
				if (fileName.matches(filePrefix+"[.]\\d+[.]txt")) {
					String fileNumString = fileName.substring(fileName.indexOf('.')+1,fileName.lastIndexOf('.'));
					int fileNum = Integer.parseInt(fileNumString);
					if (fileNum >= newFileNum) newFileNum = fileNum+1;

				}
			}
		}

		return filePrefix+"."+newFileNum+".txt";
	}
	
	public TBookCanvas showFits(int sector, int layer) {
		
		int layer_index = layer-1;
		TBookCanvas		book = new TBookCanvas(2,2);
		
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
			book.add(getH1D(sector, layer, paddle)[GEOMEAN], "");
			book.add(getF1D(sector, layer, paddle)[GEOMEAN], "same");
			
		}
		return book;
	}
	
	public void viewFits(int sector, int layer, int plotType) {
		
		// Open up canvases to show all the fits
		String[] PANEL_NAME = {"FTOF 1A", "FTOF 1B", "FTOF 2"};
		
		int layer_index = layer -1;
		TCanvas[] fitCanvases;
		fitCanvases = new TCanvas[3];
		fitCanvases[0] = new TCanvas("All fits for "+PANEL_NAME[layer_index]+" sector "+sector,"All fits for "+PANEL_NAME[layer_index]+" sector "+sector,1200,800,6,4);
		fitCanvases[0].setFontSize(7);
		fitCanvases[0].setDefaultCloseOperation(fitCanvases[0].HIDE_ON_CLOSE);
		
		int canvasNum = 0;
		int padNum = 0;
		
		for (int paddleNum=1; paddleNum <= TOFCalibration.NUM_PADDLES[layer_index]; paddleNum++) {
			
			H1D fitHist = getH1D(sector, layer, paddleNum)[plotType];
						
			fitCanvases[canvasNum].cd(padNum);
			fitHist.setLineColor(2);
			fitHist.setTitle("Paddle "+paddleNum);
			fitCanvases[canvasNum].draw(fitHist);
			
			F1D fitFunc = getF1D(sector, layer, paddleNum)[plotType];
			fitCanvases[canvasNum].draw(fitFunc, "same");
			
    		padNum = padNum+1;
    		
    		if ((paddleNum)%24 == 0) {
    			// new canvas
    			canvasNum = canvasNum+1;
    			padNum = 0;
    			
	    		fitCanvases[canvasNum] = new TCanvas("All fits for "+PANEL_NAME[layer_index]+" sector "+sector,"All fits for "+PANEL_NAME[layer_index]+" sector "+sector,1200,800,6,4);
	    		fitCanvases[canvasNum].setFontSize(7);
	    		fitCanvases[canvasNum].setDefaultCloseOperation(fitCanvases[canvasNum].HIDE_ON_CLOSE);

    		}
			
		}
				
	}	
	
	public void show(){
		for(Map.Entry<Integer,TOFH1D[]> item : this.container.entrySet()){
			System.out.println(item.getKey() + "  -->  " + item.getValue()[GEOMEAN].getMean());
		}
	}
	
}
