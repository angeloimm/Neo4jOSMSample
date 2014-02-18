package it.olegna.routing.osm.util;

import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
public class OSMElementsUtil {

	/**
	 * mean radius of the earth
	 */
	public final static double R = 6371000; // m
    
	public OSMElementsUtil() {
	}
	public static boolean isValid( OSMWayWrapper osmWayWrapper ){
		//Se non ci sono nodi nella strada...la strada è da scartare; ignoriamo strade rotte
		List<WayNode> nodes = osmWayWrapper.getOsmWay().getWayNodes();
		if( nodes == null || nodes.isEmpty() || nodes.size() < 2 ){
			return false;
		}
		//Se non ha tag viene esclusa; ignoriamo la geometria multipoligono
		Collection<Tag> tags = osmWayWrapper.getOsmWay().getTags();
		if( tags == null || tags.isEmpty()){
			return false;
		}
		//Controlliamo la strada
		return CarWayChecker.getInstance().isWayValid(osmWayWrapper) || FootWayChecker.getInstance().isWayValid(osmWayWrapper) || BikeWayChecker.getInstance().isWayValid(osmWayWrapper);
	}
	
	public static boolean isPrepareWaysWithRelationInfo( OSMRelationWrapper osmRelationWrapper ){
		
		return !osmRelationWrapper.isMetaRelation() && osmRelationWrapper.hasTag("type", "route");
	}
	
    public static double calcDist( double fromLat, double fromLon, double toLat, double toLon )
    {
        double sinDeltaLat = sin(toRadians(toLat - fromLat) / 2);
        double sinDeltaLon = sin(toRadians(toLon - fromLon) / 2);
        double normedDist = sinDeltaLat * sinDeltaLat
                + sinDeltaLon * sinDeltaLon * cos(toRadians(fromLat)) * cos(toRadians(toLat));
        return R * 2 * asin(sqrt(normedDist));
    }
}