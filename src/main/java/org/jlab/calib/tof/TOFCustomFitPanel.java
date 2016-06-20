/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.calib.tof;

import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.root.func.F1D;
import org.root.histogram.H1D;

/**
 *
 * @author gavalian
 */
public class TOFCustomFitPanel extends JPanel {
	
	JTextField[] textFields;
	
	public TOFCustomFitPanel(String[] fields){
		
		JTextField[] newTextFields = new JTextField[fields.length];
		textFields = newTextFields;
		
		this.setLayout(new GridLayout(fields.length,2));
		
		// Initialize the text fields
		for (int i=0; i< fields.length; i++) { 
			textFields[i] = new JTextField(5);
		}
		
		// Create fields
		for (int i=0; i< fields.length; i++) {
			
			this.add(new JLabel(fields[i]));
			this.add(textFields[i]);
			
		}
				
	}
	
}
