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
 * Represents the resulting table from a ReadTask processing a single data file
 */
public class ReadResult {

    /* Set up a logger */
    private final Log log = LogFactory.getLog(ReadResult.class);

    /* The path to the file that the table with be created from*/
    private Path path;

    /* The table created from the path */
    private Table table;

    /**
     * Construct an empty table result
     */
    public ReadResult() {

    }

    /**
     * Construct a table result
     * @param path  the path to the file being turned into a table
     */
    public ReadResult(Path path) {
        this.path = path;
    }

    /**
     * Get the path of the data file for this table
     * @return path the path to the data
     */
    public Path getPath() {
        return path;
    }

    /**
     * Set the data file path
     * @param path the data file path
     */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * Get the generated table
     * @return table the generated table
     */
    public Table getTable() {
        return table;
    }

    /**
     * Set the generated table
     * @param table the generated table
     */
    public void setTable(Table table) {
        this.table = table;
    }
}
