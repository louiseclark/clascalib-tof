package org.jlab.calib.tof;

import java.util.List;
import java.util.TreeMap;

import org.jlab.clas.detector.DetectorType;
import org.jlab.clas12.detector.DetectorCounter;
import org.jlab.clas12.detector.EventDecoder;
import org.jlab.clas12.detector.FADCBasicFitter;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioSource;
import org.root.histogram.H1D;
import org.root.histogram.H2D;
import org.root.pad.TGCanvas;

public class RawDataReader {

	public RawDataReader() {
		// TODO Auto-generated constructor stub
	}

	public static void read1A() {
		
		String input = "/home/louise/sector2_000251_mode7.evio.0";
		EvioSource  reader = new EvioSource();
		reader.open(input);
		EventDecoder decoder = new EventDecoder();
		decoder.addFitter(DetectorType.FTOF1A, new FADCBasicFitter(30,35,70,75));
		
		TreeMap<Integer,H1D> hADCLMap = new TreeMap<Integer,H1D>();
		TreeMap<Integer,H1D> hADCRMap = new TreeMap<Integer,H1D>();
		TreeMap<Integer,H1D> hADCGMMap = new TreeMap<Integer,H1D>();
		TreeMap<Integer,H1D> hTDCDIFFMap = new TreeMap<Integer,H1D>();
		
		for (int i=1; i<=23; i++) {
			H1D hADCL = new H1D("hADCL",200,0.0,5000.0);
			hADCL.setTitle("FTOF 1A Paddle "+i+" ADC L");			
			hADCLMap.put(i, hADCL);
			
			H1D hADCR = new H1D("hADCR",200,0.0,5000.0);
			hADCR.setTitle("FTOF 1A Paddle "+i+" ADC R");
			hADCRMap.put(i, hADCR);
			
			H1D hADCGM = new H1D("hADCGM",200,0.0,5000.0);
			hADCGM.setTitle("FTOF 1A Paddle "+i+" Geometric Mean");
			hADCGMMap.put(i, hADCGM);
						
			H1D hTDCDIFF = new H1D("hTDCDIFF",100,-2000.0,2000.0);
			hTDCDIFF.setTitle("FTOF 1A Paddle "+i+" TDC L - TDC R");
			hTDCDIFFMap.put(i, hTDCDIFF);
		}
		
		int icounter = 0;
		while(reader.hasEvent()){
		    icounter++;
		     EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
		     decoder.decode(event);
		     List<DetectorCounter> banks = decoder.getDetectorCounters(DetectorType.FTOF1A);
		     
		     for(DetectorCounter bank : banks){
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
		                 
		                 hADCLMap.get(paddle).fill(adcL);
		                 hADCRMap.get(paddle).fill(adcR);
			             hADCGMMap.get(paddle).fill(Math.sqrt((double) adcL* (double) adcR));
		                 
		                 hTDCDIFFMap.get(paddle).fill(tdcL-tdcR);
			             	                 
		             }
		         }
		     }
		}
		TGCanvas c1 = new TGCanvas("c1","FTOF1A",1200,800,2,2);
		
		for (int i=1; i<=23; i++) {
			
			c1.cd(0);
			c1.draw(hADCLMap.get(i));
			c1.cd(1);
			c1.draw(hADCRMap.get(i));
			c1.cd(2);
			c1.draw(hADCGMMap.get(i));
			c1.cd(3);
			c1.draw(hTDCDIFFMap.get(i));
			
			c1.save("/home/louise/FTOF1A_p"+i+".jpg");

		}
		
	}

	public static void read1B() {
		
		String input = "/home/louise/sector2_000251_mode7.evio.0";
		EvioSource  reader = new EvioSource();
		reader.open(input);
		EventDecoder decoder = new EventDecoder();
		decoder.addFitter(DetectorType.FTOF1B, new FADCBasicFitter(30,35,70,75));
		
		TreeMap<Integer,H1D> hADCLMap = new TreeMap<Integer,H1D>();
		TreeMap<Integer,H1D> hADCRMap = new TreeMap<Integer,H1D>();
		TreeMap<Integer,H1D> hADCGMMap = new TreeMap<Integer,H1D>();
		TreeMap<Integer,H1D> hTDCDIFFMap = new TreeMap<Integer,H1D>();
		
		for (int i=1; i<=62; i++) {
			H1D hADCL = new H1D("hADCL",200,0.0,5000.0);
			hADCL.setTitle("FTOF 1B Paddle "+i+" ADC L");			
			hADCLMap.put(i, hADCL);
			
			H1D hADCR = new H1D("hADCR",200,0.0,5000.0);
			hADCR.setTitle("FTOF 1B Paddle "+i+" ADC R");
			hADCRMap.put(i, hADCR);
			
			H1D hADCGM = new H1D("hADCGM",200,0.0,5000.0);
			hADCGM.setTitle("FTOF 1B Paddle "+i+" Geometric Mean");
			hADCGMMap.put(i, hADCGM);
						
			H1D hTDCDIFF = new H1D("hTDCDIFF",100,-2000.0,2000.0);
			hTDCDIFF.setTitle("FTOF 1B Paddle "+i+" TDC L - TDC R");
			hTDCDIFFMap.put(i, hTDCDIFF);
		}
		
		int icounter = 0;
		while(reader.hasEvent()){
		    icounter++;
		     EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
		     decoder.decode(event);
		     List<DetectorCounter> banks = decoder.getDetectorCounters(DetectorType.FTOF1B);
		     
		     for(DetectorCounter bank : banks){
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
		                 
		                 hADCLMap.get(paddle).fill(adcL);
		                 hADCRMap.get(paddle).fill(adcR);
			             hADCGMMap.get(paddle).fill(Math.sqrt((double) adcL* (double) adcR));
		                 
		                 hTDCDIFFMap.get(paddle).fill(tdcL-tdcR);
			             	                 
		             }
		         }
		     }
		}

		for (int i=1; i<=62; i++) {
			
			TGCanvas c1 = new TGCanvas("c1","FTOF1B",1200,800,2,2);
			c1.cd(0);
			c1.draw(hADCLMap.get(i));
			c1.cd(1);
			c1.draw(hADCRMap.get(i));
			c1.cd(2);
			c1.draw(hADCGMMap.get(i));
			c1.cd(3);
			c1.draw(hTDCDIFFMap.get(i));
			
			c1.save("/home/louise/FTOF1B_p"+i+".jpg");

		}
		
	}
	
	public static void main(String[] args) {
		
		read1A();
		//read1B();
		
	}
	
}
