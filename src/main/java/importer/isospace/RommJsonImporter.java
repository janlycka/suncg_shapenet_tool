package importer.isospace;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.S;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.resources.CompressionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.texttechnologylab.annotation.semaf.IsoSpatial.Vec3;
import org.texttechnologylab.annotation.semaf.IsoSpatial.Vec4;
import org.texttechnologylab.annotation.semaf.isospace.SpatialEntity;
import org.xml.sax.SAXException;

import javax.json.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

//import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

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
    Map<String, List<Float>> dimsMap_ShapeNet = null;
    Map<String, List<Float>> dimsMap_SUNCG = null;
    List<double[]> collisionBBoxes = new ArrayList<>();

    private void convert(JCas aJCas, InputStream aReader, boolean links) throws IOException, SAXException
    {
        if (idMap_SUNCG == null) {
            idMap_SUNCG = IdSUNCG();
        }
        if (idMap_ShapeNet == null) {
            idMap_ShapeNet = IdShapeNet();
        }
        if (dimsMap_ShapeNet == null) {
            dimsMap_ShapeNet = DimsShapeNet();
        }
        if (dimsMap_SUNCG == null) {
            dimsMap_SUNCG = DimsSUNCG();
        }


        //System.out.println(aReader);

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
            //System.out.println(jsonString);
        }

        JSONObject jsonContents = new JSONObject(jsonString);
        //String pageName = obj.getJSONObject("pageInfo").getString("pageName");

        JSONArray levels = jsonContents.getJSONArray("levels");
        for (int ind_levels = 0; ind_levels < levels.length(); ind_levels++)
        {
            JSONObject level = levels.getJSONObject(ind_levels);
            JSONArray nodes = level.getJSONArray("nodes");
            JSONArray min = new JSONArray(3);
            JSONArray max = new JSONArray(3);
            System.out.println(min.toString());

            for (int ind_nodes = 0; ind_nodes < nodes.length(); ind_nodes++)
            {
                JSONObject node = nodes.getJSONObject(ind_nodes);
                String nodeType = node.getString("type");
                if (nodeType.equals("Room")) {
                    System.out.println("Room");
                    JSONObject bbox = node.getJSONObject("bbox");
                    min = bbox.getJSONArray("min");
                    max = bbox.getJSONArray("max");

                    double width = max.getDouble(0) - min.getDouble(0);
                    double height = max.getDouble(1) - min.getDouble(1);
                    double depth = max.getDouble(2) - min.getDouble(2);
                    double XZratio = width / depth;

                    System.out.println(width+" dddd");

                    String objID = "room05";//later choose either at random or as per some criteria to get the most similar room
                    SpatialEntity entity = new SpatialEntity(aJCas);
                    entity.setObject_id(objID);

                    //specific scale for the ShapeNet model "room5"
                    double rm5scale = 0.6;

                    Vec3 pos = new Vec3(aJCas);
                    pos.setX(width*100/2); // m to cm conversion
                    pos.setY(90);//(height*100.0/2);
                    pos.setZ(depth*100.0/2);
                    /*pos.setX(0);
                    pos.setY(0);
                    pos.setZ(0);*/
                    entity.setPosition(pos);

                    Vec3 scale = new Vec3(aJCas);
                    scale.setX(rm5scale*100.0);
                    scale.setY(rm5scale*100.0);
                    scale.setZ(rm5scale*100.0*1.5*XZratio);
                    entity.setScale(scale);

                    Vec4 rot = new Vec4(aJCas);
                    rot.setX(0);
                    rot.setY(0);
                    rot.setZ(0);
                    rot.setW(0);
                    entity.setRotation(rot);

                    idMap.put(objID, entity);
                    entity.addToIndexes();
                }
                else if (nodeType.equals("Object")) {
                    System.out.println("\nObject  " + node + "\n");

                    String objIDSunCG = node.getString("modelId");

                    //the size of the object
                    double [] idealBBox = {0,0,0,0,0,0};

                    //modelId is in SUNCG-Format -> get object description -> search ShapeNet DB for matching objects
                    String objID = SUNCGIDtoShapeNetID(objIDSunCG);

                    System.out.println("An object from ShapeNet with the following id: "+objID + "\n and dimensions "+ dimsMap_ShapeNet.get(objID)+ " was chosen");

                    // get request to get height to place all object at the same position on the Y axis
                    double objYAxisOffset = 0.0;
                    double flipRoll = 0.0;
                    double flipYaw = 0.0;
                    System.out.println("SENDING GET REQUEST");
                    try {
                        String res = getHTML("http://shapenet.texttechnologylab.org/getFeature?id="+objID);
                        JSONObject jsonRes = new JSONObject(res);
                        JSONObject feature = jsonRes.getJSONObject("feature");
                        String alignedDims = feature.getString("alignedDims");
                        JSONArray dims = new JSONArray(alignedDims);
                        //System.out.println("alignedDims" + dims);
                        objYAxisOffset = dims.getDouble(1)/2.0;

                        String upObj = feature.getString("up");
                        JSONArray upObjArr = new JSONArray(upObj);
                        double a = upObjArr.getDouble(0);
                        double b = upObjArr.getDouble(1);
                        double c = upObjArr.getDouble(2);

                        String frontObj = feature.getString("front");
                        JSONArray frontObjArr = new JSONArray(frontObj);
                        double d = frontObjArr.getDouble(0);
                        double e = frontObjArr.getDouble(1);
                        double f = frontObjArr.getDouble(2);

                        String pars = (int)a + "" + (int)b + "" + (int)c + "" + (int)d + "" + (int)e + "" + (int)f;
                        System.out.println("pars "+pars);
                        switch(pars) {
                            case "010010":
                            case "001100":
                            case "001-100":
                                System.out.println("flipYaw++");
                                flipYaw += Math.PI;
                                break;
                            case "001010":
                                System.out.println("flipYaw+");
                                flipRoll += Math.PI;
                                break;
                            default:
                        }

                        //fix overlap

                        idealBBox[3] = dims.getDouble(0);
                        idealBBox[4] = dims.getDouble(1);
                        idealBBox[5] = dims.getDouble(2);

                        /*if(frontObjArr.getDouble(0) == 0.0 && frontObjArr.getDouble(1) == -1.0 && frontObjArr.getDouble(2) == 0.0){
                            flipYaw += Math.PI;
                            flipRoll += Math.PI;
                        }*/

                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    System.out.println("MOVING BY "+objYAxisOffset);
                    System.out.println("GET REQUEST DONE");

                    JSONArray transform = node.getJSONArray("transform");

                    SpatialEntity entity = new SpatialEntity(aJCas);
                    entity.setObject_id(objID);

                    System.out.println(min);

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

                    idealBBox[0] = m*val-100;
                    idealBBox[1] = n*val+objYAxisOffset;
                    idealBBox[2] = o*val-100;

                    //fix overlap
                    for (double[] box : collisionBBoxes){
                        double aWEST = box[0] + box[3]/2; // upper left corner from centered point X
                        double bWEST = idealBBox[0] + idealBBox[3]/2;
                        double aEAST = box[0] - box[3]/2; // upper right corner from centered point box[0]
                        double bEAST = idealBBox[0] - idealBBox[3]/2;
                        double aNORTH = box[2] + box[5]/2; // lower left corner from centered point box[0]
                        double bNORTH = idealBBox[2] + idealBBox[5]/2;
                        double aSOUTH = box[2] - box[5]/2; // lower right corner from centered point box[0]
                        double bSOUTH = idealBBox[2] - idealBBox[5]/2;

                        System.out.println("box " + box[0] + " " + box[2] + " " + " idealBBox " + idealBBox[0] + " " + idealBBox[2] + " ");

                        //enveloped
                        /*if(bWEST < aWEST && bEAST >= aWEST && bNORTH < aNORTH && bSOUTH >= aNORTH)
                        {
                            sss
                        }*/
                        if(
                                bWEST > aWEST && bEAST < aWEST && bNORTH < aNORTH && bSOUTH > aNORTH ||
                                        bWEST > aWEST && bEAST < aWEST && bNORTH < aSOUTH && bSOUTH > aSOUTH ||
                                        bWEST > aWEST && bEAST < aWEST && bNORTH > aNORTH && bSOUTH < aSOUTH
                        ) {
                            System.out.println("OVERLAP found");
                            double diffX = Math.abs(box[0]-idealBBox[0]);
                            double diffZ = Math.abs(box[2]-idealBBox[2]);
                            if(diffX > diffZ) {
                                idealBBox[0] += aWEST-bEAST;
                            } else {
                                if(box[2]>idealBBox[2]){
                                    idealBBox[2] -= bSOUTH-aNORTH;
                                } else {
                                    idealBBox[2] += aSOUTH-bNORTH;
                                }
                            }
                        }
                        if(
                                bWEST > aEAST && bEAST < aEAST && bNORTH < aNORTH && bSOUTH > aNORTH ||
                                        bWEST > aEAST && bEAST < aEAST && bNORTH < aSOUTH && bSOUTH > aSOUTH ||
                                        bWEST > aEAST && bEAST < aEAST && bNORTH > aNORTH && bSOUTH < aSOUTH
                        ) {
                            System.out.println("OVERLAP found");
                            double diffX = Math.abs(box[0]-idealBBox[0]);
                            double diffZ = Math.abs(box[2]-idealBBox[2]);
                            if(diffX > diffZ) {
                                idealBBox[0] -= bWEST-aEAST;
                            } else {
                                if(box[2]>idealBBox[2]){
                                    idealBBox[2] -= bSOUTH-aNORTH;
                                } else {
                                    idealBBox[2] += aSOUTH-bNORTH;
                                }
                            }
                        }
                    }

                    // pos
                    Vec3 pos = new Vec3(aJCas);
                    pos.setX(idealBBox[0]); // m to cm conversion
                    pos.setY(idealBBox[1]);
                    pos.setZ(idealBBox[2]);
                    entity.setPosition(pos);

                    collisionBBoxes.add(idealBBox);

                    System.out.println(m+" "+o+" "+n);
                    System.out.println(transform.getDouble(12)+" "+transform.getDouble(13)+" "+ transform.getDouble(14));

                    // scale
                    double sa = Math.sqrt(Math.pow(a,2) + Math.pow(e,2) + Math.pow(i,2));
                    double sb = Math.sqrt(Math.pow(b,2) + Math.pow(f,2) + Math.pow(j,2));
                    double sc = Math.sqrt(Math.pow(c,2) + Math.pow(g,2) + Math.pow(k,2));
                    Vec3 scale = new Vec3(aJCas);
                    /*
                    scale.setX(sa);
                    scale.setY(sb);
                    scale.setZ(sc);*/
                    scale.setX(100);
                    scale.setY(100);
                    scale.setZ(100);
                    if(objIDSunCG.equals("212")) {
                        scale.setX(1);
                        scale.setY(1);
                        scale.setZ(1);
                    }

                    entity.setScale(scale);

                    // rot
                    double ra = Math.acos(a/sa);
                    System.out.println("rotation: " + ra);
                    double rb = 0;//Math.acos(b/sb);
                    double rc = 0;//Math.acos(c/sc);

                    double [] euler = {ra, rb, rc};

/*
                    double yaw=euler[0];// * /16(Math.PI/180);
                    double roll=euler[1];// * /16;//*(Math.PI/180);
                    double pitch=euler[2];// * /16;//*(Math.PI/180);

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

                    //System.out.println("objIDobjID "+objIDSunCG);
                    //euler[0] += Math.PI;
                    /*switch (objIDSunCG) {
                        case "s__881":
                        //case "s__783":
                            //suncg bed is opposite its shapenet counterparts
                            euler[0] += Math.PI;
                            System.out.println("adjusting bed rotation by force");
                        default:
                    }*/
                    euler[0] += flipYaw;
                    euler[1] += flipRoll;

                    /*switch (objID) {
                        case "7da05d146efc0b4de32246b3665cd65f":
                        *//*case "8ed7ab9b5569c0556d469bed7949dee":
                        case "92b50fdd2157d0f9738e43095496b061":
                        case "72692864abdfa144d24546afc5de2582":*//*
                            //yaw180
                            euler[0] += Math.PI;
                        case "9ef9ea0de3fe64d4e7e684d25d4dcaf0":
                        case "2bc37c326782ecfb9bbc8ad5ece3c1b":
                            //roll180
                            euler[1] += Math.PI;
                        default:
                    }*/
                    //double [] quaternion = {0,Math.cos(0.5*euler[0]),0,-Math.sin(0.5*euler[0])};
                    //double [] quaternion = {0,t0 * t2,0,t0 * t2};

                    double yaw=euler[0];// 16(Math.PI/180);
                    double roll=euler[1];///16;//*(Math.PI/180);
                    double pitch=euler[2];///16;//*(Math.PI/180);
                    double t0 = Math.cos(yaw * 0.5);
                    double t1 = Math.sin(yaw * 0.5);
                    double t2 = Math.cos(roll * 0.5); // 1
                    double t3 = Math.sin(roll * 0.5);
                    double t4 = Math.cos(pitch * 0.5); // 1
                    double t5 = Math.sin(pitch * 0.5);
                    double [] quaternion = {t0 * t2 * t5 + t1 * t3 * t4,-(t0 * t2 * t4 + t1 * t3 * t5),-(t0 * t3 * t4 - t1 * t2 * t5),-(t1 * t2 * t4 - t0 * t3 * t5)};

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
    }

    //Dayana
    //returns the dimensions of Shape-Net rooms
    public static Map Rooms() throws FileNotFoundException {
        // Creating a dictionary: key=Id, value=fine_grained_class,coarse_grained_class and nyuv2_40class
        Map<String, double[]> map = new HashMap<>();

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
            JsonArray arrObj = empObj.getJsonArray("rooms");
            for (int i = 0; i < arrObj.size(); i++) {
                JsonObject jsonObject1 = arrObj.getJsonObject(i);
                String ind = jsonObject1.getString("id");
                JsonString curr = jsonObject1.getJsonString("alignedDims");
                String[] dims = curr.toString().replace("[", "").replace("]", "").replace("\"", "").split(",");
                double[] dim= {Double.parseDouble(dims[0]),Double.parseDouble(dims[1]),Double.parseDouble(dims[2]) };
                map.put(ind, dim);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return map;
    }

    public String SUNCGIDtoShapeNetID(String suncgId) {

        List<Float> dims = dimsMap_SUNCG.get(suncgId);
        System.out.println("looking up names for suncgID " + suncgId + " and with sizes approx. " + dims);
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
            float r=Float.parseFloat("5.0");
            Set<String> current_matches = new HashSet<>();

            while(current_matches.isEmpty()){
                for(Object m: matches.get(0)){
                    List<Float> dim = dimsMap_ShapeNet.get(m.toString());
                    if(Math.abs(dims.get(0)-dim.get(0))<r&Math.abs(dims.get(2)-dim.get(2))<r){
                        current_matches.add(m.toString());
                    }
                }
                r = r + 3;
            }
            System.out.println("GOOD MATCH:" + current_matches);
            int ln = current_matches.size();
            int ind = ThreadLocalRandom.current().nextInt(0, ln);
            //getList
            List<String> finalRes = new ArrayList<String>();
            finalRes.addAll(current_matches);
            return finalRes.get(ind);
        }

        if(!matches.get(1).isEmpty()) {
            float r=Float.parseFloat("5.0");
            Set<String> current_matches = new HashSet<>();
            while(current_matches.isEmpty()){
                for(Object m: matches.get(1)){
                    List<Float> dim = dimsMap_ShapeNet.get(m.toString());
                    if(Math.abs(dims.get(0)-dim.get(0))<r&Math.abs(dims.get(2)-dim.get(2))<r){
                        current_matches.add(m.toString());
                    }
                }
                r = r + 3;
            }
            System.out.println("FAIR MATCH:" + current_matches);
            int ln = current_matches.size();
            int ind = ThreadLocalRandom.current().nextInt(0, ln);
            //getList
            List<String> finalRes = new ArrayList<String>();
            finalRes.addAll(current_matches);
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
            float r=Float.parseFloat("5.0");
            Set<String> current_matches = new HashSet<>();
            while(current_matches.isEmpty()){
                for(Object m: matches.get(2)){
                    List<Float> dim = dimsMap_ShapeNet.get(m.toString());
                    if(Math.abs(dims.get(0)-dim.get(0))<r&Math.abs(dims.get(2)-dim.get(2))<r){
                        current_matches.add(m.toString());
                    }
                }
                r = r + 3;
            }
            System.out.println("POOR MATCH:" + current_matches);
            int ln = current_matches.size();
            int ind = ThreadLocalRandom.current().nextInt(0, ln);
            //getList
            List<String> finalRes = new ArrayList<String>();
            finalRes.addAll(current_matches);
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
    // returns a Map with all SUNCG ids each coupled with a List of
    // String arrays of possible keywords each array of different priority,
    // so as to aid finding their ShapeNet equivalent
    // do not need any pets or persons
    public static Map IdSUNCG() throws FileNotFoundException {
        // Creating a dictionary: key=Id, value=fine_grained_class,coarse_grained_class and nyuv2_40class
        Map<String, List<List<String>>> map = new HashMap<>();
        //Get scanner instance
        Scanner scanner = new Scanner(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/res.csv"));
        //Set the delimiter to split objects
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
            String data = scanner.next();
            //for each object split arguments-string into an array using comma
            //ignore the commas within quotes
            String[] elem = data.split(",");

            String[] res = new String[4];
            if (!(elem[6].contains("person"))){//if (!(elem[6].contains("person") || elem[6].contains("pet") ||elem[6].contains("kitchenware") ||elem[6].contains("books/") || elem[6].contains("toy") || elem[6].contains("book/"))){
            //fine_grained_class
                if (elem[2].replace("\"", "").replace("\n", "").length() != 0) {
                    res[0] = elem[2];
                }
                //coarse_grained_class
                if (elem[3].replace("\"", "").replace("\n", "").length() != 0) {
                    res[1] = elem[3];
                }
                //nyuv2_40class
                if (elem[5].replace("\"", "").replace("\n", "").length() != 0) {
                    res[2] = elem[5];
                }
                //wnsynsetkey
                if (elem.length>=7) {
                    if (elem[7].replace("\"", "").replace("\n", "").length() != 0) {
                        if (elem[7].split(".").length > 0) {
                            res[3] = elem[7].split(".")[0];
                        }
                    }
                }
            }

            List<List<String>> resArrays = new ArrayList<>();
            for(int i=0; i<4; i++) {
                //to make a replacement
                if (res[i] == null) {
                    res[i] = "";
                }
                res[i] = res[i].replace("_", " ").replace("\"", "").replace("[", "").replace("\n", "").replace("]", "").toLowerCase();
                List<String> resArray = Arrays.asList(res[i].split(","));
                resArrays.add(resArray);
            }
            map.put(elem[1], resArrays);
        }
        //Do not forget to close the scanner
        scanner.close();
        return map;
    }

    //Dayana
    // returns a Map with all SUNCG objects and their up and front vectors
    //ATTENTION: if not provided-> 0,1,0 and 0,0,1
    public static Map MMSUNCG() throws FileNotFoundException {
        //Creating a dictionary: key=Id, value={up, front}
        Map<String, double[][]> map = new HashMap<>();
        //Get scanner instance
        Scanner scanner = new Scanner(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/res.csv"));
        //Set the delimiter to split objects
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
            String data = scanner.next();
            //for each object split arguments-string into an array using comma
            //ignore the commas within quotes
            String[] elem = data.split(",");

            //add words in the dictionary
            String[] up = elem[9].replace("\"", "").replace("\\", "").split("/");
            String[] front = elem[10].replace("\"", "").replace("\\", "").split("/");

            double[] stUp = {0.0, 1.0,0.0};
            double[] stFront = {0.0,0.0,1.0};
            double[][] dims = {stUp, stFront};

            if (!elem[9].equals("up") & !elem[9].equals("")) {
                dims[0] = new double[]{Double.parseDouble(up[0]), Double.parseDouble(up[1]), Double.parseDouble(up[2])};
            }
            if (!elem[10].equals("front") & !elem[10].equals("")) {
                dims[1] = new double[]{Double.parseDouble(front[0]), Double.parseDouble(front[1]), Double.parseDouble(front[2])};
            }
            if(dims[0]==null || dims[1]==null){
                System.out.println(elem[1]);
            }
            if (!elem[0].equals("model_id")) {
                map.put(elem[1], dims);
            }
        }
        //Do not forget to close the scanner
        scanner.close();
        return map;
    }

    //Dayana
    public static Map DimsSUNCG() throws FileNotFoundException {
        // Creating a dictionary: key=Id, value=alignedDims
        Map<String, double[]> map = new HashMap<>();
        //Get scanner instance
        Scanner scanner = new Scanner(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/res.csv"));
        //Set the delimiter to split objects
        scanner.useDelimiter("\n");
        while (scanner.hasNext()) {
            String data = scanner.next();
            //for each object split arguments-string into an array using comma
            //ignore the commas within quotes
            String[] elem = data.split(",");

            //add words in the dictionary
            String[] dimens = elem[13].replace("\"", "").replace("\\", "").split("/");


            if (elem[13].equals("aligned.dims")) {
            } else if (elem[13].equals("")) {
            }
            else {
                double[] dims = {Double.parseDouble(dimens[0]), Double.parseDouble(dimens[1]), Double.parseDouble(dimens[2])} ;
                map.put(elem[1], dims);
            }
        }

        //Do not forget to close the scanner
        scanner.close();
        return map;
    }

    //Dayana
    // returns a Map with all ShapeNet ids each coupled with a List of
    // String arrays of possible keywords each array of different priority,
    // so as to aid finding their ShapeNet equivalent
    public static Map IdShapeNet() throws FileNotFoundException {
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

            if (elem[1].replace("\"", "").replace("\n", "").length() != 0) {
                res[0] = elem[3];
            }
            if (elem[3].replace("\"", "").replace("\n", "").length() != 0) {
                res[1] = elem[1];
            }
            if (elem[14].replace("\"", "").replace("\n", "").length() != 0) {
                res[2] = elem[14];
            }
            if (elem[15].replace("\"", "").replace("\n", "").length() != 0) {
                res[3] = elem[15];
            }

            List<List<String>> resArrays = new ArrayList<>();
            for(int i=0; i<4; i++) {
                if(res[i] == null) {
                    res[i] = "";
                }
                res[i] = res[i].replace("_", " ").replace("\"", "").replace("[", "").replace("]", "").replace("\n", "").toLowerCase();
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
        }
        //Do not forget to close the scanner
        scanner.close();
        return map;
    }

    //Dayana
    // returns a Map with all ShapeNet objects and their dimensions
    public static Map DimsShapeNet() throws FileNotFoundException {
        // Creating a dictionary: key=Id, value=alignedDIms
        Map<String, double[]> map = new HashMap<>();

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
            String[] dimens = elem[7].replace("\"", "").replace("\\", "").split(",");

            if (elem[7].equals("aligned.dims")){
            }
            else {
                double[] dims = {Double.parseDouble(dimens[0]), Double.parseDouble(dimens[1]), Double.parseDouble(dimens[2])} ;
                map.put(elem[0], dims);
            }
        }
        //Do not forget to close the scanner
        scanner.close();
        return map;
    }

    //Dayana
    // returns a Map with all ShapeNet objects and and their up and front vectors
    //ATTENTION: if not provided-> 0,1,0 and 0,1,0
    public static Map MMShapeNet() throws FileNotFoundException {
        // Creating a dictionary: key=Id, value={up, front}
        Map<String, double[][]> map = new HashMap<>();

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
            //add words in the dictionary
            String[] up = elem[4].replace("\"", "").replace("\\", "").split(",");
            String[] front = elem[5].replace("\"", "").replace("\\", "").split(",");
            double[] stUp = {0.0, 1.0, 0.0};
            double[][] dims = {stUp, stUp};

            if (!elem[4].equals("up") & !elem[4].equals("") & elem[4].length()!=0) {
                dims[0] = new double[] {Double.parseDouble(up[0]), Double.parseDouble(up[1]), Double.parseDouble(up[2])};
            }
            if (!elem[5].equals("front") & !elem[5].equals("") & elem[5].length()!=0) {
                dims[1] = new double[]{Double.parseDouble(front[0]), Double.parseDouble(front[1]), Double.parseDouble(front[2])};
            }
            if(dims[0]==null || dims[1]==null){
                System.out.println(elem[0]);
            }
            if (!elem[0].equals("fullId")) {
                map.put(elem[0], dims);
            }

        }
        //Do not forget to close the scanner
        scanner.close();
        return map;
    }

    //Jan
    public static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                result.append(line);
            }
        }
        return result.toString();
    }

}
