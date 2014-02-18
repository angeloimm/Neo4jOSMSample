package it.olegna.routing.osm.util;
public interface IConstants {
	/**
	 * Il nome del layer spaziale che conterrà i punti principali indicizzati
	 * spazialmente
	 */
	public static final String MAIN_POINTS_LAYER_NAME = "mainPointsLayer";
	public static final String MAIN_POINTS_LABEL_NAME = "nodoPrincipale";
	public static final String SECONDARY_POINTS_LABEL_NAME = "nodoSecondario";
	public static final String GEOMETRY_INFO = "pinf_geometry_info";
	public static final String OSM_NODE_ID_PROPERTY = "osmNodeId";
	public static final String OSM_WAY_ID_PROPERTY = "osmWayId";
	public static final String EDGE_LENGTH_PROPERTY = "edgeLength";
	public static final String EDGE_REAL_TIME_SPEED_PROPERTY = "edgeRtSpeed";
	/**
	 * Il nome della proprietà rappresentante la coordinata x
	 */
	public static final String X_COORDINATE = "x";
	/**
	 * Il nome della proprietà rappresentante la coordinata y
	 */
	public static final String Y_COORDINATE = "y";

	public static final int DRITTO = 1;
	public static final int GIRARE_LEGGERMENTE_DESTRA = 2;
	public static final int GIRARE_DESTRA = 3;
	public static final int GIRARE_SUBITO_DESTRA = 4;
	public static final int INVERSIONE_U = 5;
	public static final int GIRARE_SUBITO_SINISTRA = 6;
	public static final int GIRARE_SINISTRA = 7;
	public static final int GIRARE_LEGGERMENTE_SINISTRA = 8;
	public static final int DIRIGERSI_A = 9;
	public static final int IMBOCCARE = 10;
	public static final int IMMETTERSI_NELLA_ROTONDA = 11;
	public static final int USCIRE_DALLA_ROTONDA = 12;
	public static final int TENERSI_SULLA_ROTONDA = 13;
	public static final int PARTIRE_DALLA_FINE_DELLA_STRADA = 14;
	public static final int DESTINAZIONE_RAGGIUNTA = 15;
	public static final int IMMETTERSI = 16;
	public static final int USCIRE = 17;
}