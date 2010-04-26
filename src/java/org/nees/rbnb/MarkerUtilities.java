package org.nees.rbnb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * A set of utilities for event markers.
 * 
 * @author Jason P. Hanley
 */
public final class MarkerUtilities {
  /**
   * This class can not be instantiated and it's constructor
   * always throws an exception.
   */
  private MarkerUtilities() {
    throw new UnsupportedOperationException("This class can not be instantiated.");
  }
  
  /**
   * Gets a list of markers that match the filter string.
   * 
   * @param sink            the sink to use to connect to the server
   * @param filterString    the event marker filter string
   * @return                a list of event markers
   * @throws SAPIException  if there is an error connecting to the server
   */
  public static List<EventMarker> getEventMarkers(Sink sink, String filterString) throws SAPIException {
    List<EventMarker> eventMarkers = new ArrayList<EventMarker>();
    
    ChannelMap eventMarkerChannelMap = RBNBUtilities.getChannelMap(sink, EventMarker.MIME_TYPE);
    
    if (eventMarkerChannelMap.NumberOfChannels() > 0) {
      sink.Request(eventMarkerChannelMap, 0, Double.MAX_VALUE, "absolute");
      eventMarkerChannelMap = sink.Fetch(-1);
      
      Map<String,List<String>> filters = parseFilterString(filterString);
  
      for (String channel : eventMarkerChannelMap.GetChannelList()) {
        int channelIndex = eventMarkerChannelMap.GetIndex(channel);
        String[] eventMarkersData = eventMarkerChannelMap.GetDataAsString(channelIndex);
        
        for (String eventMarkerData : eventMarkersData) {
          EventMarker eventMarker = new EventMarker();
          
          try {
            eventMarker.setFromEventXml(eventMarkerData);
          } catch (IOException e) {
            continue;
          }
          
          if (matchEventMarker(eventMarker, filters)) {
            eventMarkers.add(eventMarker);                
          }
        }
      }
      
      Collections.sort(eventMarkers);      
    }
    
    return eventMarkers;
  }
  
  /**
   * Gets a map of the event marker keys and a list of their values. This parses
   * the filter string to create this.
   * 
   * The filter string is in the format: key1=value1+value2,key2=value1
   * 
   * Where the keys must be distinct and separated by commas. And the values
   * are separated by plus(+).
   * 
   * @param filterString  the filter string to parse
   * @return              a map of keys and their values
   */
  public static Map<String,List<String>> parseFilterString(String filterString) {
    Map<String,List<String>> filters = new HashMap<String,List<String>>();
    
    if (filterString != null) {
      for (String filter : filterString.split(",")) {
        String[] filterParts = filter.split("=");
        if (filterParts.length != 2) {
          throw new IllegalArgumentException("The event marker filter string is not valid.");
        }
        
        String key = filterParts[0];
        String valueString = filterParts[1];
        List<String> value = Arrays.asList(valueString.split("\\+"));
        filters.put(key, value);
      }
    }
    
    return filters;
  }
  
  /**
   * Matches the event marker to the filter map. The event marker must contain
   * all of the keys in this map, along with at least one of the values for each
   * key.
   * 
   * @param eventMarker  the event marker to match
   * @param filters      the map of filters
   * @return             true if the event marker matches, false otherwise
   */
  public static boolean matchEventMarker(EventMarker eventMarker, Map<String,List<String>> filters) {
    boolean matches = false;
    
    for (String filterKey : filters.keySet()) {
      matches = false;
      
      String value = eventMarker.getProperty(filterKey);
      for (String filterValue : filters.get(filterKey)) {
        if (filterValue.equals(value)) {
          matches = true;
          break;
        }
      }
      
      if (!matches) {
        break;
      }
    }

    return matches;
  }
  
  /**
   * Gets a list of time ranges that correspond to pairs of event markers with
   * type start and stop respectively. The event markers are filtered by the
   * filter string and bound by the minimum and maximum times.
   * 
   * @param sink            the sink to use to connect to the server
   * @param filterString    the string to filter event markers
   * @param minimumTime     the minimum time for an event marker
   * @param maximumTime     the maximum time for an event marker
   * @return                a list of time ranges
   * @throws SAPIException  if there is an error connecting to the server
   */
  public static List<TimeRange> getTimeRanges(Sink sink, String filterString, double minimumTime, double maximumTime) throws SAPIException {
    if (filterString == null || filterString.length() == 0) {
      filterString += "type=start+stop";
    } else {
      filterString += ",type=start+stop";
    }
    
    List<EventMarker> eventMarkers = getEventMarkers(sink, filterString);
    
    List<TimeRange> timeRanges = new ArrayList<TimeRange>();    
    TimeRange timeRange = null;
    
    for (EventMarker eventMarker : eventMarkers) {
      if (eventMarker.getType().equalsIgnoreCase("start") && timeRange == null) {
        timeRange = new TimeRange(eventMarker.getTimestamp());
      } else if (eventMarker.getType().equalsIgnoreCase("stop")) {
        if (timeRange == null) {
          timeRange = new TimeRange(minimumTime);
        }
        
        timeRange.setEndTime(Double.parseDouble(eventMarker.getProperty("timestamp")));
        
        timeRanges.add(timeRange);
        timeRange = null;
      }
    }
    
    if (timeRange != null) {
      timeRange.setEndTime(maximumTime);
      timeRanges.add(timeRange);
    }
    
    TimeRange boundingTimeRange = new TimeRange(minimumTime, maximumTime);
    boundTimeRanges(timeRanges, boundingTimeRange);
    
    
    return timeRanges;
  }
  
  /**
   * Ensure this list of time ranges is bound by the specified time ranges. This
   * will remove time ranges that are not contained by the bound time ranges and
   * will modify the bounds of ones that intersect with it.
   * 
   * @param timeRanges         the list of time ranges to bound
   * @param boundingTimeRange  the time range to bound by
   */
  public static void boundTimeRanges(List<TimeRange> timeRanges, TimeRange boundingTimeRange) {
    for (int i=0; i<timeRanges.size(); i++) {
      TimeRange timeRange = timeRanges.get(i);
      
      if (boundingTimeRange.contains(timeRange)) {
        continue;
      }
      
      if (boundingTimeRange.intersects(timeRange)) {
        if (timeRange.getStartTime() < boundingTimeRange.getStartTime()) {
          timeRange.setStartTime(boundingTimeRange.getStartTime());
        }
        
        if (timeRange.getEndTime() > boundingTimeRange.getEndTime()) {
          timeRange.setEndTime(boundingTimeRange.getEndTime());
        }
      } else {
        timeRanges.remove(i--);
      }
    }    
  }
}