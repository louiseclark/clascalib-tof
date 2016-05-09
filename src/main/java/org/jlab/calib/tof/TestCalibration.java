package org.jlab.calib.tof;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jlab.clas.detector.*;
import org.jlab.evio.clas12.*;
import org.root.func.*;
import org.root.histogram.*;
import org.root.basic.EmbeddedPad;

public class TestCalibration {

//	public TestCalibration() {
//		// TODO Auto-generated constructor stub
//	}

	static int s = 0;
	static int l = 0;
	static int p = 0;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		TestCalibration testCal = new TestCalibration();
		
        String file = "/home/louise/workspace/FtofInputFile_panel1a1bS6_from_root_file2.evio";
        EvioSource reader = new EvioSource();
        reader.open(file);
        System.out.println(reader.getSize());

        final TOFGeometricMean  gm = new TOFGeometricMean();
        gm.init();
        int maxEvents = 0;
        int eventNum = 0;
        while(reader.hasEvent()&&(eventNum<maxEvents||maxEvents==0)){
        	EvioDataEvent event = (EvioDataEvent) reader.getNextEvent();
        	gm.processEvent(event);
        	eventNum++;
        }

        gm.analyze();
        //gm.show();

        final EmbeddedPad c1 = new EmbeddedPad(1,1);
        H1D h = gm.getH1D(5,1,50);
        F1D f = gm.getF1D(5,1,50);

        c1.draw(h);
        c1.draw(f, "same");
        
        final ConstantsTable      table = new ConstantsTable(DetectorType.FTOF,new String[]{"Peak Position","Width"});

        gm.fillTable(5, table);
        
        final ConstantsTablePanel tablePanel = new ConstantsTablePanel(table);
        
        tablePanel.addListener(new IConstantsTableListener(){
            public void entrySelected(int sector, int layer, int component){
            	//c1.clear();
            	H1D h = gm.getH1D(sector,layer,component);
                F1D f = gm.getF1D(sector,layer,component);
                
                s=sector;
                l=layer;
                p=component;
                c1.draw(h);
                c1.draw(f, "same");
                System.out.println(" SELECTED COMPONENT   ====> " + sector + "  " + layer + " " + component);
            }
        });

		final JFrame frame = new JFrame();
		final JPanel panel3 = new JPanel();
		
        JButton customFitButton = new JButton("Custom fit");
        customFitButton.addActionListener(new java.awt.event.ActionListener() {
		    public void actionPerformed(java.awt.event.ActionEvent evt) {
		    	gm.customFit(s,l,p);
		    	F1D f = gm.getF1D(s,l,p);
		    	H1D h = gm.getH1D(s,l,p);
		    	//c1.clear();
		    	table.getEntry(s, l, p).setData(0, Math.round(f.getParameter(1)));
				table.getEntry(s, l, p).setData(1, Math.round(f.getParameter(2)));
				tablePanel.repaint();
		    	panel3.repaint();
		    	frame.repaint();
		    	c1.draw(h);
		    	c1.draw(f, "same");
		    }
		});
        
		JPanel mainPanel = new JPanel(new GridLayout(1,2) );

		JPanel panel1 = new JPanel(new GridLayout(2,1));
		panel1.add( c1 );

		JPanel panel2 = new JPanel();
		panel2.setPreferredSize( new Dimension(100, 100) );
		panel2.add(customFitButton);
		panel1.add( panel2 );
		mainPanel.add( panel1 );

		panel3.add( tablePanel );
		mainPanel.add( panel3 );
		
		frame.add(mainPanel);
		frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        
        
        JFrame frame2 = new JFrame();
		
        frame2.add(gm.showFits(5, 1));
		frame2.pack();
        frame2.setVisible(true);
        frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        
    }
	
	
}
