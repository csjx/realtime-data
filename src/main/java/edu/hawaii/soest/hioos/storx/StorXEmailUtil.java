/**
 *  Copyright: 2016 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
 *    Purpose: A utility class for managing email folders
 *
 *   Authors: Christopher Jones
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */ 
package edu.hawaii.soest.hioos.storx;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author cjones
 *
 */
public class StorXEmailUtil {

	private static String accountName = "HIOOS Water Quality Buoy Data";
	private static String server = "imap.gmail.com";
	private static String username = "hiooswqb";
	private static String password = "<password-goes-here>";
	private static String protocol = "imaps";
	private static String dataMailbox = "Backlog";
	private static String prefetch = "false";
	private static Session mailSession = null;
	private static Store mailStore = null;
	private static Folder backlog = null;
	static Log log = LogFactory.getLog(StorXEmailUtil.class);
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// get a connection to the mail server
		Properties props = System.getProperties();
		props.setProperty("mail.store.protocol", protocol);
		props.setProperty("mail.imaps.partialfetch", prefetch);

		log.debug("\n\nACCOUNT DETAILS: \n" + 
			    "accountName     : " + accountName + "\n" + 
				"server          : " + server + "\n" + 
			    "username        : " + username + "\n" + 
				"password        : " + password + "\n" + 
			    "protocol        : " + protocol + "\n" + 
				"dataMailbox     : " + dataMailbox + "\n" + 
				"prefetch        : " + prefetch + "\n");

		try {
	
			// create the imaps mail session
			mailSession = Session.getDefaultInstance(props, null);
			mailStore = mailSession.getStore(protocol);
			mailStore.connect(server, username, password);
			// get folder references for the inbox and processed data box
			backlog = mailStore.getFolder(dataMailbox);
			backlog.open(Folder.READ_WRITE);
			log.debug("total message count: " + backlog.getMessageCount());
	
			//createFolders(backlog);
			
			organizeMessages();
			
		} catch (NoSuchProviderException nspe) {
			nspe.printStackTrace();
	
		} catch (MessagingException e) {
			e.printStackTrace();
			
		} finally {
			try {
				mailStore.close();
				
			} catch (MessagingException e) {
				e.printStackTrace();
				
			}
		}
	}
	
	/**
	 * Creates a set of folders in the GMail accout based on year and month
	 * to organize the backlog of data and make it more manageable to process
	 * 
	 * @param parent - the parent folder to make the folders in
	 */
	public static void createFolders(Folder parent) {
		ArrayList<String> years = getYears();
		ArrayList<String> months = getMonths();
		
		boolean created = false;
		for ( String year : years ) {
			for (String month : months) {
				try {
					String folderName = year + "-" + month;
					Folder newFolder = parent.getFolder(folderName);   
					created = newFolder.create(Folder.HOLDS_MESSAGES);   
					log.debug("Created " + folderName + " : " + created);
					
				} catch (MessagingException e) {
					e.printStackTrace();
					
				}  

			}
		}
		
	}

	/**
	 * Move messages from the parent backlog folder to year-month folders based
	 * on the date sent of the message
	 */
	public static void organizeMessages() {
		ArrayList<String> years = getYears();
		ArrayList<String> months = getMonths();
		
		try {
			Message messages[];
			while ( ! backlog.isOpen() ) {
				backlog.open(Folder.READ_WRITE);

			}
			messages = (Message[]) backlog.getMessages();
		
			for ( Message message : messages ) {
				Date sentDate = message.getSentDate();
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM");
				String folderByDate = formatter.format(sentDate);
				// log.debug(folderByDate);

				Folder destinationFolder = backlog.getFolder(folderByDate);
				backlog.copyMessages(new Message[] {message}, destinationFolder);
				message.setFlag(Flags.Flag.DELETED, true);
				log.debug("Moved " + message.getSentDate() + " to " + destinationFolder.getFullName());
			}
			
		} catch (MessagingException e) {
			e.printStackTrace();
			
		} finally {
			try {
				mailStore.close();
				
			} catch (MessagingException e) {
				e.printStackTrace();
				
			}
		}

		
	}
	
	/**
	 * Get a list of years
	 * 
	 * @return
	 */
	public static ArrayList<String> getYears() {
		ArrayList<String> years = new ArrayList<String>();
		years.add("2012");
		years.add("2013");
		years.add("2014");
		years.add("2015");
		years.add("2016");
		
		return years;

	}

	/**
	 * Get a list of months of the year
	 * @return
	 */
	public static ArrayList<String> getMonths() {
		
		ArrayList<String> months = new ArrayList<String>();
		months.add("01");
		months.add("02");
		months.add("03");
		months.add("04");
		months.add("05");
		months.add("06");
		months.add("07");
		months.add("08");
		months.add("09");
		months.add("10");
		months.add("11");
		months.add("12");
		
		return months;
		
	}

}
