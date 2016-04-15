package org.jlab.calib.tof;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jlab.clas.detector.ConstantsTable;
import org.jlab.clas.detector.ConstantsTablePanel;
import org.jlab.clas.detector.DetectorType;

public class TOFHVAdjustPanel 	extends JPanel
								implements ActionListener {
	
	JButton fileButton;
	JFileChooser fc;
	TOFHighVoltage hv;
	ConstantsTable hvTable;
	ConstantsTablePanel tablePanel;
	JPanel tablePane;

	public TOFHVAdjustPanel(TOFHighVoltage hvIn) {
		
		hv = hvIn;
		
		setLayout(new FlowLayout());
		
		JPanel buttonPanel = new JPanel(new FlowLayout());
		hvTable = new ConstantsTable(DetectorType.FTOF,new String[]{"PMT","Current HV","New HV"});
		tablePanel= new ConstantsTablePanel(hvTable);
		tablePane = new JPanel(new BorderLayout());
		tablePane.add(tablePanel);

		
		// Create fields for sector selection and file selection
		buttonPanel.add(new JLabel("Sector:"));
		String[] sectors = new String[] {"1", "2", "3", "4", "5", "6"};
		JComboBox<String> sectorList = new JComboBox(sectors);
		buttonPanel.add(sectorList);

		buttonPanel.add(new JLabel("Input file:"));
		fc = new JFileChooser();
	    fileButton = new JButton("Select File");
	    fileButton.addActionListener(this);
	    buttonPanel.add(fileButton);
	    
    	add(buttonPanel);
    	add(tablePane);
	    
	}
	
	
	public void actionPerformed(ActionEvent e) {
		
		if(e.getSource() == fileButton) {
			
			fc.setCurrentDirectory(new File("/home/louise/FTOF_calib_rewrite/input_files/hvfiles"));
			int returnValue = fc.showOpenDialog(null);
			if (returnValue == JFileChooser.APPROVE_OPTION) {
				
				TOFHVAdjust adj = new TOFHVAdjust();
				String outputFileName = adj.processFile(hv, hvTable, fc.getSelectedFile().getAbsolutePath(), 2);
				JOptionPane.showMessageDialog(new JPanel(),"High voltage values written to "+outputFileName);
				
				hvTable.fireTableDataChanged();
				
				//TOFCalibration.testHVCalcs();
			}
		
		}
	}
}