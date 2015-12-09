package org.jlab.calib.tof;

import org.root.histogram.H1D;

public class TOFH1D extends H1D {

	public TOFH1D(String name, String title, int bins, double xMin, double xMax) {
		super(name, title, bins, xMin, xMax);
	}

	public void rebin(int nBinsCombine) {
		
		H1D histIn = this.histClone("Rebinned");
		int nBinsOrig = histIn.getAxis().getNBins();

		this.set(nBinsOrig/nBinsCombine, histIn.getAxis().min(), histIn.getAxis().max());
		
		int newBin = 0;

		for (int origBin=0; origBin<=nBinsOrig;) {

			double newBinCounts = 0;
			for (int i=0; i<nBinsCombine; i++) {
				newBinCounts = newBinCounts + histIn.getBinContent(origBin);
				origBin++;				
			}
			this.setBinContent(newBin, newBinCounts);
			newBin++;

		}
	}

}
