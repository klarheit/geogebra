// Copyright 2002, FreeHEP.
package org.freehep.graphicsio.emf.gdi;

import java.awt.Rectangle;
import java.io.IOException;

import org.freehep.graphicsio.emf.EMFInputStream;
import org.freehep.graphicsio.emf.EMFOutputStream;
import org.freehep.graphicsio.emf.EMFTag;

/**
 * StrokePath TAG.
 * 
 * @author Mark Donszelmann
 * @version $Id: StrokePath.java,v 1.5 2009-08-17 21:44:44 murkle Exp $
 */
public class StrokePath extends EMFTag {

	private Rectangle bounds;

	public StrokePath() {
		super(64, 1);
	}

	public StrokePath(Rectangle bounds) {
		this();
		this.bounds = bounds;
	}

	public EMFTag read(int tagID, EMFInputStream emf, int len)
			throws IOException {

		StrokePath tag = new StrokePath(emf.readRECTL());
		return tag;
	}

	public void write(int tagID, EMFOutputStream emf) throws IOException {
		emf.writeRECTL(bounds);
	}

	public String toString() {
		return super.toString() + "\n" + "  bounds: " + bounds;
	}
}
