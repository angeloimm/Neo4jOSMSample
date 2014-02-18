package it.olegna.routing.osm.util;

import java.util.Collection;
import java.util.HashMap;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class OsmNodeWrapper extends OSMElement{
	private double x;
	private double y;
	private long graphNodeId;
	private long osmNodeId;
	
	private Node osmNode;

	public OsmNodeWrapper() {
	}
	
	public OsmNodeWrapper(Node osmNode){
		this(osmNode.getLongitude(), osmNode.getLatitude(), osmNode.getId());
		this.osmNode = osmNode;
		this.osmNodeId = osmNode.getId();
		Collection<Tag> wayTags = osmNode.getTags();
		tags = new HashMap<String, String>(wayTags.size());
		for (Tag tag : wayTags) {

			tags.put(tag.getKey(), tag.getValue());
		}
		
	}

	public OsmNodeWrapper(double x, double y, long osmNodeId) {
		super();
		this.x = x;
		this.y = y;
		this.osmNodeId = osmNodeId;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public long getGraphNodeId() {
		return graphNodeId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("X: ");
		sb.append(getX());
		sb.append(". Y: ");
		sb.append(getY());
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