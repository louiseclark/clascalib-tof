package org.jlab.calib.tof;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.root.attr.TStyle;
import org.root.base.IDataSet;
import org.root.func.F1D;
import org.root.func.RandomFunc;
import org.root.histogram.H1D;
import org.root.basic.EmbeddedCanvas;

/**
 *
 * @author gavalian
 */
public class TOFBookCanvas extends JPanel implements ActionListener {
    
    private List<IDataSet> container = new ArrayList<IDataSet>();
    private List<String>   options   = new ArrayList<String>();
    private EmbeddedCanvas canvas    = new EmbeddedCanvas();
    private int            nDivisionsX = 1;
    private int            nDivisionsY = 1;
    private int            currentPosition = 0;
    JComboBox comboDivide = null;
    
    public TOFBookCanvas(int dx, int dy){
        super();
        
        TStyle.setFrameFillColor(250, 250, 255);
        this.setLayout(new BorderLayout());
        
        this.nDivisionsX = dx;
        this.nDivisionsY = dy;
        
        JPanel buttonPanel = new JPanel();
        //buttonPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        buttonPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        
        JButton  buttonPrev = new JButton("< Previous");
        JButton  buttonNext = new JButton("Next >");
        buttonNext.addActionListener(this);
        buttonPrev.addActionListener(this);
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(buttonPrev);
        buttonPanel.add(buttonNext);
        canvas.divide(this.nDivisionsX, this.nDivisionsY);
        
        
        JPanel canvasPane = new JPanel();
        canvasPane.setLayout(new BorderLayout());
        canvasPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        canvasPane.add(canvas,BorderLayout.CENTER);

        String[]  canvasOptions = new String[]{"1x1","2x2","2x3","3x3"};
        comboDivide = new JComboBox(canvasOptions);
        buttonPanel.add(comboDivide);
        comboDivide.addActionListener(this);
//this.add(canvas,BorderLayout.CENTER);
        this.add(canvasPane,BorderLayout.CENTER);
        this.add(buttonPanel,BorderLayout.PAGE_END);
    }
    
    public void add(IDataSet ds, String opt){
        this.container.add(ds);
        this.options.add(opt);
    }
    
    public void drawNext(){
        if(this.currentPosition>=this.container.size()){
            this.currentPosition = 0;
        }
        
        canvas.divide(this.nDivisionsX,this.nDivisionsY);
        int npads   = this.nDivisionsX*this.nDivisionsY;
        int counter = 0;
        canvas.cd(counter);
        while(this.currentPosition<this.container.size()&&counter<npads){
            IDataSet ds = this.container.get(this.currentPosition);
            String   op = this.options.get(this.currentPosition);            
            
            if(op.contains("same")==false){
                System.out.println(" Drawing Position (    ) "  + this.currentPosition + 
                        " on pad " + counter);
                canvas.cd(counter);
                canvas.draw(ds, "");
                counter++;
            } else {

                System.out.println(" Drawing Position (same)"  + this.currentPosition + 
                        " on pad " + counter);
                canvas.draw(ds, "same");
            }
            this.currentPosition++;
        }
    }
    
    public void reset(){
        this.currentPosition = 0;
        
    }
    
    public static void main(String[] args){
        JFrame frame = new JFrame();
        TOFBookCanvas book = new TOFBookCanvas(2,2);
        
        for(int loop = 0; loop < 20; loop++){
            Integer Order = loop;
            String option = "";
            //if((loop+1)%2==0) option = "same";
            H1D h1 = new H1D("h1"," HISTOGRAM " + Order,100,0.0,6.0);

            F1D f1 = new F1D("gaus",0.5,6.0);
            f1.setParameter(0, 20);
            f1.setParameter(1, 4.5);
            f1.setParameter(2, 0.5);
            RandomFunc func = new RandomFunc(f1);

            for(int nev = 0 ; nev < 200; nev++) h1.fill(func.random());
            
            h1.fit(f1);
            book.add(h1,option);
            f1.setLineColor(2);
            f1.setLineWidth(3);
            
            book.add(f1,"same");
        }
        
        frame.add(book);
        frame.pack();
        frame.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println("action " + e.getActionCommand());
        if(e.getActionCommand().compareTo("Next >")==0){
            this.drawNext();
        }
        
        if(e.getActionCommand().compareTo("< Previous")==0){
            Color newColor = JColorChooser.showDialog(
                     this,
                     "Choose Background Color",
                     null);
        }
        
        if(e.getActionCommand().compareTo("comboBoxChanged")==0){
            String selection = (String) this.comboDivide.getSelectedItem();
            System.out.println("Changed to " + selection);
            if(selection.compareTo("1x1")==0){
                this.nDivisionsX = 1;
                this.nDivisionsY = 1;
                this.reset();
            }
            
            if(selection.compareTo("2x2")==0){
                this.nDivisionsX = 2;
                this.nDivisionsY = 2;
                this.reset();
            }
        }
    }
}
