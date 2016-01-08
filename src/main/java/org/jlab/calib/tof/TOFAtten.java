package org.jlab.calib.tof;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jlab.clas.detector.ConstantsTable;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.evio.clas12.EvioDataEvent;
import org.root.func.F1D;
import org.root.histogram.H1D;
import org.root.histogram.H2D;
import org.root.pad.EmbeddedCanvas;
import org.root.pad.TBookCanvas;

public class TOFAtten {

	TreeMap<Integer,H2D> container = new TreeMap<Integer,H2D>();
	TreeMap<Integer,F1D> functions = new TreeMap<Integer,F1D>();

	
	public void processEvent(EvioDataEvent event){
		List<TOFPaddle> list = DataProvider.getPaddleList(event);        
		this.process(list);
	}

	public void process(List<TOFPaddle> paddleList){
		for(TOFPaddle paddle : paddleList){
			if(this.container.containsKey(paddle.getDescriptor().getHashCode())==true){
				
				this.container.get(paddle.getDescriptor().getHashCode()).fill(paddle.position(), paddle.logRatio());
				
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
					H2D hist = new H2D("Log Ratio vs Position : Paddle "+paddle,"Log Ratio vs Position : Paddle "+paddle, 
											100, -1000.0, 1000.0, 100, -5.0, 5.0);
					container.put(desc.getHashCode(), hist);
				}
			}
		}
	}

	public void analyze(){
		for(int sector = 0; sector < 6; sector++){
			for (int layer = 0; layer < 3; layer++) {
				for(int paddle = 0; paddle < TOFCalibration.NUM_PADDLES[layer]; paddle++){
					fit(sector, layer, paddle, 0.0, 0.0);
				}
			}
		}
	}
	
	public void drawComponent(int sector, int layer, int paddle, EmbeddedCanvas canvas) {
		
		canvas.draw(this.getH1D(sector, layer, paddle),"");
		//canvas.draw(this.getF1D(sector, layer, paddle),"same");
		
	}

	public void fit(int sector, int layer, int paddle,
			double minRange, double maxRange){

		H2D attenHist = this.getH1D(sector, layer, paddle);
		F1D func = null;
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		functions.put(desc.getHashCode(), func);
		
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

	public H2D getH1D(int sector, int layer, int paddle){
		return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	public F1D getF1D(int sector, int layer, int paddle){
		return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
	}
	
	public double getCalibrationValue(int sector, int layer, int paddle, int funcNum, int param) {
		
		double calibVal;
		DetectorDescriptor desc = new DetectorDescriptor();
		desc.setSectorLayerComponent(sector, layer, paddle);
		F1D func;
		try {
			func = functions.get(desc.getHashCode());
			calibVal = func.getParameter(param);
		} catch (NullPointerException e) {
			calibVal = 0.0;
		}
		return calibVal;
	}
	
	public void fillTable(int sector, int layer, ConstantsTable table) {
		
		for (int paddle=0; paddle<TOFCalibration.NUM_PADDLES[layer]; paddle++) {
			F1D f = getF1D(sector, layer, paddle);
			table.addEntry(sector, layer, paddle);
			//table.getEntry(sector, layer, paddle).setData(0, Double.parseDouble(new DecimalFormat("0").format(f.getMin())));
			//table.getEntry(sector, layer, paddle).setData(1, Double.parseDouble(new DecimalFormat("0").format(f.getMax())));
		}
	}

	
	public TBookCanvas showFits(int sector, int layer) {
		TBookCanvas		book = new TBookCanvas(2,2);
		
		for (int paddle=0; paddle<TOFCalibration.NUM_PADDLES[layer]; paddle++){
			book.add(getH1D(sector, layer, paddle), "");
			book.add(getF1D(sector, layer, paddle), "same");
			
		}
		return book;
	}
	
//	public void show(){
//		for(Map.Entry<Integer,H2D> item : this.container.entrySet()){
//			System.out.println(item.getKey() + "  -->  " + item.getValue().getMean());
//		}
//	}

}

