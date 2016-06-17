package org.jlab.calib.tof;

import java.awt.FlowLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jlab.clas.detector.ConstantsTable;
import org.jlab.clas.detector.ConstantsTablePanel;
import org.jlab.clas.detector.DetectorType;
import org.root.histogram.GraphErrors;
import org.root.pad.TCanvas;

public class TOFTestHVCalcs {

    
    public static void testHVCalcs(TOFHighVoltage hv) {
    	
		ConstantsTable hvTestTable = 
			new ConstantsTable(DetectorType.FTOF,new String[]{"Current HV", 			// 0
															  "Legacy - MIP ADC", 		// 1
															  "Legacy - Log Ratio", 	// 2
															  "Legacy - New HV", 		// 3
															  "Legacy - delta HV",		// 4
															  "Java - MIP ADC", 		// 5
															  "Java - Log Ratio", 		// 6
															  "Java - New HV", 			// 7
															  "Java - delta HV", 		// 8
															  "Java - New HV (from legacy values)"});	// 9
    	
		String line = null;
		
		Double[] ftof1aGains = {0.0, 836.797,
				804.636,
				769.838,
				815.984,
				779.533,
				580.099,
				811.422,
				819.381,
				792.712,
				791.489,
				809.975,
				813.837,
				819.556,
				805.765,
				807.679,
				793.505,
				780.588,
				798.16,
				811.889,
				837.246,
				793.167,
				791.128,
				854.98};
		
		Double[] ftof1aCentroids = {0.0, 0.021,
				-0.03,
				0.022,
				0.058,
				-0.011,
				0.616,
				-0.006,
				0.034,
				-0.063,
				-0.058,
				-0.014,
				0.055,
				-0.026,
				-0.081,
				0.028,
				-0.017,
				0.087,
				-0.021,
				-0.103,
				0.092,
				0.003,
				0.016,
				0.078};


		// Graphs
		
		double[] paddleNumbers = new double[23];
		double[] legacyMipAdcs = new double[23];
		double[] javaMipAdcs = new double[23];
		double[] diffMipAdcs = new double[23];
		double[] diffCentroids1a = new double[23];
		double[] diffHVLeft1a = new double[23];
		double[] diffHVRight1a = new double[23];
		double[] diffHVLeft1aTest = new double[23];
		double[] diffHVRight1aTest = new double[23];
		
		double[] paddle1bNumbers = new double[62];
		double[] diffMipAdcs1b = new double [62];
		double[] diffCentroids1b = new double[62];
		double[] diffHVLeft1b = new double[62];
		double[] diffHVRight1b = new double[62];
		double[] diffHVLeft1bTest = new double[62];
		double[] diffHVRight1bTest = new double[62];
		
		try { 

			// Open the file with Dan's calculated new values
			FileReader newHVFile1a = 
					new FileReader("/home/louise/FTOF_calib_rewrite/legacy_files_from_dan/hv1a_253-s2_out.txt");
			BufferedReader newHVReader1a = 
					new BufferedReader(newHVFile1a);
			
			line = newHVReader1a.readLine();
			
			while (line != null) {
                
                String[] lineValues;
                lineValues = line.split("  ");
                
                System.out.println(lineValues[0]);
                System.out.println(lineValues[1]);
                System.out.println(lineValues[2]);
                System.out.println(lineValues[0].substring(0,1));
                
                int sector = Integer.parseInt(lineValues[0].substring(0,1));
                int layer = 1;
                int paddle = Integer.parseInt(lineValues[0].substring(2,4));
                
                if (sector != 2) {
                	line = newHVReader1a.readLine();
                	continue;
                }
                
                double danNewVoltageLeft = Double.parseDouble(lineValues[1]);
                double danNewVoltageRight = Double.parseDouble(lineValues[2]);

                double javaMip = hv.getMipChannel(sector, layer, paddle);
                double javaCentroid = hv.getConst(sector, layer, paddle)[hv.LR_CENTROID];
                
                // Put entry in the display table
                // Left
                hvTestTable.addEntry(sector, layer, paddle);
                double origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_LEFT];
                hvTestTable.getEntry(sector, layer, paddle).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle).setData(1, ftof1aGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle).setData(2, ftof1aCentroids[paddle]);
                
    			hvTestTable.getEntry(sector, layer, paddle).setData(3, Math.round(danNewVoltageLeft));
    			hvTestTable.getEntry(sector, layer, paddle).setData(4, Math.round(danNewVoltageLeft-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
    			
    			double newHV = hv.newHV(sector, layer, paddle, origVoltage, "LEFT");
    			hvTestTable.getEntry(sector, layer, paddle).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle).setData(8, Math.round(newHV-origVoltage));
    			
    			//double newHVTest = hv.newHVTest(layer, origVoltage, ftof1aGains[paddle], ftof1aCentroids[paddle], "LEFT");
    			//hvTestTable.getEntry(sector, layer, paddle).setData(9, Math.round(newHVTest));
    			
    			// Put values in graphs
    			paddleNumbers[paddle-1] = paddle;
    			legacyMipAdcs[paddle-1] = ftof1aGains[paddle];
    			javaMipAdcs[paddle-1] = javaMip;
    			diffMipAdcs[paddle-1] = ftof1aGains[paddle] - javaMip;
    			diffCentroids1a[paddle-1] = ftof1aCentroids[paddle] - javaCentroid;
    			diffHVLeft1a[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHV);
    			//diffHVLeft1aTest[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHVTest);
    			
    			// Right
                hvTestTable.addEntry(sector, layer, paddle+100);

                origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_RIGHT];
                hvTestTable.getEntry(sector, layer, paddle+100).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(1, ftof1aGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle+100).setData(2, ftof1aCentroids[paddle]);
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(3, Math.round(danNewVoltageRight));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(4, Math.round(danNewVoltageRight-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle+100).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
                
                
                newHV = hv.newHV(sector, layer, paddle, origVoltage, "RIGHT");
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(8, Math.round(newHV-origVoltage));
    			
    			//newHVTest = hv.newHVTest(layer, origVoltage, ftof1aGains[paddle], ftof1aCentroids[paddle], "RIGHT");
    			//hvTestTable.getEntry(sector, layer, paddle+100).setData(9, Math.round(newHVTest));

    			diffHVRight1a[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHV);
    			//diffHVRight1aTest[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHVTest);
                
				//System.out.println("NEXT layer "+layer+" paddle "+paddle+" "+pmt);
    			line = newHVReader1a.readLine();
			}
		
			newHVReader1a.close();  
        }
		catch(FileNotFoundException ex) {
			ex.printStackTrace();            
        }
        catch(IOException ex) {
            ex.printStackTrace();
		}
		
		Double[] ftof1bGains = {0.0, 2009.85,
				1918.73,
				1960.45,
				2020.86,
				1964.91,
				1984.94,
				1982.24,
				1999.21,
				1932.86,
				1959.32,
				1985.62,
				1934.88,
				1934.19,
				2075.81,
				1998.83,
				1979.1,
				1979.04,
				1980.56,
				1947.85,
				1996.64,
				1950.79,
				1901.64,
				1945.84,
				1972.41,
				1904.63,
				1962.96,
				1957.97,
				1956.08,
				1953.08,
				1982.2,
				1926.68,
				1936.15,
				1959.74,
				1972.8,
				1897.41,
				1943.78,
				1958.66,
				1943.78,
				2002.05,
				1996.6,
				1948.06,
				2002.69,
				1894.05,
				1940.06,
				1979.89,
				1903.24,
				1935.04,
				1991.35,
				1995.52,
				1970.1,
				1942.66,
				1971.95,
				1961.81,
				1953.48,
				1976.38,
				1958.92,
				1947.77,
				1944.51,
				1962.48,
				1915.55,
				1862.19,
				1910.89
};
		
		Double[] ftof1bCentroids = {0.0, -0.014,
				0.127,
				-0.024,
				-0.05,
				0.002,
				-0.039,
				-0.013,
				-0.069,
				-0.015,
				0.008,
				0.009,
				0.026,
				0.008,
				0.033,
				-0.047,
				-0.043,
				0.035,
				0.033,
				0.009,
				0.078,
				-0.02,
				0.017,
				0.015,
				-0.04,
				-0.003,
				0.002,
				-0.018,
				-0.006,
				-0.021,
				-0.014,
				-0.019,
				-0.024,
				0.01,
				-0.018,
				0.008,
				0.001,
				0.023,
				0.001,
				-0.063,
				-0.007,
				-0.016,
				0.018,
				-0.018,
				0.019,
				-0.001,
				0.007,
				0.025,
				-0.057,
				0.03,
				-0.018,
				0.02,
				-0.015,
				0.033,
				0.011,
				0.066,
				0.008,
				0.017,
				-0.057,
				-0.041,
				0.02,
				0.061,
				0.035
};
		
		try { 

			// Open the file with Dan's calculated new values
			FileReader newHVFile1b = 
					new FileReader("/home/louise/FTOF_calib_rewrite/legacy_files_from_dan/hv1b_253-s2_out.txt");
			BufferedReader newHVReader1b = 
					new BufferedReader(newHVFile1b);
			
			line = newHVReader1b.readLine();
			
			while (line != null) {
                
                String[] lineValues;
                lineValues = line.split("  ");
                
                System.out.println(lineValues[0]);
                System.out.println(lineValues[1]);
                System.out.println(lineValues[2]);
                System.out.println(lineValues[0].substring(0,1));
                
                int sector = Integer.parseInt(lineValues[0].substring(0,1));
                int layer = 2;
                int paddle = Integer.parseInt(lineValues[0].substring(2,4));
                
                if (sector != 2) {
                	line = newHVReader1b.readLine();
                	continue;
                }
                
                double danNewVoltageLeft = Double.parseDouble(lineValues[1]);
                double danNewVoltageRight = Double.parseDouble(lineValues[2]);

                double javaMip = hv.getMipChannel(sector, layer, paddle);
                double javaCentroid = hv.getConst(sector, layer, paddle)[hv.LR_CENTROID];
                
                // Put entry in the display table
                // Left
                hvTestTable.addEntry(sector, layer, paddle);
                double origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_LEFT];
                hvTestTable.getEntry(sector, layer, paddle).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle).setData(1, ftof1bGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle).setData(2, ftof1bCentroids[paddle]);
                
    			hvTestTable.getEntry(sector, layer, paddle).setData(3, Math.round(danNewVoltageLeft));
    			hvTestTable.getEntry(sector, layer, paddle).setData(4, Math.round(danNewVoltageLeft-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
    			
    			double newHV = hv.newHV(sector, layer, paddle, origVoltage, "LEFT");
    			hvTestTable.getEntry(sector, layer, paddle).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle).setData(8, Math.round(newHV-origVoltage));
    			
    			//double newHVTest = hv.newHVTest(layer, origVoltage, ftof1bGains[paddle], ftof1bCentroids[paddle], "LEFT");
    			//hvTestTable.getEntry(sector, layer, paddle).setData(9, Math.round(newHVTest));
    			
    			// Put values in graphs
    			paddle1bNumbers[paddle-1] = paddle;
    			diffMipAdcs1b[paddle-1] = ftof1bGains[paddle] - javaMip;
    			diffCentroids1b[paddle-1] = ftof1bCentroids[paddle] - javaCentroid;
    			diffHVLeft1b[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHV);
    			//diffHVLeft1bTest[paddle-1] = Math.round(danNewVoltageLeft) - Math.round(newHVTest);

    			// Right
                hvTestTable.addEntry(sector, layer, paddle+100);

                origVoltage = hv.getConst(sector, layer, paddle)[hv.CURRENT_VOLTAGE_RIGHT];
                hvTestTable.getEntry(sector, layer, paddle+100).setData(0, Math.round(origVoltage));
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(1, ftof1bGains[paddle]);
                hvTestTable.getEntry(sector, layer, paddle+100).setData(2, ftof1bCentroids[paddle]);
                
                hvTestTable.getEntry(sector, layer, paddle+100).setData(3, Math.round(danNewVoltageRight));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(4, Math.round(danNewVoltageRight-origVoltage));

                hvTestTable.getEntry(sector, layer, paddle+100).setData(5, Math.round(javaMip));
                hvTestTable.getEntry(sector, layer, paddle+100).setData(6, Double.parseDouble(new DecimalFormat("0.000").format(javaCentroid)));
                
                
                newHV = hv.newHV(sector, layer, paddle, origVoltage, "RIGHT");
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(7, Math.round(newHV));
    			hvTestTable.getEntry(sector, layer, paddle+100).setData(8, Math.round(newHV-origVoltage));
    			
    			//newHVTest = hv.newHVTest(layer, origVoltage, ftof1bGains[paddle], ftof1bCentroids[paddle], "RIGHT");
    			//hvTestTable.getEntry(sector, layer, paddle+100).setData(9, Math.round(newHVTest));

    			diffHVRight1b[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHV);
    			//diffHVRight1bTest[paddle-1] = Math.round(danNewVoltageRight) - Math.round(newHVTest);
                
				//System.out.println("NEXT layer "+layer+" paddle "+paddle+" "+pmt);
    			line = newHVReader1b.readLine();
			}
		
			newHVReader1b.close();  
        }
		catch(FileNotFoundException ex) {
			ex.printStackTrace();            
        }
        catch(IOException ex) {
            ex.printStackTrace();
		}
		
		// Create graphs
		GraphErrors mip1aGraph = new GraphErrors("Panel 1a MIP ADC Channel (Legacy - Java)", paddleNumbers, diffMipAdcs);
		
		// None of these seem to make any difference to the graph
		//mip1aGraph.setTitle("Panel 1a MIP ADC Channel (Legacy - Java)");
		//mip1aGraph.setXTitle("Paddle Number");
		//mip1aGraph.setYTitle("MIP ADC Channel (Legacy - Java)");
		//adc1aLegacyMipGraph.setMarkerStyle(2);
		//adc1aLegacyMipGraph.setMarkerColor(3);
		//GraphErrors adc1aJavaMipGraph = new GraphErrors(paddleNumbers, javaMipAdcs);
		//adc1aJavaMipGraph.setMarkerStyle(2);
		//adc1aLegacyMipGraph.setMarkerColor(4);
		TCanvas c1 = new TCanvas("Panel 1a MIP ADC Channel (Legacy - Java)","Panel 1a MIP ADC Channel (Legacy - Java)",600,600);
		c1.setDefaultCloseOperation(c1.HIDE_ON_CLOSE);
		c1.draw(mip1aGraph);
		//c1.draw(adc1aJavaMipGraph,"same");
		
		GraphErrors mip1bGraph = new GraphErrors("Panel 1b MIP ADC Channel (Legacy - Java)", paddle1bNumbers, diffMipAdcs1b);
		TCanvas c2 = new TCanvas("Panel 1b MIP ADC Channel (Legacy - Java)","Panel 1b MIP ADC Channel (Legacy - Java)",600,600);
		c2.setDefaultCloseOperation(c2.HIDE_ON_CLOSE);
		c2.draw(mip1bGraph);
		
		GraphErrors cen1aGraph = new GraphErrors("Panel 1a LR Centroid (Legacy - Java)", paddleNumbers, diffCentroids1a);
		cen1aGraph.getDataSetRieman().setXTitle("Paddle");
		
		TCanvas c3 = new TCanvas("Panel 1a LR Centroid (Legacy - Java)","Panel 1a LR Centroid (Legacy - Java)",600,600);
		c3.setDefaultCloseOperation(c3.HIDE_ON_CLOSE);
		c3.draw(cen1aGraph);
		
		GraphErrors cen1bGraph = new GraphErrors("Panel 1b LR Centroid (Legacy - Java)", paddle1bNumbers, diffCentroids1b);
		TCanvas c4 = new TCanvas("Panel 1b LR Centroid (Legacy - Java)","Panel 1b LR Centroid (Legacy - Java)",600,600);
		c4.setDefaultCloseOperation(c4.HIDE_ON_CLOSE);
		c4.draw(cen1bGraph);
		
		GraphErrors hvL1aGraph = new GraphErrors("Panel 1a New HV Left (Legacy - Java)", paddleNumbers, diffHVLeft1a);
		TCanvas c5 = new TCanvas("Panel 1a New HV Left (Legacy - Java)","Panel 1a New HV Left (Legacy - Java)",600,600);
		c5.setDefaultCloseOperation(c5.HIDE_ON_CLOSE);
		c5.draw(hvL1aGraph);

		GraphErrors hvR1aGraph = new GraphErrors("Panel 1a New HV Right (Legacy - Java)", paddleNumbers, diffHVRight1a);
		TCanvas c6 = new TCanvas("Panel 1a New HV Right (Legacy - Java)","Panel 1a New HV Right (Legacy - Java)",600,600);
		c6.setDefaultCloseOperation(c6.HIDE_ON_CLOSE);
		c6.draw(hvR1aGraph);
		
		GraphErrors hvL1bGraph = new GraphErrors("Panel 1b New HV Left (Legacy - Java)", paddle1bNumbers, diffHVLeft1b);
		TCanvas c7 = new TCanvas("Panel 1b New HV Left (Legacy - Java)","Panel 1b New HV Left (Legacy - Java)",600,600);
		c7.setDefaultCloseOperation(c7.HIDE_ON_CLOSE);
		c7.draw(hvL1bGraph);

		GraphErrors hvR1bGraph = new GraphErrors("Panel 1b New HV Right (Legacy - Java)", paddle1bNumbers, diffHVRight1b);
		TCanvas c8 = new TCanvas("Panel 1b New HV Right (Legacy - Java)","Panel 1b New HV Right (Legacy - Java)",600,600);
		c8.setDefaultCloseOperation(c8.HIDE_ON_CLOSE);
		c8.draw(hvR1bGraph);

		GraphErrors hvL1aTestGraph = new GraphErrors("Panel 1a New HV Left (Legacy - Java test)", paddleNumbers, diffHVLeft1aTest);
		TCanvas c9 = new TCanvas("Panel 1a New HV Left (Legacy - Java test)","Panel 1a New HV Left (Legacy - Java test)",600,600);
		c9.setDefaultCloseOperation(c9.HIDE_ON_CLOSE);
		c9.draw(hvL1aTestGraph);

		GraphErrors hvR1aTestGraph = new GraphErrors("Panel 1a New HV Right (Legacy - Java test)", paddleNumbers, diffHVRight1aTest);
		TCanvas c10 = new TCanvas("Panel 1a New HV Right (Legacy - Java test)","Panel 1a New HV Right (Legacy - Java test)",600,600);
		c10.setDefaultCloseOperation(c10.HIDE_ON_CLOSE);
		c10.draw(hvR1aTestGraph);
		
		GraphErrors hvL1bTestGraph = new GraphErrors("Panel 1b New HV Left (Legacy - Java test)", paddle1bNumbers, diffHVLeft1bTest);
		TCanvas c11 = new TCanvas("Panel 1b New HV Left (Legacy - Java test)","Panel 1b New HV Left (Legacy - Java test)",600,600);
		c11.setDefaultCloseOperation(c11.HIDE_ON_CLOSE);
		c11.draw(hvL1bTestGraph);

		GraphErrors hvR1bTestGraph = new GraphErrors("Panel 1b New HV Right (Legacy - Java test)", paddle1bNumbers, diffHVRight1bTest);
		TCanvas c12 = new TCanvas("Panel 1b New HV Right (Legacy - Java test)","Panel 1b New HV Right (Legacy - Java test)",600,600);
		c12.setDefaultCloseOperation(c12.HIDE_ON_CLOSE);
		c12.draw(hvR1bTestGraph);
		
		// Display GUI
		
		JPanel testPanel = new JPanel();
    	testPanel.setLayout(new FlowLayout());
		
		ConstantsTablePanel tablePanel = new ConstantsTablePanel(hvTestTable);
		//JPanel tablePane = new JPanel(new BorderLayout());
		//tablePane.add(tablePanel);
		//testPanel.add(tablePane);
		testPanel.add(tablePanel);
		
    	JFrame frame = new JFrame("HV test");
        frame.setSize(1200, 700);
        
        //frame.add(testPanel);
        frame.add(tablePanel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
 
		
    }


}
