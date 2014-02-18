package it.olegna.routing.graph;

import it.olegna.routing.osm.util.IConstants;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.analysis.function.Divide;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.pipes.GeoPipeline;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class GraphMgr {
	private GraphDatabaseService graphDbService;
	private GlobalGraphOperations ggo;
	private SpatialDatabaseService sdbs;
	private static final Log logger = LogFactory.getLog(GraphMgr.class.getName());
	private String neo4jPath;
	private SimplePointLayer el;
	private double maxDistance;
	private String nodestoreMappedMemorySize;
	private String relationshipstoreMappedMemorySize;
	private String nodestorePropertystoreMappedMemorySize;
	private String stringsMappedMemorySize;
	private String arraysMappedMemorySize;
	private String allowStoreUpgrade;
	private String cypherParserVersion;
	private String keepLogicalLogs;
	private String nodeAutoIndexing;
	private String nodeKeysIndexable;
	private String relationshipAutoIndexing;
	private String relationshipKeysIndexable;
	private double distKm;
	public void initialize() {
		StopWatch sw = new StopWatch();
		sw.start("neo4j initialization");
		GraphDatabaseFactory gdbf = new GraphDatabaseFactory();
		GraphDatabaseBuilder gdbb = gdbf.newEmbeddedDatabaseBuilder(neo4jPath);
		gdbb.setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size, nodestoreMappedMemorySize);
		gdbb.setConfig(GraphDatabaseSettings.relationshipstore_mapped_memory_size, relationshipstoreMappedMemorySize);
		gdbb.setConfig(GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size, nodestorePropertystoreMappedMemorySize);
		gdbb.setConfig(GraphDatabaseSettings.strings_mapped_memory_size, stringsMappedMemorySize);
		gdbb.setConfig(GraphDatabaseSettings.arrays_mapped_memory_size, arraysMappedMemorySize);
		if (StringUtils.hasText(allowStoreUpgrade) && !allowStoreUpgrade.startsWith("${")) {

			gdbb.setConfig(GraphDatabaseSettings.allow_store_upgrade, allowStoreUpgrade);
		}
		if (StringUtils.hasText(cypherParserVersion) && !cypherParserVersion.startsWith("${")) {

			gdbb.setConfig(GraphDatabaseSettings.cypher_parser_version, cypherParserVersion);
		}
		if (StringUtils.hasText(keepLogicalLogs) && !keepLogicalLogs.startsWith("${")) {

			gdbb.setConfig(GraphDatabaseSettings.keep_logical_logs, keepLogicalLogs);
		}
		if (StringUtils.hasText(nodeAutoIndexing) && !nodeAutoIndexing.startsWith("${")) {

			gdbb.setConfig(GraphDatabaseSettings.node_auto_indexing, nodeAutoIndexing);
		}
		if (StringUtils.hasText(nodeKeysIndexable) && !nodeKeysIndexable.startsWith("${")) {

			gdbb.setConfig(GraphDatabaseSettings.node_keys_indexable, nodeKeysIndexable);
		}
		if (StringUtils.hasText(relationshipAutoIndexing) && !relationshipAutoIndexing.startsWith("${")) {

			gdbb.setConfig(GraphDatabaseSettings.relationship_auto_indexing, relationshipAutoIndexing);
		}
		if (StringUtils.hasText(relationshipKeysIndexable) && !relationshipKeysIndexable.startsWith("${")) {

			gdbb.setConfig(GraphDatabaseSettings.relationship_keys_indexable, relationshipKeysIndexable);
		}
		graphDbService = gdbb.newGraphDatabase();
		ggo = GlobalGraphOperations.at(graphDbService);
		sdbs = new SpatialDatabaseService(graphDbService);
		el = (SimplePointLayer) sdbs.getLayer(IConstants.MAIN_POINTS_LAYER_NAME);
		registerShutdownHook(graphDbService);
		sw.stop();
		if (logger.isInfoEnabled()) {

			logger.info("Inizializzazione Neo4J completata in " + sw.getLastTaskTimeMillis() + " millisecondi.");
		}
		this.distKm = (new Divide()).value(getMaxDistance(), 1000);
	}

	/**
	 * Richiamato in automatico da Spring quando si stoppa il contesto
	 */
	public void shutdown() {
		StopWatch sw = new StopWatch();
		sw.start();
		graphDbService.shutdown();
		sw.stop();
		if (logger.isInfoEnabled()) {

			logger.info("Shutdown Neo4J completato in " + sw.getLastTaskTimeMillis() + " millisecondi.");
		}
	}

	/**
	 * Restituisce il path dove è stato creato il graph DB
	 * 
	 * @return -il path dove è stato creato il graph DB
	 */
	public String getNeo4jPath() {
		return neo4jPath;
	}

	/**
	 * Valorizza il path dove è stato creato il graph DB
	 * 
	 * @param neo4jPath
	 *            -il path dove è stato creato il graph DB
	 */
	public void setNeo4jPath(String neo4jPath) {
		this.neo4jPath = neo4jPath;
	}

	public Iterable<Node> getAllNodes() throws Exception {
		Transaction tx = this.graphDbService.beginTx();
		try {
			StopWatch sw = new StopWatch();
			sw.start();
			Iterable<Node> result = ggo.getAllNodes();
			sw.stop();
			if (logger.isDebugEnabled()) {

				logger.debug("Caricamento nodi da Neo4J terminato in " + sw.getLastTaskTimeMillis() + " millisecondi");
			}
			return result;
		} catch (Exception e) {

			tx.failure();
			logger.fatal("Errore nel recupero di tutti i nodi dal graphDB; messaggio errore: " + e.getMessage(), e);
			throw e;
		}
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
	public List<SpatialDatabaseRecord> getClosestNode(Coordinate coord) {
		Transaction tx = graphDbService.beginTx();
		try{
			logger.info("Searching closest point for the following coordinates: "+coord+" x: "+coord.x+" y: "+coord.y);
			StopWatch sw = new StopWatch();
			sw.start();
			List<SpatialDatabaseRecord> results = GeoPipeline.startNearestNeighborLatLonSearch(el, coord, 1).toSpatialDatabaseRecordList();
			sw.stop();
			long pointsNumber = results != null ? results.isEmpty() ? 0 : results.size() : 0;
			if (logger.isInfoEnabled()) {

				logger.info("Trovati " + pointsNumber + " punti in " + sw.getLastTaskTimeMillis() + " millisecondi");
			}
			tx.success();
			return results;
		}finally{
			tx.close();
		}
	}
	public String getNodestoreMappedMemorySize() {
		return nodestoreMappedMemorySize;
	}

	public void setNodestoreMappedMemorySize(String nodestoreMappedMemorySize) {
		this.nodestoreMappedMemorySize = nodestoreMappedMemorySize;
	}

	public String getRelationshipstoreMappedMemorySize() {
		return relationshipstoreMappedMemorySize;
	}

	public void setRelationshipstoreMappedMemorySize(String relationshipstoreMappedMemorySize) {
		this.relationshipstoreMappedMemorySize = relationshipstoreMappedMemorySize;
	}

	public String getNodestorePropertystoreMappedMemorySize() {
		return nodestorePropertystoreMappedMemorySize;
	}

	public void setNodestorePropertystoreMappedMemorySize(String nodestorePropertystoreMappedMemorySize) {
		this.nodestorePropertystoreMappedMemorySize = nodestorePropertystoreMappedMemorySize;
	}

	public String getStringsMappedMemorySize() {
		return stringsMappedMemorySize;
	}

	public void setStringsMappedMemorySize(String stringsMappedMemorySize) {
		this.stringsMappedMemorySize = stringsMappedMemorySize;
	}

	public String getArraysMappedMemorySize() {
		return arraysMappedMemorySize;
	}

	public void setArraysMappedMemorySize(String arraysMappedMemorySize) {
		this.arraysMappedMemorySize = arraysMappedMemorySize;
	}

	public String getAllowStoreUpgrade() {
		return allowStoreUpgrade;
	}

	public void setAllowStoreUpgrade(String allowStoreUpgrade) {
		this.allowStoreUpgrade = allowStoreUpgrade;
	}

	public String getCypherParserVersion() {
		return cypherParserVersion;
	}

	public void setCypherParserVersion(String cypherParserVersion) {
		this.cypherParserVersion = cypherParserVersion;
	}

	public String getKeepLogicalLogs() {
		return keepLogicalLogs;
	}

	public void setKeepLogicalLogs(String keepLogicalLogs) {
		this.keepLogicalLogs = keepLogicalLogs;
	}

	public String getNodeAutoIndexing() {
		return nodeAutoIndexing;
	}

	public void setNodeAutoIndexing(String nodeAutoIndexing) {
		this.nodeAutoIndexing = nodeAutoIndexing;
	}

	public String getNodeKeysIndexable() {
		return nodeKeysIndexable;
	}

	public void setNodeKeysIndexable(String nodeKeysIndexable) {
		this.nodeKeysIndexable = nodeKeysIndexable;
	}

	public String getRelationshipAutoIndexing() {
		return relationshipAutoIndexing;
	}

	public void setRelationshipAutoIndexing(String relationshipAutoIndexing) {
		this.relationshipAutoIndexing = relationshipAutoIndexing;
	}

	public String getRelationshipKeysIndexable() {
		return relationshipKeysIndexable;
	}

	public void setRelationshipKeysIndexable(String relationshipKeysIndexable) {
		this.relationshipKeysIndexable = relationshipKeysIndexable;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}
}