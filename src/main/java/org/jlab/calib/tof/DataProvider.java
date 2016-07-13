/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.util.ArrayList;
import java.util.List;

import org.jlab.clas.detector.DetectorBankEntry;
import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.detector.DetectorChannelDecoder;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;

/**
 *
 * @author gavalian
 */
public class DataProvider {
	
	static boolean testMode = false;
    
	public static List<TOFPaddle> getPaddleList(EvioDataEvent event, EventDecoder decoder) {
        List<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();

        //System.out.println("Getting paddle list");

//        if (event.hasBank("FTOFRec::ftofhits")) {
//        	System.out.println("Found ftofhits bank");
//        	event.show();
//        	EvioDataBank testBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
//        	testBank.show();
//        }        
        
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
        
        
        // TEST
        if (testMode) {
        	System.out.println("New event");
        	//event.show();
        	if (event.hasBank("FTOF1A::dgtz")) {
        		EvioDataBank testBank = (EvioDataBank) event.getBank("FTOF1A::dgtz");
        		testBank.show();
        	}
        	else {
        		System.out.println("No FTOF1A bank");
        	}
        	if (event.hasBank("FTOF1B::dgtz")) {
        		EvioDataBank testBank = (EvioDataBank) event.getBank("FTOF1B::dgtz");
        		testBank.show();
        	}
        	else {
        		System.out.println("No FTOF1B bank");
        	}
        	if (event.hasBank("FTOF2B::dgtz")) {
        		EvioDataBank testBank = (EvioDataBank) event.getBank("FTOF2B::dgtz");
        		testBank.show();
        	}  
        	else {
        		System.out.println("No FTOF2B bank");
        	}
        	if (event.hasBank("FTOFRec::ftofhits")) {
        		EvioDataBank testBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
        		testBank.show();
        	}
        	else {
        		System.out.println("No FTOFRec bank");
        	}
        }
        
        String[] bankName = {"zero", "FTOF1A::dgtz", "FTOF1B::dgtz", "FTOF2B::dgtz"};
        
        if (event.hasBank("FTOFRec::ftofhits")==true) {
            EvioDataBank hitsBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
                        
            for(int hitIndex = 0; hitIndex < hitsBank.rows(); hitIndex++){
 
            	// Get corresponding dgtz bank
            	String dgtzBankName = bankName[hitsBank.getInt("panel_id", hitIndex)];
            	if (event.hasBank(dgtzBankName)) {
            		EvioDataBank dgtzBank = (EvioDataBank) event.getBank(dgtzBankName);
            		
            		for (int dgtzIndex = 0; dgtzIndex < dgtzBank.rows(); dgtzIndex++) {
            			
            			if (dgtzBank.getInt("sector",dgtzIndex)==hitsBank.getInt("sector",hitIndex)
            				&&
            				dgtzBank.getInt("paddle",dgtzIndex)==hitsBank.getInt("paddle_id",hitIndex)) {
            				
            				// found a match on sector and paddle
            				TOFPaddle  paddle = new TOFPaddle(
                        			dgtzBank.getInt("sector", dgtzIndex),
                        			hitsBank.getInt("panel_id", hitIndex),
                                    dgtzBank.getInt("paddle", dgtzIndex),
                                    dgtzBank.getInt("ADCL", dgtzIndex),
                                    dgtzBank.getInt("ADCR", dgtzIndex),
                                    dgtzBank.getInt("TDCL", dgtzIndex),
                                    dgtzBank.getInt("TDCR", dgtzIndex),
                                    hitsBank.getFloat("x", hitIndex),
                                    hitsBank.getFloat("y", hitIndex)
                            );
                            paddleList.add(paddle);
                            
                            if (testMode) {
                            	System.out.println("Creating paddle");
                            	System.out.println("Sector "+dgtzBank.getInt("sector", dgtzIndex)+
                            			" Layer "+hitsBank.getInt("panel_id", hitIndex)+
                            			" Component "+dgtzBank.getInt("paddle", dgtzIndex));
                            	System.out.println("ADCL "+dgtzBank.getInt("ADCL", dgtzIndex)+
                            			" ADCR "+dgtzBank.getInt("ADCR", dgtzIndex)+
                            			" TDCL "+dgtzBank.getInt("TDCL", dgtzIndex)+
                            			" TDCR "+dgtzBank.getInt("TDCR", dgtzIndex));
                            	System.out.println("x "+hitsBank.getFloat("x", hitIndex)+
                            			" y "+hitsBank.getFloat("y", hitIndex));
                            }

            				break;
            			}
            		}
            		
            	}
            	
            }
        }
        
        return paddleList;
	}
	
	public static List<TOFPaddle> oldgetPaddleListDgtz(EvioDataEvent event){
        ArrayList<TOFPaddle>  paddleList = new ArrayList<TOFPaddle>();
        
        // TEST
        System.out.println("New event");
        //event.show();
        if (event.hasBank("FTOF1A::dgtz")) {
        	EvioDataBank testBank = (EvioDataBank) event.getBank("FTOF1A::dgtz");
        	testBank.show();
        }
        else {
        	System.out.println("No FTOF1A bank");
        }
        if (event.hasBank("FTOF1B::dgtz")) {
        	EvioDataBank testBank = (EvioDataBank) event.getBank("FTOF1B::dgtz");
        	testBank.show();
        }
        else {
        	System.out.println("No FTOF1B bank");
        }
        if (event.hasBank("FTOF2B::dgtz")) {
        	EvioDataBank testBank = (EvioDataBank) event.getBank("FTOF2B::dgtz");
        	testBank.show();
        }  
        else {
        	System.out.println("No FTOF2B bank");
        }
        if (event.hasBank("FTOFRec::ftofhits")) {
        	EvioDataBank testBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
        	testBank.show();
        }
        else {
        	System.out.println("No FTOFRec bank");
        }
        
        // Old way of getting position from Haiyun's test data
        
//        float xpos = 0;
//        float ypos = 0;
//        if (event.hasBank("FTOFRec::ftofhits")) {
//        	EvioDataBank recBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
//        	xpos = recBank.getFloat("x",0);
//        	ypos = recBank.getFloat("y",0);
//        }
        
        if(event.hasBank("FTOF1A::dgtz")==true){
            EvioDataBank dgtzBank = (EvioDataBank) event.getBank("FTOF1A::dgtz");
            
            //System.out.println(event.toString());
            //event.show();
            //dgtzBank.show();
            
//            if (event.hasBank("FTOFRec::ftofhits")==true) {
//            	EvioDataBank recBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
//            	recBank.show();
//            }
//            else {
//            	System.out.println("No ftofhits bank");
//            }
            

            
            
            float xpos = 0;
            float ypos = 0;
            
            for(int loop = 0; loop < dgtzBank.rows(); loop++){
 
                if (event.hasBank("FTOFRec::ftofhits")==true) {
                	EvioDataBank hitsBank = (EvioDataBank) event.getBank("FTOFRec::ftofhits");
                	xpos = hitsBank.getFloat("x", loop);
                	ypos = hitsBank.getFloat("y", loop);
                }
                else {
                	xpos = 0;
                	ypos = 0;
                }
            	
            	TOFPaddle  paddle = new TOFPaddle(
            			dgtzBank.getInt("sector", loop),
                        1,
                        dgtzBank.getInt("paddle", loop),
                        dgtzBank.getInt("ADCL", loop),
                        dgtzBank.getInt("ADCR", loop),
                        dgtzBank.getInt("TDCL", loop),
                        dgtzBank.getInt("TDCR", loop),
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
        
        // TEST
//        System.out.println("Raw");
//        event.show();
        
        decoder.decode(event);  // orig method   
        //decoder.readDataEntries(event); // new method
        //List<DetectorBankEntry>  rawEntries =  decoder.getDataEntries(); // new method
        
        List<DetectorCounter> banks1A= decoder.getDetectorCounters(DetectorType.FTOF1A); // orig method
        List<DetectorCounter> banks1B = decoder.getDetectorCounters(DetectorType.FTOF1B);
        List<DetectorCounter> banks2 = decoder.getDetectorCounters(DetectorType.FTOF2);
                
        for(DetectorCounter bank : banks1A){  // orig method
        //for (DetectorBankEntry bank : rawEntries) { // new method
        	//translator.decode(rawEntries); // new method
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
