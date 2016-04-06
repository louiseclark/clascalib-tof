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
public class TOFCalibration implements IDetectorListener,IConstantsTableListener,ActionListener {
    
    private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
    
    private CalibrationPane  		calibPane = new CalibrationPane();
    
    private ConstantsTable   		constantsTable = null;
    private ConstantsTablePanel   	constantsTablePanel = null;

    private static TOFHighVoltage hv = new TOFHighVoltage();
    
	public final int GEOMEAN = 0;
	public final int LOGRATIO = 1;    
    
    public static final int[]		NUM_PADDLES = {23,62,5};
    public static final String[]	LAYER_NAME = {"FTOF1A","FTOF1B","FTOF2B"};
    public static final int			ALLOWED_MIP_DIFF = 50;
    
    private static String inputFile;
        
    
    public TOFCalibration(){
        this.initDetector();
        this.init();
    }
    
    public JPanel getView(){ return this.calibPane;}
    
    public void init(){
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"Geometric Mean Peak","Uncertainty", "Log Ratio Centroid", "Uncertainty"});

        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);
        
        JButton buttonFit = new JButton("Fit");
        buttonFit.addActionListener(this);
        
        JButton buttonViewAll = new JButton("View all");
        buttonViewAll.addActionListener(this);
        
        JButton buttonAdjust = new JButton("Adjust HV");
        buttonAdjust.addActionListener(this);
        
        JButton buttonWrite = new JButton("Write to file");
        buttonWrite.addActionListener(this);
        
        this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonViewAll);
        this.calibPane.getBottonPane().add(buttonAdjust);
        this.calibPane.getBottonPane().add(buttonWrite);
        
        hv.init();
        
        processFile(hv);
        // Display sector 1 initially
        hv.drawComponent(2, 1, 1, canvas);
        
        // Until I can delete rows from the table will just add all sectors
        for (int sector=1; sector<=6; sector++) {
        //for (int sector=2; sector<=2; sector++) {
        	for (int layer=1; layer<=3; layer++) {
        	//for (int layer=1; layer<=2; layer++) {
        		hv.fillTable(sector, layer, constantsTable);
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

        hv.drawComponent(sector, layer, paddle, canvas);

        
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
    	
    	double mipChannel = hv.getMipChannel(dsd.getDescriptor().getSector(), 
				   dsd.getDescriptor().getLayer(), 
				   dsd.getDescriptor().getComponent());
    	int layer_index = dsd.getDescriptor().getLayer()-1;
    	double expectedMipChannel = hv.EXPECTED_MIP_CHANNEL[layer_index];
    	
        if (mipChannel < expectedMipChannel - ALLOWED_MIP_DIFF ||
        	mipChannel > expectedMipChannel + ALLOWED_MIP_DIFF) {
        	
        	dsd.setColor(255, 153, 51); // amber
        	
        }
        else if (dsd.getDescriptor().getComponent()%2==0) {
            dsd.setColor(180, 255,180);
        } else {
            dsd.setColor(180, 180, 255);
        }
    	
    }
    
    public void entrySelected(int sector, int layer, int paddle) {

    	hv.drawComponent(sector, layer, paddle, canvas);
        
    }
    
    public static void processFile(TOFHighVoltage hv) {
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
        	hv.processEvent(event, decoder);
        	eventNum++;
        }

        hv.analyze();

    }
    public void actionPerformed(ActionEvent e) {
        System.out.println("ACTION PERFORMED : " + e.getActionCommand());
        if(e.getActionCommand().compareTo("Fit")==0){
        	
        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();
        	int paddle = constantsTablePanel.getSelected().getComponent();
	    	
        	hv.customFit(sector, layer, paddle);
	    	F1D f = hv.getF1D(sector, layer, paddle)[GEOMEAN];
	    	
	    	constantsTable.getEntry(sector, layer, paddle).setData(0, Math.round(f.getParameter(1)));
	    	constantsTable.getEntry(sector, layer, paddle).setData(1, Double.parseDouble(new DecimalFormat("0.0").format(f.parameter(1).error())));
			//constantsTable.fireTableDataChanged();
			
			hv.drawComponent(sector, layer, paddle, canvas);
			calibPane.repaint();
			
            
        }
        else if (e.getActionCommand().compareTo("View all")==0){

        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();

        	JFrame viewAllFrame = new JFrame();
        	viewAllFrame.add(hv.showFits(sector, layer));
        	viewAllFrame.pack();
        	viewAllFrame.setVisible(true);
        	viewAllFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        	
        	hv.viewFits(sector, layer, GEOMEAN);
        	hv.viewFits(sector, layer, LOGRATIO);
        	
        }
        else if(e.getActionCommand().compareTo("Adjust HV")==0){
        	
        	JFrame hvFrame = new JFrame("Adjust HV");
        	hvFrame.add(new TOFHVAdjustPanel(hv));
        	hvFrame.pack();
        	hvFrame.setVisible(true);
        	hvFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        	
        }
        else if (e.getActionCommand().compareTo("Write to file")==0) {
        	
        	String outputFileName = hv.writeTable(constantsTable);
			JOptionPane.showMessageDialog(new JPanel(),"Calibration values written to "+outputFileName);
        }
    }
    
    public static void testHVCalcs() {
    	
		
		ConstantsTable hvTestTable = 
			new ConstantsTable(DetectorType.FTOF,new String[]{"Current HV", 			// 0
															  "Legacy - MIP ADC", 		// 1
															  "Legacy - Log Ratio", 	// 2
															  "Legacy - New HV", 		// 3
															  "Legacy - delta HV",		// 4
															  "Java - MIP ADC", 		// 5
															  "Java - Log Ratio", 		// 6
															  "Java - New HV", 			// 7
															  "Java - delta HV", 		// 8
															  "Java - New HV (from legacy values)"});	// 9
    	
		String line = null;
		
		Double[] ftof1aGains = {0.0, 836.797,
				804.636,
				769.838,
				815.984,
				779.533,
				580.099,
				811.422,
				819.381,
				792.712,
				791.489,
				809.975,
				813.837,
				819.556,
				805.765,
				807.679,
				793.505,
				780.588,
				798.16,
				811.889,
				837.246,
				793.167,
				791.128,
				854.98};
		
		Double[] ftof1aCentroids = {0.0, 0.021,
				-0.03,
				0.022,
				0.058,
				-0.011,
				0.616,
				-0.006,
				0.034,
				-0.063,
				-0.058,
				-0.014,
				0.055,
				-0.026,
				-0.081,
				0.028,
				-0.017,
				0.087,
				-0.021,
				-0.103,
				0.092,
				0.003,
				0.016,
				0.078};


		// Graphs
		
		double[] paddleNumbers = new double[23];
		double[] legacyMipAdcs = new double[23];
		double[] javaMipAdcs = new double[23];
		double[] diffMipAdcs = new double[23];
		double[] diffCentroids1a = new double[23];
		double[] diffHVLeft1a = new double[23];
		double[] diffHVRight1a = new double[23];
		double[] diffHVLeft1aTest = new double[23];
		double[] diffHVRight1aTest = new double[23];
		
		double[] paddle1bNumbers = new double[62];
		double[] diffMipAdcs1b = new double [62];
		double[] diffCentroids1b = new double[62];
		double[] diffHVLeft1b = new double[62];
		double[] diffHVRight1b = new double[62];
		double[] diffHVLeft1bTest = new double[62];
		double[] diffHVRight1bTest = new double[62];
		
		try { 

			// Open the file with Dan's calculated new values
			FileReader newHVFile1a = 
					new FileReader("/home/louise/FTOF_calib_rewrite/legacy_files_from_dan/hv1a_253-s2_out.txt");
			BufferedReader newHVReader1a = 
					new BufferedReader(newHVFile1a);
			
			line = newHVReader1a.readLine();
			
			while (line != null) {
                
                String[] lineValues;
                lineValues = line.split("  ");
                
                System.out.println(lineValues[0]);
                System.out.println(lineValues[1]);
                System.out.println(lineValues[2]);
                System.out.println(lineValues[0].substring(0,1));
                
                int sector = Integer.parseInt(lineValues[0].substring(0,1));
                int layer = 1;
                int paddle = Integer.parseInt(lineValues[0].substring(2,4));
                
                if (sector != 2) {
                	line = newHVReader1a.readLine();
                	continue;
                }
                
                double danNewVoltageLeft = Double.parseDouble(lineValues[1]);
                double danNewVoltageRight = Double.parseDouble(lineValues[2]);

                double javaMip = hv.getMipChannel(sector, layer, paddle);
                double javaCentroid = hv.getConst(sector, layer, paddle)[hv.LR_CENTROID];
                
                // Put entry in the display table
                // Left
                hvTestTable.addEntry(sector, layer, paddle);
                double origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_LEFT];
                hvTestTable.getEntry(sector, layer, paddle).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle).setData(1, ftof1aGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle).setData(2, ftof1aCentroids[paddle]);
                
    			hvTestTable.getEntry(sector, layer, paddle).setData(3, Math.round(danNewVoltageLeft));
    			hvTestTable.getEntry(sector, layer, paddle).setData(4, Math.round(danNewVoltageLeft-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
    			
    			double newHV = hv.newHV(sector, layer, paddle, origVoltage, "LEFT");
    			hvTestTable.getEntry(sector, layer, paddle).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle).setData(8, Math.round(newHV-origVoltage));
    			
    			double newHVTest = hv.newHVTest(layer, origVoltage, ftof1aGains[paddle], ftof1aCentroids[paddle], "LEFT");
    			hvTestTable.getEntry(sector, layer, paddle).setData(9, Math.round(newHVTest));
    			
    			// Put values in graphs
    			paddleNumbers[paddle-1] = paddle;
    			legacyMipAdcs[paddle-1] = ftof1aGains[paddle];
    			javaMipAdcs[paddle-1] = javaMip;
    			diffMipAdcs[paddle-1] = ftof1aGains[paddle] - javaMip;
    			diffCentroids1a[paddle-1] = ftof1aCentroids[paddle] - javaCentroid;
    			diffHVLeft1a[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHV);
    			diffHVLeft1aTest[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHVTest);
    			
    			// Right
                hvTestTable.addEntry(sector, layer, paddle+100);

                origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_RIGHT];
                hvTestTable.getEntry(sector, layer, paddle+100).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(1, ftof1aGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle+100).setData(2, ftof1aCentroids[paddle]);
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(3, Math.round(danNewVoltageRight));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(4, Math.round(danNewVoltageRight-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle+100).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
                
                
                newHV = hv.newHV(sector, layer, paddle, origVoltage, "RIGHT");
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(8, Math.round(newHV-origVoltage));
    			
    			newHVTest = hv.newHVTest(layer, origVoltage, ftof1aGains[paddle], ftof1aCentroids[paddle], "RIGHT");
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(9, Math.round(newHVTest));

    			diffHVRight1a[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHV);
    			diffHVRight1aTest[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHVTest);
                
				//System.out.println("NEXT layer "+layer+" paddle "+paddle+" "+pmt);
    			line = newHVReader1a.readLine();
			}
		
			newHVReader1a.close();  
        }
		catch(FileNotFoundException ex) {
			ex.printStackTrace();            
        }
        catch(IOException ex) {
            ex.printStackTrace();
		}
		
		Double[] ftof1bGains = {0.0, 2009.85,
				1918.73,
				1960.45,
				2020.86,
				1964.91,
				1984.94,
				1982.24,
				1999.21,
				1932.86,
				1959.32,
				1985.62,
				1934.88,
				1934.19,
				2075.81,
				1998.83,
				1979.1,
				1979.04,
				1980.56,
				1947.85,
				1996.64,
				1950.79,
				1901.64,
				1945.84,
				1972.41,
				1904.63,
				1962.96,
				1957.97,
				1956.08,
				1953.08,
				1982.2,
				1926.68,
				1936.15,
				1959.74,
				1972.8,
				1897.41,
				1943.78,
				1958.66,
				1943.78,
				2002.05,
				1996.6,
				1948.06,
				2002.69,
				1894.05,
				1940.06,
				1979.89,
				1903.24,
				1935.04,
				1991.35,
				1995.52,
				1970.1,
				1942.66,
				1971.95,
				1961.81,
				1953.48,
				1976.38,
				1958.92,
				1947.77,
				1944.51,
				1962.48,
				1915.55,
				1862.19,
				1910.89
};
		
		Double[] ftof1bCentroids = {0.0, -0.014,
				0.127,
				-0.024,
				-0.05,
				0.002,
				-0.039,
				-0.013,
				-0.069,
				-0.015,
				0.008,
				0.009,
				0.026,
				0.008,
				0.033,
				-0.047,
				-0.043,
				0.035,
				0.033,
				0.009,
				0.078,
				-0.02,
				0.017,
				0.015,
				-0.04,
				-0.003,
				0.002,
				-0.018,
				-0.006,
				-0.021,
				-0.014,
				-0.019,
				-0.024,
				0.01,
				-0.018,
				0.008,
				0.001,
				0.023,
				0.001,
				-0.063,
				-0.007,
				-0.016,
				0.018,
				-0.018,
				0.019,
				-0.001,
				0.007,
				0.025,
				-0.057,
				0.03,
				-0.018,
				0.02,
				-0.015,
				0.033,
				0.011,
				0.066,
				0.008,
				0.017,
				-0.057,
				-0.041,
				0.02,
				0.061,
				0.035
};
		
		try { 

			// Open the file with Dan's calculated new values
			FileReader newHVFile1b = 
					new FileReader("/home/louise/FTOF_calib_rewrite/legacy_files_from_dan/hv1b_253-s2_out.txt");
			BufferedReader newHVReader1b = 
					new BufferedReader(newHVFile1b);
			
			line = newHVReader1b.readLine();
			
			while (line != null) {
                
                String[] lineValues;
                lineValues = line.split("  ");
                
                System.out.println(lineValues[0]);
                System.out.println(lineValues[1]);
                System.out.println(lineValues[2]);
                System.out.println(lineValues[0].substring(0,1));
                
                int sector = Integer.parseInt(lineValues[0].substring(0,1));
                int layer = 2;
                int paddle = Integer.parseInt(lineValues[0].substring(2,4));
                
                if (sector != 2) {
                	line = newHVReader1b.readLine();
                	continue;
                }
                
                double danNewVoltageLeft = Double.parseDouble(lineValues[1]);
                double danNewVoltageRight = Double.parseDouble(lineValues[2]);

                double javaMip = hv.getMipChannel(sector, layer, paddle);
                double javaCentroid = hv.getConst(sector, layer, paddle)[hv.LR_CENTROID];
                
                // Put entry in the display table
                // Left
                hvTestTable.addEntry(sector, layer, paddle);
                double origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_LEFT];
                hvTestTable.getEntry(sector, layer, paddle).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle).setData(1, ftof1bGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle).setData(2, ftof1bCentroids[paddle]);
                
    			hvTestTable.getEntry(sector, layer, paddle).setData(3, Math.round(danNewVoltageLeft));
    			hvTestTable.getEntry(sector, layer, paddle).setData(4, Math.round(danNewVoltageLeft-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
    			
    			double newHV = hv.newHV(sector, layer, paddle, origVoltage, "LEFT");
    			hvTestTable.getEntry(sector, layer, paddle).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle).setData(8, Math.round(newHV-origVoltage));
    			
    			double newHVTest = hv.newHVTest(layer, origVoltage, ftof1bGains[paddle], ftof1bCentroids[paddle], "LEFT");
    			hvTestTable.getEntry(sector, layer, paddle).setData(9, Math.round(newHVTest));
    			
    			// Put values in graphs
    			paddle1bNumbers[paddle-1] = paddle;
    			diffMipAdcs1b[paddle-1] = ftof1bGains[paddle] - javaMip;
    			diffCentroids1b[paddle-1] = ftof1bCentroids[paddle] - javaCentroid;
    			diffHVLeft1b[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHV);
    			diffHVLeft1bTest[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHVTest);

    			// Right
                hvTestTable.addEntry(sector, layer, paddle+100);

                origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_RIGHT];
                hvTestTable.getEntry(sector, layer, paddle+100).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(1, ftof1bGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle+100).setData(2, ftof1bCentroids[paddle]);
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(3, Math.round(danNewVoltageRight));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(4, Math.round(danNewVoltageRight-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle+100).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
                
                
                newHV = hv.newHV(sector, layer, paddle, origVoltage, "RIGHT");
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(8, Math.round(newHV-origVoltage));
    			
    			newHVTest = hv.newHVTest(layer, origVoltage, ftof1bGains[paddle], ftof1bCentroids[paddle], "RIGHT");
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(9, Math.round(newHVTest));

    			diffHVRight1b[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHV);
    			diffHVRight1bTest[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHVTest);
                
				//System.out.println("NEXT layer "+layer+" paddle "+paddle+" "+pmt);
    			line = newHVReader1b.readLine();
			}
		
			newHVReader1b.close();  
        }
		catch(FileNotFoundException ex) {
			ex.printStackTrace();            
        }
        catch(IOException ex) {
            ex.printStackTrace();
		}
		
		// Create graphs
		GraphErrors mip1aGraph = new GraphErrors("Panel 1a MIP ADC Channel (Legacy - Java)", paddleNumbers, diffMipAdcs);
		
		// None of these seem to make any difference to the graph
		//mip1aGraph.setTitle("Panel 1a MIP ADC Channel (Legacy - Java)");
		//mip1aGraph.setXTitle("Paddle Number");
		//mip1aGraph.setYTitle("MIP ADC Channel (Legacy - Java)");
		//adc1aLegacyMipGraph.setMarkerStyle(2);
		//adc1aLegacyMipGraph.setMarkerColor(3);
		//GraphErrors adc1aJavaMipGraph = new GraphErrors(paddleNumbers, javaMipAdcs);
		//adc1aJavaMipGraph.setMarkerStyle(2);
		//adc1aLegacyMipGraph.setMarkerColor(4);
		TCanvas c1 = new TCanvas("Panel 1a MIP ADC Channel (Legacy - Java)","Panel 1a MIP ADC Channel (Legacy - Java)",600,600);
		c1.setDefaultCloseOperation(c1.HIDE_ON_CLOSE);
		c1.draw(mip1aGraph);
		//c1.draw(adc1aJavaMipGraph,"same");
		
		GraphErrors mip1bGraph = new GraphErrors("Panel 1b MIP ADC Channel (Legacy - Java)", paddle1bNumbers, diffMipAdcs1b);
		TCanvas c2 = new TCanvas("Panel 1b MIP ADC Channel (Legacy - Java)","Panel 1b MIP ADC Channel (Legacy - Java)",600,600);
		c2.setDefaultCloseOperation(c2.HIDE_ON_CLOSE);
		c2.draw(mip1bGraph);
		
		GraphErrors cen1aGraph = new GraphErrors("Panel 1a LR Centroid (Legacy - Java)", paddleNumbers, diffCentroids1a);
		cen1aGraph.getDataSetRieman().setXTitle("Paddle");
		
		TCanvas c3 = new TCanvas("Panel 1a LR Centroid (Legacy - Java)","Panel 1a LR Centroid (Legacy - Java)",600,600);
		c3.setDefaultCloseOperation(c3.HIDE_ON_CLOSE);
		c3.draw(cen1aGraph);
		
		GraphErrors cen1bGraph = new GraphErrors("Panel 1b LR Centroid (Legacy - Java)", paddle1bNumbers, diffCentroids1b);
		TCanvas c4 = new TCanvas("Panel 1b LR Centroid (Legacy - Java)","Panel 1b LR Centroid (Legacy - Java)",600,600);
		c4.setDefaultCloseOperation(c4.HIDE_ON_CLOSE);
		c4.draw(cen1bGraph);
		
		GraphErrors hvL1aGraph = new GraphErrors("Panel 1a New HV Left (Legacy - Java)", paddleNumbers, diffHVLeft1a);
		TCanvas c5 = new TCanvas("Panel 1a New HV Left (Legacy - Java)","Panel 1a New HV Left (Legacy - Java)",600,600);
		c5.setDefaultCloseOperation(c5.HIDE_ON_CLOSE);
		c5.draw(hvL1aGraph);

		GraphErrors hvR1aGraph = new GraphErrors("Panel 1a New HV Right (Legacy - Java)", paddleNumbers, diffHVRight1a);
		TCanvas c6 = new TCanvas("Panel 1a New HV Right (Legacy - Java)","Panel 1a New HV Right (Legacy - Java)",600,600);
		c6.setDefaultCloseOperation(c6.HIDE_ON_CLOSE);
		c6.draw(hvR1aGraph);
		
		GraphErrors hvL1bGraph = new GraphErrors("Panel 1b New HV Left (Legacy - Java)", paddle1bNumbers, diffHVLeft1b);
		TCanvas c7 = new TCanvas("Panel 1b New HV Left (Legacy - Java)","Panel 1b New HV Left (Legacy - Java)",600,600);
		c7.setDefaultCloseOperation(c7.HIDE_ON_CLOSE);
		c7.draw(hvL1bGraph);

		GraphErrors hvR1bGraph = new GraphErrors("Panel 1b New HV Right (Legacy - Java)", paddle1bNumbers, diffHVRight1b);
		TCanvas c8 = new TCanvas("Panel 1b New HV Right (Legacy - Java)","Panel 1b New HV Right (Legacy - Java)",600,600);
		c8.setDefaultCloseOperation(c8.HIDE_ON_CLOSE);
		c8.draw(hvR1bGraph);

		GraphErrors hvL1aTestGraph = new GraphErrors("Panel 1a New HV Left (Legacy - Java test)", paddleNumbers, diffHVLeft1aTest);
		TCanvas c9 = new TCanvas("Panel 1a New HV Left (Legacy - Java test)","Panel 1a New HV Left (Legacy - Java test)",600,600);
		c9.setDefaultCloseOperation(c9.HIDE_ON_CLOSE);
		c9.draw(hvL1aTestGraph);

		GraphErrors hvR1aTestGraph = new GraphErrors("Panel 1a New HV Right (Legacy - Java test)", paddleNumbers, diffHVRight1aTest);
		TCanvas c10 = new TCanvas("Panel 1a New HV Right (Legacy - Java test)","Panel 1a New HV Right (Legacy - Java test)",600,600);
		c10.setDefaultCloseOperation(c10.HIDE_ON_CLOSE);
		c10.draw(hvR1aTestGraph);
		
		GraphErrors hvL1bTestGraph = new GraphErrors("Panel 1b New HV Left (Legacy - Java test)", paddle1bNumbers, diffHVLeft1bTest);
		TCanvas c11 = new TCanvas("Panel 1b New HV Left (Legacy - Java test)","Panel 1b New HV Left (Legacy - Java test)",600,600);
		c11.setDefaultCloseOperation(c11.HIDE_ON_CLOSE);
		c11.draw(hvL1bTestGraph);

		GraphErrors hvR1bTestGraph = new GraphErrors("Panel 1b New HV Right (Legacy - Java test)", paddle1bNumbers, diffHVRight1bTest);
		TCanvas c12 = new TCanvas("Panel 1b New HV Right (Legacy - Java test)","Panel 1b New HV Right (Legacy - Java test)",600,600);
		c12.setDefaultCloseOperation(c12.HIDE_ON_CLOSE);
		c12.draw(hvR1bTestGraph);
		
		// Display GUI
		
		JPanel testPanel = new JPanel();
    	testPanel.setLayout(new FlowLayout());
		
		ConstantsTablePanel tablePanel = new ConstantsTablePanel(hvTestTable);
		//JPanel tablePane = new JPanel(new BorderLayout());
		//tablePane.add(tablePanel);
		//testPanel.add(tablePane);
		testPanel.add(tablePanel);
		
    	JFrame frame = new JFrame("HV test");
        frame.setSize(1200, 700);
        
        //frame.add(testPanel);
        frame.add(tablePanel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
 
		
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
        TOFCalibration calib = new TOFCalibration();
        
        frame.add(calib.getView());
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
   }

}