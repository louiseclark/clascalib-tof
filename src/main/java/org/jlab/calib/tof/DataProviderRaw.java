/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.util.ArrayList;
import java.util.List;

import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.evio.clas12.EvioDataEvent;

/**
 *
 * @author gavalian
 */
public class DataProviderRaw {
    
    public static List<TOFPaddle> getPaddleList(EvioDataEvent event, EventDecoder decoder){
    	
        ArrayList<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();
        decoder.decode(event);   
        List<DetectorCounter>[] banks = null;
        List<DetectorCounter> banks1A= decoder.getDetectorCounters(DetectorType.FTOF1A);
        List<DetectorCounter> banks1B = decoder.getDetectorCounters(DetectorType.FTOF1B);
        List<DetectorCounter> banks2 = decoder.getDetectorCounters(DetectorType.FTOF2);
        
        banks[0] = banks1A;
        banks[1] = banks1B;
        banks[2] = banks2;
        
        for (int bankIndex=0; bankIndex<3; bankIndex++) {
        	for(DetectorCounter bank : banks[bankIndex]){
        		if(bank.getChannels().size()==2){
        			if(bank.isMultiHit()==false){
        				// isMultihit() method returns false when
        				//  (bank.getChannels().get(0).getADC().size()==1&&
        				//  bank.getChannels().get(1).getADC().size()==1&&
        				//  bank.getChannels().get(0).getTDC().size()==1&&
        				//  bank.getChannels().get(1).getTDC().size()==1)
        				// it checks if each channel has one ADC and one TDC.

        				int adcL = bank.getChannels().get(0).getADC().get(0);
        				int adcR = bank.getChannels().get(1).getADC().get(0);
        				int tdcL = bank.getChannels().get(0).getTDC().get(0);
        				int tdcR = bank.getChannels().get(1).getTDC().get(0);

        				int sector = bank.getDescriptor().getSector();
        				int layer  = bank.getDescriptor().getLayer();
        				int paddle = bank.getDescriptor().getComponent();

        				TOFPaddle  tofPaddle = new TOFPaddle(
        						sector,
        						bankIndex+1, //layer,
        						paddle,
        						adcL,
        						adcR,
        						tdcL,
        						tdcR

        						);
        				paddleList.add(tofPaddle);

        			}
        		}
        	}
    	}

        return paddleList;
    }
}
