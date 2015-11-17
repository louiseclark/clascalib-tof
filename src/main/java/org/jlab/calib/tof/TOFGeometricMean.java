/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.jlab.clas.detector.*;
import org.jlab.evio.clas12.*;
import org.root.func.*;
import org.root.histogram.*;
import org.root.pad.*;

/**
 *
 * @author gavalian
 */
public class TOFGeometricMean {

	TreeMap<Integer,H1D> container = new TreeMap<Integer,H1D>();
	TreeMap<Integer,F1D> functions = new TreeMap<Integer,F1D>();
	int[]	NUM_PADDLES = {23,62,5};

	
	public void processEvent(EvioDataEvent event){
		List<TOFPaddle> list = DataProvider.getPaddleList(event);        
		this.process(list);
	}

	public void process(List<TOFPaddle> paddleList){
		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
				this.container.get(paddle.getDescriptor().getHashCode()).fill(paddle.geometricMean());
			} else {
				System.out.println("Cant find : " + paddle.getDescriptor().toString() );
			}
		}
	}


	public void init(){
		DetectorDescriptor desc = new DetectorDescriptor();
		for(int sector = 0; sector < 6; sector++){
			for(int paddle = 0; paddle < 23; paddle++){
				desc.setSectorLayerComponent(sector, 0,paddle);
				H1D h1 = new H1D("Paddle "+paddle,"Paddle "+paddle, 200,0.0,5000.0);
				container.put(desc.getHashCode(), h1);
				System.out.println("Init done for "+sector+" 0 "+paddle);
			}
			for(int paddle = 0; paddle < 62; paddle++){
				desc.setSectorLayerComponent(sector, 1,paddle);
				H1D h1 = new H1D("Paddle "+paddle,"Paddle "+paddle, 150,0.0,15000.0);
				container.put(desc.getHashCode(), h1);
				System.out.println("Init done for "+sector+" 1 "+paddle);
			}
		}
	}

	public void analyze(){
		//for(int sector = 0; sector < 6; sector++){
		int sector=5;
			for(int paddle = 0; paddle < 23; paddle++){
				System.out.println("Fitting paddle "+paddle);
				fit(sector, 0, paddle, 0.0, 0.0);
			}
			for(int paddle = 0; paddle < 62; paddle++){
				System.out.println("Fitting paddle "+paddle);
				fit(sector, 1, paddle, 0.0, 0.0);
			}
		//}
	}

	public void fit(int sector, int layer, int paddle,
			double minRange, double maxRange){

		System.out.println("Fitting with range "+minRange+" - "+maxRange);
		System.out.println("Fitting sector "+sector+" "+layer+" "+paddle);

		H1D h = this.getH1D(sector, layer, paddle);

		// Work out the range for the fit
		double maxChannel = h.getAxis().getBinCenter(h.getAxis().getNBins()-1);
		double startChannelForFit = 0.0;
		double endChannelForFit = 0.0;
		if (minRange==0.0) {
			startChannelForFit = maxChannel * 0.1;
		}
		else {
			startChannelForFit = minRange;
			System.out.println("Setting to custom value");
		}
		if (maxRange==0.0) {
			endChannelForFit = maxChannel * 0.9;
		}
		else {
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
		F1D f = new F1D("landau+exp",startChannelForFit, endChannelForFit);

		f.setParameter(0, maxCounts);
		f.setParameter(1, maxPos);
		f.setParameter(2, 200.0);
		f.setParLimits(2, 0.0,800.0);
		f.setParameter(3, 20.0);
		f.setParameter(4, 0.0);
		h.fit(f);

		System.out.println("Geomean"+ " "+sector+" "+layer+" "+paddle+" "+f.getParameter(1));
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		functions.put(desc.getHashCode(), f);

	}

	//    public void customFit(H1D h, F1D f){
	public void customFit(int sector, int layer, int paddle){

		//JDialog dialog = new JDialog();
		
		H1D h = getH1D(sector, layer, paddle);
		F1D f = getF1D(sector, layer, paddle);        
		
		TOFCustomFitPanel panel = new TOFCustomFitPanel(h,f);

		int result = JOptionPane.showConfirmDialog(null, panel, 
				"Adjust Fit for paddle "+paddle, JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {

			double minRange = Double.parseDouble(panel.minRange.getText());
			double maxRange = Double.parseDouble(panel.maxRange.getText());

			fit(sector, layer, paddle, minRange, maxRange);

		}	 

		//        dialog.add(panel);
		//        dialog.setVisible(true);
	}

	public H1D getH1D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public F1D getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	
	public void fillTable(int sector, ConstantsTable table) {

//		for (int paddle=0; paddle<23; paddle++) {
//			F1D f = getF1D(sector, 0,paddle);
//			table.addEntry(sector, 0, paddle);
//			System.out.println("filling paddle "+paddle);
//			table.getEntry(sector, 0, paddle).setData(0, Math.round(f.getParameter(1)));
//			table.getEntry(sector, 0, paddle).setData(1, Math.round(f.getParameter(2)));
//			
//		}
		for (int paddle=0; paddle<23; paddle++) {
			F1D f = getF1D(sector, 0,paddle);
			table.addEntry(sector, 0, paddle);
			System.out.println("filling paddle "+paddle);
			table.getEntry(sector, 0, paddle).setData(0, Math.round(f.getParameter(1)));
			table.getEntry(sector, 0, paddle).setData(1, Math.round(f.getParameter(2)));
			
		}
		
	}

	
	public TBookCanvas showFits(int sector, int layer) {
		TBookCanvas		book = new TBookCanvas(2,2);
		
		for (int paddle=0; paddle<NUM_PADDLES[layer]; paddle++){
			book.add(getH1D(sector, layer, paddle), "");
			book.add(getF1D(sector, layer, paddle), "same");
			
		}
		return book;
	}
	
	public void show(){
		for(Map.Entry<Integer,H1D> item : this.container.entrySet()){
			System.out.println(item.getKey() + "  -->  " + item.getValue().getMean());
		}
	}



}
