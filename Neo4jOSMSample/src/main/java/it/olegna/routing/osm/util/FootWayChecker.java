package it.olegna.routing.osm.util;

import java.util.HashSet;
import java.util.Set;

public class FootWayChecker extends AbstractChecker {
    protected HashSet<String> intended = new HashSet<String>();
    protected HashSet<String> sidewalks = new HashSet<String>();
    private final Set<String> safeHighwayTags = new HashSet<String>();
    private final Set<String> allowedHighwayTags = new HashSet<String>();
    private static FootWayChecker theInstance;
    public static FootWayChecker getInstance(){
    	if( theInstance == null ){
    		
    		theInstance = new FootWayChecker();
    	}
    	return theInstance;
    }
    private FootWayChecker(){
    	super();
    	
        restrictions = new String[]{"foot", "access"};
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");
        intended.add("yes");
        intended.add("designated");
        intended.add("official");
        intended.add("permissive");
        sidewalks.add("yes");
        sidewalks.add("both");
        sidewalks.add("left");
        sidewalks.add("right");
        potentialBarriers.add("gate");
        //potentialBarriers.add( "lift_gate" );   you can always pass them on foot
        potentialBarriers.add("swing_gate");
        acceptedRailways.add("station");
        acceptedRailways.add("platform");
        safeHighwayTags.add("footway");
        safeHighwayTags.add("path");
        safeHighwayTags.add("steps");
        safeHighwayTags.add("pedestrian");
        safeHighwayTags.add("living_street");
        safeHighwayTags.add("track");
        safeHighwayTags.add("residential");
        safeHighwayTags.add("service");
        allowedHighwayTags.addAll(safeHighwayTags);
        allowedHighwayTags.add("trunk");
        allowedHighwayTags.add("trunk_link");
        allowedHighwayTags.add("primary");
        allowedHighwayTags.add("primary_link");
        allowedHighwayTags.add("secondary");
        allowedHighwayTags.add("secondary_link");
        allowedHighwayTags.add("tertiary");
        allowedHighwayTags.add("tertiary_link");
        allowedHighwayTags.add("unclassified");
        allowedHighwayTags.add("road");
        // disallowed in some countries
        //allowedHighwayTags.add("bridleway");
    }
	@Override
	public boolean isWayValid(OSMWayWrapper way) {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null)
        {
            if (way.hasTag("route", ferries))
            {
                String footTag = way.getTag("foot");
                if (footTag == null || "yes".equals(footTag))
                    return true;
            }
            return false;
        }

        String sacScale = way.getTag("sac_scale");
        if (sacScale != null)
        {
            if (!"hiking".equals(sacScale) && !"mountain_hiking".equals(sacScale))
                // other scales are too dangerous, see http://wiki.openstreetmap.org/wiki/Key:sac_scale
                return false;
        }

        if (way.hasTag("sidewalk", sidewalks))
            return true;

        // no need to evaluate ferries or fords - already included here
        if (way.hasTag("foot", intended))
            return true;

        if (!allowedHighwayTags.contains(highwayValue))
            return false;

        if (way.hasTag("motorroad", "yes"))
            return false;

        // do not get our feet wet, "yes" is already included above
        if (way.hasTag("highway", "ford") || way.hasTag("ford"))
            return false;

        if (way.hasTag("bicycle", "official"))
            return false;

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues))
            return false;

        // do not accept railways (sometimes incorrectly mapped!)
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
		// movable barriers block if they are not marked as passable
		if (node.hasTag("barrier", potentialBarriers)
				&& !node.hasTag(restrictions, intended)
				&& !node.hasTag("locked", "no")) {
			return directionBitMask;
		}

		if ((node.hasTag("highway", "ford") || node.hasTag("ford"))
				&& !node.hasTag(restrictions, intended)) {
			return directionBitMask;
		}

		return 0;
	}
}
