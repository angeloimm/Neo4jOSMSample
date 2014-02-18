package it.olegna.routing.osm.util;

import java.util.HashSet;

public abstract class AbstractChecker {
    protected String[] restrictions;
    protected HashSet<String> intended = new HashSet<String>();
    protected HashSet<String> restrictedValues = new HashSet<String>(5);
    protected HashSet<String> ferries = new HashSet<String>(5);
    protected HashSet<String> oneways = new HashSet<String>(5);
    protected HashSet<String> acceptedRailways = new HashSet<String>(5);
    protected HashSet<String> absoluteBarriers = new HashSet<String>(5);
    protected HashSet<String> potentialBarriers = new HashSet<String>(5);
    
    protected long directionBitMask = 0;
    
    public AbstractChecker(){
    	
    	//Valori comuni a tutti i checker
        oneways.add("yes");
        oneways.add("true");
        oneways.add("1");
        oneways.add("-1");
        ferries.add("shuttle_train");
        ferries.add("ferry");
        acceptedRailways.add("tram");
    }
    
    public abstract boolean isWayValid( OSMWayWrapper way );
    
    public abstract long handleRelationTags( long oldRelationFlags, OSMRelationWrapper relation );
    
    /**
     * Parse tags on nodes. Node tags can add to speed (like traffic_signals) where the value is
     * strict negative or blocks access (like a barrier), then the value is strict positive.
     */
    public long analyzeNodeTags( OsmNodeWrapper node )
    {
        // movable barriers block if they are not marked as passable
        if (node.hasTag("barrier", potentialBarriers) && !node.hasTag(restrictions, intended) && !node.hasTag("locked", "no"))
            return directionBitMask;

        if ((node.hasTag("highway", "ford") || node.hasTag("ford")) && !node.hasTag(restrictions, intended))
            return directionBitMask;

        return 0;
    }
}
