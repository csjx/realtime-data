/*
 * RDV
 * Real-time Data Viewer
 * http://it.nees.org/software/rdv/
 * 
 * Copyright (c) 2005-2007 University at Buffalo
 * Copyright (c) 2005-2007 NEES Cyberinfrastructure Center
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * $URL$
 * $Revision$
 * $Date$
 * $Author$
 */

package org.nees.rbnb;

/**
 * A time range. This is a range in time specified by a start and end time.
 * 
 * @author Jason P. Hanley
 */
public class TimeRange implements Comparable<TimeRange> {
  /** The start of the time range */
  private double startTime;
  
  /** The end of the time range */
  private double endTime;
  
  /**
   * Creates a time range starting at 0 and ending at Double.MAX_VALUE.
   */
  public TimeRange() {
    this(0);
  }
  
  /**
   * Creates a time range starting at the specified start time and ending at
   * Double.MAX_VALUE
   * 
   * @param startTime  the start of the time range
   */
  public TimeRange(double startTime) {
    this(startTime, Double.MAX_VALUE);
  }
  
  /**
   * Creates a time range starting and ending at the specified times.
   * 
   * @param startTime  the start of the time range
   * @param endTime    the end of the time range
   */
  public TimeRange(double startTime, double endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }
  
  /**
   * Gets the start time.
   * 
   * @return  the start time
   */
  public double getStartTime() {
    return startTime;
  }

  /**
   * Sets the start time.
   * 
   * @param startTime  the start time
   */
  public void setStartTime(double startTime) {
    this.startTime = startTime;
  }
  
  /**
   * Gets the end time.
   * 
   * @return  the end time
   */
  public double getEndTime() {
    return endTime;
  }

  /**
   * Sets the end time.
   * 
   * @param endTime  the end time
   */
  public void setEndTime(double endTime) {
    this.endTime = endTime;
  }

  /**
   * The length of the time range. Simply end-start.
   * 
   * @return  the length of the time range
   */
  public double length() {
    return endTime-startTime;
  }
  
  /**
   * See if the time is within the time range.
   * 
   * @param time  the time to check
   * @return      true if the time is within the time range, false otherwise
   */
  public boolean contains(double time) {
    return ((time >= startTime) && (time <= endTime));
  }
  
  public boolean contains(TimeRange t) {
    return startTime<=t.getStartTime() && endTime>=t.getEndTime();
  }
  
  /**
   * See if the two time ranges intersect.
   * 
   * @param t  the time range to compare with
   * @return   true if they intersect, false otherwise
   */
  public boolean intersects(TimeRange t) {
    if (endTime < t.getStartTime()) {
      return false;
    } else if (startTime > t.getEndTime()) {
      return false;
    } else {
      return true;
    }
  }
  
  /**
   * Compare time ranges. This is based first on their start, and then on
   * their end (if needed).
   * 
   * @param d  the time range to compare with
   * @return   0 if they are the same, -1 if this is less than the other, and
   *           1 if this is greater than the other.
   */
  public int compareTo(TimeRange t) {
    if (startTime == t.startTime) {
      if (endTime == t.endTime) {
        return 0;
      } else if (endTime < t.endTime) {
        return -1;
      } else {
        return 1;
      }
    } else if (startTime < t.startTime) {
      return -1;
    } else {
      return 1;
    }
  }
}