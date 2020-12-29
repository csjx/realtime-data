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
package edu.hawaii.soest.pacioos.text.concurrent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tech.tablesaw.api.Table;

import java.nio.file.Path;

/**
 * Represents the result of writing a samples table to a file path
 */
public class WriteResult {

    Log log = LogFactory.getLog(WriteResult.class);

    /* The path to the written file */
    private Path path;

    /* The table that was written */
    private Table table;

    /* Any message created during an exception */
    private String message;

    /**
     * Construct a write result
     * @param path the path to the written file
     */
    public WriteResult(Path path) {
        this.path = path;
    }

    /**
     * Get the write result path
     * @return the write result path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Set the write result path
     * @param path the write result path
     */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * Get the write result table
     * @return the write result table
     */
    public Table getTable() {
        return table;
    }

    /**
     * Set the write result table
     * @param table the write result table
     */
    public void setTable(Table table) {
        this.table = table;
    }

    /**
     * Get the write result message
     * @return the write result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the write result message
     * @param message the write result message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
