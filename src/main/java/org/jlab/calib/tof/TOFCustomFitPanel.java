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

	JTextField minRange = new JTextField(5);
	JTextField maxRange = new JTextField(5);
	
	
	public TOFCustomFitPanel(){
		
		this.setLayout(new GridLayout(4,2));
		
		// Create fields for function range
		this.add(new JLabel("Function range"));
		this.add(new JLabel(""));
		
		this.add(Box.createHorizontalStrut(15)); // a spacer
		this.add(Box.createHorizontalStrut(15)); // a spacer
		
		this.add(new JLabel("Minimum:"));
		this.add(minRange);
		this.add(new JLabel("Maximum:"));
		this.add(maxRange);
	}
	
}
