/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JDialog;
import org.jlab.clas.detector.DetectorDescriptor;
import org.jlab.evio.clas12.EvioDataEvent;
import org.root.func.F1D;
import org.root.histogram.H1D;

/**
 *
 * @author gavalian
 */
public class TOFGeometricMean {
    
    TreeMap<Integer,H1D> container = new TreeMap<Integer,H1D>();
    TreeMap<Integer,F1D> functions = new TreeMap<Integer,F1D>();
            
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
        for(int sector = 1; sector <= 6; sector++){
            for(int paddle = 1; paddle <= 23; paddle++){
                desc.setSectorLayerComponent(sector, 0,paddle);
                container.put(desc.getHashCode(), new H1D("H1A",200,0.0,5000.0));
            }
        }
    }
    
    
    
    public void customFit(int sector, int layer, int paddle){
        JDialog dialog = new JDialog();
        H1D h = this.getH1D(sector, layer, paddle);
        F1D f = this.getF1D(sector, layer, paddle);        
        TOFCustomFitPanel panel = new TOFCustomFitPanel(h,f);
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    public H1D getH1D(int sector, int layer, int paddle){
        return this.container.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
    }
    public F1D getF1D(int sector, int layer, int paddle){
        return this.functions.get(DetectorDescriptor.generateHashCode(sector, layer, paddle));
    }
    public void show(){
        for(Map.Entry<Integer,H1D> item : this.container.entrySet()){
            System.out.println(item.getKey() + "  -->  " + item.getValue().getMean());
        }
    }
}
