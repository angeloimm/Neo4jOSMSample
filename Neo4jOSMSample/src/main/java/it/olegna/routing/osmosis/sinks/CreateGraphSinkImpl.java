package it.olegna.routing.osmosis.sinks;

import it.olegan.routing.exception.GraphCreatorException;
import it.olegna.routing.graph.relations.RelTypes;
import it.olegna.routing.osm.util.IConstants;
import it.olegna.routing.osm.util.OSMElementsUtil;
import it.olegna.routing.osm.util.OSMWayWrapper;
import it.olegna.routing.osm.util.OsmNodeWrapper;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.util.Precision;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.MapUtil;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import com.carrotsearch.hppc.LongLongOpenHashMap;
import com.carrotsearch.hppc.LongObjectOpenHashMap;
import com.carrotsearch.hppc.LongOpenHashSet;
import com.carrotsearch.hppc.LongSet;
import com.carrotsearch.hppc.ObjectArrayList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.io.WKTWriter;

public class CreateGraphSinkImpl implements Sink {

	private static final Log logger = LogFactory.getLog(CreateGraphSinkImpl.class.getName());
	private RelationshipIndex indiceElementoStradaleDbId;
	private GraphDatabaseService graphDb;
	private SimplePointLayer mainPointsLayer;
	private long numeroNodi;
	private long numeroRelazioniNodiPrincipali;
	private long numeroRelazioniPedonaliNodiPrincipali;
	private Label mainNodeLabel;
	private long nodesNumber;
	private String neo4jDbPath;
	private LongLongOpenHashMap nodesMap;
	private boolean continueReading = true;
	private GraphCreatorException graphCreationException;
	private LongObjectOpenHashMap<org.neo4j.graphdb.Node> graphMainNodes;
	private LongObjectOpenHashMap<OsmNodeWrapper> graphSecondaryNodes;
	private LongLongOpenHashMap osmWayIdToRouteWeightMap;
	private LongSet osmIdStoreRequiredSet;
	private LongLongOpenHashMap osmNodeIdToNodeFlagsMap;
	int numeroRecord = 1000;
	public CreateGraphSinkImpl(String nomeFile, String neo4jDbPath, LongLongOpenHashMap nodesMap, LongLongOpenHashMap osmWayIdToRouteWeightMap, LongSet osmIdStoreRequiredSet) {

		if (nomeFile == null || nomeFile.trim().equals("")) {

			throw new IllegalArgumentException("Impossibile proseguire; passato un nome file vuoto o null <" + nomeFile + ">");
		}
		this.nodesMap = nodesMap;
		nodesNumber = nodesMap.size();
		this.neo4jDbPath = neo4jDbPath;
		graphMainNodes = new LongObjectOpenHashMap<org.neo4j.graphdb.Node>();
		graphSecondaryNodes = new LongObjectOpenHashMap<OsmNodeWrapper>();
		this.osmWayIdToRouteWeightMap = osmWayIdToRouteWeightMap;
		this.osmIdStoreRequiredSet = osmIdStoreRequiredSet;
		this.osmNodeIdToNodeFlagsMap = new LongLongOpenHashMap();
		if (nodesNumber < 10) {

			numeroRecord = 1;
		} else if (nodesNumber < 100) {

			numeroRecord = 10;
		} else if (nodesNumber < 1000) {

			numeroRecord = 100;
		}
	}
	@Override
	public void initialize(Map<String, Object> arg0) {

		if (logger.isInfoEnabled()) {

			logger.info("Inizializzazione neo4J e preparazioni indici");
		}
		Transaction tx = null;
		try {
			StopWatch sw = new StopWatch();
			sw.start("inizializzazione");
			Properties props = new Properties();
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("configuration.properties"));
			mainNodeLabel = DynamicLabel.label(IConstants.MAIN_POINTS_LABEL_NAME);
			GraphDatabaseFactory gdbf = new GraphDatabaseFactory();
			GraphDatabaseBuilder gdbb = gdbf.newEmbeddedDatabaseBuilder(neo4jDbPath);
			gdbb.setConfig(GraphDatabaseSettings.nodestore_mapped_memory_size, props.getProperty("nodestore_mapped_memory_size"));
			gdbb.setConfig(GraphDatabaseSettings.relationshipstore_mapped_memory_size, props.getProperty("relationshipstore_mapped_memory_size"));
			gdbb.setConfig(GraphDatabaseSettings.nodestore_propertystore_mapped_memory_size, props.getProperty("nodestore_propertystore_mapped_memory_size"));
			gdbb.setConfig(GraphDatabaseSettings.strings_mapped_memory_size, props.getProperty("strings_mapped_memory_size"));
			gdbb.setConfig(GraphDatabaseSettings.arrays_mapped_memory_size, props.getProperty("arrays_mapped_memory_size"));
			if (props.getProperty("allow_store_upgrade") != null && !props.getProperty("allow_store_upgrade").trim().equals("")) {

				gdbb.setConfig(GraphDatabaseSettings.allow_store_upgrade, props.getProperty("allow_store_upgrade"));
			}
			if (props.getProperty("cypher_parser_version") != null && !props.getProperty("cypher_parser_version").trim().equals("")) {

				gdbb.setConfig(GraphDatabaseSettings.cypher_parser_version, props.getProperty("cypher_parser_version"));
			}
			if (props.getProperty("keep_logical_logs") != null && !props.getProperty("keep_logical_logs").trim().equals("")) {

				gdbb.setConfig(GraphDatabaseSettings.keep_logical_logs, props.getProperty("keep_logical_logs"));
			}
			if (props.getProperty("node_auto_indexing") != null && !props.getProperty("node_auto_indexing").trim().equals("")) {

				gdbb.setConfig(GraphDatabaseSettings.node_auto_indexing, props.getProperty("node_auto_indexing"));
			}
			if (props.getProperty("node_keys_indexable") != null && !props.getProperty("node_keys_indexable").trim().equals("")) {

				gdbb.setConfig(GraphDatabaseSettings.node_keys_indexable, props.getProperty("node_keys_indexable"));
			}
			if (props.getProperty("relationship_auto_indexing") != null && !props.getProperty("relationship_auto_indexing").trim().equals("")) {

				gdbb.setConfig(GraphDatabaseSettings.relationship_auto_indexing, props.getProperty("relationship_auto_indexing"));
			}
			if (props.getProperty("relationship_keys_indexable") != null && !props.getProperty("relationship_keys_indexable").trim().equals("")) {

				gdbb.setConfig(GraphDatabaseSettings.relationship_keys_indexable, props.getProperty("relationship_keys_indexable"));
			}
			graphDb = gdbb.newGraphDatabase();
			tx = graphDb.beginTx();
			Schema dbSchema = graphDb.schema();
			dbSchema.indexFor(mainNodeLabel).on(IConstants.X_COORDINATE).create();
			dbSchema.indexFor(mainNodeLabel).on(IConstants.Y_COORDINATE).create();
			dbSchema.indexFor(mainNodeLabel).on("giunzioneDbId").create();
			IndexManager im = graphDb.index();
			this.indiceElementoStradaleDbId = im.forRelationships("ELEMENTO_STRADALE_DB_ID", MapUtil.stringMap("type", "exact"));
			tx.success();
			sw.stop();
			if (logger.isInfoEnabled()) {

				logger.info("Fase di inizializzazione neo4J e preparazioni indici terminata in " + sw.getLastTaskTimeMillis() + " millisecondi. Inizio creazione grafo");
			}
		} catch (Exception e) {

			graphCreationException = new GraphCreatorException(e);
			continueReading = false;
			if (tx != null) {

				tx.failure();
			}
		} finally {
			if (tx != null) {

				tx.close();
			}
		}
		SpatialDatabaseService sdb = new SpatialDatabaseService(graphDb);
		mainPointsLayer = sdb.createSimplePointLayer(IConstants.MAIN_POINTS_LAYER_NAME, IConstants.X_COORDINATE, IConstants.Y_COORDINATE);
	}
	@Override
	public void complete() {
	}
	@Override
	public void release() {

		if (logger.isInfoEnabled()) {

			logger.info("Processo di lettura e creazione grafo terminato. " + "Creati " + numeroNodi + " nodi, " + numeroRelazioniNodiPrincipali + " relazioni automobilistiche tra i nodi principali, " + numeroRelazioniPedonaliNodiPrincipali + " relazioni pedonali tra i nodi principali. " + "Totale delle relazioni create: " + (numeroRelazioniNodiPrincipali + numeroRelazioniPedonaliNodiPrincipali) + "; rilascio tutte le risorse e chiudo il DB");
		}
		graphDb.shutdown();
	}
	@Override
	public void process(EntityContainer entityContainer) {
		if (continueReading) {
			Entity entity = entityContainer.getEntity();
			try {
				if (entity instanceof Node) {
					Node osmFileNode = (Node) entity;
					OsmNodeWrapper osmNodeWrapper = new OsmNodeWrapper(osmFileNode);
					long osmFileNodeId = osmFileNode.getId();
					Long nodeType = this.nodesMap.remove(osmFileNodeId);
					if (nodeType != 0) {
						if (nodeType == PrepareNodeIdsSinkImpl.MAIN_NODE) {

							createGraphNode(osmNodeWrapper, true);
						} else {

							createGraphNode(osmNodeWrapper, false);
						}
					}
				} else if (entity instanceof Way) {

					OSMWayWrapper osmWayWrapper = new OSMWayWrapper((Way) entity);
					List<WayNode> wayNodes = osmWayWrapper.getOsmWay().getWayNodes();

					if (OSMElementsUtil.isValid(osmWayWrapper)) {

						String oneWay = null, nome = null, refName = null, highway = null;
						highway = osmWayWrapper.getTag("highway");
						oneWay = osmWayWrapper.getTag("oneway");
						nome = osmWayWrapper.getTag("name");
						refName = osmWayWrapper.getTag("ref");
						boolean doppioSenso = false;
						boolean reverse = false;
						reverse = isReverse(oneWay);
						if (!reverse)
							doppioSenso = isDoppioSenso(oneWay);
						if (refName != null && !refName.isEmpty()) {
							if (nome == null || nome.isEmpty()) {
								nome = refName;
							} else {
								nome += ", " + refName;
							}
						}
						insertNeo4jWays(osmWayWrapper.getOsmWay().getId(), highway, doppioSenso, reverse, wayNodes);
					} else {
						if (logger.isDebugEnabled()) {
							logger.debug("La strada con id OSM " + osmWayWrapper.getOsmWay().getId() + " non è una strada valida in base ai controlli effettuati; non verrà processata");
						}
					}
				} else if (entity instanceof Relation) {
					Relation osmFileRelation = (Relation) entity;
					Map<String, String> mappaTagsRelazioni = new HashMap<String, String>();
					for (Tag tag : osmFileRelation.getTags()) {
						if (logger.isDebugEnabled()) {

							logger.debug("key : " + tag.getKey() + " - Value : " + tag.getValue());
						}
						mappaTagsRelazioni.put(tag.getKey(), tag.getValue());
					}
				}
			} catch (Exception e) {

				continueReading = false;
				String message = "Errore nel precessamento dell'entità OSM " + entity + ". Messaggio errore: " + e.getMessage();
				graphCreationException = new GraphCreatorException(message, e);
			}
		}
	}

	private void insertNeo4jWays(long wayId, String highway, boolean doppioSenso, boolean reverse, List<WayNode> wayNodes) throws Exception {
		ObjectArrayList<OsmNodeWrapper> geometryInfo = new ObjectArrayList<OsmNodeWrapper>();
		long startNodeId = -1;
		long endNodeId = -1;
		for( int i = 0; i < wayNodes.size(); i++ ){
			long nodeId = wayNodes.get(i).getNodeId();
			if( i==0 && !this.graphMainNodes.containsKey(nodeId) && graphSecondaryNodes.containsKey(nodeId) ){

				createGraphNode(graphSecondaryNodes.get(nodeId), true);
				if( logger.isDebugEnabled() ){

					logger.debug("Nodo con ID OSM "+nodeId +" convertito in tower");
				}
			}else if( i==(wayNodes.size()-1) && !this.graphMainNodes.containsKey(nodeId) && graphSecondaryNodes.containsKey(nodeId) ){

				createGraphNode(graphSecondaryNodes.get(nodeId), true);
				if( logger.isWarnEnabled() ){

					logger.warn("Nodo con ID "+nodeId +" convertito in tower");
				}
			} 
			if( graphMainNodes.containsKey(nodeId) ){

				if( startNodeId == -1 ){

					startNodeId = nodeId;
				}else{

					endNodeId = nodeId;
				}
			}else if( graphSecondaryNodes.containsKey(nodeId) ){

				geometryInfo.add(graphSecondaryNodes.get(nodeId));
			}else{

				if( logger.isWarnEnabled() ){

					logger.warn("Il nodo con ID "+nodeId+" non appartiene né ai nodi principali né a quelli secondari");
				}
			}
			if( startNodeId > -1 && endNodeId > -1 ){
				if( doppioSenso ){

					createRelationship(endNodeId, startNodeId, wayId, "Test", highway, geometryInfo);
					createRelationship(startNodeId, endNodeId, wayId, "Test", highway, geometryInfo);
				}else if( reverse ){

					createRelationship(endNodeId, startNodeId, wayId, "Test", highway, geometryInfo);
				}else{

					createRelationship(startNodeId, endNodeId, wayId, "Test", highway, geometryInfo);
				}
				startNodeId = endNodeId;
				endNodeId = -1;
				geometryInfo.clear();
			}
		}
	}
	private boolean isDoppioSenso(String oneWay) {
		// Se oneway==null ha senso considerarlo doppio senso? Da quanto si
		// legge qui http://wiki.openstreetmap.org/wiki/Key:oneway sembrerebbe
		// di si
		return ((oneWay == null) || (oneWay != null && (oneWay.trim().equalsIgnoreCase("0") || oneWay.trim().equalsIgnoreCase("no") || oneWay.trim().equalsIgnoreCase("false"))));
	}
	private boolean isReverse(String oneWay) {
		return (oneWay != null && (oneWay.trim().equalsIgnoreCase("-1") || oneWay.trim().equalsIgnoreCase("reverse")));
	}

	private double getDistanzaInMetri(org.neo4j.graphdb.Node startNode, org.neo4j.graphdb.Node endNode) {

		double distance = DefaultEllipsoid.WGS84.orthodromicDistance((Double) startNode.getProperty(IConstants.Y_COORDINATE), (Double) startNode.getProperty(IConstants.X_COORDINATE), (Double) endNode.getProperty(IConstants.Y_COORDINATE), (Double) endNode.getProperty(IConstants.X_COORDINATE));
		return Precision.round(distance, 3);
	}

	private void createRelationship(long startNodeId, long endNodeId, long wayId, String nome, String highway, ObjectArrayList<OsmNodeWrapper> geometryInfo) throws Exception {
		if (logger.isDebugEnabled()) {

			logger.debug("Tento creazione la relazione tra il nodo iniziale con ID su file OSM: " + startNodeId + " ed il nodo finale con ID su file OSM: " + endNodeId);
		}
		boolean addNomeStrada = false;
		RelTypes relationType = null;
		org.neo4j.graphdb.Node startGraphNode = null;
		org.neo4j.graphdb.Node endGraphNode = null;
		if (graphMainNodes.containsKey(startNodeId)) {

			startGraphNode = graphMainNodes.get(startNodeId);
		}
		if (graphMainNodes.containsKey(endNodeId)) {

			endGraphNode = graphMainNodes.get(endNodeId);
		}

		if (!StringUtils.hasText(highway)) {

			relationType = RelTypes.MAIN_NODES_RELATION;
		} else if (highway.toLowerCase().equals("pedestrian") || highway.toLowerCase().equals("footway")) {

			relationType = RelTypes.PEDESTRIAN_MAIN_NODES_RELATION;
		} else {

			relationType = RelTypes.MAIN_NODES_RELATION;
		}
		addNomeStrada = true;

		if (relationType != null && startGraphNode != null && endGraphNode != null) {
			Transaction tx = graphDb.beginTx();
			try {
				double lunghezzaArco = getDistanzaInMetri(startGraphNode, endGraphNode);
				double velocitaRealTime = ((double) Math.random() * 100);
				Relationship rs = startGraphNode.createRelationshipTo(endGraphNode, relationType);
				rs.setProperty(IConstants.OSM_WAY_ID_PROPERTY, wayId);
				rs.setProperty(IConstants.EDGE_LENGTH_PROPERTY, lunghezzaArco);
				rs.setProperty(IConstants.EDGE_REAL_TIME_SPEED_PROPERTY, velocitaRealTime);
				if (logger.isDebugEnabled()) {

					logger.debug("nome : " + nome + "; way id : " + wayId);
				}
				if (addNomeStrada) {

					rs.setProperty("nomeStrada", nome == null ? "No name" : nome);
				}
				indiceElementoStradaleDbId.add(rs, IConstants.OSM_WAY_ID_PROPERTY, wayId);
				if( geometryInfo != null && !geometryInfo.isEmpty() ){
					final int size = geometryInfo.size();
					Object[] elements = geometryInfo.buffer;
					Coordinate[] geoInfo = new Coordinate[size];
					Geometry geom = null;
					GeometryFactory gf = new GeometryFactory();
					for (int i = 0; i < size; i++) {

						geoInfo[i] = new Coordinate( ((OsmNodeWrapper)(elements[i])).getX(), ((OsmNodeWrapper)(elements[i])).getY() );
					}
					if( geometryInfo.size() > 1 ){

						geom = new LineString(new CoordinateArraySequence(geoInfo), gf);

					}else{

						geom = new Point(new CoordinateArraySequence(geoInfo), gf);
					}
					addGeometryInfo( geom, startGraphNode );
				}else{
					if( logger.isDebugEnabled() ){

						logger.debug("Strada con ID osm "+wayId+" numero di punti che compongono la strada: "+(geometryInfo != null ? geometryInfo.size() : 0)+". Non conservo nessuna informazione sulla strada; aggiungo solo geometria al nodo");
					}
					Coordinate[] coords = new Coordinate[1];
					coords[0] = new Coordinate((Double)startGraphNode.getProperty(IConstants.X_COORDINATE), (Double)startGraphNode.getProperty(IConstants.Y_COORDINATE));
					Geometry geom = new Point( new CoordinateArraySequence(coords), new GeometryFactory());
					addGeometryInfo( geom, startGraphNode );
				}
				tx.success();
				switch (relationType) {
				case MAIN_NODES_RELATION:
					numeroRelazioniNodiPrincipali++;
					break;
				case PEDESTRIAN_MAIN_NODES_RELATION:
					numeroRelazioniPedonaliNodiPrincipali++;
					break;
				default:
					logger.warn("Tipo relazione non riconosciuto: " + relationType.name());
					break;
				}
			} catch (Exception e) {
				logger.fatal("Errore nella creazione delle relazioni; messaggio errore: " + e.getMessage(), e);
				tx.failure();
			} finally {
				if (tx != null) {

					tx.close();
				}
			}
		} else {

		}
	}
	private void addGeometryInfo(Geometry geometry, PropertyContainer geoNode) throws Exception{
		Transaction tx = geoNode.getGraphDatabase().beginTx();
		try{
			WKTWriter wktWriter = new WKTWriter();
			StringWriter sw = new StringWriter();
			wktWriter.write(geometry, sw);
			geoNode.setProperty(IConstants.GEOMETRY_INFO, sw.toString());
			tx.success();
		}catch(Exception e){

			String message = "Errore nell'aggiunta della geometria del punto; messaggio errore: "+e.getMessage();
			logger.fatal(message, e);
			tx.failure();
			throw e;
		}finally{

			tx.close();
		}
	}
	private void createGraphNode(OsmNodeWrapper osmNodeWrapper, boolean isMainNode) {
		if (isMainNode) {
			Transaction tx = graphDb.beginTx();
			try {

				double x = osmNodeWrapper.getX();
				double y = osmNodeWrapper.getY();
				long osmFileNodeId = osmNodeWrapper.getOsmNodeId();
				// Se è nodo principale lo taggo come nodo principale
				org.neo4j.graphdb.Node graphNode = graphDb.createNode(mainNodeLabel);
				graphNode.setProperty(IConstants.X_COORDINATE, x);
				graphNode.setProperty(IConstants.Y_COORDINATE, y);
				graphNode.setProperty(IConstants.OSM_NODE_ID_PROPERTY, osmFileNodeId);
				mainPointsLayer.add(graphNode);
				graphMainNodes.put(osmFileNodeId, graphNode);
				getNodesMap().put(osmFileNodeId, graphNode.getId());
				numeroNodi++;
				tx.success();
				if ((numeroNodi % numeroRecord) == 0) {
					if (logger.isInfoEnabled()) {

						logger.info("Creati " + numeroNodi + " nodi su " + nodesNumber + ". Processo creazione nodi completo al " + printNodeCreationPercentage() + "%");
					}
				}
				if (logger.isInfoEnabled()) {
					if (numeroNodi == nodesNumber) {

						logger.info("Creati tutti i " + nodesNumber + " nodi; creazione relazioni");
					}
				}
			} catch (Exception e) {

				String message = "Errore durante la creazione del nodo con ID osm " + osmNodeWrapper.getOsmNode().getId() + ". Messaggio errore: " + e.getMessage();
				logger.fatal(message, e);
				tx.failure();
				throw new IllegalStateException(message);
			} finally {

				if (tx != null) {

					tx.close();
				}
			}
		} else {

			graphSecondaryNodes.put(osmNodeWrapper.getOsmNodeId(), osmNodeWrapper);
		}
	}
	private double printNodeCreationPercentage() {

		return Precision.round(((double) (((double) numeroNodi) / ((double) nodesNumber))) * 100, 2);
	}

	public LongLongOpenHashMap getRelFlagsMap() {
		return osmWayIdToRouteWeightMap;
	}

	public LongLongOpenHashMap getNodesMap() {

		return this.nodesMap;
	}


	/**
	 * Restituisce l'eventuale eccezione avvenuta in questo thread
	 * 
	 * @return -l'eventuale eccezione
	 */
	public GraphCreatorException getGraphCreationException() {
		return graphCreationException;
	}

	public LongSet getOsmIdStoreRequiredSet() {

		if (osmIdStoreRequiredSet == null)
			osmIdStoreRequiredSet = new LongOpenHashSet();

		return osmIdStoreRequiredSet;
	}

	LongLongOpenHashMap getNodeFlagsMap() {
		return osmNodeIdToNodeFlagsMap;
	}
}