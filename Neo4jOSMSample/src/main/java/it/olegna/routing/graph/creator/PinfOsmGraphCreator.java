package it.olegna.routing.graph.creator;

import it.olegan.routing.exception.GraphCreatorException;
import it.olegna.routing.osmosis.sinks.CreateGraphSinkImpl;
import it.olegna.routing.osmosis.sinks.PrepareNodeIdsSinkImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;
import org.springframework.util.StopWatch;

import com.carrotsearch.hppc.LongLongOpenHashMap;
import com.carrotsearch.hppc.LongSet;

public class PinfOsmGraphCreator {
	/**
	 * The logger
	 */
	private static final Log logger = LogFactory.getLog(PinfOsmGraphCreator.class.getName());
	private String osmFilesDirectory;
	private String neo4jDbPath;

	public PinfOsmGraphCreator() {

		try {
			Properties props = new Properties();
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));
			osmFilesDirectory = props.getProperty("pinf.osm.files.directory");
			neo4jDbPath = props.getProperty("pinf.neo4j.db.path");
		} catch (Exception e) {

			String s = "Errore durante il caricamento del file di properties; messaggio errore: " + e.getMessage();
			logger.fatal(s, e);
			throw new IllegalStateException(s, e);
		}
	}
	public void createGraph() throws Exception {
		if (osmFilesDirectory == null || osmFilesDirectory.trim().equals("")) {

			throw new IllegalArgumentException("Impossibile importare dati OSM; none directory vuoto o nullo <" + osmFilesDirectory + ">");
		}
		File graphDb = new File(neo4jDbPath);
		if( graphDb.exists() && graphDb.listFiles() != null && graphDb.listFiles().length > 0 ){

			if( logger.isWarnEnabled() ){

				logger.warn("Nuovo processo di import iniziato; cancellazione vecchio grafo");
			}
			try {
				FileUtils.cleanDirectory(graphDb);
			} catch (IOException e) {
				
				logger.fatal("Errore nella cancellazione del vecchio grafo", e);
			}
		}
		File osm = new File(osmFilesDirectory);
		// Se è una directory leggo tutti i file OSM
		if (osm.isDirectory()) {
			File[] osmFiles = (new File(osmFilesDirectory)).listFiles();
			for (int i = 0; i < osmFiles.length; i++) {
				File file = osmFiles[i];
				try {
					process(file);
				} catch (Exception e) {

					String message = "Errore nella lettura del file OSM " + file.getAbsolutePath() + ". Messaggio errore: " + e.getMessage();
					logger.fatal(message, e);
					throw e;
				}
			}
		} else {

			try {
				process(osm);
			} catch (Exception e) {

				String message = "Errore nella lettura del file OSM " + osm.getAbsolutePath() + ". Messaggio errore: " + e.getMessage();
				logger.fatal(message, e);
				throw e;
			}
		}
	}

	private void process(File file) throws Exception {
		if (logger.isInfoEnabled()) {

			logger.info("Inizio prima fase creazione grafo; recupero gli ID dei nodi da creare");
		}
		StopWatch sw = new StopWatch();
		Sink prepareNodeIdsSink = new PrepareNodeIdsSinkImpl();
		sw.start("recupero id nodi da file " + file.getAbsolutePath());
		readOsmFile(file, prepareNodeIdsSink);
		sw.stop();
		LongLongOpenHashMap nodesMap = ( (PrepareNodeIdsSinkImpl)prepareNodeIdsSink ).getNodesMap();
		LongSet osmIdStoreRequiredSet =  ( (PrepareNodeIdsSinkImpl)prepareNodeIdsSink ).getOsmIdStoreRequiredSet();
		LongLongOpenHashMap osmWayIdToRouteWeightMap = ( (PrepareNodeIdsSinkImpl)prepareNodeIdsSink ).getRelFlagsMap();
		
		Sink createGraphSink = new CreateGraphSinkImpl(file.getAbsolutePath(), neo4jDbPath, nodesMap, osmWayIdToRouteWeightMap, osmIdStoreRequiredSet);
		sw.start("creazione grafo");
		readOsmFile(file, createGraphSink);
		sw.stop();
		GraphCreatorException graphCreationException = ((CreateGraphSinkImpl) createGraphSink).getGraphCreationException();
		if (graphCreationException != null) {

			throw graphCreationException;
		}
		if (logger.isInfoEnabled()) {

			logger.info("Seconda fase (" + sw.getLastTaskName() + ") terminata in " + sw.getLastTaskTimeMillis() + " millisecondi; grafo creato");
		}
	}

	private void readOsmFile(File file, Sink currentReader) throws FileNotFoundException {
		boolean pbf = false;
		CompressionMethod compression = CompressionMethod.None;
		if (file.getName().endsWith(".pbf")) {
			pbf = true;
		} else if (file.getName().endsWith(".gz")) {
			compression = CompressionMethod.GZip;
		} else if (file.getName().endsWith(".bz2")) {
			compression = CompressionMethod.BZip2;
		}
		RunnableSource reader;
		if (pbf) {
			reader = new crosby.binary.osmosis.OsmosisReader(new FileInputStream(file));
		} else {
			reader = new XmlReader(file, false, compression);
		}
		reader.setSink(currentReader);
		Thread readerThread = new Thread(reader);
		readerThread.start();
		while (readerThread.isAlive()) {
			try {
				readerThread.join();
			} catch (InterruptedException e) {
				/* do nothing */
			}
		}
	}
}
