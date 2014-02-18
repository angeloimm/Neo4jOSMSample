package it.olegna.routing.osmosis.sinks;


import it.olegna.routing.osm.util.BikeWayChecker;
import it.olegna.routing.osm.util.CarWayChecker;
import it.olegna.routing.osm.util.FootWayChecker;
import it.olegna.routing.osm.util.OSMElementsUtil;
import it.olegna.routing.osm.util.OSMRelationWrapper;
import it.olegna.routing.osm.util.OSMWayWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.carrotsearch.hppc.LongLongOpenHashMap;
import com.carrotsearch.hppc.LongObjectOpenHashMap;
import com.carrotsearch.hppc.LongOpenHashSet;
import com.carrotsearch.hppc.LongSet;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * Classe che si preoccupa di leggere il file OSM e di salvare gli ID dei nodi
 * da creare Sul grafo non viene effettuata nessuna operazione; si individuano
 * solo i nodi da creare
 * 
 * @author Angelo Immediata
 * 
 */
public class PrepareNodeIdsSinkImpl implements Sink {

	private LongLongOpenHashMap nodesMap;
	public static final byte MAIN_NODE = 2;
	public static final byte SECONDARY_NODE = 1;

	// contatore per il debug
	public static int towerNodeNumber;

	private LongLongOpenHashMap osmWayIdToRouteWeightMap;

	private LongSet osmIdStoreRequiredSet; // stores osm ids used by relations
											// to identify which edge ids needs
											// to be mapped later

	private LongObjectOpenHashMap<LineString> wayLineStringMap;// mappa
																// associativa
																// di way e
																// geometria

	private static final Log logger = LogFactory.getLog(PrepareNodeIdsSinkImpl.class.getName());

	public PrepareNodeIdsSinkImpl() {

		// Aggiunta inizializzazione
		osmWayIdToRouteWeightMap = new LongLongOpenHashMap();
		nodesMap = new LongLongOpenHashMap();
		wayLineStringMap = new LongObjectOpenHashMap<LineString>();
	}

	@Override
	public void initialize(Map<String, Object> metaData) {
	}

	@Override
	public void complete() {
		if( logger.isInfoEnabled() ){
			
			logger.info("Trovati "+nodesMap.size()+" nodi dei quali "+towerNodeNumber+" nodi principali e "+(nodesMap.size()-towerNodeNumber)+" secondari");
		}
	}

	@Override
	public void release() {
	}

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();

		if (entity instanceof Way) {

			Way aWay = (Way) entity;
			OSMWayWrapper oww = new OSMWayWrapper(aWay);

			// Controllo se la strada è valida o meno; se si aggiungo i nodi....
			if (OSMElementsUtil.isValid(oww)) {
				List<WayNode> wayNodes = aWay.getWayNodes();
				if (wayNodes != null && !wayNodes.isEmpty()) {

					List<Coordinate> coords = new ArrayList<Coordinate>();
					GeometryFactory fac = new GeometryFactory();

					for (WayNode wayNode : wayNodes) {

						long nodeId = wayNode.getNodeId();
						addNodeToMap(nodeId);

					}
					if (coords.size() != 0) {
						// Creo la geometria della way e la inserisco nella
						// mappa WayId - Geometria
						LineString lineString = fac.createLineString(coords.toArray(new Coordinate[0]));
						lineString.setSRID(4326);
						wayLineStringMap.put(oww.getOsmWay().getId(), lineString);
					}
				}
			}
		} else if (entity instanceof Relation) {

			Relation relation = (Relation) entity;
			OSMRelationWrapper orw = new OSMRelationWrapper(relation);

			if (OSMElementsUtil.isPrepareWaysWithRelationInfo(orw)) {
				prepareWaysWithRelationInfo(orw);
			}


		}

	}

	private void prepareWaysWithRelationInfo(OSMRelationWrapper osmRelationWrapper) {

		if (logger.isDebugEnabled()) {

			logger.debug("Gestisco le strade con relazioni interessanti, date dalla relazione : " + osmRelationWrapper.getOsmRelation());
		}

		if ((CarWayChecker.getInstance().handleRelationTags(0, osmRelationWrapper) == 0) && (BikeWayChecker.getInstance().handleRelationTags(0, osmRelationWrapper) == 0) && (FootWayChecker.getInstance().handleRelationTags(0, osmRelationWrapper) == 0)) {

			return;
		}
		List<RelationMember> rms = osmRelationWrapper.getOsmRelation().getMembers();
		for (RelationMember rm : rms) {

			if (rm.getMemberType() != EntityType.Way) {

				continue;
			}
			long osmId = rm.getMemberId();
			long oldRelationFlags = getRelFlagsMap().get(osmId);

			// Check if our new code is better comparated to the the last
			// occured before
			long newRelationFlags = 0;
			newRelationFlags |= CarWayChecker.getInstance().handleRelationTags(oldRelationFlags, osmRelationWrapper);
			newRelationFlags |= BikeWayChecker.getInstance().handleRelationTags(oldRelationFlags, osmRelationWrapper);
			newRelationFlags |= FootWayChecker.getInstance().handleRelationTags(oldRelationFlags, osmRelationWrapper);
			if (oldRelationFlags != newRelationFlags)
				getRelFlagsMap().put(osmId, newRelationFlags);
		}
	}
	private void addNodeToMap(long nodeId) {

		// Controllo se il map contiene già l'id del nodo o meno
		if (nodesMap.containsKey(nodeId)) {

			nodesMap.put(nodeId, MAIN_NODE);
			towerNodeNumber++;
		} else {

			nodesMap.put(nodeId, SECONDARY_NODE);
		}
	}

	public LongLongOpenHashMap getNodesMap() {

		return this.nodesMap;
	}

	public LongLongOpenHashMap getRelFlagsMap() {

		return osmWayIdToRouteWeightMap;
	}

	public LongSet getOsmIdStoreRequiredSet() {

		if (osmIdStoreRequiredSet == null)
			osmIdStoreRequiredSet = new LongOpenHashSet();

		return osmIdStoreRequiredSet;
	}

	public LongObjectOpenHashMap<LineString> getWayLineStringMap() {
		return wayLineStringMap;
	}
}