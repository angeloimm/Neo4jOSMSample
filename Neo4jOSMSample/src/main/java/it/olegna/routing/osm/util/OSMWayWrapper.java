package it.olegna.routing.osm.util;

import java.util.Collection;
import java.util.HashMap;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

public class OSMWayWrapper extends OSMElement {

	private Way osmWay;

	public OSMWayWrapper(Way way) {

		osmWay = way;
		Collection<Tag> wayTags = way.getTags();
		tags = new HashMap<String, String>(wayTags.size());
		for (Tag tag : wayTags) {

			tags.put(tag.getKey(), tag.getValue());
		}
	}

	public Way getOsmWay() {
		return osmWay;
	}

}
