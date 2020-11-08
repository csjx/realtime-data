/*
 *  Copyright: 2020 Regents of the University of Hawaii and the
 *             School of Ocean and Earth Science and Technology
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
package edu.hawaii.soest.pacioos.text.configure;

/**
 * The properties needed to configure an archiver as defined in the
 * instrument XML configuration file
 */
public class ArchiverConfiguration {

    /* The archive type */
    private String archiveType;

    /* The archive interval (hourly, daily) */
    private int archiveInterval;

    /* The archive base directory for storing data */
    private String archiveBaseDirectory;

    /**
     * Construct an empty archive configuration
     */
    public ArchiverConfiguration() {

    }

    /**
     * Construct an archiver configuration
     * @param archiveType the archive type
     * @param archiveInterval the archive interval
     * @param archiveBaseDirectory the archive base directory
     */
    public ArchiverConfiguration(String archiveType, int archiveInterval,
                                 String archiveBaseDirectory) {
        this.archiveType = archiveType;
        this.archiveInterval = archiveInterval;
        this.archiveBaseDirectory = archiveBaseDirectory;
    }

    /**
     * Get the archive type
     * @return archiveType the archive type
     */
    public String getArchiveType() {
        return archiveType;
    }

    /**
     * Set the archive type
     * @param archiveType  the archive type
     */
    public void setArchiveType(String archiveType) {
        this.archiveType = archiveType;
    }

    /**
     * Get the archive interval
     * @return archiveInterval the archive interval
     */
    public int getArchiveInterval() {
        return archiveInterval;
    }

    /**
     * Set the archive interval
     * @param archiveInterval  the archive interval
     */
    public void setArchiveInterval(int archiveInterval) {
        this.archiveInterval = archiveInterval;
    }

    /**
     * Get the archive base directory
     * @return archiveBaseDirectory the archive base directory
     */
    public String getArchiveBaseDirectory() {
        return archiveBaseDirectory;
    }

    /**
     * Set the archive base directory
     * @param archiveBaseDirectory  the archive base directory
     */
    public void setArchiveBaseDirectory(String archiveBaseDirectory) {
        this.archiveBaseDirectory = archiveBaseDirectory;
    }
}
