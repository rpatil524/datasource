/*
 * Copyright (C) 2009 Center for Computational Pharmacology, University of Colorado School of Medicine
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */
package edu.ucdenver.ccp.fileparsers.ncbi.taxonomy;

import org.apache.log4j.Logger;

import edu.ucdenver.ccp.common.file.reader.Line;
import edu.ucdenver.ccp.datasource.fileparsers.License;
import edu.ucdenver.ccp.datasource.fileparsers.Record;
import edu.ucdenver.ccp.datasource.fileparsers.RecordField;
import edu.ucdenver.ccp.datasource.fileparsers.SingleLineFileRecord;
import edu.ucdenver.ccp.datasource.identifiers.DataSource;
import edu.ucdenver.ccp.datasource.identifiers.ncbi.taxonomy.NcbiTaxonomyID;

/**
 * This class represents data contained in the NCBI Taxonomy names.dmp file, located in
 * taxdump.tar.gz here: ftp://ftp.ncbi.nih.gov/pub/taxonomy/
 * 
 * @author Bill Baumgartner
 * 
 */
@Record(dataSource = DataSource.NCBI_TAXON,
	comment="ftp://ftp.ncbi.nlm.nih.gov/pub/taxonomy/taxdump_readme.txt",
	license=License.NCBI,
	citation="The NCBI handbook [Internet]. Bethesda (MD): National Library of Medicine (US), National Center for Biotechnology Information; 2002 Oct. Chapter 4, The Taxonomy Project. Available from http://www.ncbi.nlm.nih.gov/books/NBK21091")
public class NcbiTaxonomyNamesDmpFileData extends SingleLineFileRecord {

	private static Logger logger = Logger.getLogger(NcbiTaxonomyNamesDmpFileData.class);

	@RecordField(comment="the id of node associated with this name", isKeyField=true)
	private final NcbiTaxonomyID taxonomyID;

	@RecordField(comment="name itself")
	private final String taxonomyName;

	@RecordField(comment="the unique variant of this name if name not unique")
	private final String uniqueName;

	@RecordField(comment="(synonym, common name, ...)")
	private final String nameClass;

	public NcbiTaxonomyNamesDmpFileData(NcbiTaxonomyID taxonomyID, String taxonomyName, String uniqueName,
			String nameClass, long byteOffset, long lineNumber) {
		super(byteOffset, lineNumber);
		this.taxonomyID = taxonomyID;
		this.taxonomyName = taxonomyName;
		this.uniqueName = uniqueName;
		this.nameClass = nameClass;
	}

	public NcbiTaxonomyID getTaxonomyID() {
		return taxonomyID;
	}

	public String getTaxonomyName() {
		return taxonomyName;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public String getNameClass() {
		return nameClass;
	}

	/**
	 * Parse a line from the NCBI Taxonomy nodes.dmp file
	 * 
	 * @param line
	 * @return
	 */
	public static NcbiTaxonomyNamesDmpFileData parseNCBITaxonomyNamesDmpLine(Line line) {
		String[] toks = line.getText().split("\\t\\|\\t");
		if (toks.length != 4) {
			logger.error("Unexpected number of tokens (" + toks.length + ") on line:"
					+ line.getText().replaceAll("\\t", " [TAB] "));
		}

		NcbiTaxonomyID taxonomyID = new NcbiTaxonomyID(toks[0]);
		String taxonomyName = toks[1];
		String uniqueName = toks[2];
		String nameClass = toks[3];
		return new NcbiTaxonomyNamesDmpFileData(taxonomyID, taxonomyName, uniqueName, nameClass, line.getByteOffset(),
				line.getLineNumber());

	}

}