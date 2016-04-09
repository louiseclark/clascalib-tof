/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.util.ArrayList;
import java.util.List;
import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;

/**
 *
 * @author gavalian
 */
public class DataProvider {
    
    public static List<TOFPaddle> getPaddleList(EvioDataEvent event){
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
}
