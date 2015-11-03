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
        if(event.hasBank("FTOF1A::dgtz")==true){
            EvioDataBank bank = (EvioDataBank) event.getBank("FTOF1A::dgtz");
            for(int loop = 0; loop < bank.rows(); loop++){
                TOFPaddle  paddle = new TOFPaddle(
                        bank.getInt("sector", loop),
                        0,
                        bank.getInt("paddle", loop),
                        bank.getInt("ADCL", loop),
                        bank.getInt("ADCR", loop),
                        bank.getInt("TDCL", loop),
                        bank.getInt("TDCR", loop)
                        
                );
                paddleList.add(paddle);
            }
        }
        return paddleList;
    }
}