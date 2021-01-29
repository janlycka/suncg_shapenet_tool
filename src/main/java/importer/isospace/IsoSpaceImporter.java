package importer.isospace;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.resources.CompressionUtils;
import org.texttechnologylab.annotation.semaf.isobase.Entity;
import org.texttechnologylab.annotation.semaf.isospace.Location;
import org.texttechnologylab.annotation.semaf.isospace.MLink;
import org.texttechnologylab.annotation.semaf.isospace.Measure;
import org.texttechnologylab.annotation.semaf.isospace.Motion;
import org.texttechnologylab.annotation.semaf.isospace.MoveLink;
import org.texttechnologylab.annotation.semaf.isospace.NonMotionEvent;
import org.texttechnologylab.annotation.semaf.isospace.OLink;
import org.texttechnologylab.annotation.semaf.isospace.Path;
import org.texttechnologylab.annotation.semaf.isospace.Place;
import org.texttechnologylab.annotation.semaf.isospace.QsLink;
import org.texttechnologylab.annotation.semaf.isospace.SRelation;
import org.texttechnologylab.annotation.semaf.isospace.SpatialEntity;
import org.texttechnologylab.annotation.semaf.meta.MetaLink;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;



public class IsoSpaceImporter extends JCasResourceCollectionReader_ImplBase {
	//public static final Object PARAM_PATTERNS = ;
    final String SPLIT_CHAR = ";";
	
	DocumentBuilder builder;

	HashMap<String, Object> idMap;
	ArrayList<List<String>> entityRelations;
	
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
    }
	
    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        InputStream reader = CompressionUtils.getInputStream(res.getLocation(), res.getInputStream());
        
        setDocumentID(aJCas, res.getLocation());


        idMap = new HashMap<>();
        entityRelations = new ArrayList<>();
        try {
            convert(aJCas, reader, false);

			JCas goldcas = aJCas.createView("SpaceEvalGold");
			reader = CompressionUtils.getInputStream(res.getLocation(), res.getInputStream());
			convert(goldcas, reader, true);

        } catch (SAXException | CASException e) {
			e.printStackTrace();
		}
        finally {
        	reader.close();
        }
    	//System.out.println(XmlFormatter.getPrettyString(aJCas.getCas()));
    	
    	if(entityRelations.size() > 0) {
    		System.out.println(entityRelations);
    		System.exit(0);
    	}
    }

    private void convert(JCas aJCas, InputStream aReader, boolean links) throws IOException, SAXException
    {
    	System.out.println("===============================================");
    	JCasBuilder jcasdoc = new JCasBuilder(aJCas);
    	Document xmldoc = builder.parse(aReader);
    	Element root = xmldoc.getDocumentElement();
    	
    	NodeList nodeList = root.getElementsByTagName("TEXT");
    	assert nodeList.getLength() == 1;
    	jcasdoc.add(nodeList.item(0).getTextContent());
    	jcasdoc.close();

    	// Order is important!
    	
    	nodeList = root.getElementsByTagName("MEASURE");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		measureHandler(aJCas, eElement);
    	}
    	
    	nodeList = root.getElementsByTagName("PLACE");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		placeHandler(aJCas, eElement);
    	}
    	
    	nodeList = root.getElementsByTagName("LOCATION");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		locationHandler(aJCas, eElement);
    	}
    	
    	
    	nodeList = root.getElementsByTagName("SPATIAL_ENTITY");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		spatialEntityHandler(aJCas, eElement);
    	}
    	
    	nodeList = root.getElementsByTagName("PATH");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		pathHandler(aJCas, eElement);
    	}
    	
    	nodeList = root.getElementsByTagName("NONMOTION_EVENT");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		nonMotionHandler(aJCas, eElement);
    	}
    	
    	nodeList = root.getElementsByTagName("MOTION");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		motionHandler(aJCas, eElement);
    	}
    	
    	/////////////////////////////
    	
    	//TODO: Scopes!
    	
    	///////////////////////////
    	
    	nodeList = root.getElementsByTagName("SPATIAL_SIGNAL");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		spatialSignalHandler(aJCas, eElement);
    	}
    	
    	nodeList = root.getElementsByTagName("MOTION_SIGNAL");
    	for(int i = 0; i < nodeList.getLength(); i++) {
    		Element eElement = (Element) nodeList.item(i);
    		motionSignalHandler(aJCas, eElement);
    	}
    	
    	///////////////////



		////////////
		if(links) {

			nodeList = root.getElementsByTagName("QSLINK");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element eElement = (Element) nodeList.item(i);
				qsLinkHandler(aJCas, eElement);
			}

			nodeList = root.getElementsByTagName("OLINK");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element eElement = (Element) nodeList.item(i);
				oLinkHandler(aJCas, eElement);
			}

			nodeList = root.getElementsByTagName("MEASURELINK");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element eElement = (Element) nodeList.item(i);
				measureLinkHandler(aJCas, eElement);
			}

			nodeList = root.getElementsByTagName("MLINK");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element eElement = (Element) nodeList.item(i);
				measureLinkHandler(aJCas, eElement);
			}

			nodeList = root.getElementsByTagName("MOVELINK");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element eElement = (Element) nodeList.item(i);
				moveLinkHandler(aJCas, eElement);
			}

			nodeList = root.getElementsByTagName("METALINK");
			for (int i = 0; i < nodeList.getLength(); i++) {
				Element eElement = (Element) nodeList.item(i);
				metalinkHandler(aJCas, eElement);
			}
		}
    }


    private void setDocumentID(JCas aJCas, String path) {
    	String[] pathSplit = path.split("/");
    	String docname = pathSplit[pathSplit.length-1];
    	docname = docname.substring(0, docname.length() - 4);
    	DocumentMetaData m = DocumentMetaData.get(aJCas);
    	m.setDocumentId(docname);
    	m.setDocumentTitle(docname);
    }
    
    private void placeHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	Place place;
    	if(start != -1 && end != -1) {
    		place = new Place(aJCas, start, end);
    	}else {
    		place = new Place(aJCas);
    	}
    	
    	place.setSpatial_entitiy_type(e.getAttribute("type"));
    	place.setDimensionality(e.getAttribute("dimensionality"));
    	place.setForm(e.getAttribute("form"));
    	place.setContinent(e.getAttribute("continent"));
    	place.setState(e.getAttribute("state"));
    	place.setCountry(e.getAttribute("country"));
    	place.setCtv(e.getAttribute("ctv"));
    	place.setGazref(e.getAttribute("gazref"));
    	place.setLat(e.getAttribute("latLong"));
    	place.setLong(e.getAttribute("latLong"));
    	
    	if(!e.getAttribute("elevation").equals("")) {
    		place.setElevation((Measure) idMap.get(e.getAttribute("elevation")));
    	}
    	
    	place.setMod(e.getAttribute("mod"));

    	if(e.getAttribute("scopes").trim() != "") {
    		System.out.println("1"+ e.getAttribute("scopes"));
        	String[] scopesplit = e.getAttribute("scopes").split(SPLIT_CHAR);
        	for(int i = 0; i < scopesplit.length; i++) {
        		entityRelations.add(Arrays.asList(id, "scopes", scopesplit[i]));
        	}
    	}

    	place.setDcl(Boolean.parseBoolean(e.getAttribute("dcl")));
    	place.setCountable(Boolean.parseBoolean(e.getAttribute("countable")));
    	place.setGquant(e.getAttribute("gquant"));
    	place.setComment(e.getAttribute("comment"));
    	place.setDomain(e.getAttribute("domain"));
    	idMap.put(id, place);
    	place.addToIndexes();
    }
    
    private void locationHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	Location location;
    	if(start != -1 && end != -1) {
    		location = new Place(aJCas, start, end);
    	}else {
    		location = new Place(aJCas);
    	}
    	
    	location.setSpatial_entitiy_type(e.getAttribute("type"));
    	location.setDimensionality(e.getAttribute("dimensionality"));
    	location.setForm(e.getAttribute("form"));
    	location.setGazref(e.getAttribute("gazref"));
    	location.setLat(e.getAttribute("latLong"));
    	location.setLong(e.getAttribute("latLong"));
    	location.setDomain(e.getAttribute("domain"));
    	
    	if(!e.getAttribute("elevation").equals("")) {
    		location.setElevation((Measure) idMap.get(e.getAttribute("elevation")));
    	}
    	
    	location.setMod(e.getAttribute("mod"));

    	if(e.getAttribute("scopes").trim() != "") {
    		System.out.println("2"+ e.getAttribute("scopes"));
        	String[] scopesplit = e.getAttribute("scopes").split(SPLIT_CHAR);
        	for(int i = 0; i < scopesplit.length; i++) {
        		entityRelations.add(Arrays.asList(id, "scopes", scopesplit[i]));
        	}
    	}

    	location.setDcl(Boolean.parseBoolean(e.getAttribute("dcl")));
    	location.setCountable(Boolean.parseBoolean(e.getAttribute("countable")));
    	location.setGquant(e.getAttribute("gquant"));
    	location.setComment(e.getAttribute("comment"));

    	
    	idMap.put(id, location);
    	location.addToIndexes();
    }
    
    
    private void metalinkHandler(JCas aJCas, Element e) {
    	String id = e.getAttribute("id");
    	
    	String fromID = e.getAttribute("objectID1");
    	String toID = e.getAttribute("objectID2");
    	String relType = e.getAttribute("relType");
    	String comment = e.getAttribute("comment");
    	
    	MetaLink mLink = new MetaLink(aJCas);
    	mLink.setFigure((Entity) idMap.get(fromID));
    	mLink.setGround((Entity) idMap.get(toID));
    	mLink.setRel_type(relType);
    	mLink.setComment(comment);
    	
    	idMap.put(id, mLink);
    	mLink.addToIndexes();
    }
    
    private void spatialEntityHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	SpatialEntity entity;
    	if(start != -1 && end != -1) {
    		entity = new SpatialEntity(aJCas, start, end);
    	}else {
    		entity = new SpatialEntity(aJCas);
    	}
    	
    	entity.setSpatial_entitiy_type(e.getAttribute("type"));
    	entity.setDimensionality(e.getAttribute("dimensionality"));
    	entity.setForm(e.getAttribute("form"));
    	
    	entity.setLat(e.getAttribute("latLong"));
    	entity.setLong(e.getAttribute("latLong"));
    	
    	if(!e.getAttribute("elevation").equals("")) {
    		entity.setElevation((Measure) idMap.get(e.getAttribute("elevation")));
    	}
    	
    	entity.setMod(e.getAttribute("mod"));

    	if(e.getAttribute("scopes").trim() != "") {
    		System.out.println("3"+ e.getAttribute("scopes"));
        	String[] scopesplit = e.getAttribute("scopes").split(SPLIT_CHAR);
        	for(int i = 0; i < scopesplit.length; i++) {
        		entityRelations.add(Arrays.asList(id, "scopes", scopesplit[i]));
        	}
    	}

    	entity.setDcl(Boolean.parseBoolean(e.getAttribute("dcl")));
    	entity.setCountable(Boolean.parseBoolean(e.getAttribute("countable")));
    	entity.setGquant(e.getAttribute("gquant"));
    	entity.setComment(e.getAttribute("comment"));
    	entity.setDomain(e.getAttribute("domain"));
    	
    	idMap.put(id, entity);
    	entity.addToIndexes();
    }
    
    private void pathHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	Path path;
    	if(start != -1 && end != -1) {
    		path = new Path(aJCas, start, end);
    	}else {
    		path = new Path(aJCas);
    	}
    	
    	if(!e.getAttribute("beginID").equals("")) {
        	Object bID = idMap.get(e.getAttribute("beginID"));
        	path.setBeginID((SpatialEntity) bID); 
    	}
    	
    	if(!e.getAttribute("endID").equals("")) {
        	Object eID = idMap.get(e.getAttribute("endID"));
        	path.setEndID((SpatialEntity) eID); 
    	}
    	
    	if(!e.getAttribute("midIDs").equals("")) {
        	String[] midSplit = e.getAttribute("midIDs").split(SPLIT_CHAR);
        	FSArray fslist = new FSArray(aJCas,  midSplit.length);
        	for (int i = 0; i < midSplit.length; i++) {
        		SpatialEntity midEntity = (SpatialEntity) idMap.get(midSplit[i]);
        		fslist.set(i, midEntity);
        		//fslist = addToFSList(aJCas, fslist, midEntity);
        	}
        	path.setMidID_array(fslist);
    	}
    	
    	
    	path.setSpatial_entitiy_type(e.getAttribute("type"));
    	path.setDimensionality(e.getAttribute("dimensionality"));
    	path.setForm(e.getAttribute("form"));
    	
    	path.setLat(e.getAttribute("latLong"));
    	path.setLong(e.getAttribute("latLong"));
    	
    	if(!e.getAttribute("elevation").equals("")) {
    		path.setElevation((Measure) idMap.get(e.getAttribute("elevation")));
    	}
    	
    	path.setMod(e.getAttribute("mod"));

    	if(e.getAttribute("scopes") != "") {
    		System.out.println("4"+ e.getAttribute("scopes"));
        	String[] scopesplit = e.getAttribute("scopes").split(SPLIT_CHAR);
        	for(int i = 0; i < scopesplit.length; i++) {
        		entityRelations.add(Arrays.asList(id, "scopes", scopesplit[i]));
        	}
    	}

    	path.setGazref(e.getAttribute("gazref"));
    	path.setDcl(Boolean.parseBoolean(e.getAttribute("dcl")));
    	path.setCountable(Boolean.parseBoolean(e.getAttribute("countable")));
    	path.setGquant(e.getAttribute("gquant"));
    	path.setComment(e.getAttribute("comment"));
    	path.setDomain(e.getAttribute("domain"));
    	
    	idMap.put(id, path);
    	path.addToIndexes();
    }
    
    private void measureHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	Measure measure = new Measure(aJCas);

    	measure.setBegin(start);
    	measure.setEnd(end);
    	
    	measure.setValue(e.getAttribute("value"));
    	measure.setUnit(e.getAttribute("unit"));
    	measure.setComment(e.getAttribute("comment"));
    	
    	idMap.put(id, measure);
    	measure.addToIndexes();
    }
    
    private void nonMotionHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	
    	NonMotionEvent motion = new NonMotionEvent(aJCas, start, end);
    	motion.setDomain(e.getAttribute("domain"));
    	motion.setLat(e.getAttribute("latLong")); 
    	motion.setLong(e.getAttribute("latLong"));

    	if(!e.getAttribute("elevation").equals("")) {
    		motion.setElevation((Measure) idMap.get(e.getAttribute("elevation")));
    	}
    	
    	motion.setMod(e.getAttribute("mod"));
    	motion.setCountable(Boolean.parseBoolean(e.getAttribute("countable")));
    	motion.setGquant(e.getAttribute("gquant"));
    	motion.setComment(e.getAttribute("comment"));
    	
    	if(e.getAttribute("scopes") != "") {
    		System.out.println("5"+ e.getAttribute("scopes"));
        	String[] scopesplit = e.getAttribute("scopes").split(SPLIT_CHAR);
        	for(int i = 0; i < scopesplit.length; i++) {
        		entityRelations.add(Arrays.asList(id, "scopes", scopesplit[i]));
        	}
    	}
    	
    	idMap.put(id, motion);
    	motion.addToIndexes();
    }
    
    private void motionHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	
    	Motion motion = new Motion(aJCas, start, end);
    	motion.setDomain(e.getAttribute("domain"));
    	motion.setLat(e.getAttribute("latLong"));
    	motion.setLong(e.getAttribute("latLong"));
    	
    	motion.setMotion_type(e.getAttribute("motion_type"));
    	motion.setMotion_class(e.getAttribute("motion_class"));
    	motion.setMotion_sense(e.getAttribute("motion_sense"));

    	if(!e.getAttribute("elevation").equals("")) {
    		motion.setElevation((Measure) idMap.get(e.getAttribute("elevation")));
    	}
    	
    	motion.setMod(e.getAttribute("mod"));
    	motion.setCountable(Boolean.parseBoolean(e.getAttribute("countable")));
    	motion.setGquant(e.getAttribute("gquant"));
    	motion.setComment(e.getAttribute("comment"));
    	
    	if(e.getAttribute("scopes") != "") {
    		System.out.println("6"+ e.getAttribute("scopes"));
        	String[] scopesplit = e.getAttribute("scopes").split(SPLIT_CHAR);
        	for(int i = 0; i < scopesplit.length; i++) {
        		entityRelations.add(Arrays.asList(id, "scopes", scopesplit[i]));
        	}
    	}
    	
    	idMap.put(id, motion);
    	motion.addToIndexes();
    }
    
    private void spatialSignalHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	
    	SRelation signal = new SRelation(aJCas, start, end);
    	
    	signal.setCluster(e.getAttribute("cluster"));
    	signal.setRelation_type(e.getAttribute("semantic_type"));
    	//signal.setMod(e.getAttribute("mod"));
    	signal.setComment(e.getAttribute("comment"));
    	//(TODO: Value fehlt ....), ABer wohl nur "noetig" bei MoveLinks.
    	
    	idMap.put(id, signal);
    	signal.addToIndexes();
    }
    
    
    private void motionSignalHandler(JCas aJCas, Element e) {
    	int start = Integer.valueOf(e.getAttribute("start"));
    	int end = Integer.valueOf(e.getAttribute("end"));
    	String id = e.getAttribute("id");
    	
    	SRelation signal = new SRelation(aJCas, start, end);
    	signal.setRelation_type(e.getAttribute("motion_signal_type"));
    	//TODO: Type=MANNER kommt in das Event mit rein ...
    	//Loesung: Manner ebenfalls als SIgnal lassen und in Motion.manner ablegen.
    	//signal.setMod(e.getAttribute("mod"));
    	signal.setComment(e.getAttribute("comment"));

    	idMap.put(id, signal);
    	signal.addToIndexes();
    }
    
    
    
    private void qsLinkHandler(JCas aJCas, Element e) {
    	String id = e.getAttribute("id");
    	
    	QsLink link = new QsLink(aJCas);
    	link.setRel_type(e.getAttribute("relType"));
    	
    	if(!e.getAttribute("trajector").equals("")) {
    		link.setFigure((Entity) idMap.get(e.getAttribute("trajector")));
    	}
    	
    	if(!e.getAttribute("landmark").equals("")) {
    		link.setGround((Entity) idMap.get(e.getAttribute("landmark")));
    	}
    	
    	if(!e.getAttribute("trigger").equals("")) {
    		link.setTrigger((Entity) idMap.get(e.getAttribute("trigger")));
    	}
    	
    	link.setComment(e.getAttribute("comment"));
    	
    	idMap.put(id, link);
    	link.addToIndexes();
    }
    
    
    private void oLinkHandler(JCas aJCas, Element e) {
    	String id = e.getAttribute("id");
    	
    	OLink link = new OLink(aJCas);
    	link.setRel_type(e.getAttribute("relType"));
    	
    	if(!e.getAttribute("trajector").equals("")) {
    		link.setFigure((Entity) idMap.get(e.getAttribute("trajector")));
    	}
    	
    	if(!e.getAttribute("landmark").equals("")) {
    		link.setGround((Entity) idMap.get(e.getAttribute("landmark")));
    	}
    	
    	if(!e.getAttribute("trigger").equals("")) {
    		link.setTrigger((Entity) idMap.get(e.getAttribute("trigger")));
    	}
    	
    	link.setFrame_type(e.getAttribute("frame_type"));
    	
    	if(!e.getAttribute("referencePt").equals("")) {
    		link.setReference_pt((Entity) idMap.get(e.getAttribute("referencePt")));
    	}
    	
    	link.setProjective(Boolean.parseBoolean(e.getAttribute("projective")));
    	link.setComment(e.getAttribute("comment"));
    	
    	idMap.put(id, link);
    	link.addToIndexes();
    }
    
    
    private void measureLinkHandler(JCas aJCas, Element e) {
    	//Measure Links koennen auch fuer MRelationen verwendet werden. Hier nicht ergaenzt, da nicht in den Spaceeval Daten relevant ....
    	String id = e.getAttribute("id");
    	
    	MLink link = new MLink(aJCas);
    	link.setRel_type(e.getAttribute("relType"));
    	
    	if(!e.getAttribute("trajector").equals("")) {
    		link.setFigure((Entity) idMap.get(e.getAttribute("trajector")));
    	}
    	
    	if(!e.getAttribute("landmark").equals("")) {
    		link.setGround((Entity) idMap.get(e.getAttribute("landmark")));
    	}
    	
    	if(!e.getAttribute("trigger").equals("")) {
    		link.setTrigger((Measure) idMap.get(e.getAttribute("trigger")));
    		link.setVal((Measure) idMap.get(e.getAttribute("trigger")));
    	}
    	
    	if(!e.getAttribute("endPoint1").equals("")) {
    		link.setEnd_point1((Entity) idMap.get(e.getAttribute("endPoint1")));
    	}
    	
    	if(!e.getAttribute("endPoint2").equals("")) {
    		link.setEnd_point1((Entity) idMap.get(e.getAttribute("endPoint2")));
    	}
    	
    	link.setComment(e.getAttribute("comment"));
    	
    	idMap.put(id, link);
    	link.addToIndexes();
    }
    
    
    private void moveLinkHandler(JCas aJCas, Element e) {
    	String id = e.getAttribute("id");
    	
    	MoveLink link = new MoveLink(aJCas);
    	
    	if(!e.getAttribute("trigger").equals("")) {
    		link.setTrigger((Motion) idMap.get(e.getAttribute("trigger")));
    	}
    	
    	if(!e.getAttribute("mover").equals("")) {
    		link.setFigure((Entity) idMap.get(e.getAttribute("mover")));
    	}
    	
    	if(!e.getAttribute("landmark").equals("")) {
    		link.setGround((Entity) idMap.get(e.getAttribute("landmark")));
    	}
    	
    	if(!e.getAttribute("source").equals("")) {
    		link.setSource((Entity) idMap.get(e.getAttribute("source")));
    	}
    	if(!e.getAttribute("goal").equals("")) {
    		link.setGoal((Entity) idMap.get(e.getAttribute("goal")));
    	}
    	if(!e.getAttribute("PathID").equals("")) {
    		link.setPath_id((Path) idMap.get(e.getAttribute("PathID")));
    	}
    	    	
    	if(!e.getAttribute("midPoint").equals("")) {
        	String[] midSplit = e.getAttribute("midPoint").split(SPLIT_CHAR);
        	FSArray fslist = new FSArray(aJCas, midSplit.length);
        	for (int i = 0; i < midSplit.length; i++) {
        		SpatialEntity midEntity = (SpatialEntity) idMap.get(midSplit[i]);
        		fslist.set(i, midEntity);
        	}
        	link.setMid_point_array(fslist);
    	}
    	    	
    	if(!e.getAttribute("motion_signalID").equals("")) {
    		SRelation rel = ((SRelation) idMap.get(e.getAttribute("motion_signalID")));
    		if(rel == null) {
    			
    		}
    		else if(rel.getRelation_type().equals("MANNER")) {
    			((Motion) idMap.get(e.getAttribute("trigger"))).setManner(rel); //Normal keine SIgnals, aber besser so ....
    			
    		}else if(rel.getRelation_type().equals("PATH")) {
    			//link.setMotionsignal_id((SRelation) idMap.get(e.getAttribute("motion_signalID")));
    			//Die gehen eh unter -.-
    		}
    	}
    	
    	link.setGoal_reached(e.getAttribute("goal_reached"));
    	
    	link.setComment(e.getAttribute("comment"));
    	
    	idMap.put(id, link);
    	link.addToIndexes();
    }
       

}
