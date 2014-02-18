package it.olegna.routing.graph.relations;
import org.neo4j.graphdb.RelationshipType;

public enum RelTypes implements RelationshipType {
	MAIN_NODES_RELATION, 
	PEDESTRIAN_MAIN_NODES_RELATION,
	BIKE_MAIN_NODES_RELATION,
	GEOMETRY_INFO
}