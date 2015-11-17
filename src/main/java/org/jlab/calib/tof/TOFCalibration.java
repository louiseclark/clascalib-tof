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
    
    private EmbeddedCanvas   canvas = new EmbeddedCanvas();
    
    private CalibrationPane  calibPane = new CalibrationPane();
    
    private ConstantsTable   constantsTable = null;
    private ConstantsTablePanel   constantsTablePanel = null;
    private TreeMap<Integer,H1D>  histograms = new TreeMap<Integer,H1D>();

    private TOFGeometricMean gm = new TOFGeometricMean();
    
    private int[]	NUM_PADDLES = {23,62,5};
        
    public TOFCalibration(){
        this.initDetector();
        this.init();
    }
    
    public JPanel getView(){ return this.calibPane;}
    
    public void init(){
        this.calibPane.getCanvasPane().add(canvas);
        this.constantsTable = new ConstantsTable(DetectorType.FTOF,
                new String[]{"Peak Mean","Mean Error","Peak Sigma","Sigma Error"});
        
//        for(int sector = 0; sector < 6; sector++){
//            for(int layer = 0; layer < 3; layer++){
//            	for (int paddle = 0; paddle < NUM_PADDLES[layer]; paddle++) {
//            		this.constantsTable.addEntry(sector, layer, paddle);
//            	}
//            }
//        }
        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);
        
        JButton buttonFit = new JButton("Fit");
        buttonFit.addActionListener(this);
        
        JButton buttonViewAll = new JButton("View all");
        buttonViewAll.addActionListener(this);
        
        this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonViewAll);
        
        gm.init();
        processFile(gm);
        H1D h = gm.getH1D(5,0,0);
        F1D f = gm.getF1D(5,0,0);

        canvas.draw(h);
        canvas.draw(f, "same");

        gm.fillTable(5, constantsTable);
        
    }
    
    public void initDetector(){
        
        DetectorShapeView2D view = new DetectorShapeView2D("FTOF");
        for(int sector = 0; sector < 6; sector++){
            for(int region = 0; region < 23; region++){
                double arcStart = 40 + region*60;
                double arcEnd   = 40 + (region+1)*60;
                double midAngle = sector*60;
                double rotation = Math.toRadians(midAngle);
                DetectorShape2D  shape = new DetectorShape2D(DetectorType.FTOF,sector,region,0);
                shape.createArc(arcStart, arcEnd, midAngle-25, midAngle+25);
                if(region%2==0){
                    shape.setColor(180, 180, 255);
                } else {
                    shape.setColor(180, 255, 180);
                }
                view.addShape(shape);
            }
        }
        view.addDetectorListener(this);
        this.calibPane.getDetectorView().addDetectorLayer(view);
    }
    /**
     * This method comes from detector listener interface.
     * @param dd 
     */
    public void detectorSelected(DetectorDescriptor dd) {
        System.out.println(" DETECTOR SELECTED " + dd.toString());
        
        int sector = 5; //dd.getSector();
        int layer = 0; //dd.getLayer();
        int paddle = dd.getLayer();
        
        H1D h = gm.getH1D(sector, layer, paddle);
        F1D f = gm.getF1D(sector, layer, paddle);

        canvas.draw(h);
        canvas.draw(f, "same");
        
    }

    public void update(DetectorShape2D dsd) {
    }
    
    public void entrySelected(int i, int i1, int i2) {
        System.out.println(" ENTRY SELECTED FROM TABLE = " + i + i1 + i2);
        H1D h = gm.getH1D(i,i1,i2);
        F1D f = gm.getF1D(i,i1,i2);
        
        canvas.draw(h);
        canvas.draw(f, "same");
        
    }
    
    public static void processFile(TOFGeometricMean gm) {
        String file = "/home/louise/workspace/FtofInputFile_panel1a1bS6_from_root_file2.evio";
        EvioSource reader = new EvioSource();
        reader.open(file);
        System.out.println(reader.getSize());

        int maxEvents = 0;
        int eventNum = 0;
        while(reader.hasEvent()&&(eventNum<maxEvents||maxEvents==0)){
        	EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
        	gm.processEvent(event);
        	eventNum++;
        }

        gm.analyze();

    }
    public void actionPerformed(ActionEvent e) {
        System.out.println("ACTION PERFORMED : " + e.getActionCommand());
        if(e.getActionCommand().compareTo("Fit")==0){
        	
        	System.out.println("---> Louise I think you want me to fit something.");
        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();
        	int paddle = constantsTablePanel.getSelected().getComponent();
	    	
        	gm.customFit(sector, layer, paddle);
	    	F1D f = gm.getF1D(sector, layer, paddle);
	    	H1D h = gm.getH1D(sector, layer, paddle);
	    	
	    	constantsTable.getEntry(sector, layer, paddle).setData(0, Math.round(f.getParameter(1)));
			constantsTable.getEntry(sector, layer, paddle).setData(1, Math.round(f.getParameter(2)));
			constantsTablePanel.repaint();
	    	canvas.draw(h);
	    	canvas.draw(f, "same");
            
        }
        else if (e.getActionCommand().compareTo("View all")==0){
        	System.out.println("---> Louise I think you want me to process something.");
        	int sector = constantsTablePanel.getSelected().getSector();
        	int layer = constantsTablePanel.getSelected().getLayer();

        	JFrame viewAllFrame = new JFrame();
        	viewAllFrame.add(gm.showFits(sector, layer));
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