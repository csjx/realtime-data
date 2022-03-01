package org.nees.rbnb;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;
import com.rbnb.sapi.ChannelTree.Node;

/** a class that provides utility methods to interconvert between rbnb
  * frames and other parameters of interest
  * @author Lawrence J. Miller <ljmiler@sdsc.edu>
  * @since 060519
  */

public final class RBNBUtilities {

  
  /**
   * This class can not be instantiated and it's constructor
   * always throws an exception.
   */
  private RBNBUtilities() {
    throw new UnsupportedOperationException("This class can not be instantiated.");
  }
  
  /**
   * a method that will
   * 
   * @return the number of rbnb frames that will correspond to the
   * @param time
   *          desired to be spanned (in days) by these frames given
   * @param flush
   *          rate of a ChannelMap (in Hz)
   */
  public static int getFrameCountFromTime(double hours, double flushRate) {
    double seconds = hours * 60.0 * 60.0;
    double frames = seconds * flushRate;
    return (int) frames;
  } // getFrameCountFromTime ()

  public static int getFrameCountFromTime(double hours, int flushRate) {
    double seconds = hours * 60.0 * 60.0;
    double frames = seconds * (double) flushRate;
    return (int) frames;
  } // getFrameCountFromTime ()

  /**
   * Using the given channel map, finds the start time for the specified channel.
   * If the channel is not found, -1 is returned.
   * 
   * @param channelMap   the <code>ChannelMap</code> containing the times
   * @param channelName  the name of the channel
   * @return             the start time for the channel
   * @since              1.2
   */
  public static double getStartTime(ChannelMap channelMap, String channelName) {
    int channelIndex = channelMap.GetIndex(channelName);
    if (channelIndex != -1) {
      double start = channelMap.GetTimeStart(channelIndex);
      return start;
    } else {
      return -1;
    }
  }
  
  /**
   * Returns the start time for the given channel map. If the channel map
   * is empty, -1 is returned.
   * 
   * @param channelMap  the <code>ChannelMap</code> containing the times
   * @return            the start time for all the channels
   * @see               #getStartTime(ChannelMap, String)
   * @since             1.2
   */
  public static double getStartTime(ChannelMap channelMap) {
    double start = Double.MAX_VALUE;
    
    String[] channels = channelMap.GetChannelList();
    for (int i=0; i<channels.length; i++) {
      String channelName = channels[i];
      double channelStart = getStartTime(channelMap, channelName);
      if (channelStart != -1) {
        start = Math.min(channelStart, start);
      }
    }
    
    if (start != Double.MAX_VALUE) {
      return start;
    } else {
      return -1;
    }
  }
  
  /**
   * Using the given channel map, finds the end time for the specified channel.
   * If the channel is not found, -1 is returned.
   * 
   * @param channelMap   the <code>ChannelMap</code> containing the times
   * @param channelName  the name of the channel
   * @return             the end time for the channel
   * @since              1.2
   */
  public static double getEndTime(ChannelMap channelMap, String channelName) {
    int channelIndex = channelMap.GetIndex(channelName);
    if (channelIndex != -1) {
      double start = channelMap.GetTimeStart(channelIndex);
      double duration = channelMap.GetTimeDuration(channelIndex);
      double end = start+duration;
      return end;
    } else {
      return -1;
    }
  }
  
  /**
   * Returns the end time for the given channel map. If the channel map
   * is empty, -1 is returned.
   * 
   * @param channelMap  the <code>ChannelMap</code> containing the times
   * @return            the end time for all the channels
   * @see               #getEndTime(ChannelMap, String)
   * @since             1.2
   */
  public static double getEndTime(ChannelMap channelMap) {
    double end = -1;
    
    String[] channels = channelMap.GetChannelList();
    for (int i=0; i<channels.length; i++) {
      String channelName = channels[i];
      double channelEnd = getEndTime(channelMap, channelName);
      if (channelEnd != -1) {
        end = Math.max(channelEnd, end);
      }
    }
    
    return end;
  }
  
  /**
   * Returns a list of sorted children for the root of the Channel Tree.
   * 
   * @param ctree  the chanel tree to find the children in
   * @return       a sorted list of children of the root element
   * @since        1.3
   */
  public static List getSortedChildren(ChannelTree ctree) {
    return getSortedChildren(ctree, true);
  }
  
  /**
   * Returns a list of sorted children for the root of the Channel Tree. If
   * showHiddenChildren is set, children starting with '_' will be omitted.
   * 
   * @param ctree               the chanel tree to find the children in
   * @param showHiddenChildren  include/discard hidden children
   * @return                    a sorted list of children of the root element
   * @since                     1.3
   */
  public static List getSortedChildren(ChannelTree ctree, boolean showHiddenChildren) {
    return getSortedChildren(ctree.rootIterator(), showHiddenChildren);
  }
  
  /**
   * Returns a list of sorted children for this node.
   * 
   * @param node  the parent to find the children
   * @return      a sorted list of children
   * @since       1.3
   */
  public static List getSortedChildren(ChannelTree.Node node) {
    return getSortedChildren(node, true);
  }
  
  /**
   * Returns a list of sorted children for this node. If showHiddenChildren is
   * set, children starting with '_' will be omitted.
   * 
   * @param node                the parent to find the children
   * @param showHiddenChildren  include/discard hidden children
   * @return                    a sorted list of children
   * @since                     1.3
   */
  public static List getSortedChildren(ChannelTree.Node node, boolean showHiddenChildren) {
    return getSortedChildren(node.getChildren().iterator(), showHiddenChildren);
  }  
  
  @SuppressWarnings("unchecked")
  private static List getSortedChildren(Iterator it, boolean showHiddenChildren) {
    List list = new ArrayList();

    while (it.hasNext()) {
      ChannelTree.Node node = (ChannelTree.Node)it.next();
      boolean isHidden = node.getName().startsWith("_");
      ChannelTree.NodeTypeEnum nodeType = node.getType();
      if ((showHiddenChildren || !isHidden) &&
          (nodeType == ChannelTree.CHANNEL || node.getType() == ChannelTree.FOLDER ||
           nodeType == ChannelTree.SERVER || nodeType == ChannelTree.SOURCE ||
           nodeType == ChannelTree.PLUGIN)) {
        list.add(node);       
      }
    }
    
    Collections.sort(list, new HumanComparator());

    return list;
  }
  
  public static class HumanComparator implements Comparator {
    private Pattern p;
    
    public HumanComparator() {
      p = Pattern.compile("(\\D*)(\\d+)(\\D*)");  
    }
    
    public int compare(Object o1, Object o2) {      
      String s1;
      if (o1 instanceof ChannelTree.Node) {
        s1 = ((ChannelTree.Node)o1).getName();
      } else {
        s1 = (String)o1;
      }
      s1 = s1.toLowerCase();
      
      String s2;
      if (o2 instanceof ChannelTree.Node) {
        s2 = ((ChannelTree.Node)o2).getName();        
      } else {
        s2 = (String)o2;
      }
      s2 = s2.toLowerCase();
      
      if (s1.equals(s2)) {
        return 0;  
      }
      
      Matcher m1 = p.matcher(s1);
      Matcher m2 = p.matcher(s2);
      
      if (m1.matches() && m2.matches() &&
          m1.group(1).equals(m2.group(1)) &&
          m1.group(3).equals(m2.group(3))) {
        long l1 = Long.parseLong(m1.group(2));
        long l2 = Long.parseLong(m2.group(2));
        return l1<l2?-1:1;
      } else {
        return s1.compareTo(s2);
      }
    }    
  }  

  /**
   * Make a guess at the mime type for a channel that has not specified one.
   * 
   * @param mime         the original mime type
   * @param channelName  the name of the channel
   * @return             the (possibly) modified mime type
   * @since              1.3
   */
  public static String fixMime(String mime, String channelName) {
    if (mime != null) {
      return mime;
    }
    
    if (channelName.endsWith(".jpg")) {
      mime = "image/jpeg";
    } else if (channelName.startsWith("_Log")) {
      mime = "text/plain";
    } else {
      mime = "application/octet-stream";
    }
    return mime;
  }
  
  /**
   * Returns a list of all the names of the channels in the channel tree.
   * 
   * @param ctree   the channel tree
   * @param hidden  include hidden channels
   * @return        a list of channel names
   * @sicne         1.3
   */
  @SuppressWarnings("unchecked")
  public static List getAllChannels(ChannelTree ctree, boolean hidden) {
    ArrayList channels = new ArrayList();
    Iterator it = ctree.iterator();
    while (it.hasNext()) {
      ChannelTree.Node node = (ChannelTree.Node)it.next();
      if (node.getType() == ChannelTree.CHANNEL &&
          ((!node.getName().startsWith("_") && !node.getParent().getName().startsWith("_"))|| hidden)) {
        channels.add(node.getFullName());
      }
    }
    return channels;
  }
  
  /**
   * Returns all the names of children of this node that are channels.
   * 
   * @param source  the source node
   * @param hidden  include hidden channels
   * @return        a list of channel names
   * @since         1.3
   */
  @SuppressWarnings("unchecked")
  public static List getChildChannels(ChannelTree.Node source, boolean hidden) {
    ArrayList channels = new ArrayList();
    Iterator children = source.getChildren().iterator();
    while (children.hasNext()) {
      ChannelTree.Node node = (ChannelTree.Node)children.next();
      if (node.getType() == ChannelTree.CHANNEL &&
          ((!node.getName().startsWith("_") && !node.getParent().getName().startsWith("_"))|| hidden)) {
        channels.add(node.getFullName());
      }
    }
    return channels;
  }
  
  /**
   * A date format for IS8601 date and time representation. This representation
   * is to the second in UTC time.
   */
  private static final SimpleDateFormat ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  static {
    ISO8601_DATE_FORMAT.setTimeZone(new SimpleTimeZone(0, "UTC"));
  }
  
  /**
   * Converts the date and time provided in numbers of seconds since the epoch
   * to a ISO8601 date and time representation (UTC to the millisecond).
   * 
   * @param date  seconds since the epoch
   * @return      ISO8601 date and time
   * @since       1.3
   */
  public static String secondsToISO8601(double date) {
    return ISO8601_DATE_FORMAT.format(new Date(((long)(date*1000))));
  }
  
  /**
   * Converts an ISO8601 timestamp into a RBNB timestamp.
   * 
   * @param iso8601          an IS8601 timestamp
   * @return                 a RBNB timestamp
   * @throws ParseException  if the timestamp is not valid
   */
  public static double ISO8601ToSeconds(String iso8601) throws ParseException {
    return ISO8601_DATE_FORMAT.parse(iso8601).getTime()/1000d;
  }
  
  /**
   * Converts the date and time provided in numbers of milliseconds since the
   * epoch to a ISO8601 date and time representation (UTC to the millisecond).
   * 
   * @param date  milliseconds since the epoch
   * @return      ISO8601 date and time
   * @since       1.3
   */
  public static String millisecondsToISO8601(long date) {
    return ISO8601_DATE_FORMAT.format(new Date(date));
  }
  
  
  /**
   * returns List of all channels in a ChannelMap
   * 
   * @param channelMap  ChannelMap
   * @return List<String> of channels
   */
  public static List<String> getChannelList(ChannelMap channelMap) {
    
    String[] channels = channelMap.GetChannelList();
    List<String> channelList = new ArrayList<String>();
    
    for (int i=0; i<channels.length; i++) {
      channelList.add(channels[i]);
    }
    
    return channelList;
  }
  
  /**
   * Gets a channel map with the channels with the specified MIME types added to
   * it.
   * 
   * @param sink            the sink to use to connect to the server
   * @param desiredMime     the mime type to look for
   * @return                a channel map with channels of the specified mime
   *                        type
   * @throws SAPIException  if there is an error connecting to the server
   */
  public static ChannelMap getChannelMap(Sink sink, String desiredMime) throws SAPIException {
    ChannelMap metadataChannelMap = RBNBUtilities.getMetadata(sink);
    ChannelTree metadataChannelTree = ChannelTree.createFromChannelMap(metadataChannelMap);
    
    ChannelMap channelMap = new ChannelMap();
    
    for (String channel : metadataChannelMap.GetChannelList()) {
      String mime = metadataChannelTree.findNode(channel).getMime();
      if (mime != null && mime.compareToIgnoreCase(desiredMime) == 0) {
        channelMap.Add(channel);
      }
    }

    return channelMap;
  }
  
  /**
   * Get the metadata for all channels using the specified sink.
   * 
   * @param sink            the sink to use
   * @return                the metadata for all the channels
   * @throws SAPIException  if there is an error connecting to the server
   */
  public static ChannelMap getMetadata(Sink sink) throws SAPIException {
    return getMetadata(sink, new ChannelMap());
  }
  
  /**
   * Get the metadata for the channels in the channel map using the specified
   * sink.
   * 
   * @param sink               the sink to use
   * @param requestChannelMap  a channel map with the channels to get
   * @return                   the metadata for all the channels
   * @throws SAPIException     if there is an error connecting to the server
   */
  public static ChannelMap getMetadata(Sink sink, ChannelMap requestChannelMap) throws SAPIException {
    sink.RequestRegistration(requestChannelMap);
    ChannelMap metadataChannelMap = sink.Fetch(-1);
    return metadataChannelMap;    
  }
  
  /**
   * Get the metadata for the specified channel.
   * 
   * @param serverAddress   the address of the RBNB server
   * @param channel         the channel to get the metadata for
   * @return                the channel metadata, or null if the channel is not
   *                        found
   * @throws SAPIException  if there is an error connecting to the server
   */
  public static Node getMetadata(String serverAddress, String channel) throws SAPIException {
    ChannelMap cmap = getMetadata(serverAddress, Collections.singletonList(channel));
    ChannelTree ctree = ChannelTree.createFromChannelMap(cmap);
    return ctree.findNode(channel);
  }
  
  /**
   * Gets the metadata channel map from the specified server for the specified
   * channels.
   * 
   * @param serverAddress   the address of the server
   * @param channels        the list of channels
   * @return                the metadata channel map
   * @throws SAPIException  if there is an error connecting to the server
   */
  public static ChannelMap getMetadata(String serverAddress, List<String> channels) throws SAPIException {
    ChannelMap cmap = new ChannelMap();
    
    for (String channel : channels) {
      cmap.Add(channel);
    }
    
    Sink sink = new Sink();
    
    try {
      sink.OpenRBNBConnection(serverAddress, "RBNBUtilities");
      sink.RequestRegistration(cmap);
      cmap = sink.Fetch(-1);
    } finally {
      sink.CloseRBNBConnection();
    }
    
    return cmap;
  }
  
  /** left "CVS" in name for legacy compatibility */
  public static String getCVSVersionString() {
    return ("$LastChangedDate$\n"
        + "$LastChangedRevision$"
        + "$LastChangedBy$"
        + "$HeadURL$");
  } // getCVSVersionString ()
  
  
} // class