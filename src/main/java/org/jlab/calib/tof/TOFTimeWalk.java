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
import java.util.ArrayList;
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
		
	public void processEvent(EvioDataEvent event){
		
		//List<TOFPaddle> list = DataProvider.getPaddleList(event);
		List<TOFPaddle> list = DataProvider.getPaddleList(event);
		this.process(list);
	}

	public void process(List<TOFPaddle> paddleList){
		
		// paddle list is processed 5 times each time correcting the time using refined values for lambda and order
		//double[] lambda = {0.0,0.0};
		//double[] order = {2.0,2.0};
		
		double[] lambda = {1.0,1.0};
		double[] order = {0.5,0.5};
		
		//double[] lambda = {-21435.6,21435.6};
		//double[] order = {2.0,2.0};
		
		//for (int i=0; i < NUM_ITERATIONS; i++) {
			
			for(TOFPaddle paddle : paddleList){
				if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
					
					// fill timeResidual vs ADC
					double [] tr = paddle.timeResiduals(lambda, order);
					
					if (paddle.includeInTimeWalk()) {
						double adcL = paddle.ADCL - paddle.getPedestalL();
						double adcR = paddle.ADCR - paddle.getPedestalR();
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
							100, 0.0, 2000.0,
							100, -10.0, 10.0),
					new H2D("Time residual vs ADC RIGHT Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							"Time residual vs ADC RIGHT Sector "+desc.getSector()+
							" Paddle "+desc.getComponent(),
							100, 0.0, 2000.0,
							100, -10.0, 10.0)};
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
					fitTimeWalk(sector, layer, paddle);
				}
			}
		}
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
				h.fit(f, "RN");
				
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
//			F1D trFunc = new F1D("[0]+([1]/Math.pow(x,[2]))");
//			double[] initParams = {0.0,5.0,0.5};
//			trFunc.setParameters(initParams);
//			meanGraph.fit(trFunc);
//			c2.draw(trFunc,"same");
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
			System.out.println(hist.getName());
			//hist.setLineColor(1);
			
			canvas.draw(hist,"");
			//canvas.draw(this.getF1D(sector, layer, paddle)[i],"same");
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
			
			//F1D f = getF1D(sector, layer, paddle)[LEFT];
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
