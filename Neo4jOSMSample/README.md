Simple test project for the creation of a graph from an OSM file
In order to execute the tests we have to execute the following steps:
<ul>
<li>
1) Launch the following test: it.olegna.test.OsmGraphCreationTest.osmGraphCreationTest(). 
This test creates the neo4j graph from the OSM file configured in the configuration.properties file 
</li>
<li>
2) Launch the test it.olegna.test.ClosestPointsSearchTest.closestPointSearchTest(). 
This test will search the closest points by accessing to the created SimplePointLayer
</li>
</ul>

I provided also a little OSM file called "milanoFragment.osm" and located under "src/test/resources"; 
the "milanoFragment.png" shows the little zone of Milan I imported in the graph
In order to correctly execute the tests you have to configure the "configuration.properties" file. 
Basically you have to configure the following properties:
<ul>
<li>
<strong>pinf.osm.files.directory</strong>: directory where is located the OSM file
</li>
<li>
<strong>pinf.neo4j.db.path</strong>: path where to create the neo4j DB
</li>
</ul>
 