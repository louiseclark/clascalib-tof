/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jlab.clas.detector.ConstantsTable;
import org.jlab.clas.detector.ConstantsTablePanel;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas.detector.IConstantsTableListener;
import org.jlab.clas12.calib.CalibrationPane;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clas12.detector.FADCBasicFitter;
import org.jlab.clasrec.main.DetectorMonitoring;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioSource;
import org.root.func.F1D;
import org.root.group.TBrowser;
import org.root.histogram.GraphErrors;
import org.root.histogram.H1D;
import org.root.pad.EmbeddedCanvas;
import org.root.pad.TCanvas;

/**
 *
 * @author gavalian
 */
public class TOFCalTimeWalk implements IDetectorListener,IConstantsTableListener,ActionListener {
    
    private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
    
    private CalibrationPane  		calibPane = new CalibrationPane();
    
    private ConstantsTable   		constantsTable = null;
    private ConstantsTablePanel   	constantsTablePanel = null;

    private static TOFTimeWalk tw = new TOFTimeWalk();
    
	public final int GEOMEAN = 0;
	public final int LOGRATIO = 1;    
    
    public static final int[]		NUM_PADDLES = {23,62,5};
    public static final String[]	LAYER_NAME = {"FTOF1A","FTOF1B","FTOF2B"};
    
    private static String inputFile;
        
    
    public TOFCalTimeWalk(){
        this.initDetector();
        this.init();
    }
    
    public JPanel getView(){ return this.calibPane;}
    
    public void init(){
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"Lambda left","Order left", "Lambda right", "Order right"});

        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);
        
        JButton buttonFit = new JButton("Fit");
        buttonFit.addActionListener(this);
        
        JButton buttonViewAll = new JButton("View all");
        buttonViewAll.addActionListener(this);
                
        JButton buttonWrite = new JButton("Write to file");
        buttonWrite.addActionListener(this);
        
        this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonViewAll);
        this.calibPane.getBottonPane().add(buttonWrite);
        
        tw.init();
        
        processFile(tw);
        // Display sector 1 initially
        tw.drawComponent(2, 1, 1, canvas);
        
        // Until I can delete rows from the table will just add all sectors
        for (int sector=1; sector<=6; sector++) {
        //for (int sector=2; sector<=2; sector++) {
        	for (int layer=1; layer<=3; layer++) {
        	//for (int layer=1; layer<=2; layer++) {
        		tw.fillTable(sector, layer, constantsTable);
        	}
        }
        
    }
    
    public void initDetector(){
        
    	for (int layer = 1; layer <= 3; layer++) {
    		int layer_index = layer-1;
    		DetectorShapeView2D view = new DetectorShapeView2D(LAYER_NAME[layer_index]);
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
                    view.addShape(shape);

        		}
    		}
            view.addDetectorListener(this);
            this.calibPane.getDetectorView().addDetectorLayer(view);
    	}
    }
    /**
     * This method comes from detector listener interface.
     * @param dd 
     */
    public void detectorSelected(DetectorDescriptor dd) {
        
        int sector = dd.getSector();
        int layer =  dd.getLayer();
        int paddle = dd.getComponent();

        tw.drawComponent(sector, layer, paddle, canvas);

        
        // If the sector or layer has changed then redraw the table
//        if (sector != Integer.parseInt((String)constantsTable.getValueAt(0, 1)) ||
//        	layer != Integer.parseInt((String)constantsTable.getValueAt(0, 2))) {
//        	System.out.println("Refilling table with sector " + sector + " layer " + layer );
//        	hv.fillTable(sector, layer, constantsTable);
//        	constantsTable.fireTableDataChanged();
//        }
        
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

    	tw.drawComponent(sector, layer, paddle, canvas);
        
    }
    
    public static void processFile(TOFTimeWalk tw) {
        // Haiyun's text file
    	// use DataProvider for this
    	// 
    	// original file "/home/louise/local/ftof/data/s1p10.dat";
    	// converted to evio by ...
    	
    	// Cole's file - use DataProviderRaw
    	//String file = "/home/louise/FTOF_calib_rewrite/input_files/sector2_000251_mode7.evio.0";
    	String file = inputFile;
    	
        EvioSource reader = new EvioSource();
        reader.open(file);
        System.out.println(reader.getSize());
        
        int maxEvents = 0;
        int eventNum = 0;
        while(reader.hasEvent()&&(eventNum<maxEvents||maxEvents==0)){
        	EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
        	tw.processEvent(event);
        	eventNum++;
        }

        tw.analyze();

    }
    public void actionPerformed(ActionEvent e) {
        System.out.println("ACTION PERFORMED : " + e.getActionCommand());
        if(e.getActionCommand().compareTo("Fit")==0){
        	
        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();
        	int paddle = constantsTablePanel.getSelected().getComponent();
	    	
        	tw.customFit(sector, layer, paddle);
	    	F1D f = tw.getF1D(sector, layer, paddle)[GEOMEAN];
	    	
	    	constantsTable.getEntry(sector, layer, paddle).setData(0, Math.round(f.getParameter(1)));
	    	constantsTable.getEntry(sector, layer, paddle).setData(1, Double.parseDouble(new DecimalFormat("0.0").format(f.parameter(1).error())));
			//constantsTable.fireTableDataChanged();
			
			tw.drawComponent(sector, layer, paddle, canvas);
			calibPane.repaint();
			
            
        }
        else if (e.getActionCommand().compareTo("View all")==0){

        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();

        	JFrame viewAllFrame = new JFrame();
        	viewAllFrame.add(tw.showFits(sector, layer));
        	viewAllFrame.pack();
        	viewAllFrame.setVisible(true);
        	viewAllFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        	
        	tw.viewFits(sector, layer, GEOMEAN);
        	tw.viewFits(sector, layer, LOGRATIO);
        	
        }
        else if (e.getActionCommand().compareTo("Write to file")==0) {
        	
        	String outputFileName = tw.writeTable(constantsTable);
			JOptionPane.showMessageDialog(new JPanel(),"Calibration values written to "+outputFileName);
        }
    }
    

    
    public static void main(String[] args){
    	
    	
    	// Select the input file
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
        TOFCalTimeWalk calib = new TOFCalTimeWalk();
        
        frame.add(calib.getView());
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
   }

}