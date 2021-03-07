package importer.isospace;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
//import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.resources.CompressionUtils;
import org.texttechnologylab.annotation.semaf.IsoSpatial.Vec3;
import org.texttechnologylab.annotation.semaf.IsoSpatial.Vec4;
import org.texttechnologylab.annotation.semaf.isobase.Entity;
import org.texttechnologylab.annotation.semaf.isospace.*;
import org.texttechnologylab.annotation.semaf.meta.MetaLink;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.rmi.server.ObjID;
import java.util.*;
import java.lang.Math;
import java.util.concurrent.ThreadLocalRandom;

;

import org.json.*;

/*
 * Anmerkungen:
 * 
 * Scopes fehlen
 * -> Nicht relevant fuer SpaceEval
 * 
 * Value bei Signales fehlen. SInd wohl aber auch nur wichtig bei Paths.
 * -> Und da gehen die eh unter? :... (Werden die referiert)
 * 
 * Measure Links koennen auch fuer MRelationen verwendet werden. Hier nicht ergaenzt, da nicht in den Spaceeval Daten relevant ....
 * 
 * Manner wird weiterhin als Signal behalten.
 */

public class RommJsonImporter extends JCasResourceCollectionReader_ImplBase {

	DocumentBuilder builder;

	
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

        idMap = new HashMap<>();
        try {
            //convert(aJCas, reader, false);

            reader = CompressionUtils.getInputStream(res.getLocation(), res.getInputStream());
			convert(aJCas, reader, true);

        } catch (SAXException e) {
			e.printStackTrace();
		}
        finally {
        	reader.close();
        }
    	//System.out.println(XmlFormatter.getPrettyString(aJCas.getCas()));
    }

    //potential wall
    //36b2472a8f979c7298f210d66638d842

    HashMap<String, Object> idMap;
    Map<String, List<List<String>>> idMap_SUNCG = null;
    Map<String, List<List<String>>> idMap_ShapeNet = null;

    private void convert(JCas aJCas, InputStream aReader, boolean links) throws IOException, SAXException
    {
        if (idMap_SUNCG == null) {
            idMap_SUNCG = IdSUNCG();
        }
        if (idMap_ShapeNet == null) {
            idMap_ShapeNet = IdShapeNet();
        }

        System.out.println("xxx");
        System.out.println(aReader);

        String encoding = "utf-8";
        String jsonString = "";

        try( BufferedReader br = new BufferedReader( new InputStreamReader(aReader, encoding ))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while(( line = br.readLine()) != null ) {
                sb.append( line );
                sb.append( '\n' );
            }
            jsonString = sb.toString();
            System.out.println(jsonString);
        }

        JSONObject jsonContents = new JSONObject(jsonString);
        //String pageName = obj.getJSONObject("pageInfo").getString("pageName");

        JSONArray levels = jsonContents.getJSONArray("levels");
        for (int ind_levels = 0; ind_levels < levels.length(); ind_levels++)
        {
            JSONObject level = levels.getJSONObject(ind_levels);
            JSONArray nodes = level.getJSONArray("nodes");
            JSONArray min = new JSONArray(3);

            for (int ind_nodes = 0; ind_nodes < nodes.length(); ind_nodes++)
            {
                JSONObject node = nodes.getJSONObject(ind_nodes);
                String nodeType = node.getString("type");
                if (nodeType.equals("Room")) {
                    JSONObject bbox = node.getJSONObject("bbox");
                    min = bbox.getJSONArray("min");
                }
                else if (nodeType.equals("Object")) {
                    System.out.println("node");
                    System.out.println(node);
                    String objIDSunCG = node.getString("modelId");

                    //modelId is in SUNCG-Format -> get object description -> search ShapeNet DB for matching objects
                    String objID = obtainShapeNetModelIdForSUNCGModelID(objIDSunCG);

                    JSONArray transform = node.getJSONArray("transform");

                    // id
                    System.out.println(objID);
                    SpatialEntity entity = new SpatialEntity(aJCas);
                    entity.setObject_id(objID);

                    // 4x4 Transformation-matrix (parat in SUNCG-JSON Datei)
                    // a b c d
                    // e f g h
                    // i j k l
                    // m n o p

                    // https://math.stackexchange.com/questions/237369/given-this-transformation-matrix-how-do-i-decompose-it-into-translation-rotati
                    double a = transform.getDouble(0);
                    double b = transform.getDouble(1);
                    double c = transform.getDouble(2);
                    double d = transform.getDouble(3);
                    double e = transform.getDouble(4);
                    double f = transform.getDouble(5);
                    double g = transform.getDouble(6);
                    double h = transform.getDouble(7);
                    double i = transform.getDouble(8);
                    double j = transform.getDouble(9);
                    double k = transform.getDouble(10);
                    double l = transform.getDouble(11);
                    double m = transform.getDouble(12) - min.getDouble(0);
                    double n = transform.getDouble(13) - min.getDouble(1);
                    double o = transform.getDouble(14) - min.getDouble(2);
                    double p = transform.getDouble(15);

                    int val = 100 + 60;
                    // pos
                    Vec3 pos = new Vec3(aJCas);
                    pos.setX(m*val); // m to cm conversion
                    pos.setY(n*val);
                    pos.setZ(o*val);
                    entity.setPosition(pos);

                    System.out.println(m+" "+o+" "+n);

                    // scale
                    double sa = Math.sqrt(Math.pow(a,2) + Math.pow(e,2) + Math.pow(i,2));
                    double sb = Math.sqrt(Math.pow(b,2) + Math.pow(f,2) + Math.pow(j,2));
                    double sc = Math.sqrt(Math.pow(c,2) + Math.pow(g,2) + Math.pow(k,2));
                    Vec3 scale = new Vec3(aJCas);/*
                    scale.setX(sa);
                    scale.setY(sb);
                    scale.setZ(sc);*/
                    scale.setX(100);
                    scale.setY(100);
                    scale.setZ(100);
                    entity.setScale(scale);

                    // rot
                    double ra = Math.acos(a/sa);
                    System.out.println("rot: " + ra);
                    double rb = 0;//Math.acos(b/sb);
                    double rc = 0;//Math.acos(c/sc);

                    double [] euler = {ra, rb, rc};


                    double yaw=euler[0];//*/16(Math.PI/180);
                    double roll=euler[1];//*/16;//*(Math.PI/180);
                    double pitch=euler[2];//*/16;//*(Math.PI/180);

                    double rollOver2 = roll * 0.5f;
                    double sinRollOver2 = (double)Math.sin ((double)rollOver2);
                    double cosRollOver2 = (double)Math.cos ((double)rollOver2);
                    double pitchOver2 = pitch * 0.5f;
                    double sinPitchOver2 = (double)Math.sin ((double)pitchOver2);
                    double cosPitchOver2 = (double)Math.cos ((double)pitchOver2);
                    double yawOver2 = yaw * 0.5f;
                    double sinYawOver2 = (double)Math.sin ((double)yawOver2);
                    double cosYawOver2 = (double)Math.cos ((double)yawOver2);

                    double qw = cosYawOver2 * cosPitchOver2 * cosRollOver2 + sinYawOver2 * sinPitchOver2 * sinRollOver2;
                    double qx = cosYawOver2 * sinPitchOver2 * cosRollOver2 + sinYawOver2 * cosPitchOver2 * sinRollOver2;
                    double qy = sinYawOver2 * cosPitchOver2 * cosRollOver2 - cosYawOver2 * sinPitchOver2 * sinRollOver2;
                    double qz = cosYawOver2 * cosPitchOver2 * sinRollOver2 - sinYawOver2 * sinPitchOver2 * cosRollOver2;
                    /*
                    double yaw=euler[0];// 16(Math.PI/180);
                    double roll=euler[1];///16;//*(Math.PI/180);
                    double pitch=euler[2];///16;//*(Math.PI/180);
                    double t0 = Math.cos(yaw * 0.5);
                    double t1 = Math.sin(yaw * 0.5);
                    double t2 = Math.cos(roll * 0.5); // 1
                    double t3 = Math.sin(roll * 0.5);
                    double t4 = Math.cos(pitch * 0.5); // 1
                    double t5 = Math.sin(pitch * 0.5);
                    double [] quaternion = {t0 * t2 * t5 + t1 * t3 * t4,-(t0 * t2 * t4 + t1 * t3 * t5),-(t0 * t3 * t4 - t1 * t2 * t5),-(t1 * t2 * t4 - t0 * t3 * t5)};*/
                    //double [] quaternion = {0,0.92388,0,-0.38268};

                    System.out.println("objIDobjID "+objIDSunCG);
                    switch (objIDSunCG) {
                        case "s__881":
                        case "s__783":
                            //suncg bed is opposite its shapenet counterparts
                            euler[0] += Math.PI;
                            System.out.println("adjusting bed rotation by force");
                        default:
                    }
                    double [] quaternion = {0,Math.cos(0.5*euler[0]),0,-Math.sin(0.5*euler[0])};
                    //double [] quaternion = {0,t0 * t2,0,t0 * t2};

                    Vec4 rot = new Vec4(aJCas);
                    rot.setX(quaternion[0]);
                    rot.setY(quaternion[1]);
                    rot.setZ(quaternion[2]);
                    rot.setW(quaternion[3]);
                    /*rot.setX(qx);
                    rot.setY(qy);
                    rot.setZ(qz);
                    rot.setW(qw);*/
                    entity.setRotation(rot);

                    // elevation
                    /*int elev = transform.getInt(15);
                    entity.setElevation(elev);*/

                    idMap.put(objID, entity);
                    entity.addToIndexes();
                }
            }
        }
/*        Document jsondoc = builder.parse(aReader);
        Element root = jsondoc.getDocumentElement();

		String objID = json["id"];

		SpatialEntity entiy = new SpatialEntity(aJCas);

		entiy.setObject_id(objID);

		Vec3 pos = new Vec3(aJCas);
		pos.setX();

		entiy.setRotation(objID);
		entiy.setScale(objID);
		entiy.setPosition(pos);

		idMap.put(id, entity);
		entity.addToIndexes();*/

    }

    public String obtainShapeNetModelIdForSUNCGModelID(String suncgId) {
        System.out.println("looking up names for suncgID " + suncgId);
        List<List<String>> descriptionRanks = idMap_SUNCG.get(suncgId);
        // { {}, {}, {}, {} }
        System.out.println("got description " + descriptionRanks);

        List<Set> matches = new ArrayList<>();
        for (int i=0; i<4; i++) {
            Set<String> match = new HashSet<>();
            matches.add(match);
        }

        //look for a very good fit
        for (int a=0; a<2; a++) {
            List<String> suncgRankWords = descriptionRanks.get(a);
            for (String suncgWord: suncgRankWords){
                if(suncgWord.equals("")) {
                    continue;
                }
                // now loop throught all shapenet Data
                for (String shapenetKey : idMap_ShapeNet.keySet()) {
                    List<List<String>> shapenetRanks = idMap_ShapeNet.get(shapenetKey);
                    //only now have we selected a shapenet object
                    for (int b=0; b<2; b++) {
                        //select words of a specific rank
                        List<String> shapenetRankWords = shapenetRanks.get(b);

                        if(shapenetRankWords.contains(suncgWord)){
                            if (a==0 && b==0) {
                                matches.get(0).add(shapenetKey);
                            } else {
                                matches.get(1).add(shapenetKey);
                            }
                        }
                    }
                }
            }
        }

        if(!matches.get(0).isEmpty()) {
            System.out.println("GOOD MATCH:" + matches.get(0));
            int ln = matches.get(0).size();
            int ind = ThreadLocalRandom.current().nextInt(0, ln);
            //getList
            List<String> finalRes = new ArrayList<String>();
            finalRes.addAll(matches.get(0));
            return finalRes.get(ind);
        }

        if(!matches.get(1).isEmpty()) {
            System.out.println("FAIR MATCH:" + matches.get(1));
            int ln = matches.get(1).size();
            int ind = ThreadLocalRandom.current().nextInt(0, ln);
            //getList
            List<String> finalRes = new ArrayList<String>();
            finalRes.addAll(matches.get(1));
            return finalRes.get(ind);
        }

        //chuck it, just return anything remotely similar
        for (int a=0; a<4; a++) {
            List<String> suncgRankWords = descriptionRanks.get(a);
            for (String suncgWord: suncgRankWords){
                if(suncgWord.equals("")) {
                    continue;
                }
                // now loop throught all shapenet Data
                for (String shapenetKey : idMap_ShapeNet.keySet()) {
                    List<List<String>> shapenetRanks = idMap_ShapeNet.get(shapenetKey);
                    //only now have we selected a shapenet object
                    for (int b=0; b<4; b++) {
                        //select words of a specific rank
                        List<String> shapenetRankWords = shapenetRanks.get(b);

                        if(shapenetRankWords.contains(suncgWord)){
                            matches.get(2).add(shapenetKey);
                        }
                    }
                }
            }
        }

        if(!matches.get(2).isEmpty()) {
            System.out.println("POOR MATCH:" + matches.get(2));
            int ln = matches.get(2).size();
            int ind = ThreadLocalRandom.current().nextInt(0, ln);
            //getList
            List<String> finalRes = new ArrayList<String>();
            finalRes.addAll(matches.get(2));
            return finalRes.get(ind);
        }

        /*for (String descriptionRankArray: descriptions){
            for (int a=0; a<4; a++) {
                for (String key : idMap_ShapeNet.keySet()) {
                    Set<String> tags = idMap_ShapeNet.get(key);
                    if (tags.contains(description)) {
                        matches
                        return key;
                        //break;
                    }
                }
            }
        }*/
        System.out.println("NOT FOUND");
        //System.out.println("IdShapeNet " + idMap_ShapeNet);

        return "00000";
    }


    //Dayana, Jan
    // returns a Map with all SUNCG ids each coupled with a List of String arrays of possible keywords each array of different priority, so as to aid finding their ShapeNet equivalent
    private static Map IdSUNCG() throws FileNotFoundException {
        // Creating a dictionary: key=Id, value=wnlemmas
        Map<String, List<List<String>>> map = new HashMap<>();
        //Get scanner instance
        Scanner scanner = new Scanner(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/ModelCategoryMapping.v2.csv"));
        //Set the delimiter to split objects
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
            String data = scanner.next();
            //for each object split arguments-string into an array using comma
            //ignore the commas within quotes
            String[] elem = data.split(",");
            //add words in the dictionary
            //map.put(elem[1], elem[2].replaceAll("_", " "));


            String[] res = new String[4];

            if (elem[2].replace("\"", "").length() != 0) {
                res[0] = elem[2];
            }
            if (elem[3].replace("\"", "").length() != 0) {
                res[1] = elem[3];
            }
            if (elem[5].replace("\"", "").length() != 0) {
                res[2] = elem[5];
            }
            if (elem[7].replace("\"", "").length() != 0) {
                if(elem[7].split(".").length > 0){
                    res[3] = elem[7].split(".")[0];
                }
            }

            List<List<String>> resArrays = new ArrayList<>();
            for(int i=0; i<4; i++) {
                if(res[i] == null) {
                    res[i] = "";
                }
                res[i] = res[i].replace("_", " ").replace("\"", "").replace("[", "").replace("]", "").toLowerCase();
                List<String> resArray = Arrays.asList(res[i].split(","));

                //rm duplicates
                Set<String> resSet = new HashSet<String>(resArray);

                //getList
                List<String> finalRes = new ArrayList<String>();
                finalRes.addAll(resSet);

                // get { hit1, hit2, hit3 } , asc priority
                resArrays.add(finalRes);
            }

            map.put(elem[1], resArrays);

        }
        //Do not forget to close the scanner
        scanner.close();
        return map;
    }


    //Dayana
    private static Map IdShapeNet() throws FileNotFoundException {
        // Creating a dictionary: key=Id, value=wnlemmas
        Map<String, List<List<String>>> map = new HashMap<>();

        //Get scanner instance
        Scanner scanner = new Scanner(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/metadata_filter.csv"));
        //Set the delimiter to split objects
        scanner.useDelimiter("wss.");
        while (scanner.hasNext()) {
            String data = scanner.next();
            //for each object split arguments-string into an array using comma
            //ignore the commas within quotes
            String[] elem = data.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            //add words in the dictionary
            String[] res = new String[4];

            if (elem[1].replace("\"", "").length() != 0) {
                res[0] = elem[1];
            }
            if (elem[3].replace("\"", "").length() != 0) {
                res[1] = elem[3];
            }
            if (elem[14].replace("\"", "").length() != 0) {
                res[2] = elem[14];
            }
            if (elem[15].replace("\"", "").length() != 0) {
                res[3] = elem[15];
            }


            List<List<String>> resArrays = new ArrayList<>();
            for(int i=0; i<4; i++) {
                if(res[i] == null) {
                    res[i] = "";
                }
                res[i] = res[i].replace("_", " ").replace("\"", "").replace("[", "").replace("]", "").toLowerCase();
                List<String> resArray = Arrays.asList(res[i].split(","));

                //rm duplicates
                Set<String> resSet = new HashSet<String>(resArray);

                //getList
                List<String> finalRes = new ArrayList<String>();
                finalRes.addAll(resSet);

                // get { hit1, hit2, hit3 } , asc priority
                resArrays.add(finalRes);
            }

            map.put(elem[0], resArrays);
            /*
            res = res.replace("\"", "").replace("[", "").replace("]", "").toLowerCase();

            List<String> resArray = Arrays.asList(res.split(","));
            Set<String> resSet = new HashSet<String>(resArray);

            map.put(elem[0], resSet);*/
            //System.out.println(elem[0] + "\n " + map.get(elem[0]));
        }
        //Do not forget to close the scanner
        scanner.close();
/*
        // if we make a search
        File jsonInputFile = new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/search.json");
        InputStream is;
        try {
            is = new FileInputStream(jsonInputFile);
            // Create JsonReader from Json.
            JsonReader reader = Json.createReader(is);
            // Get the JsonObject structure from JsonReader.
            JsonObject empObj = reader.readObject();
            reader.close();
            // read json array
            JsonArray arrObj = empObj.getJsonArray("results");
            for (int i = 0; i < arrObj.size(); i++) {
                JsonObject jsonObject1 = arrObj.getJsonObject(i);
                String ind = jsonObject1.getString("id");
                if (map.get(ind) == null) {
                    String wnlemmas = jsonObject1.getJsonArray("wnlemmas").toString().replace("\"", "").replace("[", "").replace("]", "").toLowerCase();
                    if (wnlemmas.length() == 0) {
                        map.put(ind, jsonObject1.getJsonArray("categories").toString().replace("\"", "").replace("[", "").replace("]", "").toLowerCase());
                    } else {
                        map.put(ind, wnlemmas);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        return map;
    }

}
