package org.osmdroid.bonuspack.kml;

import android.os.Parcelable;

import java.io.Writer;

/**
 * Handling of a KML StyleSelector (abstract class). 
 * @author M.Kergall
 */
public abstract class StyleSelector implements Parcelable {

	/** default constructor */
	public StyleSelector(){
	}
	
	abstract public void writeAsKML(Writer writer, String styleId);
	
	//TODO: need to implement Parcelable?
}
