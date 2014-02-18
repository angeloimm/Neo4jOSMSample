package it.olegna.test;

import java.util.List;

import it.olegna.routing.graph.GraphMgr;
import it.olegna.routing.osm.util.IConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.vividsolutions.jts.geom.Coordinate;

@ContextConfiguration(value={
		"classpath:application-context.xml"
})

@RunWith(SpringJUnit4ClassRunner.class)
public class ClosestPointsSearchTest {

	private static final Log logger = LogFactory.getLog(ClosestPointsSearchTest.class.getName());
	@Autowired
	private GraphMgr graphMgr;

	@Test
	public void closestPointSearchTest() throws Exception{

		try {
			Iterable<Node> allNodes = graphMgr.getAllNodes();
			for (Node node : allNodes) {
				if(node.hasProperty(IConstants.LATITUDE_PROPERTY) && node.hasProperty(IConstants.LONGITUDE_PROPERTY)) 
					logger.info("Node ID: "+node.getId()+" X: "+node.getProperty(IConstants.LATITUDE_PROPERTY)+" Y: "+node.getProperty(IConstants.LONGITUDE_PROPERTY)+" id OSM: "+node.getProperty(IConstants.OSM_NODE_ID_PROPERTY));
				else
					logger.info("Node ID: "+node.getId()+" no X and Y");
			}
			Coordinate coord = new Coordinate(45.4653788, 9.18891398507115);
//			Coordinate coord = new Coordinate(9.1892546, 45.465609);
			List<SpatialDatabaseRecord> records = graphMgr.getClosestNode(coord);
			if( records != null && !records.isEmpty() ){

				for (SpatialDatabaseRecord spatialDatabaseRecord : records) {

					Node nod = spatialDatabaseRecord.getGeomNode();
					logger.info("ID: "+nod.getId());
				}
			}
		} catch (Exception e) {

			logger.fatal("Error "+e.getMessage(), e);
		}
	}
}
