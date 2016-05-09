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
import org.root.pad.TBookCanvas;
import org.root.basic.*;


/**
 *
 * @author louiseclark
 */
public class TOFLeftRight {

	TreeMap<Integer,H1D> container = new TreeMap<Integer,H1D>();
	TreeMap<Integer,F1D[]> functions = new TreeMap<Integer,F1D[]>();

	final double LEFT_RIGHT_RATIO = 0.3;
	final int EDGE_TO_EDGE = 0;
	final int ERROR = 1;
	
	public void processEvent(EvioDataEvent event){
		List<TOFPaddle> list = DataProvider.getPaddleListDgtz(event);        
		this.process(list);
	}

	public void process(List<TOFPaddle> paddleList){
		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
				
				this.container.get(paddle.getDescriptor().getHashCode()).fill(paddle.leftRight());
				
			} else {
				System.out.println("Cant find : " + paddle.getDescriptor().toString() );
			}
		}
	}
	
	public void init(){
		DetectorDescriptor desc = new DetectorDescriptor();
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer-1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){

					desc.setSectorLayerComponent(sector, layer, paddle);
					H1D hist = new H1D("Left Right Sector "+sector+" Paddle "+paddle,"Left Right Sector "+sector+" Paddle "+paddle, 
											200, -1000.0, 1000.0);
					container.put(desc.getHashCode(), hist);
				}
			}
		}
	}

	public void analyze(){
		for(int sector = 1; sector <= 6; sector++){
			for (int layer = 1; layer <= 3; layer++) {
				int layer_index = layer -1;
				for(int paddle = 1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
					fit(sector, layer, paddle, 0.0, 0.0);
				}
			}
		}
	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		canvas.draw(this.getH1D(sector, layer, paddle),"");
		canvas.draw(this.getF1D(sector, layer, paddle)[EDGE_TO_EDGE],"same");
		
	}

	public void fit(int sector, int layer, int paddle,
			double minRange, double maxRange){

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
		F1D[] funcs = {edgeToEdgeFunc,errorFunc};
		functions.put(desc.getHashCode(), funcs);
		
	}
	
//
//	public void customFit(int sector, int layer, int paddle){
//
//		H1D h = getH1D(sector, layer, paddle)[GEOMEAN];
//		F1D f = getF1D(sector, layer, paddle)[GEOMEAN];        
//		
//		TOFCustomFitPanel panel = new TOFCustomFitPanel();
//
//		int result = JOptionPane.showConfirmDialog(null, panel, 
//				"Adjust Fit for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
//		if (result == JOptionPane.OK_OPTION) {
//
//			double minRange = Double.parseDouble(panel.minRange.getText());
//			double maxRange = Double.parseDouble(panel.maxRange.getText());
//
//			fitGeoMean(sector, layer, paddle, minRange, maxRange);
//
//		}	 
//	}

	public H1D getH1D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public F1D[] getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
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
	
	public void fillTable(int sector, int layer, ConstantsTable table) {
		
		int layer_index = layer -1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++) {
			F1D f = getF1D(sector, layer, paddle)[EDGE_TO_EDGE];
			table.addEntry(sector, layer, paddle);
			table.getEntry(sector, layer, paddle).setData(0, Double.parseDouble(new DecimalFormat("0").format(f.getMin())));
			table.getEntry(sector, layer, paddle).setData(1, Double.parseDouble(new DecimalFormat("0").format(f.getMax())));
		}
	}

	
	public TBookCanvas showFits(int sector, int layer) {
		TBookCanvas		book = new TBookCanvas(2,2);
		
		int layer_index = layer -1;
		for (int paddle=1; paddle <= TOFCalibration.NUM_PADDLES[layer_index]; paddle++){
			book.add(getH1D(sector, layer, paddle), "");
			book.add(getF1D(sector, layer, paddle)[EDGE_TO_EDGE], "same");
			
		}
		return book;
	}
	
	public void show(){
		for(Map.Entry<Integer,H1D> item : this.container.entrySet()){
			System.out.println(item.getKey() + "  -->  " + item.getValue().getMean());
		}
	}

}
