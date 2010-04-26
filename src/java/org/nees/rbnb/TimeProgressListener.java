/*
 * Created on May 26, 2005
 */
package org.nees.rbnb;

/**
 * The interface for listeners to process that want to report progress. See
 * JpgSaverSink for an example of an application that accepts listeners with
 * this interface.
 * 
 * @author Terry E Weymouth
 * 
 * @see JpgSaverSink
 * 
 */
public interface TimeProgressListener {
	public void progressUpdate(double estimatedDuration, double consumedTime);
}
