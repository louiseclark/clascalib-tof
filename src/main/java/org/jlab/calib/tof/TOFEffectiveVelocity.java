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
public class TOFEffectiveVelocity   implements IDetectorListener,IConstantsTableListener,ActionListener {

	private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
	private CalibrationPane  		calibPane = new CalibrationPane();
	private ConstantsTable   			constantsTable = null;
	private ConstantsTablePanel			constantsTablePanel;
		
	TreeMap<Integer,H2D> container = new TreeMap<Integer,H2D>();
	TreeMap<Integer,F1D> functions = new TreeMap<Integer,F1D>();
	TreeMap<Integer, Double[]> constants = new TreeMap<Integer, Double[]>();

	// constants for indexing the constants array
	public final int VEFF_OVERRIDE = 0;
	public final int VEFF_UNC_OVERRIDE = 1;

	
	public CalibrationPane getView() {
		return calibPane;
	}	
	
	public void process(List<TOFPaddle> paddleList){
		

		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){

				// fill (Time Left - Time Right / 2) vs position
				if (paddle.includeInTimeWalk()) {
					this.container.get(paddle.getDescriptor().getHashCode()).fill(paddle.YPOS, paddle.halfTimeDiff());
				}

			} else {
				System.out.println("Cant find : " + paddle.getDescriptor().toString() );
			}
		}
		
	}
	
	public void init(){
		
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"veff","veff_err"});

        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);

        JButton buttonFit = new JButton("Adjust Fit / Override");
        buttonFit.addActionListener(this);
        
        JButton buttonViewAll = new JButton("View all");
        buttonViewAll.addActionListener(this);
                
        JButton buttonWrite = new JButton("Write to file");
        buttonWrite.addActionListener(this);
        
        this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonViewAll);
        this.calibPane.getBottonPane().add(buttonWrite);
		
		DetectorDescriptor desc = new DetectorDescriptor();
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){

					desc.setSectorLayerComponent(sector, layer, paddle);
									
					H2D hist = 
					new H2D("Time Diff/2 vs Position - Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							"Time Diff/2 vs Position - Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							100, lowY(paddle), highY(paddle), 
							200, -10.0, 10.0);
					container.put(desc.getHashCode(), hist);

					// initialize the treemap of constants array
					Double[] consts = { 0.0, 0.0};
					constants.put(desc.getHashCode(), consts);

				}
			}
		}
	}
	
	private double halfLength(int paddle) {
		return 85.0;
		// *** hard coded for paddle 10 at the moment - read from geometry???
	}
	
	public double lowY(int paddle) {
		return -halfLength(paddle);
	}
	
	public double highY(int paddle) {
		return halfLength(paddle);
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

    	if(e.getActionCommand().compareTo("Adjust Fit / Override")==0){
        	
        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();
        	int paddle = constantsTablePanel.getSelected().getComponent();
	    	
        	customFit(sector, layer, paddle);
	    	
	    	constantsTable.getEntry(sector, layer, paddle).setData(0, 
	    			Double.parseDouble(new DecimalFormat("0.000").format(this.getVeff(sector, layer, paddle))));
	    	constantsTable.getEntry(sector, layer, paddle).setData(1, 
	    			Double.parseDouble(new DecimalFormat("0.000").format(this.getVeffError(sector, layer, paddle))));
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
    
    public void analyze(){
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
					fitEffectiveVelocity(sector, layer, paddle, 0.0, 0.0);
				}
			}
		}
	}
	
	public void fitEffectiveVelocity(int sector, int layer, int paddle,
			double minRange, double maxRange) {
		
		H2D veffHist = getH2D(sector, layer, paddle);

		// fit function to the graph of means
		GraphErrors meanGraph = veffHist.getProfileX();

		// find the range for the fit
		double lowLimit;
		double highLimit;
		
		if (minRange != 0.0 && maxRange != 0.0) {

			// use custom values for fit
			lowLimit = minRange;
			highLimit = maxRange;
		}
		else {

			int lowIndex = 20;
			int highIndex = 80;
			int graphMaxIndex = meanGraph.getDataSize(0)-1;
	
			int centreIndex = meanGraph.getDataSize(0)/2;
	
			for (int pos=centreIndex; pos < graphMaxIndex; pos++) {
				
			    if(meanGraph.getDataY(pos) < meanGraph.getDataY(pos-1)){
				      highIndex = pos-2;
				      break;
			    }
			}
			
			for (int pos=centreIndex; pos>=1; pos--) {
			    if(meanGraph.getDataY(pos) > meanGraph.getDataY(pos+1)){
				      lowIndex = pos+2;
				      break;
			    }
			}
			
			lowLimit = meanGraph.getDataX(lowIndex);
			highLimit = meanGraph.getDataX(highIndex);
		}
		
		F1D veffFunc = new F1D("p1", lowLimit, highLimit);
		meanGraph.fit(veffFunc,"RNQ");

		// Store the fitted function
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		functions.put(desc.getHashCode(), veffFunc);
		
		// TEST CODE
		if (paddle==9 && sector==1) {
			TCanvas c2 = new TCanvas("Mean Graph","Mean Graph",1200,800);
			c2.setDefaultCloseOperation(c2.HIDE_ON_CLOSE);
			c2.draw(meanGraph);
			c2.draw(veffFunc, "same");
		}
				
	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		int numHists = 1; //this.getH2D(sector, layer, paddle).length;
		canvas.divide(numHists, 1);
		
		for (int i=0; i<numHists; i++) {			

			canvas.cd(i);

			Font font = new Font("Verdana", Font.PLAIN, 7);		
			canvas.getPad().setFont(font);
			
			H2D hist = getH2D(sector, layer, paddle);
			
			canvas.draw(hist,"");
			canvas.draw(this.getF1D(sector, layer, paddle),"same");
		}
		
	}

	private double toDouble(String stringVal) {

		double doubleVal;
		try {
			doubleVal = Double.parseDouble(stringVal);
		} catch (NumberFormatException e) {
			doubleVal = 0.0;
		}
		return doubleVal;
	}

	
	public void customFit(int sector, int layer, int paddle){

		String[] fields = { "Min range for fit:", "Max range for fit:",
				"Override Effective Velocity:", "Override Effective Velocity uncertainty:"};
				
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit / Override for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = toDouble(panel.textFields[0].getText());
			double maxRange = toDouble(panel.textFields[1].getText());
			double overrideValue = toDouble(panel.textFields[2].getText());
			double overrideUnc = toDouble(panel.textFields[3].getText());

			// put the constants in the treemap
			Double[] consts = getConst(sector, layer, paddle);
			consts[VEFF_OVERRIDE] = overrideValue;
			consts[VEFF_UNC_OVERRIDE] = overrideUnc;

			DetectorDescriptor desc = new DetectorDescriptor();
			desc.setSectorLayerComponent(sector, layer, paddle);
			constants.put(desc.getHashCode(), consts);

			fitEffectiveVelocity(sector, layer, paddle, minRange, maxRange);

		}	 
	}
	
	public H2D getH2D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	
	public F1D getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	
	public Double[] getConst(int sector, int layer, int paddle) {
		return this.constants.get(DetectorDescriptor.generateHashCode(sector,
				layer, paddle));
	}


	public Double getVeff(int sector, int layer, int paddle) {
		
		double veff = 0.0;
		
		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(
				sector, layer, paddle))[VEFF_OVERRIDE];

		if (overrideVal != 0.0) {
			veff = overrideVal;
		} else {

			double gradient = this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle)).getParameter(1);
			if (gradient==0.0) {
				veff=0.0;
			}
			else {
				veff = 1/gradient;
			}
		}
		return veff;
	}
	
	public Double getVeffError(int sector, int layer, int paddle){
		
		double veffError = 0.0;
		
		// has the value been overridden?
		double overrideUnc = constants.get(DetectorDescriptor.generateHashCode(
				sector, layer, paddle))[VEFF_UNC_OVERRIDE];

		if (overrideUnc != 0.0) {
			veffError = overrideUnc;
		} else {

			// Calculate the error
			// fractional error in veff = fractional error in 1/veff
			double gradient = this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle)).getParameter(1);
			double gradientError = this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle)).getParError(1);
			double veff = getVeff(sector, layer, paddle);
			
			if (gradient==0.0) {
				veffError = 0.0;
			}
			else {
				veffError = (gradientError/gradient) * veff;
			}
		}
		
		return veffError;
	}	
		
	public void fillTable(int sector, int layer, final ConstantsTable table) {
		
		int layer_index = layer-1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++) {
			
			table.addEntry(sector, layer, paddle);
			
	    	table.getEntry(sector, layer, paddle).setData(0, 
	    			Double.parseDouble(new DecimalFormat("0.000").format(this.getVeff(sector, layer, paddle))));
	    	table.getEntry(sector, layer, paddle).setData(1, 
	    			Double.parseDouble(new DecimalFormat("0.000").format(this.getVeffError(sector, layer, paddle))));
			
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
		String filePrefix = "FTOF_CALIB_VEFF_"+todayString;
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
			book.add(getH2D(sector, layer, paddle), "");
			book.add(getF1D(sector, layer, paddle), "same");
			
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
			
			H2D fitHist = getH2D(sector, layer, paddleNum);
						
			fitCanvases[canvasNum].cd(padNum);
			//fitHist.setLineColor(2);
			fitHist.setTitle("Paddle "+paddleNum);
			fitCanvases[canvasNum].draw(fitHist);
			
			F1D fitFunc = getF1D(sector, layer, paddleNum);
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
