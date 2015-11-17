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

	F1D f1 = null;
	H1D h1 = null;
	int numParams;
	JTextField minRange = new JTextField(5);
	JTextField maxRange = new JTextField(5);
//	JTextField[] parField = new JTextField[numParams];
	
	
	public TOFCustomFitPanel(H1D h, F1D f){

		f1 = f;
		h1 = h;
		
		//numParams = f.getNParams();
		//this.setLayout(new GridLayout(numParams+7,2));
		this.setLayout(new GridLayout(4,2));
//		this.add(new JLabel("Function parameters for:"));
//		this.add(new JLabel("landau+exp"));
//		
//		this.add(Box.createHorizontalStrut(15)); // a spacer
//		this.add(Box.createHorizontalStrut(15)); // a spacer
//		
//		// Create fields for all function parameters
//		for (int parNum=0; parNum<numParams; parNum++) {
//
//			this.add(new JLabel("Parameter "+parNum+":"));
//			parField[parNum] = new JTextField(5);
//			this.add(parField[parNum]);
//		}
//		
//		this.add(Box.createHorizontalStrut(15)); // a spacer
//		this.add(Box.createHorizontalStrut(15)); // a spacer
		
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
	
//	public void fit(){
//		f1.setRange(3000, 5000);
//		h1.fit(f1);
//	}
}
