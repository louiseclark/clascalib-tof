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
public class TOFLeftRight   implements IDetectorListener,IConstantsTableListener,ActionListener {

	private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
	private CalibrationPane  		calibPane = new CalibrationPane();
	private ConstantsTable   			constantsTable = null;
	private ConstantsTablePanel			constantsTablePanel;
		
	TreeMap<Integer,H1D> container = new TreeMap<Integer,H1D>();
	TreeMap<Integer,F1D> functions = new TreeMap<Integer,F1D>();
	TreeMap<Integer, Double[]> constants = new TreeMap<Integer, Double[]>();

	// constants for indexing the constants array
	public final int LR_OVERRIDE = 0;
	
	final double LEFT_RIGHT_RATIO = 0.3;
	
	public CalibrationPane getView() {
		return calibPane;
	}	
	
	public void process(List<TOFPaddle> paddleList){
		

		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){

				// fill (Time Left - Time Right / 2) vs position
				if (paddle.includeInTimeWalk()) {
					this.container.get(paddle.getDescriptor().getHashCode()).fill(paddle.leftRight());
				}

			} else {
				System.out.println("Cant find : " + paddle.getDescriptor().toString() );
			}
		}
		
	}
	
	public void init(){
		
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"centroid"});

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
									
					H1D hist = new H1D("Left Right Sector "+sector+" Paddle "+paddle,"Left Right Sector "+sector+" Paddle "+paddle, 
							200, -960.0, 960.0);
					container.put(desc.getHashCode(), hist);
					
					// initialize the treemap of constants array
					Double[] consts = { 0.0 };
					constants.put(desc.getHashCode(), consts);
				}
			}
		}
	}

	public void initDisplay(){
		
        // Display sector 2 initially
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
	    			Double.parseDouble(new DecimalFormat("0.000").format(this.getCentroid(sector, layer, paddle))));
			
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
					fitLeftRight(sector, layer, paddle);
				}
			}
		}
	}
	
	public void fitLeftRight(int sector, int layer, int paddle) {
		
		H1D leftRightHist = this.getH1D(sector, layer, paddle);
		
		int nBin = leftRightHist.getXaxis().getNBins();

		// calculate the 'average' of all bins
		double averageAllBins=0;
		for(int i=1;i<=nBin;i++)
			averageAllBins+=leftRightHist.getBinContent(i);
		averageAllBins/=nBin;

		// find the first points left and right of centre with bin content < average
		int lowRangeFirstCut=0,highRangeFirstCut=0;
		for(int i=nBin/2;i>=1;i--){
			if(leftRightHist.getBinContent(i)<averageAllBins){
				lowRangeFirstCut=i;
				break;
			}
		}
		for(int i=nBin/2;i<=nBin;i++){
			if(leftRightHist.getBinContent(i)<averageAllBins){
				highRangeFirstCut=i;
				break;
			}
		}
		
		// now calculate the 'average' in this range
		double averageCentralRange=0;
		for(int i=lowRangeFirstCut;i<=highRangeFirstCut;i++)
			averageCentralRange+=leftRightHist.getBinContent(i);
		averageCentralRange/=(highRangeFirstCut-lowRangeFirstCut+1);
		
		// find the first points left and right of centre with bin content < 0.3 * average in the range
		double threshold=averageCentralRange*LEFT_RIGHT_RATIO;
		//if(averageCentralRange<20) return;
		int lowRangeSecondCut=0, highRangeSecondCut=0;
		for(int i=nBin/2;i>=1;i--){
			if(leftRightHist.getBinContent(i)<threshold){
				lowRangeSecondCut=i;
				break;
			}
		}
		for(int i=nBin/2;i<=nBin;i++){
			if(leftRightHist.getBinContent(i)<threshold){
				highRangeSecondCut=i;
				break;
			}
		}

		
		// find error
		// find the points left and right of centre with bin content < 0.3 * (average + sqrt of average)
		double errorThreshold = (averageCentralRange + Math.sqrt(averageCentralRange))*LEFT_RIGHT_RATIO;
		int lowRangeError=0, highRangeError=0;
		for(int i=nBin/2;i>=1;i--){
			if(leftRightHist.getBinContent(i)<errorThreshold){
				lowRangeError=i;
				break;
			}
		}
		for(int i=nBin/2;i<=nBin;i++){
			if(leftRightHist.getBinContent(i)<errorThreshold){
				highRangeError=i;
				break;
			}
		}
		
		// store the function showing the width of the spread
		F1D edgeToEdgeFunc = new F1D("p1",leftRightHist.getAxis().getBinCenter(lowRangeSecondCut), 
									      leftRightHist.getAxis().getBinCenter(highRangeSecondCut));
		edgeToEdgeFunc.setParameter(1, 0.0);
		edgeToEdgeFunc.setParameter(0, averageCentralRange*LEFT_RIGHT_RATIO); // height to draw line at

		// store the function with range = error values
		F1D errorFunc = new F1D("p1",leftRightHist.getAxis().getBinCenter(lowRangeError) -
										 leftRightHist.getAxis().getBinCenter(lowRangeSecondCut),
										 leftRightHist.getAxis().getBinCenter(highRangeError) -
									     leftRightHist.getAxis().getBinCenter(highRangeSecondCut));
		errorFunc.setParameter(1, 0.0);
		errorFunc.setParameter(0, averageCentralRange*LEFT_RIGHT_RATIO); // height to draw line at
		
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		functions.put(desc.getHashCode(), edgeToEdgeFunc);
				
	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		int numHists = 1; //this.getH2D(sector, layer, paddle).length;
		canvas.divide(numHists, 1);
		
		for (int i=0; i<numHists; i++) {			

			canvas.cd(i);

			Font font = new Font("Verdana", Font.PLAIN, 7);		
			canvas.getPad().setFont(font);
			
			H1D hist = getH1D(sector, layer, paddle);
			
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

		String[] fields = { "Override centroid:" };
		TOFCustomFitPanel panel = new TOFCustomFitPanel(fields);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Override value for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double overrideValue = toDouble(panel.textFields[0].getText());
			
			// put the constants in the treemap
			Double[] consts = getConst(sector, layer, paddle);
			consts[LR_OVERRIDE] = overrideValue;

			DetectorDescriptor desc = new DetectorDescriptor();
			desc.setSectorLayerComponent(sector, layer, paddle);
			constants.put(desc.getHashCode(), consts);

		}	 
	}
	
	public H1D getH1D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	
	public F1D getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	
	public Double[] getConst(int sector, int layer, int paddle) {
		return this.constants.get(DetectorDescriptor.generateHashCode(sector,
				layer, paddle));
	}	

	public Double getCentroid(int sector, int layer, int paddle) {
		
		double leftRight = 0.0;
		
		// has the value been overridden?
		double overrideVal = constants.get(DetectorDescriptor.generateHashCode(
				sector, layer, paddle))[LR_OVERRIDE];

		if (overrideVal != 0.0) {
			leftRight = overrideVal;
		} else {

			double min = this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle)).getMin();
			double max = this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle)).getMax();
			leftRight = (min+max)/2.0;
		}
		
		return leftRight;
	}
	
		
	public void fillTable(int sector, int layer, final ConstantsTable table) {
		
		int layer_index = layer-1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++) {
			
			table.addEntry(sector, layer, paddle);
			
	    	table.getEntry(sector, layer, paddle).setData(0, 
	    			Double.parseDouble(new DecimalFormat("0.000").format(this.getCentroid(sector, layer, paddle))));
			
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
		String filePrefix = "FTOF_CALIB_LR_"+todayString;
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
			book.add(getH1D(sector, layer, paddle), "");
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
			
			H1D fitHist = getH1D(sector, layer, paddleNum);
						
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
