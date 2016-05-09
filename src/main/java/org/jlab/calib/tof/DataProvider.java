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
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;

/**
 *
 * @author gavalian
 */
public class DataProvider {
    
	public static List<TOFPaddle> getPaddleList(EvioDataEvent event, EventDecoder decoder) {
        List<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();

        // is the event digitized?
        if (event.hasBank("FTOF1A::dgtz")||event.hasBank("FTOF1B::dgtz")||event.hasBank("FTOF2B::dgtz")) {
        	paddleList = getPaddleListDgtz(event);
        }
        else {
        	paddleList = getPaddleListRaw(event, decoder);
        }
        
        return paddleList;
	}
	
	public static List<TOFPaddle> getPaddleListDgtz(EvioDataEvent event){
        ArrayList<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();
        
        float xpos = 0;
        float ypos = 0;
        if (event.hasBank("FTOFRec::ftofhits")) {
        	EvioDataBank recBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
        	xpos = recBank.getFloat("x",0);
        	ypos = recBank.getFloat("y",0);
        }
        
        if(event.hasBank("FTOF1A::dgtz")==true){
            EvioDataBank bank = (EvioDataBank) event.getBank("FTOF1A::dgtz");
            for(int loop = 0; loop < bank.rows(); loop++){
                TOFPaddle  paddle = new TOFPaddle(
                        bank.getInt("sector", loop),
                        1,
                        bank.getInt("paddle", loop),
                        bank.getInt("ADCL", loop),
                        bank.getInt("ADCR", loop),
                        bank.getInt("TDCL", loop),
                        bank.getInt("TDCR", loop),
                        xpos,
                        ypos
                );
                paddleList.add(paddle);
            }
        }
        if(event.hasBank("FTOF1B::dgtz")==true){
            EvioDataBank bank = (EvioDataBank) event.getBank("FTOF1B::dgtz");
            for(int loop = 0; loop < bank.rows(); loop++){
                TOFPaddle  paddle = new TOFPaddle(
                        bank.getInt("sector", loop),
                        2,
                        bank.getInt("paddle", loop),
                        bank.getInt("ADCL", loop),
                        bank.getInt("ADCR", loop),
                        bank.getInt("TDCL", loop),
                        bank.getInt("TDCR", loop),
                        xpos,
                        ypos
                        
                );
                paddleList.add(paddle);
            }
        }
        if(event.hasBank("FTOF2B::dgtz")==true){
            EvioDataBank bank = (EvioDataBank) event.getBank("FTOF2B::dgtz");
            for(int loop = 0; loop < bank.rows(); loop++){
                TOFPaddle  paddle = new TOFPaddle(
                        bank.getInt("sector", loop),
                        3,
                        bank.getInt("paddle", loop),
                        bank.getInt("ADCL", loop),
                        bank.getInt("ADCR", loop),
                        bank.getInt("TDCL", loop),
                        bank.getInt("TDCR", loop),
                        xpos,
                        ypos
                        
                );
                paddleList.add(paddle);
            }
        }
        return paddleList;
    }
    
    public static List<TOFPaddle> getPaddleListRaw(EvioDataEvent event, EventDecoder decoder){
    	
        ArrayList<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();
        decoder.decode(event);   
        List<DetectorCounter> banks1A= decoder.getDetectorCounters(DetectorType.FTOF1A);
        List<DetectorCounter> banks1B = decoder.getDetectorCounters(DetectorType.FTOF1B);
        List<DetectorCounter> banks2 = decoder.getDetectorCounters(DetectorType.FTOF2);
                
        for(DetectorCounter bank : banks1A){
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
        					1, //layer,
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
        
        for(DetectorCounter bank : banks1B){
	         if(bank.getChannels().size()==2){
	             //if(bank.isMultiHit()==false){
	                 // isMultihit() method returns false when
	                 //  (bank.getChannels().get(0).getADC().size()==1&&
	                 //  bank.getChannels().get(1).getADC().size()==1&&
	                 //  bank.getChannels().get(0).getTDC().size()==1&&
	                 //  bank.getChannels().get(1).getTDC().size()==1)
	                 // it checks if each channel has one ADC and one TDC.
	        	 if (bank.getChannels().get(0).getADC().size()==1&&
		                   bank.getChannels().get(1).getADC().size()==1&&
		                   bank.getChannels().get(0).getTDC().size()>0&&
		                   bank.getChannels().get(1).getTDC().size()>0) {	            	 
	            	 
	                 int adcL = bank.getChannels().get(0).getADC().get(0);
	                 int adcR = bank.getChannels().get(1).getADC().get(0);
	                 int tdcL = bank.getChannels().get(0).getTDC().get(0);
	                 int tdcR = bank.getChannels().get(1).getTDC().get(0);
	                 int sector = bank.getDescriptor().getSector();
	                 int layer  = bank.getDescriptor().getLayer();
	                 int paddle = bank.getDescriptor().getComponent();
	                 
	                 TOFPaddle  tofPaddle = new TOFPaddle(
	                         sector,
	                         2, //layer,
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

        for(DetectorCounter bank : banks2){
	         if(bank.getChannels().size()==2){
	             //if(bank.isMultiHit()==false){
	                 // isMultihit() method returns false when
	                 //  (bank.getChannels().get(0).getADC().size()==1&&
	                 //  bank.getChannels().get(1).getADC().size()==1&&
	                 //  bank.getChannels().get(0).getTDC().size()==1&&
	                 //  bank.getChannels().get(1).getTDC().size()==1)
	                 // it checks if each channel has one ADC and one TDC.
	        	 if (bank.getChannels().get(0).getADC().size()==1&&
		                   bank.getChannels().get(1).getADC().size()==1&&
		                   bank.getChannels().get(0).getTDC().size()>0&&
		                   bank.getChannels().get(1).getTDC().size()>0) {	            	 
	            	 
	                 int adcL = bank.getChannels().get(0).getADC().get(0);
	                 int adcR = bank.getChannels().get(1).getADC().get(0);
	                 int tdcL = bank.getChannels().get(0).getTDC().get(0);
	                 int tdcR = bank.getChannels().get(1).getTDC().get(0);
	                 int sector = bank.getDescriptor().getSector();
	                 int layer  = bank.getDescriptor().getLayer();
	                 int paddle = bank.getDescriptor().getComponent();
	                 
	                 TOFPaddle  tofPaddle = new TOFPaddle(
	                         sector,
	                         3, //layer,
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

        return paddleList;
    }

}
