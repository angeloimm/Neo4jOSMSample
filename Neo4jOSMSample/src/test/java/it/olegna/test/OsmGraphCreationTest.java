package it.olegna.test;

import it.olegna.routing.graph.creator.PinfOsmGraphCreator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.util.StopWatch;

public class OsmGraphCreationTest {

	private static final Log logger = LogFactory.getLog(OsmGraphCreationTest.class.getName());

	@Test
	public void osmGraphCreationTest() throws Exception{
		
		try {
			StopWatch sw = new StopWatch();
			sw.start("creazione grafo da file OSM");
			PinfOsmGraphCreator osmFileReader = new PinfOsmGraphCreator();
			osmFileReader.createGraph();
			sw.stop();
			logger.info("Task "+sw.getLastTaskName()+" terminato in "+sw.getLastTaskTimeMillis()+" millisecondi");
		} catch (Exception e) {
			
			logger.fatal(e.getMessage(), e);
			throw e;
		}
	}
}