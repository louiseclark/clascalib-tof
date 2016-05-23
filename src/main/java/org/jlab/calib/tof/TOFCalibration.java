/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioSource;
import org.root.histogram.H1D;

/**
 *
 * @author louiseclark
 * 
 */
public class TOFCalibration {
	
	public List<TOFPaddlePair>     eventList = new ArrayList<TOFPaddlePair>();
        
    private TOFHighVoltage 			hv = new TOFHighVoltage();
    private TOFLeftRight				lr = new TOFLeftRight();
    private TOFTimeWalk 				tw = new TOFTimeWalk();
    private TOFEffectiveVelocity		veff = new TOFEffectiveVelocity();
    private TOFAtten					atten = new TOFAtten();
    private TOFPaddle2Paddle			p2p = new TOFPaddle2Paddle();
        
    public final static int[]		NUM_PADDLES = {23,62,5};
    public final String[]	LAYER_NAME = {"FTOF1A","FTOF1B","FTOF2"};
    
    public TOFCalibration(String inputFile){
        this.initDetector();
        this.init(inputFile);
    }
        
    public void init(String inputFile){
        
        hv.init();
        lr.init();
        tw.init();
        veff.init();
        atten.init();
        p2p.init();
        
        processFile(inputFile);

        hv.initDisplay();
        lr.initDisplay();
        tw.initDisplay();
        veff.initDisplay();
        atten.initDisplay();
        p2p.initDisplay();
        
    }
    
    public void initDetector(){
        
    	for (int layer = 1; layer <= 3; layer++) {
    		
    		int layer_index = layer-1;
    		
    		DetectorShapeView2D viewHv = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		DetectorShapeView2D viewLr = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		DetectorShapeView2D viewTw = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		DetectorShapeView2D viewVeff = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		DetectorShapeView2D viewAtten = new DetectorShapeView2D(LAYER_NAME[layer_index]);
    		DetectorShapeView2D viewP2P = new DetectorShapeView2D(LAYER_NAME[layer_index]);

    		
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
                    viewLr.addShape(shape);
                    viewTw.addShape(shape);
                    viewVeff.addShape(shape);
                    viewAtten.addShape(shape);
                    viewP2P.addShape(shape);

        		}
    		}
    		
            viewHv.addDetectorListener(hv);
            hv.getView().getDetectorView().addDetectorLayer(viewHv);
            viewLr.addDetectorListener(lr);
            lr.getView().getDetectorView().addDetectorLayer(viewLr);
            viewTw.addDetectorListener(tw);
            tw.getView().getDetectorView().addDetectorLayer(viewTw);
            viewVeff.addDetectorListener(veff);
            veff.getView().getDetectorView().addDetectorLayer(viewVeff);
            viewAtten.addDetectorListener(atten);
            atten.getView().getDetectorView().addDetectorLayer(viewAtten);
            viewP2P.addDetectorListener(p2p);
            p2p.getView().getDetectorView().addDetectorLayer(viewP2P);
    	}

    	DetectorShapeView2D viewHv = new DetectorShapeView2D("Summary");
    	DetectorShapeView2D viewLr = new DetectorShapeView2D("Summary");
    	DetectorShapeView2D viewTw = new DetectorShapeView2D("Summary");
    	DetectorShapeView2D viewVeff = new DetectorShapeView2D("Summary");
    	DetectorShapeView2D viewAtten = new DetectorShapeView2D("Summary");
    	DetectorShapeView2D viewP2P = new DetectorShapeView2D("Summary");
    	
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
    			viewLr.addShape(shape);
    			viewTw.addShape(shape);
    			viewVeff.addShape(shape);
    			viewAtten.addShape(shape);
    			viewP2P.addShape(shape);

    		}
    	}
        viewHv.addDetectorListener(hv);
        hv.getView().getDetectorView().addDetectorLayer(viewHv);
        viewLr.addDetectorListener(lr);
        lr.getView().getDetectorView().addDetectorLayer(viewLr);
        viewTw.addDetectorListener(tw);
        tw.getView().getDetectorView().addDetectorLayer(viewTw);
        viewVeff.addDetectorListener(veff);
        veff.getView().getDetectorView().addDetectorLayer(viewVeff);
        viewAtten.addDetectorListener(atten);
        atten.getView().getDetectorView().addDetectorLayer(viewAtten);
        viewP2P.addDetectorListener(p2p);
        p2p.getView().getDetectorView().addDetectorLayer(viewP2P);

    }
        
    public void processFile(String inputFile) {
    	
    	String file = inputFile;
    	
    	if (inputFile.endsWith("evio")) {
    	
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
    	}
    	else {
    		
    		processTextFile(inputFile);
    	}
	        
        hv.analyze();
        lr.analyze();
        tw.analyze();
        veff.analyze();
        atten.analyze();
        p2p.analyze(eventList);

    }
    
    private void processTextFile(String inputFile) {
    	
		// store list of paddle pairs for P2P step
    	
    	String line = null;
    	int maxLines=0;   
    	
    	System.out.println("Opening text file");
    	
    	try { 
			
            // Open the file
            FileReader fileReader = 
                new FileReader(inputFile);

            // Always wrap FileReader in BufferedReader
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);            

            // Read each line of the file up to a maximum count
            int lineNum=0;
            line = bufferedReader.readLine();
            line = bufferedReader.readLine(); // skip the header line
            
            while ((maxLines==0 || lineNum<maxLines) && (line != null)) {

            	//                    0       1             2                 3                    4                 5
            	// Each line contains RF_time target_center electron_TOF_time electron_flight_time electron_vertex_z electron_paddle_index 
            	//											pion_TOF_time pion_flight_time pion_vertex_z pion_paddle_index
            	//                                          6             7                8             9
            	
            	if (line.contains("inf")) {
            		line = bufferedReader.readLine();
                	lineNum++;
            		continue;
            	}
            	
            	String[] lineValues;
            	lineValues = line.split(" ");
            	
            	int electronSector = Integer.parseInt(lineValues[5])/100;
            	int electronPaddle = Integer.parseInt(lineValues[5])%100;
            	double RFTime = Double.parseDouble(lineValues[0]);
            	double electronTOFTime = Double.parseDouble(lineValues[2]);
            	double electronFlightTime = Double.parseDouble(lineValues[3]);
            	double electronVertexZ = Double.parseDouble(lineValues[4]);
            	
            	int pionSector = Integer.parseInt(lineValues[9])/100;
            	int pionPaddle = Integer.parseInt(lineValues[9])%100;
            	double pionTOFTime = Double.parseDouble(lineValues[6]);
            	double pionFlightTime = Double.parseDouble(lineValues[7]);
            	double pionVertexZ = Double.parseDouble(lineValues[8]);
            	
            	TOFPaddle  electronTOFPaddle = new TOFPaddle(electronSector, 2, electronPaddle);
            	electronTOFPaddle.RF_TIME = RFTime;
            	electronTOFPaddle.TOF_TIME = electronTOFTime;
            	electronTOFPaddle.FLIGHT_TIME = electronFlightTime;
            	electronTOFPaddle.VERTEX_Z = electronVertexZ;
            	
            	TOFPaddle  pionTOFPaddle = new TOFPaddle(pionSector, 2, pionPaddle);
            	pionTOFPaddle.RF_TIME = RFTime;
            	pionTOFPaddle.TOF_TIME = pionTOFTime;
            	pionTOFPaddle.FLIGHT_TIME = pionFlightTime;
            	pionTOFPaddle.VERTEX_Z = pionVertexZ;
            	
            	TOFPaddlePair paddlePair = new TOFPaddlePair();
            	paddlePair.electronPaddle = electronTOFPaddle;
            	paddlePair.pionPaddle = pionTOFPaddle;
            	
                eventList.add(paddlePair);
            	
            	line = bufferedReader.readLine();
            	lineNum++;
            }    


            bufferedReader.close();            
        }
		catch(FileNotFoundException ex) {
			ex.printStackTrace();
            System.out.println(
                "Unable to open file '" + 
                inputFile + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + inputFile + "'");                   
            // Or we could just do this: 
            // ex.printStackTrace();
        }		
    	
    }
    
	public void processEvent(EvioDataEvent event, EventDecoder decoder){
		
		// get the paddle list for this event to pass to calibration steps that can function with 1 event at a time
		List<TOFPaddle> list = DataProvider.getPaddleList(event, decoder);
		
		hv.process(list);
		lr.process(list);
		tw.process(list);
		veff.process(list);
		atten.process(list);
		
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
 		tabbedPane.addTab( "High Voltage", calib.hv.getView());
 		tabbedPane.addTab( "Left Right", calib.lr.getView());
 		tabbedPane.addTab( "Timewalk", calib.tw.getView());
 		tabbedPane.addTab( "Effective Velocity", calib.veff.getView());
 		tabbedPane.addTab( "Attenuation Length", calib.atten.getView());
 		tabbedPane.addTab( "Paddle to paddle", calib.p2p.getView());
 		
        frame.add(tabbedPane);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
   }

}