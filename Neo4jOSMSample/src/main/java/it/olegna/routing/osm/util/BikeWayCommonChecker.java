package it.olegna.routing.osm.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BikeWayCommonChecker extends AbstractChecker {
    protected static final int DEFAULT_REL_CODE = 4;
    protected static final int PUSHING_SECTION_SPEED = 4;
    // Pushing section heighways are parts where you need to get off your bike and push it (German: Schiebestrecke)
    private final HashSet<String> pushingSections = new HashSet<String>();
    private final HashSet<String> oppositeLanes = new HashSet<String>();
    private final Set<String> unpavedSurfaceTags = new HashSet<String>();
    private final Map<String, Integer> trackTypeSpeed = new HashMap<String, Integer>();
    private final Map<String, Integer> surfaceSpeed = new HashMap<String, Integer>();
    private final Set<String> roadValues = new HashSet<String>();
    private final Map<String, Integer> highwaySpeed = new HashMap<String, Integer>();
    //Convert network tag of bicycle routes into a way route code stored in the wayMAP
    private final Map<String, Integer> bikeNetworkToCode = new HashMap<String, Integer>();
    
    
    private static BikeWayCommonChecker theInstance;
    public static BikeWayCommonChecker getInstance(){
    	if( theInstance == null ){
    		
    		theInstance = new BikeWayCommonChecker();
    	}
    	return theInstance;
    }
    protected BikeWayCommonChecker(){
    	super();
        // strict set, usually vehicle and agricultural/forestry are ignored by cyclists
        restrictions = new String[]
        {
            "bicycle", "access"
        };
        restrictedValues.add("private");
        restrictedValues.add("no");
        restrictedValues.add("restricted");

        intended.add("yes");
        intended.add("designated");
        intended.add("official");
        intended.add("permissive");

        oppositeLanes.add("opposite");
        oppositeLanes.add("opposite_lane");
        oppositeLanes.add("opposite_track");

        // With a bike one usually can pass all those barriers:
        // potentialBarriers.add("gate");
        // potentialBarriers.add("lift_gate");
        // potentialBarriers.add("swing_gate");
        // potentialBarriers.add("cycle_barrier");
        // potentialBarriers.add("block");
        absoluteBarriers.add("kissing_gate");
        absoluteBarriers.add("stile");
        absoluteBarriers.add("turnstile");
        // very dangerous
        // acceptedRailways.remove("tram");

        unpavedSurfaceTags.add("unpaved");
        unpavedSurfaceTags.add("gravel");
        unpavedSurfaceTags.add("ground");
        unpavedSurfaceTags.add("dirt");
        unpavedSurfaceTags.add("paving_stones");
        unpavedSurfaceTags.add("grass");
        unpavedSurfaceTags.add("cobblestone");

        roadValues.add("living_street");
        roadValues.add("road");
        roadValues.add("service");
        roadValues.add("unclassified");
        roadValues.add("residential");
        roadValues.add("trunk");
        roadValues.add("trunk_link");
        roadValues.add("primary");
        roadValues.add("primary_link");
        roadValues.add("secondary");
        roadValues.add("secondary_link");
        roadValues.add("tertiary");
        roadValues.add("tertiary_link");

        setCyclingNetworkPreference("deprecated", RelationMapCode.AVOID_AT_ALL_COSTS.getValue());

    }
    
	@Override
	public boolean isWayValid(OSMWayWrapper way) {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null)
        {
            if (way.hasTag("route", ferries))
            {
                // if bike is NOT explictly tagged allow bike but only if foot is not specified
                String bikeTag = way.getTag("bicycle");
                if (bikeTag == null && !way.hasTag("foot") || "yes".equals(bikeTag))
                    return true;
            }
            return false;
        }

        if (!highwaySpeed.containsKey(highwayValue))
            return false;

        // use the way if it is tagged for bikes
        if (way.hasTag("bicycle", intended))
            return true;

        if (way.hasTag("motorroad", "yes"))
            return false;

        // do not use fords with normal bikes, flagged fords are in included above
        if (way.hasTag("highway", "ford") || way.hasTag("ford"))
            return false;

        // check access restrictions
        if (way.hasTag(restrictions, restrictedValues))
            return false;

        // do not accept railways (sometimes incorrectly mapped!)
        if (way.hasTag("railway") && !way.hasTag("railway", acceptedRailways))
            return false;

        return true;
	}
    public enum RelationMapCode
    {
        /* Inspired by http://wiki.openstreetmap.org/wiki/Class:bicycle
         "-3" = Avoid at all cost. 
         "-2" = Only use to reach your destination, not well suited. 
         "-1" = Better take another way 
         "0" = as well as other ways around. 
         Try to to avoid using 0 but decide on -1 or +1. 
         class:bicycle shall only be used as an additional key. 
         "1" = Prefer 
         "2" = Very Nice way to cycle 
         "3" = This way is so nice, it pays out to make a detour also if this means taking 
         many unsuitable ways to get here. Outstanding for its intended usage class.
         */
        //We can't store negative numbers into our map, therefore we add 
        //unspecifiedRelationWeight=4 to the schema from above
        AVOID_AT_ALL_COSTS(1),
        REACH_DEST(2),
        AVOID_IF_POSSIBLE(3),
        UNCHANGED(DEFAULT_REL_CODE),
        PREFER(5),
        VERY_NICE(6),
        OUTSTANDING_NICE(7);

        private final int value;

        private RelationMapCode( int value )
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }
    };



    public void setTrackTypeSpeed( String tracktype, int speed )
    {
        trackTypeSpeed.put(tracktype, speed);
    }

    public void setSurfaceSpeed( String surface, int speed )
    {
        surfaceSpeed.put(surface, speed);
    }

    public void setHighwaySpeed( String highway, int speed )
    {
        highwaySpeed.put(highway, speed);
    }

    public void setCyclingNetworkPreference( String network, int code )
    {
        bikeNetworkToCode.put(network, code);
    }

    public void setPushingSection( String highway )
    {
        pushingSections.add(highway);
    }
    
	@Override
	public long handleRelationTags(long oldRelationFlags,
			OSMRelationWrapper relation) {
		
		int code = RelationMapCode.UNCHANGED.getValue();
		if (relation.hasTag("route", "bicycle")) {
			Integer val = bikeNetworkToCode.get(relation.getTag("network"));
			if (val != null)
				code = val;
		}
				
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
