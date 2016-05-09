/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.io.File;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioSource;

/**
 *
 * @author louiseclark
 * 
 */
public class TOFCalibration {
        
    private static TOFHighVoltage 			hv = new TOFHighVoltage();
    private static TOFTimeWalk 				tw = new TOFTimeWalk();
    private static TOFEffectiveVelocity		veff = new TOFEffectiveVelocity();
        
    public final static int[]		NUM_PADDLES = {23,62,5};
    public final String[]	LAYER_NAME = {"FTOF1A","FTOF1B","FTOF2"};
    
    public TOFCalibration(String inputFile){
        this.initDetector();
        this.init(inputFile);
    }
        
    public void init(String inputFile){
        
        hv.init();
        tw.init();
        veff.init();
        
        processFile(inputFile);
        
        hv.initDisplay();
        tw.initDisplay();
        veff.initDisplay();
        
    }
    
    public void initDetector(){
        
    	for (int layer = 1; layer <= 3; layer++) {
    		int layer_index = layer-1;
    		DetectorShapeView2D viewHv = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		DetectorShapeView2D viewTw = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		DetectorShapeView2D viewVeff = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		for(int sector = 1; sector <= 6; sector++){
    			int sector_index = sector -1;
        		for(int paddle = 1; paddle <= NUM_PADDLES[layer_index]; paddle++){
        			
        			int paddle_index = paddle-1;
        			DetectorShape2D  shape = new DetectorShape2D();
                    shape.getDescriptor().setType(DetectorType.FTOF1A);
                    shape.getDescriptor().setSectorLayerComponent(sector, layer, paddle);
                    shape.createBarXY(18, 80 + paddle_index*20);
                    shape.getShapePath().translateXYZ(120+20*paddle_index, 0, 0);
                    shape.getShapePath().rotateZ(Math.toRadians((sector_index*60.0)+180.0));
                    if(paddle%2==0){
                        shape.setColor(180, 255,180);
                    } else {
                        shape.setColor(180, 180, 255);
                    }
                    viewHv.addShape(shape);
                    viewTw.addShape(shape);
                    viewVeff.addShape(shape);

        		}
    		}
    		
            viewHv.addDetectorListener(hv);
            hv.getView().getDetectorView().addDetectorLayer(viewHv);
            viewTw.addDetectorListener(tw);
            tw.getView().getDetectorView().addDetectorLayer(viewTw);
            viewVeff.addDetectorListener(veff);
            veff.getView().getDetectorView().addDetectorLayer(viewVeff);
    	}

    	DetectorShapeView2D viewHv = new DetectorShapeView2D("Summary");
    	DetectorShapeView2D viewTw = new DetectorShapeView2D("Summary");
    	DetectorShapeView2D viewVeff = new DetectorShapeView2D("Summary");
    	for(int layer = 1; layer <= 3; layer++){
    		int layer_index = layer-1;
    		for(int sector = 1; sector <= 6; sector++){
    			int sector_index = sector -1;

    			DetectorShape2D  shape = new DetectorShape2D();
    			//shape.createTrapXY(30, 20, 80+layer_index*20);
    			shape.getDescriptor().setType(DetectorType.FTOF1A);
    			shape.getDescriptor().setSectorLayerComponent(sector, layer, 0);
    			shape.createBarXY(18, 80 + layer_index*20);
    			//shape.getShapePath().rotateZ(Math.toRadians((sector_index*60.0)+180.0));
    			shape.getShapePath().translateXYZ(120+20*layer_index, 0, 0);
    			shape.getShapePath().rotateZ(Math.toRadians((sector_index*60.0)+180.0));
    			shape.setColor(180, 180, 255);
    			
    			viewHv.addShape(shape);
    			viewTw.addShape(shape);
    			viewVeff.addShape(shape);

    		}
    	}
        viewHv.addDetectorListener(hv);
        hv.getView().getDetectorView().addDetectorLayer(viewHv);
        viewTw.addDetectorListener(tw);
        tw.getView().getDetectorView().addDetectorLayer(viewTw);
        viewVeff.addDetectorListener(veff);
        veff.getView().getDetectorView().addDetectorLayer(viewVeff);

    }
        
    public void processFile(String inputFile) {
        // my file with Haiyun's data turned into evio events
    	// use DataProvider for this
    	//String file = "/home/louise/coatjava/FtofInputFile_panel1a1bS6_from_root_file1.evio";
    	
    	// Cole's file - use DataProviderRaw
    	//String file = "/home/louise/FTOF_calib_rewrite/input_files/sector2_000251_mode7.evio.0";
    	
    	String file = inputFile;
    	
        EvioSource reader = new EvioSource();
        reader.open(file);
        System.out.println(reader.getSize());
        
        EventDecoder decoder = new EventDecoder();
        
        int maxEvents = 0;
        int eventNum = 0;
        while(reader.hasEvent()&&(eventNum<maxEvents||maxEvents==0)){
        	EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
        	processEvent(event, decoder);
        	eventNum++;
        }

        hv.analyze();
        tw.analyze();
        veff.analyze();

    }
    
	public void processEvent(EvioDataEvent event, EventDecoder decoder){
		
		List<TOFPaddle> list = DataProvider.getPaddleList(event, decoder);
		
		hv.process(list);
		tw.process(list);
		veff.process(list);
	}
    
    
    public static void main(String[] args){
    	
    	
    	// Select the input file
    	String inputFile = null;
    	JFileChooser fc = new JFileChooser();
    	fc.setDialogTitle("Choose input evio file for FTOF calibration");
    	fc.setCurrentDirectory(new File("/home/louise/FTOF_calib_rewrite/input_files"));
        int returnValue = fc.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			inputFile = fc.getSelectedFile().getAbsolutePath();
		}  
		else {
			return;
		}

    	JFrame frame = new JFrame("FTOF Calibration");
        frame.setSize(1200, 700);

        TOFCalibration calib = new TOFCalibration(inputFile);
        
        // Create a tabbed pane
     	JTabbedPane	tabbedPane = new JTabbedPane();
 		tabbedPane.addTab( "High Voltage", hv.getView());
 		tabbedPane.addTab( "Timewalk", tw.getView());
 		tabbedPane.addTab( "Effective Velocity", veff.getView());
 		
        frame.add(tabbedPane);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
   }

}