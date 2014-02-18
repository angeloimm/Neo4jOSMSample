package it.olegna.routing.osm.util;


public class BikeWayChecker extends BikeWayCommonChecker {
	
	private static BikeWayChecker theInstance;
	public static BikeWayChecker getInstance(){
		if( theInstance == null ){
			
			theInstance = new BikeWayChecker();
		}
		return theInstance;
	}
	private BikeWayChecker(){
		super();
        setTrackTypeSpeed("grade1", 20); // paved
        setTrackTypeSpeed("grade2", 12); // now unpaved ...
        setTrackTypeSpeed("grade3", 12);
        setTrackTypeSpeed("grade4", 10);
        setTrackTypeSpeed("grade5", 8); // like sand/grass     

        setSurfaceSpeed("asphalt", 20);
        setSurfaceSpeed("concrete", 20);
        setSurfaceSpeed("paved", 20);
        setSurfaceSpeed("unpaved", 16);
        setSurfaceSpeed("gravel", 12);
        setSurfaceSpeed("ground", 12);
        setSurfaceSpeed("dirt", 10);
        setSurfaceSpeed("paving_stones", 8);
        setSurfaceSpeed("grass", 8);
        setSurfaceSpeed("cobblestone", 6);

        setHighwaySpeed("living_street", 15);
        setHighwaySpeed("steps", PUSHING_SECTION_SPEED / 2);

        setHighwaySpeed("cycleway", 18);
        setHighwaySpeed("path", 18);
        setHighwaySpeed("footway", 18);
        setHighwaySpeed("pedestrian", 18);
        setHighwaySpeed("road", 10);
        setHighwaySpeed("track", 20);
        setHighwaySpeed("service", 20);
        setHighwaySpeed("unclassified", 20);
        setHighwaySpeed("residential", 20);

        setHighwaySpeed("trunk", 18);
        setHighwaySpeed("trunk_link", 18);
        setHighwaySpeed("primary", 18);
        setHighwaySpeed("primary_link", 15);
        setHighwaySpeed("secondary", 16);
        setHighwaySpeed("secondary_link", 16);
        setHighwaySpeed("tertiary", 18);
        setHighwaySpeed("tertiary_link", 18);

        setPushingSection("path");
        setPushingSection("track");
        setPushingSection("footway");
        setPushingSection("pedestrian");
        setPushingSection("steps");

        setCyclingNetworkPreference("icn", RelationMapCode.OUTSTANDING_NICE.getValue());
        setCyclingNetworkPreference("ncn", RelationMapCode.OUTSTANDING_NICE.getValue());
        setCyclingNetworkPreference("rcn", RelationMapCode.VERY_NICE.getValue());
        setCyclingNetworkPreference("lcn", RelationMapCode.PREFER.getValue());
        setCyclingNetworkPreference("mtb", RelationMapCode.UNCHANGED.getValue());
		
	}
}
