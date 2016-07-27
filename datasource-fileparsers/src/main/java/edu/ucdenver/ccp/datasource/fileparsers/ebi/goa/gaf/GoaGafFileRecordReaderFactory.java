package edu.ucdenver.ccp.datasource.fileparsers.ebi.goa.gaf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import edu.ucdenver.ccp.common.collections.CollectionsUtil;
import edu.ucdenver.ccp.common.download.DownloadUtil;
import edu.ucdenver.ccp.common.file.CharacterEncoding;
import edu.ucdenver.ccp.common.file.FileUtil;
import edu.ucdenver.ccp.common.ftp.FTPUtil;
import edu.ucdenver.ccp.common.ftp.FTPUtil.FileType;
import edu.ucdenver.ccp.datasource.fileparsers.RecordReader;
import edu.ucdenver.ccp.datasource.identifiers.IdResolver;
import edu.ucdenver.ccp.datasource.identifiers.ncbi.taxonomy.NcbiTaxonomyID;

/**
 * A factory class for producing {@link RecordReader} instances for the GOA data set.
 * @author Center for Computational Pharmacology, UC Denver; ccpsupport@ucdenver.edu
 *
 */
public class GoaGafFileRecordReaderFactory {

	/**
	 * The new file structure for GOA divides the annotations into 4 different
	 * sets: We now publish the following four annotation sets per species,
	 * which we provide in both GAF and GPAD format, the format being indicated
	 * by the file suffix:
	 * 
	 * - goa_{species}.[gaf|gpa] - annotations to canonical accessions from the
	 * UniProt reference proteome <br>
	 * - goa_{species}_isoform.[gaf|gpa] - annotations to isoforms from the
	 * UniProt reference proteome <br>
	 * - goa_{species}_complex.[gaf|gpa] - annotations to complexes <br>
	 * - goa_{species}_rna.[gaf|gpa] - annotations to RNAs
	 * 
	 * Note that annotations to proteins that are not part of the UniProt
	 * reference proteome for a species are still available in the full
	 * goa_uniprot annotation set (goa_uniprot_all.[gaf|gpa]).
	 */
	public enum AnnotationType {
		CANONICAL(".gaf.gz"), 
		ISOFORM("_isoform.gaf.gz"), 
		COMPLEX("_complex.gaf.gz"), 
		RNA("_rna.gaf.gz");
		
		private final String suffix;
		private AnnotationType(String suffix) {
			this.suffix = suffix;
		}
		public String suffix() {
			return this.suffix;
		}
	}
	
	/* @formatter:off */
	//@formatter:off
	private static final Map<NcbiTaxonomyID, FileInfo> taxonomyID2goaFilePathMap = new HashMap<NcbiTaxonomyID, FileInfo>();
	static {
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.ARABIDOPSIS, new FileInfo("pub/databases/GO/goa/ARABIDOPSIS","goa_arabidopsis"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.CHICKEN, new FileInfo("pub/databases/GO/goa/CHICKEN","goa_chicken"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.COW, new FileInfo("pub/databases/GO/goa/COW","goa_cow"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.DICTY, new FileInfo("pub/databases/GO/goa/DICTY","goa_dicty"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.DOG, new FileInfo("pub/databases/GO/goa/DOG","goa_dog"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.FLY, new FileInfo("pub/databases/GO/goa/FLY","goa_fly"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.HUMAN, new FileInfo("pub/databases/GO/goa/HUMAN","goa_human"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.MOUSE, new FileInfo("pub/databases/GO/goa/MOUSE","goa_mouse"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.PIG, new FileInfo("pub/databases/GO/goa/PIG","goa_pig"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.RAT, new FileInfo("pub/databases/GO/goa/RAT","goa_rat"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.WORM, new FileInfo("pub/databases/GO/goa/WORM","goa_worm"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.SACCHAROMYCES_CEREVISIAE_S288C, new FileInfo("pub/databases/GO/goa/YEAST","goa_yeast"));
		taxonomyID2goaFilePathMap.put(NcbiTaxonomyID.ZEBRAFISH, new FileInfo("pub/databases/GO/goa/ZEBRAFISH","goa_zebrafish"));
	}
	/* @formatter:on */

	private static final CharacterEncoding ENCODING = CharacterEncoding.UTF_8;
	private static final Class<? extends IdResolver> ID_RESOLVER_CLASS = GoaFileIdResolver.class;

	private static final FileInfo ALL_SPECIES = new FileInfo("pub/databases/GO/goa/UNIPROT","goa_uniprot_all");
	
	private GoaGafFileRecordReaderFactory() {
		// not instantiable - utility class
	}

	/**
	 * @param dataDirectory
	 * @param taxonIds
	 * @param annotType
	 * @param clean
	 * @return an initialized {@link GoaGaf2FileRecordReader} instance based on
	 *         the input taxonomy identifiers and annotation type. Note: if
	 *         there is only one taxonomy ID, an attempt will be made to
	 *         download data specific to that taxon if it is available.
	 *         Otherwise, the "ALL" data set will be downloaded and parsed to
	 *         extract data for a single taxon or multiple taxons. If the "all"
	 *         data set is downloaded, then the annotation type is ignored as
	 *         there are not separate files for the different annotations types
	 *         for the "all" data set.
	 * @throws IOException
	 */
	public static GoaGaf2FileRecordReader getRecordReader(File dataDirectory, Set<NcbiTaxonomyID> taxonIds, AnnotationType annotType, boolean clean) throws IOException {
		if (taxonIds != null && taxonIds.size() == 1) {
			/* if there is only a single taxon Id, then try to download it individually to save time*/
			return getRecordReader(dataDirectory, CollectionsUtil.getSingleElement(taxonIds), annotType, clean);
		}
		/* otherwise, if there are no taxon ids or multiple taxon ids, then download the ALL file */
		File goaFile = downloadGoaFile(dataDirectory, ALL_SPECIES, AnnotationType.CANONICAL, clean);
		if (!DownloadUtil.readySemaphoreFileExists(goaFile)) {
			DownloadUtil.writeReadySemaphoreFile(goaFile);
		}
		return new GoaGaf2FileRecordReader(goaFile, ENCODING, 
				taxonIds, ID_RESOLVER_CLASS);
	}
	
	
	/**
	 * @param dataDirectory
	 * @param taxonomyID
	 * @param annotType
	 * @param clean
	 * @return an initialized {@link GoaGaf2FileRecordReader} instance based on
	 *         the input taxonomy identifiers and annotation type. If there is
	 *         not a download specific to the taxon of interest, then the "all"
	 *         data set will be downloaded and processed to extract data for the
	 *         taxon of interest.
	 * @throws IOException
	 */
	public static GoaGaf2FileRecordReader getRecordReader(File dataDirectory, NcbiTaxonomyID taxonomyID, AnnotationType annotType, boolean clean)
			throws IOException {
		File goaFile = null;
		if (!taxonomyID2goaFilePathMap.containsKey(taxonomyID)) {
			goaFile = downloadGoaFile(dataDirectory, ALL_SPECIES, annotType, clean);
		} else {
			goaFile	= downloadGoaFile(dataDirectory, taxonomyID2goaFilePathMap.get(taxonomyID), annotType, clean);
		}
		if (!DownloadUtil.readySemaphoreFileExists(goaFile)) {
			DownloadUtil.writeReadySemaphoreFile(goaFile);
		}
		return new GoaGaf2FileRecordReader(goaFile, ENCODING, 
				CollectionsUtil.createSet(taxonomyID), ID_RESOLVER_CLASS);
	}

	/**
	 * @param dataDirectory
	 * @param fileInfo
	 * @param annotType
	 * @param clean
	 * @return a reference to the local file that was downloaded
	 * @throws IOException
	 */
	private static File downloadGoaFile(File dataDirectory, FileInfo fileInfo, AnnotationType annotType, boolean clean)
			throws IOException {
		String fileName = fileInfo.getFilePrefix() + annotType.suffix();
		File localFile = FileUtil.appendPathElementsToDirectory(dataDirectory, fileName);
		if (!DownloadUtil.fileExists(localFile, null, clean, false)) {
			localFile = FTPUtil.downloadFile(fileInfo.getFtpServer(), fileInfo.getFtpPath(), fileName, FileType.BINARY, dataDirectory);
		}
		return localFile;

	}
	
	/**
	 * A simple class to store FTP information for a file
	 */
	@Data
	private static class FileInfo {
		private final String ftpPath;
		private final String filePrefix;
		private final String ftpServer = "ftp.ebi.ac.uk";
	}
	
}
