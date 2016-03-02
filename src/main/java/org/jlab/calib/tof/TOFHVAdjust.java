package org.jlab.calib.tof;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jlab.clas.detector.ConstantsTable;

public class TOFHVAdjust {

	public TOFHVAdjust() {
		// TODO Auto-generated constructor stub
	}

	public String processFile(TOFHighVoltage hv, ConstantsTable hvTable, String fileName, int sector) {
		
		String line = null;
		String outputFileName = nextFileName(sector);
		
		try { 

			// Open the input file
			FileReader fileReader = 
					new FileReader(fileName);
			BufferedReader bufferedReader = 
					new BufferedReader(fileReader);
			
			// Open the output file
			File outputFile = new File(outputFileName);
			FileWriter outputFw = new FileWriter(outputFile.getAbsoluteFile());
			BufferedWriter outputBw = new BufferedWriter(outputFw);
			
			line = bufferedReader.readLine();
			int paddle = 1;
			int layer = 1;
			String pmt = "LEFT";
			while (line != null) {
                
				//System.out.println("layer "+layer+" paddle "+paddle+" "+pmt);
				//System.out.println("Input "+line);
                String[] lineValues;
                lineValues = line.split(" ");
                
                double origVoltage = Double.parseDouble(lineValues[2]) * (-1.0);
                if (origVoltage==0.0) {
                	//System.out.println("Skipping");
                	line = bufferedReader.readLine();
                	continue;
                }
                
                double newHV = hv.newHV(sector, layer, paddle, origVoltage, pmt);
                
                //System.out.println("Writing "+lineValues[0]+" "+lineValues[1]+" "+newHV);
                outputBw.write(lineValues[0]+" "+lineValues[1]+" "+(int) Math.round(newHV * (-1.0))+".");
				outputBw.newLine();
                
                // Put entry in the display table
                hvTable.addEntry(sector, layer, paddle);
                if (pmt=="LEFT") {
                	hvTable.getEntry(sector, layer, paddle).setData(0, 0);
                }
                else {
                	hvTable.getEntry(sector, layer, paddle).setData(0, 1);                	
				}
                hvTable.getEntry(sector, layer, paddle).setData(1, (int) Math.round(origVoltage));
    			hvTable.getEntry(sector, layer, paddle).setData(2, (int) Math.round(newHV));
                
                
				if (layer==1 && paddle==23) {
					// move on to 1b
					layer=2;
					paddle=1;
				}
				else if (layer==2 && paddle==62) {
					// move on to 2
					layer=3;
					paddle=1;
				}
				else if (layer==3 && paddle==5 && pmt=="LEFT") {
					// back to 1a but change to right pmt
					layer=1;
					paddle=1;
					pmt="RIGHT";
				}
				else if (layer==3 && paddle==5 && pmt=="RIGHT") {
					// finish 
					break;
				}
				else {
					paddle++;
				}
                
				//System.out.println("NEXT layer "+layer+" paddle "+paddle+" "+pmt);
                line = bufferedReader.readLine();
			}
		
            bufferedReader.close();    
    		outputBw.close();
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
            ex.printStackTrace();
		}
		
		return outputFileName;
		
	}
	
	public String nextFileName(int sector) {
		
		// Get the next file name
		Date today = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String todayString = dateFormat.format(today);
		String filePrefix = "FTOF_HVADJUST_"+sector+"_"+todayString;
		int newFileNum = 0;

		File dir = new File(".");
		File[] filesList = dir.listFiles();

		for (File file : filesList) {
			if (file.isFile()) {
				String fileName = file.getName();
				if (fileName.matches(filePrefix+"[.]\\d+[.]txt")) {
					String fileNumString = fileName.substring(fileName.indexOf('.')+1,fileName.lastIndexOf('.'));
					int fileNum = Integer.parseInt(fileNumString);
					if (fileNum >= newFileNum) newFileNum = fileNum+1;

				}
			}
		}

		return filePrefix+"."+newFileNum+".txt";
	}
	
}
