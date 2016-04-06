/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.awt.Font;
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

import javax.swing.JOptionPane;

import org.jlab.clas.detector.*;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.evio.clas12.*;
import org.root.func.*;
import org.root.histogram.*;
import org.root.pad.*;


/**
 *
 * @author gavalian
 */
public class TOFTimeWalk {

	TreeMap<Integer,H2D[]> container = new TreeMap<Integer,H2D[]>();
	TreeMap<Integer,F1D[]> functions = new TreeMap<Integer,F1D[]>();
	TreeMap<Integer,Double[]> constants = new TreeMap<Integer,Double[]>();
	
	// constants for indexing the histogram and constant arrays
	public final int LEFT = 0;
	public final int RIGHT = 1;

	
	private final double[]		GM_HIST_MAX = {4000.0,8000.0,3000.0};
	private final int[]			GM_HIST_BINS = {200, 300, 150};
	// the number of cycles of corrections
	private final int			NUM_ITERATIONS = 5;
		
	public void processEvent(EvioDataEvent event, EventDecoder decoder){
		
		//List<TOFPaddle> list = DataProvider.getPaddleList(event);
		List<TOFPaddle> list = DataProviderRaw.getPaddleList(event, decoder);
		this.process(list);
	}

	private double veffOffset(TOFPaddle paddle) {
		return 0.0; // get from calibration database, store locally to save going to database for every event
	}
	
	private double veff(TOFPaddle paddle) {
		return 16.0; // get from calibration database, store locally to save going to database for every event
	}
	
	private double[] timeResiduals(TOFPaddle paddle, double[] lambda, double[] order) {
		double[] tr = {0.0, 0.0};
		
		double timeL = paddle.TDCL - veffOffset(paddle);
		double timeR = paddle.TDCR + veffOffset(paddle);
		
		double timeLCorr = timeL + (lambda[LEFT]/Math.pow(paddle.ADCL, order[LEFT]));
		double timeRCorr = timeR + (lambda[RIGHT]/Math.pow(paddle.ADCL, order[RIGHT]));
		
		tr[LEFT] = ((timeL - timeRCorr)/2) - (paddle.POSITION/veff(paddle));
		tr[RIGHT] =  - ((timeLCorr - timeR)/2) + (paddle.POSITION/veff(paddle));
		
		return tr;
	}
	
	public void process(List<TOFPaddle> paddleList){
		
		// paddle list is processed 5 times each time correcting the time using refined values for lambda and order
		double[] lambda = {0.0,0.0};
		double[] order = {2.0,2.0};
		
		//for (int i=0; i < NUM_ITERATIONS; i++) {
			
			for(TOFPaddle paddle : paddleList){
				if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
					
					// fill timeResidual vs ADC
					double [] tr = timeResiduals(paddle, lambda, order);
					
					this.container.get(paddle.getDescriptor().getHashCode())[LEFT].fill(paddle.ADCL, tr[LEFT]);
					this.container.get(paddle.getDescriptor().getHashCode())[RIGHT].fill(paddle.ADCL, tr[RIGHT]);

				} else {
					System.out.println("Cant find : " + paddle.getDescriptor().toString() );
				}
			}
		//}
		
	}
	
	public void init(){
		DetectorDescriptor desc = new DetectorDescriptor();
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){

					desc.setSectorLayerComponent(sector, layer, paddle);
									
					H2D[] hists = {
					new H2D("Time residual vs ADC Sector LEFT"+desc.getSector()+
							" Paddle "+desc.getComponent(),
							"Time residual vs ADC Sector LEFT"+desc.getSector()+
							" Paddle "+desc.getComponent(),
							100, 0.0, 2000.0,
							100, 0.0, 2000.0),
					new H2D("Time residual vs ADC Sector RIGHT"+desc.getSector()+
							" Paddle "+desc.getComponent(),
							"Time residual vs ADC Sector RIGHT"+desc.getSector()+
							" Paddle "+desc.getComponent(),
							100, 0.0, 2000.0,
							100, 0.0, 2000.0)};
					container.put(desc.getHashCode(), hists);
				}
			}
		}
	}

	public void analyze(){
		for(int sector = 1; sector <= 6; sector++){
		//for(int sector = 2; sector <= 2; sector++){
			for (int layer = 1; layer <= 3; layer++) {
			//for (int layer = 1; layer <= 2; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
					//fitGeoMean(sector, layer, paddle, 0.0, 0.0);
					//fitLogRatio(sector, layer, paddle, 0.0, 0.0);
				}
			}
		}

	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		int numHists = this.getH2D(sector, layer, paddle).length;
		canvas.divide(numHists, 1);
		
		for (int i=0; i<numHists; i++) {			

			canvas.cd(i);

			Font font = new Font("Verdana", Font.PLAIN, 7);		
			canvas.getPad().setFont(font);
			
			H2D hist = getH2D(sector, layer, paddle)[i];
			//hist.setLineColor(1);
			
			canvas.draw(hist,"");
			canvas.draw(this.getF1D(sector, layer, paddle)[i],"same");
		}
		
	}

	
	public void customFit(int sector, int layer, int paddle){

		H2D h = getH2D(sector, layer, paddle)[LEFT];
		F1D f = getF1D(sector, layer, paddle)[LEFT];        
		
		TOFCustomFitPanel panel = new TOFCustomFitPanel();

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = Double.parseDouble(panel.minRange.getText());
			double maxRange = Double.parseDouble(panel.maxRange.getText());

			//fitGeoMean(sector, layer, paddle, minRange, maxRange);

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
		
	public double getCalibrationValue(int sector, int layer, int paddle, int funcNum, int param) {
		
		double calibVal;
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		F1D func;
		try {
			func = functions.get(desc.getHashCode())[funcNum];
			calibVal = func.getParameter(param);
		} catch (NullPointerException e) {
			calibVal = 0.0;
		}
		return calibVal;
	}
	
	public void fillTable(int sector, int layer, final ConstantsTable table) {
		
		
		int layer_index = layer-1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++) {
			
			F1D f = getF1D(sector, layer, paddle)[LEFT];
//			Double lrCentroid = getConst(sector, layer, paddle)[LR_CENTROID];
			table.addEntry(sector, layer, paddle);
//			table.getEntry(sector, layer, paddle).setData(0, Double.parseDouble(new DecimalFormat("0.0").format(f.getParameter(1))));
//			if (Double.isFinite(gmError)){
//				table.getEntry(sector, layer, paddle).setData(1, Double.parseDouble(new DecimalFormat("0.0").format(gmError)));
//			}
//			else {
//				table.getEntry(sector, layer, paddle).setData(1, 9999.0);
//			}
//			table.getEntry(sector, layer, paddle).setData(2, Double.parseDouble(new DecimalFormat("0.000").format(lrCentroid)));
//			if (Double.isFinite(lrError)) {
//				table.getEntry(sector, layer, paddle).setData(3, Double.parseDouble(new DecimalFormat("0.000").format(lrError)));
//			}
//			else {
//				table.getEntry(sector, layer, paddle).setData(3, 9999.0);
//			}

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
	
//	public void show(){
//		for(Map.Entry<Integer,H2D[]> item : this.container.entrySet()){
//			System.out.println(item.getKey() + "  -->  " + item.getValue()[GEOMEAN].getMean());
//		}
//	}

}
