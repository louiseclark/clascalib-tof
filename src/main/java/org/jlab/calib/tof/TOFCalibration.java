/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JFrame;
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
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioSource;
import org.root.func.F1D;
import org.root.histogram.H1D;
import org.root.pad.EmbeddedCanvas;

/**
 *
 * @author gavalian
 */
public class TOFCalibration implements IDetectorListener,IConstantsTableListener,ActionListener {
    
    private EmbeddedCanvas   		canvas = new EmbeddedCanvas();
    
    private CalibrationPane  		calibPane = new CalibrationPane();
    
    private ConstantsTable   		constantsTable = null;
    private ConstantsTablePanel   	constantsTablePanel = null;

    private TOFHighVoltage hv = new TOFHighVoltage();
    
    public static final int[]		NUM_PADDLES = {23,62,5};
    public static final String[]	LAYER_NAME = {"FTOF1A","FTOF1B","FTOF2B"};
        
    
    public TOFCalibration(){
        this.initDetector();
        this.init();
    }
    
    public JPanel getView(){ return this.calibPane;}
    
    public void init(){
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"Geometric Mean Peak","Log Ratio Mean"});

        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);
        
        JButton buttonFit = new JButton("Fit");
        buttonFit.addActionListener(this);
        
        JButton buttonViewAll = new JButton("View all");
        buttonViewAll.addActionListener(this);
        
        this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonViewAll);
        
        hv.init();
        processFile(hv);

        hv.drawComponent(5, 0, 0, canvas);
        hv.fillTable(5, 1, constantsTable);
        
    }
    
    public void initDetector(){
        
    	for (int layer = 0; layer < 3; layer++) {
    		DetectorShapeView2D view = new DetectorShapeView2D(LAYER_NAME[layer]);
    		for(int sector = 0; sector < 6; sector++){
        		for(int region = 0; region < NUM_PADDLES[layer]; region++){
        			double arcStart = 40 + region*60;
        			double arcEnd   = 40 + (region+1)*60;
        			double midAngle = sector*60;
        			double rotation = Math.toRadians(midAngle);
        			DetectorShape2D  shape = new DetectorShape2D(DetectorType.FTOF,sector,layer,region);
        			shape.createArc(arcStart, arcEnd, midAngle-25, midAngle+25);
        			if(region%2==0){
        				shape.setColor(180, 255, 180);
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
        
//        H1D h = hv.getH1D(sector, layer, paddle)[0];
//        F1D f = hv.getF1D(sector, layer, paddle)[0];
//
//        canvas.draw(h);
//        canvas.draw(f, "same");
        hv.drawComponent(sector, layer, paddle, canvas);
        
        // If the sector or layer has changed then redraw the table
        if (sector != Integer.parseInt((String)constantsTable.getValueAt(0, 1)) ||
        	layer != Integer.parseInt((String)constantsTable.getValueAt(0, 2))) {
        	System.out.println("Refilling table with sector " + sector + " layer " + layer );
        	hv.fillTable(sector, layer, constantsTable);
        	constantsTablePanel.repaint();
        }
        
    }

    public void update(DetectorShape2D dsd) {
    }
    
    public void entrySelected(int sector, int layer, int paddle) {

    	hv.drawComponent(sector, layer, paddle, canvas);
        
    }
    
    public static void processFile(TOFHighVoltage hv) {
        String file = "/home/louise/coatjava/FtofInputFile_panel1a1bS6_from_root_file1.evio";
        EvioSource reader = new EvioSource();
        reader.open(file);
        System.out.println(reader.getSize());

        int maxEvents = 0;
        int eventNum = 0;
        while(reader.hasEvent()&&(eventNum<maxEvents||maxEvents==0)){
        	EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
        	hv.processEvent(event);
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
	    	F1D f = hv.getF1D(sector, layer, paddle)[0];
	    	H1D h = hv.getH1D(sector, layer, paddle)[0];
	    	
	    	constantsTable.getEntry(sector, layer, paddle).setData(0, Math.round(f.getParameter(1)));
			constantsTablePanel.repaint();
			
			hv.drawComponent(sector, layer, paddle, canvas);
            
        }
        else if (e.getActionCommand().compareTo("View all")==0){

        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();

        	JFrame viewAllFrame = new JFrame();
        	viewAllFrame.add(hv.showFits(sector, layer));
        	viewAllFrame.pack();
        	viewAllFrame.setVisible(true);
        	viewAllFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }
    }
    
    public static void main(String[] args){
        JFrame frame = new JFrame();
        frame.setSize(1200, 700);
        TOFCalibration calib = new TOFCalibration();
        
        frame.add(calib.getView());
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
   }

}