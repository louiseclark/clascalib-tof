/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.calib.sample;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jlab.clas.detector.ConstantsTable;
import org.jlab.clas.detector.ConstantsTablePanel;
import org.jlab.clas.detector.DetectorCollection;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas.detector.IConstantsTableListener;
import org.jlab.clas12.calib.CalibrationPane;
import org.jlab.clas12.calib.DetectorDatasetPane;
import org.jlab.clas12.calib.DetectorShape2D;
import org.jlab.clas12.calib.DetectorShapeView2D;
import org.jlab.clas12.calib.IDetectorListener;
import org.root.attr.TStyle;
import org.root.histogram.H1D;
import org.root.pad.EmbeddedCanvas;
import org.root.pad.TBookCanvas;

/**
 *
 * @author gavalian
 */
public class SampleCalibration implements IDetectorListener,IConstantsTableListener,ActionListener {
    
    private EmbeddedCanvas   canvas = new EmbeddedCanvas();    
    private CalibrationPane  calibPane = new CalibrationPane();
    private DetectorCollection<H1D>   tdcHistograms = new DetectorCollection<H1D>();
    
    private ConstantsTable   constantsTable = null;
    private ConstantsTablePanel   constantsTablePanel = null;
    private TreeMap<Integer,H1D>  histograms = new TreeMap<Integer,H1D>();
    
    public SampleCalibration(){
        this.initDetector();
        this.init();
        
        TStyle.setFrameFillColor(235, 255, 245);
        TStyle.setFrameBackgroundColor(245, 255, 245);
    }
    
    public JPanel getView(){ return this.calibPane;}
    
    public void init(){
        this.calibPane.getCanvasPane().add(canvas);
        
        
        this.constantsTable = new ConstantsTable(DetectorType.LTCC,
                new String[]{"Peak Mean","Mean Error","Peak Sigma","Sigma Error"});
        
        
        
        this.tdcHistograms.setName("LTCC_TDC");
        for(int sector = 0; sector < 6; sector++){
            for(int region = 0; region < 8; region++){
                this.constantsTable.addEntry(sector, region, 0);
                this.tdcHistograms.add(sector, region, 0, 
                        new H1D("TDC_" + sector + "_"+region,
                                "TDC (SECTOR " + sector + " REGION " + region + ")", 
                                200,0.0,4000.0));
            }
        }
        this.constantsTablePanel = new ConstantsTablePanel(this.constantsTable);
        this.constantsTablePanel.addListener(this);        
        this.calibPane.getTablePane().add(this.constantsTablePanel);
        
        JButton buttonFit = new JButton("Fit");
        buttonFit.addActionListener(this);
        
        JButton buttonProc = new JButton("Process");
        buttonProc.addActionListener(this);
        
        JButton buttonTDC = new JButton("Show TDC");
        buttonTDC.addActionListener(this);
        
        
        this.calibPane.getBottonPane().add(buttonFit);
        this.calibPane.getBottonPane().add(buttonProc);
        this.calibPane.getBottonPane().add(buttonTDC);

    }
    
    public void initDetector(){
        
        DetectorShapeView2D view = new DetectorShapeView2D("LTCC");
        for(int sector = 0; sector < 6; sector++){
            for(int region = 0; region < 8; region++){
                for(int half = 0; half < 2; half++){
                    double arcStart = 280 + region*60;
                    double arcEnd   = 280 + (region+1)*60;
                    double midAngle = sector*60;
                    double rotation = Math.toRadians(midAngle);
                    DetectorShape2D  shape = new DetectorShape2D(DetectorType.LTCC,sector,region,half);
                    if(half==0){
                        shape.createArc(arcStart, arcEnd, midAngle-25, midAngle);
                    } else {
                        shape.createArc(arcStart, arcEnd, midAngle, midAngle+25);
                    }
                    if(region%2==0){
                        shape.setColor(180, 180, 255);
                    } else {
                        shape.setColor(180, 255, 180);
                    }
                    view.addShape(shape);
                }
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
    }

    public void update(DetectorShape2D dsd) {
        
    }
    
    public void entrySelected(int i, int i1, int i2) {
        System.out.println(" ENTRY SELECTED FROM TABLE = " + i + i1 + i2);
    }
    
   

    public void actionPerformed(ActionEvent e) {
        System.out.println("ACTION PERFORMED : " + e.getActionCommand());
        if(e.getActionCommand().compareTo("Fit")==0){
            System.out.println("---> I think you want me to fit something.");
        }
        
        if(e.getActionCommand().compareTo("Show TDC")==0){
            DetectorDatasetPane.showDialog(this.tdcHistograms);            
        }
        
    }
        
     public static void main(String[] args){
        JFrame frame = new JFrame();
        frame.setSize(1200, 700);
        SampleCalibration calib = new SampleCalibration();
        frame.add(calib.getView());
        frame.pack();
        frame.setVisible(true);
    }
}
