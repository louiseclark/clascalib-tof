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
		
		// how many spaces
		int numFields = fields.length;
		for (int i=0; i < fields.length; i++) {
			if (fields[i] == "SPACE") {
				numFields--;
			}
		}
		
		JTextField[] newTextFields = new JTextField[numFields];
		textFields = newTextFields;
		
		this.setLayout(new GridLayout(fields.length,2));
		
		// Initialize the text fields
		for (int i=0; i< numFields; i++) { 
			textFields[i] = new JTextField(5);
		}
		
		// Create fields
		int fieldNum = 0;
		for (int i=0; i< fields.length; i++) {
			
			if (fields[i] == "SPACE") {
				this.add(new JLabel(""));
				this.add(new JLabel(""));
			}
			else {
				this.add(new JLabel(fields[i]));
				this.add(textFields[fieldNum]);
				fieldNum++;
			}
			
		}
				
	}
	
}
