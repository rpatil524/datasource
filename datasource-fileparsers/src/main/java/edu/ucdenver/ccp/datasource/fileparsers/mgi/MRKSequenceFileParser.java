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
package edu.ucdenver.ccp.fileparsers.mgi;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.ucdenver.ccp.common.download.FtpDownload;
import edu.ucdenver.ccp.common.file.CharacterEncoding;
import edu.ucdenver.ccp.common.file.reader.Line;
import edu.ucdenver.ccp.common.file.reader.StreamLineReader;
import edu.ucdenver.ccp.common.ftp.FTPUtil.FileType;
import edu.ucdenver.ccp.common.string.RegExPatterns;
import edu.ucdenver.ccp.datasource.fileparsers.SingleLineFileRecordReader;
import edu.ucdenver.ccp.datasource.identifiers.DataSourceIdentifier;
import edu.ucdenver.ccp.datasource.identifiers.NucleotideAccessionResolver;
import edu.ucdenver.ccp.datasource.identifiers.ebi.uniprot.UniProtID;
import edu.ucdenver.ccp.datasource.identifiers.ensembl.EnsemblGeneID;
import edu.ucdenver.ccp.datasource.identifiers.mgi.MgiGeneID;
import edu.ucdenver.ccp.datasource.identifiers.ncbi.UniGeneID;
import edu.ucdenver.ccp.datasource.identifiers.ncbi.refseq.RefSeqID;
import edu.ucdenver.ccp.datasource.identifiers.other.VegaID;
import edu.ucdenver.ccp.fileparsers.download.FtpHost;
import edu.ucdenver.ccp.fileparsers.field.ChromosomeNumber;

/**
 * This class is used to parse MGI MRK_Sequence.rpt files
 * 
 * @author Bill Baumgartner
 * 
 */
public class MRKSequenceFileParser extends SingleLineFileRecordReader<MRKSequenceFileData> {

	private static final String HEADER = "MGI Marker Accession ID\tMarker Symbol\tStatus\tMarker Type\tMarker Name\tcM position\tChromosome\tGenome Coordinate Start\tGenome Coordinate End\tStrand\tGenBank ID\tRefSeq transcript ID\tVEGA transcript ID\tEnsembl transcript ID\tUniProt ID\tTrEMBL ID\tVEGA protein ID\tEnsembl protein ID\tRefSeq protein ID\tUniGene ID";

	private static final Logger logger = Logger.getLogger(MRKSequenceFileParser.class);

	public static final String FTP_FILE_NAME = "MRK_Sequence.rpt";
	public static final CharacterEncoding ENCODING = CharacterEncoding.US_ASCII;

	@FtpDownload(server = FtpHost.MGI_HOST, path = FtpHost.MGI_REPORTS_PATH, filename = FTP_FILE_NAME, filetype = FileType.ASCII)
	private File mrkSequenceRptFile;

	public MRKSequenceFileParser(File file, CharacterEncoding encoding) throws IOException {
		super(file, encoding, null);
	}

	public MRKSequenceFileParser(File workDirectory, boolean clean) throws IOException {
		super(workDirectory, ENCODING, null, null, null, clean);
	}

	@Override
	protected StreamLineReader initializeLineReaderFromDownload(CharacterEncoding encoding, String skipLinePrefix)
			throws IOException {
		return new StreamLineReader(mrkSequenceRptFile, encoding, skipLinePrefix);
	}

	@Override
	protected String getFileHeader() throws IOException {
		return readLine().getText();
	}

	@Override
	protected String getExpectedFileHeader() throws IOException {
		return HEADER;
	}

	@Override
	protected MRKSequenceFileData parseRecordFromLine(Line line) {
		String[] toks = line.getText().split("\\t", -1);

		MgiGeneID mgiAccessionID = new MgiGeneID(toks[0]);
		String markerSymbol = new String(toks[1]);
		String status = toks[2];
		MgiGeneType markerType = MgiGeneType.getValue(toks[3]);
		String markerName = new String(toks[4]);
		String cM_Position = toks[5];
		ChromosomeNumber chromosome = null;
		if (!toks[6].equals("UN")) {
			chromosome = new ChromosomeNumber(toks[6]);
		}
		Integer genomeCoordinateStart = null;
		if (!toks[7].isEmpty()) {
			genomeCoordinateStart = Integer.parseInt(toks[7]);
		}
		Integer genomeCoordinateEnd = null;
		if (!toks[8].isEmpty()) {
			genomeCoordinateEnd = Integer.parseInt(toks[8]);
		}
		String strand = null;
		if (!toks[9].isEmpty()) {
			strand = toks[9];
		}
		Set<DataSourceIdentifier<?>> genBankAccessionIDs = new HashSet<DataSourceIdentifier<?>>();
		if (!toks[10].isEmpty()) {
			String[] genBankIDs = toks[10].split(RegExPatterns.PIPE);
			for (String genBankID : genBankIDs) {
				if (genBankID.trim().length() > 0) {
					try {
						DataSourceIdentifier<String> resolveNucleotideAccession = NucleotideAccessionResolver
								.resolveNucleotideAccession(genBankID);
						genBankAccessionIDs.add(resolveNucleotideAccession);
					} catch (IllegalArgumentException e) {
						logger.warn("Unable to resolve supposed GenBank id: " + genBankID);
					}
				}
			}
		}
		Set<RefSeqID> refseqTranscriptIds = new HashSet<RefSeqID>();
		if (!toks[11].isEmpty()) {
			String[] ids = toks[11].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					refseqTranscriptIds.add(new RefSeqID(id));
				}
			}
		}
		Set<VegaID> vegaTranscriptIds = new HashSet<VegaID>();
		if (!toks[12].isEmpty()) {
			String[] ids = toks[12].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					vegaTranscriptIds.add(new VegaID(id));
				}
			}
		}
		Set<EnsemblGeneID> ensemblTranscriptIds = new HashSet<EnsemblGeneID>();
		if (!toks[13].isEmpty()) {
			String[] ids = toks[13].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					ensemblTranscriptIds.add(new EnsemblGeneID(id));
				}
			}
		}
		Set<UniProtID> uniprotIds = new HashSet<UniProtID>();
		if (!toks[14].isEmpty()) {
			String[] ids = toks[14].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					uniprotIds.add(new UniProtID(id));
				}
			}
		}
		Set<UniProtID> tremblIds = new HashSet<UniProtID>();
		if (!toks[15].isEmpty()) {
			String[] ids = toks[15].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					tremblIds.add(new UniProtID(id));
				}
			}
		}
		Set<VegaID> vegaProteinIds = new HashSet<VegaID>();
		if (!toks[16].isEmpty()) {
			String[] ids = toks[16].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					vegaProteinIds.add(new VegaID(id));
				}
			}
		}
		Set<EnsemblGeneID> ensemblProteinIds = new HashSet<EnsemblGeneID>();
		if (!toks[17].isEmpty()) {
			String[] ids = toks[17].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					ensemblProteinIds.add(new EnsemblGeneID(id));
				}
			}
		}
		Set<RefSeqID> refseqProteinIds = new HashSet<RefSeqID>();
		if (!toks[18].isEmpty()) {
			String[] ids = toks[18].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					try {
						RefSeqID refSeqID = new RefSeqID(id);
						refseqProteinIds.add(refSeqID);
					} catch (IllegalArgumentException e) {
						logger.warn("Detected illegal RefSeq identifier: " + id + " on line: " + line.getLineNumber());
					}
				}
			}
		}
		Set<UniGeneID> unigeneIds = new HashSet<UniGeneID>();
		if (!toks[19].isEmpty()) {
			String[] ids = toks[19].split(RegExPatterns.PIPE);
			for (String id : ids) {
				if (id.trim().length() > 0) {
					unigeneIds.add(new UniGeneID(id));
				}
			}
		}

		return new MRKSequenceFileData(mgiAccessionID, markerSymbol, status, markerType, markerName, cM_Position,
				chromosome, genomeCoordinateStart, genomeCoordinateEnd, strand, genBankAccessionIDs,
				refseqTranscriptIds, vegaTranscriptIds, ensemblTranscriptIds, uniprotIds, tremblIds, vegaProteinIds,
				ensemblProteinIds, refseqProteinIds, unigeneIds, line.getByteOffset(), line.getLineNumber());

	}

	/**
	 * Returns a map from refseq ID to MGI IDs
	 * 
	 * @param mrkSequenceRptFile
	 * @return
	 */
	public static Map<RefSeqID, Set<MgiGeneID>> getRefSeqID2MgiIDsMap(File mrkSequenceRptFile,
			CharacterEncoding encoding) throws IOException {
		Map<RefSeqID, Set<MgiGeneID>> refseqID2MgiIDMap = new HashMap<RefSeqID, Set<MgiGeneID>>();

		MRKSequenceFileParser parser = null;
		try {
			parser = new MRKSequenceFileParser(mrkSequenceRptFile, encoding);
			while (parser.hasNext()) {
				MRKSequenceFileData dataRecord = parser.next();
				MgiGeneID mgiID = dataRecord.getMgiAccessionID();
				Set<RefSeqID> refSeqIDs = dataRecord.getRefseqProteinIds();
				refSeqIDs.addAll(dataRecord.getRefseqTranscriptIds());

				for (RefSeqID refseqID : refSeqIDs) {
					if (refseqID2MgiIDMap.containsKey(refseqID)) {
						refseqID2MgiIDMap.get(refseqID).add(mgiID);
					} else {
						Set<MgiGeneID> mgiIDs = new HashSet<MgiGeneID>();
						mgiIDs.add(mgiID);
						refseqID2MgiIDMap.put(refseqID, mgiIDs);
					}
				}
			}
			return refseqID2MgiIDMap;
		} finally {
			if (parser != null) {
				parser.close();
			}
		}
	}

}
