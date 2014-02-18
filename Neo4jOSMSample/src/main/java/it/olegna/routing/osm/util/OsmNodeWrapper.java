package it.olegna.routing.osm.util;

import java.util.Collection;
import java.util.HashMap;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class OsmNodeWrapper extends OSMElement{
	private double latitude;
	private double longitude;
	private long graphNodeId;
	private long osmNodeId;
	
	private Node osmNode;

	public OsmNodeWrapper() {
	}
	
	public OsmNodeWrapper(Node osmNode){
		
		this(osmNode.getLatitude(), osmNode.getLongitude(), osmNode.getId());
		this.osmNode = osmNode;
		this.osmNodeId = osmNode.getId();
		Collection<Tag> wayTags = osmNode.getTags();
		tags = new HashMap<String, String>(wayTags.size());
		for (Tag tag : wayTags) {

			tags.put(tag.getKey(), tag.getValue());
		}
		
	}

	public OsmNodeWrapper(double latitude, double longitude, long osmNodeId) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.osmNodeId = osmNodeId;
	}



	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public long getGraphNodeId() {
		return graphNodeId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Latitude (y): ");
		sb.append(getLatitude());
		sb.append(". Longitude (x): ");
		sb.append(getLongitude());
		sb.append(". ID grafo: ");
		sb.append(getGraphNodeId());
		return sb.toString();
	}

	public Node getOsmNode() {
		return osmNode;
	}

	public long getOsmNodeId() {
		return osmNodeId;
	}

	public void setOsmNodeId(long osmNodeId) {
		this.osmNodeId = osmNodeId;
	}
}