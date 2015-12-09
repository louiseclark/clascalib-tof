/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import org.jlab.clas.detector.DetectorDescriptor;

/**
 *
 * @author gavalian
 */
public class TOFPaddle {
    
    private DetectorDescriptor desc = new DetectorDescriptor();
    
    private int ADCL = 0;
    private int ADCR = 0;
    private int TDCL = 0;
    private int TDCR = 0;
    
    public TOFPaddle(int sector, int layer, int paddle){
        this.desc.setSectorLayerComponent(sector, layer, paddle);
    }
    
    public TOFPaddle(int sector, int layer, int paddle,
            int adcL, int adcR, int tdcL, int tdcR){
        this.desc.setSectorLayerComponent(sector, layer, paddle);
        this.ADCL = adcL;
        this.ADCR = adcR;
        this.TDCL = tdcL;
        this.TDCR = tdcR;
    }
    
    public final void setData(int adcL, int adcR, int tdcL, int tdcR){
        this.ADCL = adcL;
        this.ADCR = adcR;
        this.TDCL = tdcL;
        this.TDCR = tdcR;
    }
    
    public double geometricMean(){
        return Math.sqrt(ADCL*ADCR);
    }
    
    public double logRatio(){
    	return Math.log((double)ADCR/(double)ADCL);
    }
    
    public boolean isValidLogRatio() {
    	// only if geometric mean is over a minimum
		// only if both TDCs are non-zero - otherwise ADCs are equal and log ratio is always 0
    	//return (this.geometricMean() > 500.0) && (TDCL != 0) && (TDCR != 0);
    	return (this.geometricMean() > 500.0) && (ADCR != ADCL);
    }
    
    public DetectorDescriptor getDescriptor(){ return this.desc;}
    
    public String toString() {
    	return "S " + desc.getSector() + " L " + desc.getLayer() + " C " + desc.getComponent() +
    		   " ADCR " + ADCR +
    		   " ADCL " + ADCL +
    		   " TDCR " + TDCR +
    		   " TDCL " + TDCL +
    		   " Geometric Mean " + geometricMean() +
    		   " Log ratio " + logRatio();
    }
    
    
}
