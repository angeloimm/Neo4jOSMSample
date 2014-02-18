package it.olegna.routing.osm.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CarWayChecker extends AbstractChecker {
    private static final Map<String, Integer> TRACKTYPE_SPEED = new HashMap<String, Integer>();
    private static final Set<String> BAD_SURFACE = new HashSet<String>();
    /**
     * A map which associates string to speed. Get some impression:
     * http://www.itoworld.com/map/124#fullscreen
     * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Maxspeed
     */
    private static final Map<String, Integer> SPEED = new HashMap<String, Integer>();
    private static CarWayChecker theInstance;
    static{

        TRACKTYPE_SPEED.put("grade1", 20); // paved
        TRACKTYPE_SPEED.put("grade2", 15); // now unpaved - gravel mixed with ...
        TRACKTYPE_SPEED.put("grade3", 10); // ... hard and soft materials
        TRACKTYPE_SPEED.put("grade4", 5); // ... some hard or compressed materials
        TRACKTYPE_SPEED.put("grade5", 5); // ... no hard materials. soil/sand/grass
        BAD_SURFACE.add("cobblestone");
        BAD_SURFACE.add("grass_paver");
        BAD_SURFACE.add("gravel");
        BAD_SURFACE.add("sand");
        BAD_SURFACE.add("paving_stones");
        BAD_SURFACE.add("dirt");
        BAD_SURFACE.add("ground");
        BAD_SURFACE.add("grass");
        // autobahn
        SPEED.put("motorway", 100);
        SPEED.put("motorway_link", 70);
        // bundesstraße
        SPEED.put("trunk", 70);
        SPEED.put("trunk_link", 65);
        // linking bigger town
        SPEED.put("primary", 65);
        SPEED.put("primary_link", 60);
        // linking towns + villages
        SPEED.put("secondary", 60);
        SPEED.put("secondary_link", 50);
        // streets without middle line separation
        SPEED.put("tertiary", 50);
        SPEED.put("tertiary_link", 40);
        SPEED.put("unclassified", 30);
        SPEED.put("residential", 30);
        // spielstraße
        SPEED.put("living_street", 5);
        SPEED.put("service", 20);
        // unknown road
        SPEED.put("road", 20);
        // forestry stuff
        SPEED.put("track", 15);
    }
    public static CarWayChecker getInstance(){
    	if( theInstance == null ){
    		
    		theInstance = new CarWayChecker();
    	}
    	return theInstance;
    }
	private CarWayChecker(){
		super();
        restrictions = new String[] { "motorcar", "motor_vehicle", "vehicle", "access" };
        restrictedValues.add("private");
        restrictedValues.add("agricultural");
        restrictedValues.add("forestry");
        restrictedValues.add("no");
        restrictedValues.add("restricted");
        intended.add("yes");
        intended.add("permissive");
        potentialBarriers.add("gate");
        potentialBarriers.add("lift_gate");
        potentialBarriers.add("kissing_gate");
        potentialBarriers.add("swing_gate");
        absoluteBarriers.add("bollard");
        absoluteBarriers.add("stile");
        absoluteBarriers.add("turnstile");
        absoluteBarriers.add("cycle_barrier");
        absoluteBarriers.add("block");
	}
	@Override
	public boolean isWayValid(OSMWayWrapper way) {
		
        String highwayValue = way.getTag("highway");
        if (highwayValue == null)
        {
            if (way.hasTag("route", ferries))
            {
                String motorcarTag = way.getTag("motorcar");
                if (motorcarTag == null)
                    motorcarTag = way.getTag("motor_vehicle");

                if (motorcarTag == null && !way.hasTag("foot") && !way.hasTag("bicycle") || "yes".equals(motorcarTag))
                    return true;
            }
            return false;
        }

        if (!SPEED.containsKey(highwayValue))
            return false;

        if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
            return false;

        // do not drive street cars into fords
        if ((way.hasTag("highway", "ford") || way.hasTag("ford")) && !way.hasTag(restrictions, intended))
            return false;

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues))
            return false;

        // do not drive cars over railways (sometimes incorrectly mapped!)
        if (way.hasTag("railway") && !way.hasTag("railway", acceptedRailways))
            return false;

        return true;
	}
	
	@Override
	public long handleRelationTags(long oldRelationFlags,
			OSMRelationWrapper relation) {
		return oldRelationFlags;
	}

	@Override
	public long analyzeNodeTags(OsmNodeWrapper node) {
		// absolute barriers always block
		if (node.hasTag("barrier", absoluteBarriers))
			return directionBitMask;

		return super.analyzeNodeTags(node);
	}
}
