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
//import org.jlab.groot.math.F1D;
import org.root.histogram.*;
import org.root.pad.TCanvas;
import org.root.pad.TBookCanvas;
import org.root.basic.EmbeddedCanvas;


/**
 *
 * @author louiseclark
 */
public class TOFTimeWalk   implements IDetectorListener,IConstantsTableListener,ActionListener {

	private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
	private CalibrationPane  		calibPane = new CalibrationPane();
	private ConstantsTable   			constantsTable = null;
	private ConstantsTablePanel			constantsTablePanel;
	private CalibrationConstants veffConstants = new CalibrationConstants();
	
	private List<TOFPaddle>     eventList = new ArrayList<TOFPaddle>();
	
	TreeMap<Integer,H2D[]> container = new TreeMap<Integer,H2D[]>();
	TreeMap<Integer,F1D[]> functions = new TreeMap<Integer,F1D[]>();
	TreeMap<Integer,Double[]> constants = new TreeMap<Integer,Double[]>();
	
	// constants for indexing the histograms
	public final int LEFT = 0;
	public final int RIGHT = 1;

	// constants for indexing the constant arrays
	public final int LAMBDA_LEFT_OVERRIDE = 0;
	public final int ORDER_LEFT_OVERRIDE = 1;
	public final int LAMBDA_RIGHT_OVERRIDE = 2;
	public final int ORDER_RIGHT_OVERRIDE = 3;
	
	private final double[]		ADC_MAX = {0.0,4000.0,8000.0,3000.0};

	
	// the number of cycles of corrections
	private final int			NUM_ITERATIONS = 5;
	
	public CalibrationPane getView() {
		return calibPane;
	}	
	
	public void process(List<TOFPaddle> paddleList) {

		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
				eventList.add(paddle);
			}
		}
	}
	
	public void processEvents(){
		
		// paddle list is processed 5 times each time correcting the time using refined values for lambda and order
		double[] lambda = {0.0,0.0};
		double[] order = {2.0,2.0};
		
//		double[] lambda = {1.0,1.0};
//		double[] order = {0.5,0.5};
		
		//double[] lambda = {-21435.6,21435.6};
		//double[] order = {2.0,2.0};
		
		//for (int i=0; i < NUM_ITERATIONS; i++) {
			
			for(TOFPaddle paddle : eventList){
				if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
					
					// fill timeResidual vs ADC
					double [] tr = paddle.timeResiduals(lambda, order);
					
					if (paddle.includeInTimeWalk()) {
						double adcL = paddle.ADCL; // - paddle.getPedestalL();
						double adcR = paddle.ADCR; // - paddle.getPedestalR();
						this.container.get(paddle.getDescriptor().getHashCode())[LEFT].fill(adcL, tr[LEFT]);
						this.container.get(paddle.getDescriptor().getHashCode())[RIGHT].fill(adcR, tr[RIGHT]);
					}

				} else {
					System.out.println("Cant find : " + paddle.getDescriptor().toString() );
				}
			}
		//}
		
	}
	
	public void init(){
		
		// Get effective velocity values from calDB
		veffConstants.define("eff", "/calibration/ftof/effective_velocity");
		int runID = 10;
		veffConstants.init(runID, "default");
		
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"Lambda left","Order left", "Lambda right", "Order right"});

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
									
					H2D[] hists = {
					new H2D("Time residual vs ADC LEFT Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							"Time residual vs ADC LEFT Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							100, 0.0, ADC_MAX[layer],
							100, -10.0, 10.0),
					new H2D("Time residual vs ADC RIGHT Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							"Time residual vs ADC RIGHT Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							100, 0.0, ADC_MAX[layer],
							100, -10.0, 10.0)};
					container.put(desc.getHashCode(), hists);
					
					// initialize the treemap of constant arrays
					Double[] consts = { 0.0, 0.0, 0.0, 0.0};
					constants.put(desc.getHashCode(), consts);
				}
			}
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

    	if(e.getActionCommand().compareTo("Adjust Fit / Override")==0){
        	
        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();
        	int paddle = constantsTablePanel.getSelected().getComponent();
	    	
        	customFit(sector, layer, paddle);
	    	
			constantsTable.getEntry(sector, layer, paddle).setData(
					0,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getLambdaLeft(sector, layer, paddle))));
			constantsTable.getEntry(sector, layer, paddle).setData(
					1,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getOrderLeft(sector, layer, paddle))));
			constantsTable.getEntry(sector, layer, paddle).setData(
					2,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getLambdaRight(sector, layer, paddle))));
			constantsTable.getEntry(sector, layer, paddle).setData(
					3,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getOrderRight(sector, layer, paddle))));
			
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
        	
        	viewFits(sector, layer, 0);
        	viewFits(sector, layer, 1);
        	
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
    	
    	System.out.println("TW before processEvents");
    	// fill the time residual vs ADC histograms
    	//processEvents();
    	System.out.println("TW after processEvents");    	
    	
		for(int sector = 1; sector <= 6; sector++){
		//for(int sector = 2; sector <= 2; sector++){
			for (int layer = 1; layer <= 3; layer++) {
			//for (int layer = 1; layer <= 2; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
					fitTimeWalk(sector, layer, paddle);
				}
			}
		}
		System.out.println("TW after fit");
	}
	
	public void fitTimeWalk(int sector, int layer, int paddle) {
		
		if (paddle==9 && sector==1 && layer==1) {
			H2D twL = getH2D(sector, layer, paddle)[LEFT];
			
			ArrayList<H1D> twLSlices = twL.getSlicesX();
			int numBins = twL.getXAxis().getNBins();
			
			double[] binSlices = new double[numBins];
			double[] means = new double[numBins];
			
			for (int i=0; i<numBins; i++) {
				
				H1D h = twLSlices.get(i);
				F1D f = new F1D("gaus",-2.0,2.0);
				f.setParameter(0, 250.0);
				f.setParameter(1, 0.0);
				f.setParameter(2, 2.0);
				h.fit(f, "RNQ");
				
				binSlices[i] = twL.getXAxis().getBinCenter(i);
				means[i] = f.getParameter(1);
			
				if (i==50) {
					TCanvas c1 = new TCanvas("Test slices","Test slices",1200,800,1,1);
					c1.setDefaultCloseOperation(c1.HIDE_ON_CLOSE);
					c1.cd(0);
					c1.draw(h);
					c1.draw(f,"same");
				}
		
			}
			GraphErrors meanGraph = new GraphErrors("Mean Graph", binSlices, means);
			TCanvas c2 = new TCanvas("Mean Graph","Mean Graph",1200,800);
			c2.setDefaultCloseOperation(c2.HIDE_ON_CLOSE);
			c2.draw(meanGraph);
			
			// fit function to the graph of means
			//F1D trFunc = new F1D("func","[0]+([1]/x^[2]))");
//			F1D trFunc = new F1D("[0]+([1]/x^[2]))");
//			double[] initParams = {0.0,5.0,0.5};
//			trFunc.setParameters(initParams);
//			meanGraph.fit(trFunc);
//			c2.draw(trFunc,"same");
			
//			System.out.println("New lambda is "+trFunc.getParameter(1));
//			System.out.println("New order is "+trFunc.getParameter(2));
		}
	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		// test calDB read
		double veff = veffConstants.getEntryDouble("eff", "veff_left", sector, layer, paddle);
		
		System.out.println("Veff is " + veff);
		
		
		int numHists = this.getH2D(sector, layer, paddle).length;
		canvas.divide(numHists, 1);
		
		for (int i=0; i<numHists; i++) {			

			canvas.cd(i);

			Font font = new Font("Verdana", Font.PLAIN, 7);		
			canvas.getPad().setFont(font);
			
			H2D hist = getH2D(sector, layer, paddle)[i];
			System.out.println(hist.getName());
			//hist.setLineColor(1);
			
			canvas.draw(hist,"");
			//canvas.draw(this.getF1D(sector, layer, paddle)[i],"same");
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

		String[] fields = { "Override Lambda Left:", "Override Order Left:", "SPACE",
							"Override Lambda Right:", "Override Order Right:"};
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields);

		int result = JOptionPane
				.showConfirmDialog(null, panel, "Override values for paddle "
						+ paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double overrideLL = toDouble(panel.textFields[0].getText());
			double overrideOL = toDouble(panel.textFields[1].getText());
			double overrideLR = toDouble(panel.textFields[2].getText());
			double overrideOR = toDouble(panel.textFields[3].getText());

			// put the constants in the treemap
			Double[] consts = getConst(sector, layer, paddle);
			consts[LAMBDA_LEFT_OVERRIDE] = overrideLL;
			consts[ORDER_LEFT_OVERRIDE] = overrideOL;
			consts[LAMBDA_RIGHT_OVERRIDE] = overrideLR;
			consts[ORDER_RIGHT_OVERRIDE] = overrideOR;
			
			DetectorDescriptor desc = new DetectorDescriptor();
			desc.setSectorLayerComponent(sector, layer, paddle);
			constants.put(desc.getHashCode(), consts);

		}	 
	}
	
	public H2D[] getH2D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public F1D[] getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public Double[] getConst(int sector, int layer, int paddle){
		return this.constants.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}	
		
	public Double getLambdaLeft(int sector, int layer, int paddle) {

		double ll = 0.0;

		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(
				sector, layer, paddle))[LAMBDA_LEFT_OVERRIDE];

		if (overrideVal != 0.0) {
			ll = overrideVal;
		} else {
			
//			ll = functions.get(
//					DetectorDescriptor.generateHashCode(sector, layer, paddle))[LEFT]
//					.getParameter(0);
		}

		return ll;
	}	

	public Double getOrderLeft(int sector, int layer, int paddle) {

		double ol = 0.0;

		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(
				sector, layer, paddle))[ORDER_LEFT_OVERRIDE];

		if (overrideVal != 0.0) {
			ol = overrideVal;
		} else {
//			ol = functions.get(
//					DetectorDescriptor.generateHashCode(sector, layer, paddle))[LEFT]
//					.getParameter(1);
		}

		return ol;
	}	
		
	public Double getLambdaRight(int sector, int layer, int paddle) {

		double lr = 0.0;

		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(
				sector, layer, paddle))[LAMBDA_RIGHT_OVERRIDE];

		if (overrideVal != 0.0) {
			lr = overrideVal;
		} else {
//			lr = functions.get(
//					DetectorDescriptor.generateHashCode(sector, layer, paddle))[RIGHT]
//					.getParameter(0);
		}

		return lr;
	}	

	public Double getOrderRight(int sector, int layer, int paddle) {

		double or = 0.0;

		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(
				sector, layer, paddle))[ORDER_RIGHT_OVERRIDE];

		if (overrideVal != 0.0) {
			or = overrideVal;
		} else {
//			or = functions.get(
//					DetectorDescriptor.generateHashCode(sector, layer, paddle))[RIGHT]
//					.getParameter(1);
		}

		return or;
	}	
	
	public void fillTable(int sector, int layer, final ConstantsTable table) {
		
		int layer_index = layer-1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++) {
			
			table.addEntry(sector, layer, paddle);
			constantsTable.getEntry(sector, layer, paddle).setData(
					0,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getLambdaLeft(sector, layer, paddle))));
			constantsTable.getEntry(sector, layer, paddle).setData(
					1,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getOrderLeft(sector, layer, paddle))));
			constantsTable.getEntry(sector, layer, paddle).setData(
					2,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getLambdaRight(sector, layer, paddle))));
			constantsTable.getEntry(sector, layer, paddle).setData(
					3,
					Double.parseDouble(new DecimalFormat("0.0").format(this
							.getOrderRight(sector, layer, paddle))));

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
		String filePrefix = "FTOF_CALIB_TW_"+todayString;
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
			book.add(getH2D(sector, layer, paddle)[LEFT], "");
			//book.add(getF1D(sector, layer, paddle)[GEOMEAN], "same");
			
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
			
			H2D fitHist = getH2D(sector, layer, paddleNum)[plotType];
						
			fitCanvases[canvasNum].cd(padNum);
			//fitHist.setLineColor(2);
			fitHist.setTitle("Paddle "+paddleNum);
			fitCanvases[canvasNum].draw(fitHist);
			
			//F1D fitFunc = getF1D(sector, layer, paddleNum)[plotType];
			//fitCanvases[canvasNum].draw(fitFunc, "same");
			
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
