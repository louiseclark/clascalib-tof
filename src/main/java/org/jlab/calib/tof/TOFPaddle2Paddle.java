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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jlab.clas.detector.*;
import org.jlab.clas12.calib.CalibrationPane;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clasrec.utils.CalibrationConstants;
import org.jlab.evio.clas12.*;
import org.root.func.*;
import org.root.histogram.*;
import org.root.pad.TCanvas;
import org.root.pad.TBookCanvas;
import org.root.basic.EmbeddedCanvas;


/**
 *
 * @author louiseclark
 */
public class TOFPaddle2Paddle   implements IDetectorListener,IConstantsTableListener,ActionListener {

	private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
	private CalibrationPane  		calibPane = new CalibrationPane();
	private ConstantsTable   			constantsTable = null;
	private ConstantsTablePanel			constantsTablePanel;
		
	TreeMap<Integer,H1D> fineHists = new TreeMap<Integer,H1D>();
	TreeMap<Integer,F1D> fineFuncs = new TreeMap<Integer,F1D>();
	TreeMap<Integer,H1D> crudeFirstHists = new TreeMap<Integer,H1D>();
	TreeMap<Integer,H1D> sectorHists = new TreeMap<Integer,H1D>();
	TreeMap<Integer,H1D> crudeLastHists = new TreeMap<Integer,H1D>();
	TreeMap<Integer,H1D> crudeFirstAgainHists = new TreeMap<Integer,H1D>();
	TreeMap<Integer,Double> offsets = new TreeMap<Integer,Double>();
		
	public final double TARGET_CENTRE = -25.0;
	public final double RF_STRUCTURE = 2.004;

	// *** using 1b so I can use same reference paddle as Haiyun while testing
	// *** change to 1a later
	public final int REF_PADDLE = 28;
	public final int REF_LAYER = 2;
	public final int NUM_FIRST_PADDLES = 10;
	
	public CalibrationPane getView() {
		return calibPane;
	}	
	
	public void init(){
		
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"offset"});

        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);

        //JButton buttonFit = new JButton("Fit");
        //buttonFit.addActionListener(this);
        
        JButton buttonViewAll = new JButton("View all");
        buttonViewAll.addActionListener(this);
                
        JButton buttonWrite = new JButton("Write to file");
        buttonWrite.addActionListener(this);
        
        //this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonViewAll);
        this.calibPane.getBottonPane().add(buttonWrite);
		
		DetectorDescriptor desc = new DetectorDescriptor();
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){

					desc.setSectorLayerComponent(sector, layer, paddle);
					
					// create hist for fine offset for every paddle
					H1D fineHist = 
						new H1D("Fine Offset: Sector "+sector+" Paddle "+paddle,"Fine Offset Sector "+sector+" Paddle "+paddle, 
							100, -2.0, 2.0);
	
					fineHists.put(desc.getHashCode(), fineHist);

					// create a dummy function in case there's no data to fit 
					F1D nodataFunc = new F1D("p0", -1.0, 1.0);
					nodataFunc.setParameter(0, 0.0);
					fineFuncs.put(desc.getHashCode(), nodataFunc);
					offsets.put(desc.getHashCode(), 0.0);
					
					// crudeFirst offset hists for first 10 paddles in each sector in reference sector
					// these will be corrected to one reference paddle
					// crudeFirstAgain hists also for first 10 paddles in each sector in reference sector
					if (layer==REF_LAYER && paddle <= NUM_FIRST_PADDLES) {
						H1D hist = 
								new H1D("Crude First Offset: Sector "+sector+" Paddle "+paddle,"Crude First Offset Sector "+sector+" Paddle "+paddle, 
										99,-49.5*RF_STRUCTURE,49.5*RF_STRUCTURE);
						hist.setXTitle("Offset (ns)");
			
						crudeFirstHists.put(desc.getHashCode(), hist);

						H1D hist2 = 
								new H1D("Crude First Again Offset: Sector "+sector+" Paddle "+paddle,"Crude First Again Offset Sector "+sector+" Paddle "+paddle, 
										99,-49.5*RF_STRUCTURE,49.5*RF_STRUCTURE);
						hist2.setXTitle("Offset (ns)");
						crudeFirstAgainHists.put(desc.getHashCode(), hist2);

					}
					
					// crudeLast offset hists for paddles other than first 10 in each sector for layer 1
					// these will be corrected to one reference paddle
					if (paddle > NUM_FIRST_PADDLES || layer != REF_LAYER) {
						H1D hist = 
								new H1D("Crude Last Offset: Sector "+sector+" Paddle "+paddle,"Crude Last Offset Sector "+sector+" Paddle "+paddle, 
										99,-49.5*RF_STRUCTURE,49.5*RF_STRUCTURE);
						hist.setXTitle("Offset (ns)");
			
						crudeLastHists.put(desc.getHashCode(), hist);
						
					}
					
				}
			}
		}
		
		// create the sector histograms
		for (int sector = 1; sector <= 6; sector++) {
			H1D hist = 
					new H1D("Sector Offset: Sector "+sector,"Sector Offset: Sector "+sector, 
							99,-49.5*RF_STRUCTURE,49.5*RF_STRUCTURE);
			hist.setXTitle("Offset (ns)");

			sectorHists.put(sector, hist);
		}
	}	

	public void initDisplay(){
		
        // Display sector 1 initially
        drawComponent(1, 1, 9, canvas);
        
        // Until I can delete rows from the table will just add all sectors
        for (int sector=1; sector<=6; sector++) {
        	for (int layer=1; layer<=3; layer++) {
        		fillTable(sector, layer, constantsTable);
        	}
        }
	}
	
    public void update(DetectorShape2D dsd) {
    	// check any constraints
    	
//    	double mipChannel = hv.getMipChannel(dsd.getDescriptor().getSector(), 
//				   dsd.getDescriptor().getLayer(), 
//				   dsd.getDescriptor().getComponent());
//    	int layer_index = dsd.getDescriptor().getLayer()-1;
//    	double expectedMipChannel = hv.EXPECTED_MIP_CHANNEL[layer_index];
//    	
//        if (mipChannel < expectedMipChannel - ALLOWED_MIP_DIFF ||
//        	mipChannel > expectedMipChannel + ALLOWED_MIP_DIFF) {
//        	
//        	dsd.setColor(255, 153, 51); // amber
//        	
//        }
//        else if (dsd.getDescriptor().getComponent()%2==0) {
//            dsd.setColor(180, 255,180);
//        } else {
//            dsd.setColor(180, 180, 255);
//        }
//    	
    }
    
    public void entrySelected(int sector, int layer, int paddle) {

    	drawComponent(sector, layer, paddle, canvas);
        
    }
    
    public void actionPerformed(ActionEvent e) {

    	if (e.getActionCommand().compareTo("View all")==0){

        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();

        	JFrame viewAllFrame = new JFrame();
        	viewAllFrame.add(showFits(sector, layer));
        	viewAllFrame.pack();
        	viewAllFrame.setVisible(true);
        	viewAllFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        	
        	viewFits(sector, layer);
        	viewFits(sector, layer);
        	
        }
        else if (e.getActionCommand().compareTo("Write to file")==0) {
        	
        	String outputFileName = writeTable(constantsTable);
			JOptionPane.showMessageDialog(new JPanel(),"Calibration values written to "+outputFileName);
        }
    }

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
    
    private Double formatDouble(double val) {
    	return Double.parseDouble(new DecimalFormat("0.000").format(val));
    }
    
    public void analyze(List<TOFPaddlePair> eventList){
    	
    	// fill the fineHists
		for(TOFPaddlePair paddlePair : eventList){
			if(this.fineHists.containsKey(paddlePair.electronPaddle.getDescriptor().getHashCode())==true){
				 
				// Fill the first set of histograms
				this.fineHists.get(paddlePair.electronPaddle.getDescriptor().getHashCode()).fill(
						(paddlePair.electronPaddle.refTime(TARGET_CENTRE) + (1000*RF_STRUCTURE) + (0.5*RF_STRUCTURE))%RF_STRUCTURE - (0.5*RF_STRUCTURE)
						// find the "fine" offset within the RF pulse width
						);
			}
			else {
				System.out.println("Cant find : " + paddlePair.electronPaddle.getDescriptor().toString() );
			}
			if(this.fineHists.containsKey(paddlePair.pionPaddle.getDescriptor().getHashCode())==true){
				 
				// Fill the first set of histograms
				this.fineHists.get(paddlePair.pionPaddle.getDescriptor().getHashCode()).fill(
						(paddlePair.pionPaddle.refTime(TARGET_CENTRE) + (1000*RF_STRUCTURE) + (0.5*RF_STRUCTURE))%RF_STRUCTURE - (0.5*RF_STRUCTURE)
						// find the "fine" offset within the RF pulse width
						);
			}
			else {
				System.out.println("Cant find : " + paddlePair.pionPaddle.getDescriptor().toString() );
			}			
		}
    	
    	// calculate the fine offsets
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
					fitFineHist(sector, layer, paddle);
				}
			}
		}
		
		// fill the crudeFirst histograms 
		for(TOFPaddlePair paddlePair : eventList){
			
			// Fill the crude first set of histograms
			// electron hit in paddle 1-10 of layer 1
			// pion hit in ref paddle of layer 1 for electron sector+4
			// find offset of paddles 1-10 using these data
			
			int eComp = paddlePair.electronPaddle.getDescriptor().getComponent();
			int piComp = paddlePair.pionPaddle.getDescriptor().getComponent();
			int eSect = paddlePair.electronPaddle.getDescriptor().getSector();
			int piSect = paddlePair.pionPaddle.getDescriptor().getSector();
			int eLayer = paddlePair.electronPaddle.getDescriptor().getLayer();
			int piLayer = paddlePair.pionPaddle.getDescriptor().getLayer();
			
			if (eLayer == REF_LAYER && eComp <= NUM_FIRST_PADDLES && piLayer == REF_LAYER && piComp == REF_PADDLE && (piSect+6 - eSect)%6 == 4) {
								
				// correct the electron and pion time with the fine offset
				double eCorrTime = paddlePair.electronPaddle.refTime(TARGET_CENTRE) - getOffset(eSect, eLayer, eComp);
				double piCorrTime = paddlePair.pionPaddle.refTime(TARGET_CENTRE) - getOffset(piSect, piLayer, piComp);
				
				this.crudeFirstHists.get(paddlePair.electronPaddle.getDescriptor().getHashCode()).fill(eCorrTime - piCorrTime);
				
			}
		}
		
    	// calculate the crudeFirst offsets
		for(int sector = 1; sector <= 6; sector++){
			for(int paddle = 1; paddle <= NUM_FIRST_PADDLES; paddle++){
				
				H1D crudeFirstHist = getH1D(sector, REF_LAYER, paddle, "CRUDE_FIRST");
				int maxBin = crudeFirstHist.getMaximumBin();
				double offset = crudeFirstHist.getXaxis().getBinCenter(maxBin);
				crudeFirstHist.setTitle(crudeFirstHist.getTitle() + " Offset = " + formatDouble(offset));
				
				double newOffset = getOffset(sector, REF_LAYER, paddle) + offset;
				
				DetectorDescriptor desc = new DetectorDescriptor();
				desc.setSectorLayerComponent(sector, REF_LAYER, paddle);
				offsets.put(desc.getHashCode(), newOffset);
				
				
			}
		}	
		
		// fill the crudeSect histograms 
		for(TOFPaddlePair paddlePair : eventList){
			
			// Sector to sector corrections
			// electron hit in paddle 1-10 of layer 1
			// pion hit in sector 1 paddle 1-10
			
			int eComp = paddlePair.electronPaddle.getDescriptor().getComponent();
			int piComp = paddlePair.pionPaddle.getDescriptor().getComponent();
			int eSect = paddlePair.electronPaddle.getDescriptor().getSector();
			int piSect = paddlePair.pionPaddle.getDescriptor().getSector();
			int eLayer = paddlePair.electronPaddle.getDescriptor().getLayer();
			int piLayer = paddlePair.pionPaddle.getDescriptor().getLayer();
			
			if (eComp <= NUM_FIRST_PADDLES && piSect == 1 && piComp <= NUM_FIRST_PADDLES) {
								
				// correct the electron and pion time with the current offset
				double eCorrTime = paddlePair.electronPaddle.refTime(TARGET_CENTRE) - getOffset(eSect, eLayer, eComp);
				double piCorrTime = paddlePair.pionPaddle.refTime(TARGET_CENTRE) - getOffset(piSect, piLayer, piComp);
				
				this.sectorHists.get(eSect).fill(eCorrTime - piCorrTime);
				
			}
		}
		
    	// calculate the crudeSect offsets and then apply them to all paddles in each sector
		for(int sector = 1; sector <= 6; sector++){
				
			H1D sectorHist = getH1D(sector, 1, 1, "SECTOR");
			int maxBin = sectorHist.getMaximumBin();
			double offset = sectorHist.getXaxis().getBinCenter(maxBin);
			
			sectorHist.setTitle(sectorHist.getTitle() + " Sector offset = " + formatDouble(offset));
			
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
					double newOffset = getOffset(sector, layer, paddle) + offset;

					DetectorDescriptor desc = new DetectorDescriptor();
					desc.setSectorLayerComponent(sector, layer, paddle);
					offsets.put(desc.getHashCode(), newOffset);
				}
			}
		}	
		
		// fill the crudeLast histograms 
		for(TOFPaddlePair paddlePair : eventList){
			
			// Fill the crude last set of histograms
			// electron hit in paddle 1-10 of layer 1
			// pion hit in any other paddle
			
			int eComp = paddlePair.electronPaddle.getDescriptor().getComponent();
			int piComp = paddlePair.pionPaddle.getDescriptor().getComponent();
			int eSect = paddlePair.electronPaddle.getDescriptor().getSector();
			int piSect = paddlePair.pionPaddle.getDescriptor().getSector();
			int eLayer = paddlePair.electronPaddle.getDescriptor().getLayer();
			int piLayer = paddlePair.pionPaddle.getDescriptor().getLayer();
			
			if (eLayer == REF_LAYER && eComp <= NUM_FIRST_PADDLES && (piLayer != REF_LAYER || piComp > NUM_FIRST_PADDLES)) {
								
				// correct the electron and pion time with the current offset
				double eCorrTime = paddlePair.electronPaddle.refTime(TARGET_CENTRE) - getOffset(eSect, eLayer, eComp);
				double piCorrTime = paddlePair.pionPaddle.refTime(TARGET_CENTRE) - getOffset(piSect, piLayer, piComp);
				
				this.crudeLastHists.get(paddlePair.pionPaddle.getDescriptor().getHashCode()).fill(piCorrTime - eCorrTime);
																							// note change in order as we are now using
																							// pion for correction
				
			}
		}
		
    	// calculate the crudeLast offsets
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){

					if (layer != REF_LAYER || paddle > NUM_FIRST_PADDLES) {
						H1D crudeLastHist = getH1D(sector, layer, paddle, "CRUDE_LAST");
						int maxBin = crudeLastHist.getMaximumBin();
						double offset = crudeLastHist.getXaxis().getBinCenter(maxBin);
						crudeLastHist.setTitle(crudeLastHist.getTitle() + " Offset = " + formatDouble(offset));

						double newOffset = getOffset(sector, layer, paddle) + offset;

						DetectorDescriptor desc = new DetectorDescriptor();
						desc.setSectorLayerComponent(sector, layer, paddle);
						offsets.put(desc.getHashCode(), newOffset);
					}
				}
			}
		}	
		
		// fill the crudeFirstAgain histograms 
		for(TOFPaddlePair paddlePair : eventList){
			
			// Fill the crude first set of histograms
			// electron hit in paddle 1-10 of layer 1
			// pion hit in ref paddle of layer 1 for electron sector+4
			// find offset of paddles 1-10 using these data
			
			int eComp = paddlePair.electronPaddle.getDescriptor().getComponent();
			int piComp = paddlePair.pionPaddle.getDescriptor().getComponent();
			int eSect = paddlePair.electronPaddle.getDescriptor().getSector();
			int piSect = paddlePair.pionPaddle.getDescriptor().getSector();
			int eLayer = paddlePair.electronPaddle.getDescriptor().getLayer();
			int piLayer = paddlePair.pionPaddle.getDescriptor().getLayer();
			
			if (eLayer == REF_LAYER && eComp <= NUM_FIRST_PADDLES && piComp > NUM_FIRST_PADDLES) {
								
				// correct the electron and pion time with the current offset
				double eCorrTime = paddlePair.electronPaddle.refTime(TARGET_CENTRE) - getOffset(eSect, eLayer, eComp);
				double piCorrTime = paddlePair.pionPaddle.refTime(TARGET_CENTRE) - getOffset(piSect, piLayer, piComp);
				
				this.crudeFirstAgainHists.get(paddlePair.electronPaddle.getDescriptor().getHashCode()).fill(eCorrTime - piCorrTime);
				
			}
		}
		
    	// calculate the crudeFirstAgain offsets
		for(int sector = 1; sector <= 6; sector++){
			for(int paddle = 1; paddle <= NUM_FIRST_PADDLES; paddle++){
				
				H1D crudeFirstAgainHist = getH1D(sector, REF_LAYER, paddle, "CRUDE_FIRST_AGAIN");
				int maxBin = crudeFirstAgainHist.getMaximumBin();
				double offset = crudeFirstAgainHist.getXaxis().getBinCenter(maxBin);
				crudeFirstAgainHist.setTitle(crudeFirstAgainHist.getTitle() + " Offset = " + formatDouble(offset));
				
				double newOffset = getOffset(sector, REF_LAYER, paddle) + offset;
				
				DetectorDescriptor desc = new DetectorDescriptor();
				desc.setSectorLayerComponent(sector, REF_LAYER, paddle);
				offsets.put(desc.getHashCode(), newOffset);
			}
		}			
    }
	
	public void fitFineHist(int sector, int layer, int paddle) {
		
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		
		H1D fineHist = this.fineHists.get(desc.getHashCode());
		int maxBin = fineHist.getMaximumBin();
		double maxPos = fineHist.getxAxis().getBinCenter(maxBin);
		
		// arrangeFine

		// if maxPos > 0.65 move bin contents of (-1,0) to (1,2)
		if (maxPos > 0.65) {
			int iBin=fineHist.getxAxis().getBin(-1.0+1.0e-10);
			int jBin=fineHist.getxAxis().getBin(1.0+1.0e-10);
			do {
				fineHist.setBinContent(jBin, fineHist.getBinContent(iBin));
				fineHist.setBinContent(iBin,0);
				iBin++;
				jBin++;
			}
			while (fineHist.getXaxis().getBinCenter(iBin) < 0);
		}

		// if maxPos < -0.65 move bin contents of (0,1) to (-2,-1)
		if (maxPos < -0.65) {
			int iBin=fineHist.getxAxis().getBin(0.0+1.0e-10);
			int jBin=fineHist.getxAxis().getBin(-2.0+1.0e-10);
			do {
				fineHist.setBinContent(jBin, fineHist.getBinContent(iBin));
				fineHist.setBinContent(iBin,0);
				iBin++;
				jBin++;
			}
			while (fineHist.getXaxis().getBinCenter(iBin) < 1);
		}

		
		// fit gaussian
		F1D fineFunc = new F1D("gaus", maxPos-0.5, maxPos+0.5);
		fineFunc.setParameter(0, fineHist.getBinContent(maxBin));
		fineFunc.setParameter(1, maxPos);
		fineFunc.setParameter(2, 0.5);
		
		try {
			fineHist.fit(fineFunc, "RN");
			fineFuncs.put(desc.getHashCode(), fineFunc);
			offsets.put(desc.getHashCode(), fineFunc.getParameter(1));
			
			fineHist.setTitle(fineHist.getTitle() + " Fine offset = " + formatDouble(fineFunc.getParameter(1)));
		}
		catch(Exception ex) {
            ex.printStackTrace();
        }	
	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		canvas.divide(3, 2);
		
		canvas.cd(0);

		Font font = new Font("Verdana", Font.PLAIN, 7);		
		canvas.getPad().setFont(font);
			
		H1D fineHist = getH1D(sector, layer, paddle, "FINE");
			
		canvas.draw(fineHist,"");
		canvas.draw(this.getFineFunc(sector, layer, paddle),"same");
		
		if (layer == REF_LAYER && paddle <=NUM_FIRST_PADDLES) {
			canvas.cd(1);
			H1D crudeFirstHist = getH1D(sector, layer, paddle, "CRUDE_FIRST");
			canvas.draw(crudeFirstHist,"");
		}
		
		canvas.cd(2);
		H1D sectorHist = getH1D(sector, 1, 1, "SECTOR");
		canvas.draw(sectorHist,"");
		
		if (layer != REF_LAYER || paddle  > NUM_FIRST_PADDLES) {
			canvas.cd(3);
			H1D crudeLastHist = getH1D(sector, layer, paddle, "CRUDE_LAST");
			canvas.draw(crudeLastHist,"");
		}
		
		if (layer == REF_LAYER && paddle <=NUM_FIRST_PADDLES) {
			canvas.cd(4);
			H1D crudeFirstAgainHist = getH1D(sector, layer, paddle, "CRUDE_FIRST_AGAIN");
			canvas.draw(crudeFirstAgainHist,"");
		}		
		
	}

	
	public void customFit(int sector, int layer, int paddle){

		H1D h = getH1D(sector, layer, paddle, "FINE");
		F1D f = getFineFunc(sector, layer, paddle);        
		
		TOFCustomFitPanel panel = new TOFCustomFitPanel();

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = Double.parseDouble(panel.minRange.getText());
			double maxRange = Double.parseDouble(panel.maxRange.getText());

			//fitGeoMean(sector, layer, paddle, minRange, maxRange);

		}	 
	}
	
	public H1D getH1D(int sector, int layer, int paddle, String histType){
		
		switch ( histType ) {
		case "FINE":
			return this.fineHists.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
		case "CRUDE_FIRST":
			return this.crudeFirstHists.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
		case "SECTOR":
			return this.sectorHists.get(sector);
		case "CRUDE_LAST":
			return this.crudeLastHists.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));			
		case "CRUDE_FIRST_AGAIN":
			return this.crudeFirstAgainHists.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));			
		default:
			return this.fineHists.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
		}
	}
	
	public F1D getFineFunc(int sector, int layer, int paddle){
		return this.fineFuncs.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}

	public Double getOffset(int sector, int layer, int paddle) {
		
		return this.offsets.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
			
	public void fillTable(int sector, int layer, final ConstantsTable table) {
		
		int layer_index = layer-1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++) {
			
			table.addEntry(sector, layer, paddle);
			
	    	table.getEntry(sector, layer, paddle).setData(0, 
	    			Double.parseDouble(new DecimalFormat("0.000").format(this.getOffset(sector, layer, paddle))));
			
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
		String filePrefix = "FTOF_CALIB_P2P_"+todayString;
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
			book.add(getH1D(sector, layer, paddle, "FINE"), "");
			book.add(getFineFunc(sector, layer, paddle), "same");
			
		}
		return book;
	}
	
	public void viewFits(int sector, int layer) {
		
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
			
			H1D fitHist = getH1D(sector, layer, paddleNum, "FINE");
						
			fitCanvases[canvasNum].cd(padNum);
			//fitHist.setLineColor(2);
			fitHist.setTitle("Paddle "+paddleNum);
			fitCanvases[canvasNum].draw(fitHist);
			
			F1D fitFunc = getFineFunc(sector, layer, paddleNum);
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
	
//	public void show(){
//		for(Map.Entry<Integer,H2D[]> item : this.container.entrySet()){
//			System.out.println(item.getKey() + "  -->  " + item.getValue()[GEOMEAN].getMean());
//		}
//	}

}

