package org.jlab.calib.tof;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.jlab.evio.clas12.EvioDataBank;
import org.jlab.evio.clas12.EvioDataEvent;
import org.jlab.evio.clas12.EvioDataSync;
import org.jlab.evio.clas12.EvioFactory;

public class TWTextToEvio {

	public TWTextToEvio() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String fileName = "/home/louise/local/ftof/data/s1p10.dat";
		String line = null;

		boolean success = (new File
		         ("TWInputFile.0.evio")).delete();
		
		EvioDataSync writer = new EvioDataSync();
		writer.open("TWInputFile.evio");
		
        int maxLines=0;        	
		
		try { 
			
            // Open the file
            FileReader fileReader = 
                new FileReader(fileName);

            // Always wrap FileReader in BufferedReader
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);            

            // Read each line of the file up to a maximum count
            int lineNum=0;
            line = bufferedReader.readLine();
            line = bufferedReader.readLine(); // skip the header line
            
            EvioDataEvent  event = EvioFactory.createEvioEvent();
            while ((maxLines==0 || lineNum<maxLines) && (line != null)) {

            	// Each line contains sector, paddle, Adc L, Adc R, Tdc L, Tdc R, x, y, z
            	String[] lineValues;
            	lineValues = line.split(" ");

            	int sector = Integer.parseInt(lineValues[0]);
            	int paddle = Integer.parseInt(lineValues[1]);
            	int adcLeft = Integer.parseInt(lineValues[2]) -440; // subtract the pedestal for S1P10
            	int adcRight = Integer.parseInt(lineValues[3]) -410;
            	int tdcLeft = Integer.parseInt(lineValues[4]);
            	int tdcRight = Integer.parseInt(lineValues[5]);
            	float xpos = Float.parseFloat(lineValues[6]);
            	float ypos = Float.parseFloat(lineValues[7]);

            	event = EvioFactory.createEvioEvent();


            	EvioDataBank   bankFTOF1A = EvioFactory.createEvioBank  ("FTOF1A::dgtz", 1);
            	EvioDataBank   bankFTOFRec = EvioFactory.createEvioBank("FTOFRec::ftofhits", 1);

            	bankFTOF1A.setInt("sector", 0, sector);
            	bankFTOF1A.setInt("paddle", 0, paddle);
            	bankFTOF1A.setInt("ADCL",   0, adcLeft);
            	bankFTOF1A.setInt("ADCR",   0, adcRight);
            	bankFTOF1A.setInt("TDCL",	0, tdcLeft);
            	bankFTOF1A.setInt("TDCR",   0, tdcRight);
            	
            	bankFTOFRec.setFloat("x", 0, xpos);
            	bankFTOFRec.setFloat("y", 0, ypos);

            	event.appendBanks(bankFTOF1A);
            	event.appendBanks(bankFTOFRec);

            	writer.writeEvent(event);
            	
            	line = bufferedReader.readLine();
            	lineNum++;
            }    


            bufferedReader.close();            
        }
		catch(FileNotFoundException ex) {
			ex.printStackTrace();
            System.out.println(
                "Unable to open file '" + 
                fileName + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + fileName + "'");                   
            // Or we could just do this: 
            // ex.printStackTrace();
        }		
		
		writer.close();

	}	

}
