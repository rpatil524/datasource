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
package edu.ucdenver.ccp.fileparsers.kegg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import edu.ucdenver.ccp.common.collections.CollectionsUtil;
import edu.ucdenver.ccp.common.file.CharacterEncoding;
import edu.ucdenver.ccp.common.file.FileUtil;
import edu.ucdenver.ccp.datasource.fileparsers.RecordReader;
import edu.ucdenver.ccp.datasource.fileparsers.test.RecordReaderTester;
import edu.ucdenver.ccp.datasource.identifiers.DataSourceIdentifier;
import edu.ucdenver.ccp.datasource.identifiers.kegg.KeggGeneID;
import edu.ucdenver.ccp.datasource.identifiers.ncbi.gene.EntrezGeneID;

/**
 * 
 * @author Bill Baumgartner
 * 
 */
public class KeggGeneIdListFileParserTest extends RecordReaderTester {

	private static final String SAMPLE_KEGG_GENE_ID_LIST_FILE_NAME = "KEGG_aae_ncbi-geneid.list";

	@Override
	protected String getSampleFileName() {
		return SAMPLE_KEGG_GENE_ID_LIST_FILE_NAME;
	}

	@Override
	protected RecordReader<?> initSampleRecordReader() throws IOException {
		return new KeggGeneIdListFileParser(sampleInputFile, CharacterEncoding.US_ASCII);
	}

	@Test
	public void testParser() {
		try {
			KeggGeneIdListFileParser parser = new KeggGeneIdListFileParser(sampleInputFile, CharacterEncoding.US_ASCII);

			if (parser.hasNext()) {
				/* aae:aq_001 ncbi-geneid:1192533 */
				KeggGeneIdListFileData record1 = parser.next();
				assertEquals(new KeggGeneID("aq_001"), record1.getKeggGeneID());
				assertEquals(new EntrezGeneID("1192533"), record1.getExternalGeneID());
			} else {
				fail("Parser should have returned a record here.");
			}

			if (parser.hasNext()) {
				/* aae:aq_005 ncbi-geneid:1192534 */
				KeggGeneIdListFileData record2 = parser.next();
				assertEquals(new KeggGeneID("aq_005"), record2.getKeggGeneID());
				assertEquals(new EntrezGeneID("1192534"), record2.getExternalGeneID());
			} else {
				fail("Parser should have returned a record here.");
			}

			if (parser.hasNext()) {
				/* aae:aq_008 ncbi-geneid:1192535 */
				KeggGeneIdListFileData record3 = parser.next();
				assertEquals(new KeggGeneID("aq_008"), record3.getKeggGeneID());
				assertEquals(new EntrezGeneID("1192535"), record3.getExternalGeneID());
			} else {
				fail("Parser should have returned a record here.");
			}

			if (parser.hasNext()) {
				/* aae:aq_008 ncbi-geneid:1192535 */
				KeggGeneIdListFileData record3 = parser.next();
				assertEquals(new KeggGeneID("aq_008"), record3.getKeggGeneID());
				assertEquals(new EntrezGeneID("1234567"), record3.getExternalGeneID());
			} else {
				fail("Parser should have returned a record here.");
			}

			assertFalse(parser.hasNext());

		} catch (IOException ioe) {
			ioe.printStackTrace();
			fail("Parser threw an IOException");
		}

	}

	@Test
	public void testGetInternal2ExternalGeneIDMap() throws Exception {
		Map<KeggGeneID, Set<DataSourceIdentifier<?>>> keggInternal2ExternalGeneIDMap = KeggGeneIdListFileParser
				.getInternal2ExternalGeneIDMap(sampleInputFile, CharacterEncoding.US_ASCII);

		Map<KeggGeneID, Set<EntrezGeneID>> expectedMap = new HashMap<KeggGeneID, Set<EntrezGeneID>>();
		expectedMap.put(new KeggGeneID("aq_001"), CollectionsUtil.createSet(new EntrezGeneID("1192533")));
		expectedMap.put(new KeggGeneID("aq_005"), CollectionsUtil.createSet(new EntrezGeneID("1192534")));
		expectedMap.put(new KeggGeneID("aq_008"),
				CollectionsUtil.createSet(new EntrezGeneID("1192535"), new EntrezGeneID("1234567")));

		assertEquals(expectedMap, keggInternal2ExternalGeneIDMap);
	}

	protected Map<File, List<String>> getExpectedOutputFile2LinesMap() {
		final String NS = "<http://kabob.ucdenver.edu/ice/kegg/";
		List<String> lines = CollectionsUtil
				.createList(
						NS
								+ "KEGG_GENE_aq_001_ICE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://kabob.ucdenver.edu/ice/kegg/KeggGeneIce1> .",
						NS + "KEGG_GENE_aq_001_ICE> <http://www.genome.jp/kegg/hasKeggGeneID> \"aq_001\"@en .",
						NS
								+ "KEGG_GENE_aq_001_ICE> <http://www.genome.jp/kegg/isLinkedToEntrezGeneICE> <http://www.ncbi.nlm.nih.gov/gene/EG_1192533_ICE> .",
						NS
								+ "KEGG_GENE_aq_005_ICE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://kabob.ucdenver.edu/ice/kegg/KeggGeneIce1> .",
						NS + "KEGG_GENE_aq_005_ICE> <http://www.genome.jp/kegg/hasKeggGeneID> \"aq_005\"@en .",
						NS
								+ "KEGG_GENE_aq_005_ICE> <http://www.genome.jp/kegg/isLinkedToEntrezGeneICE> <http://www.ncbi.nlm.nih.gov/gene/EG_1192534_ICE> .",
						NS
								+ "KEGG_GENE_aq_008_ICE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://kabob.ucdenver.edu/ice/kegg/KeggGeneIce1> .",
						NS + "KEGG_GENE_aq_008_ICE> <http://www.genome.jp/kegg/hasKeggGeneID> \"aq_008\"@en .",
						NS
								+ "KEGG_GENE_aq_008_ICE> <http://www.genome.jp/kegg/isLinkedToEntrezGeneICE> <http://www.ncbi.nlm.nih.gov/gene/EG_1192535_ICE> .",
						NS
								+ "KEGG_GENE_aq_008_ICE> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://kabob.ucdenver.edu/ice/kegg/KeggGeneIce1> .",
						NS + "KEGG_GENE_aq_008_ICE> <http://www.genome.jp/kegg/hasKeggGeneID> \"aq_008\"@en .",
						NS
								+ "KEGG_GENE_aq_008_ICE> <http://www.genome.jp/kegg/isLinkedToEntrezGeneICE> <http://www.ncbi.nlm.nih.gov/gene/EG_1234567_ICE> .");
		Map<File, List<String>> file2LinesMap = new HashMap<File, List<String>>();
		file2LinesMap.put(FileUtil.appendPathElementsToDirectory(outputDirectory, "kegg-genelist-KEGG.nt"), lines);
		return file2LinesMap;
	}

	protected Map<String, Integer> getExpectedFileStatementCounts() {
		Map<String, Integer> counts = new HashMap<String, Integer>();
		counts.put("kegg-genelist.nt", 12);
		counts.put("kabob-meta-kegg-genelist.nt", 6);
		return counts;
	}

}
