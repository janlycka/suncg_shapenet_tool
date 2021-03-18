
package importer.isospace;
import java.io.*;

import java.util.*;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
//import org.ejml.ops.ReadCsv;
//import org.graalvm.compiler.nodes.calc.ObjectEqualsNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.json.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;

public class XMLToJSON {

    public static Map<String, List<List<String>>> idMap_SUNCG = null;
    public static Map<String, List<List<String>>> idMap_ShapeNet = null;
    public static Map<String, double[]> dimsMap_ShapeNet = null;
    public static Map<String, double[]> dimsMap_SUNCG = null;
    public static Map<String, double[][]> mmMap_SUNCG = null;
    public static Map<String, double[][]> mmMap_ShapeNet= null;
    public static Map<String, double[]> roomsShapeNet= null;


    //room params (here it is  room05 in ShapeNet and room with index 80 from SUNCG)
    //ATTENTION: room dims are given for standard up and front
    public static double[] mins= { 36.83515466266676, 0, 40.72485748816598};
    public static double[] maxs= {42.481718864233464, 2.739999938756228, 44.52473533291959};

    public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException, SAXException, ParserConfigurationException {
        String inputfolder = "/resources/spaceeval/xml/ACL21_Example.xml";
        //String outputfolder = System.getProperty("user.dir")+"/resources/spaceeval/xml/XML_To_JSON";
        String outputfolder = "C:/Users/Dayana Khadush/Text2Scene/suncg_shapenet_tool-jan2/resources/spaceeval/json/final_results_synth_bedroom";
        File directory = new File(String.valueOf(outputfolder));
        if (!directory.exists()) {
            directory.mkdir();
        }
        //Attention: standard up in both data sets is 0,1,0. However the front of objects in ShapeNet is 0,1,0 if not provided (in metadata_filter) and 0,0,1 in SUNCG
        double[] up={0.0, 1.0, 0.0};
        double[] front={0.0, 0.0, 1.0};

        // 1.Initialisierung des JSON files und JSONObject

        //Initialize JSON to write to
        //FileWriter fileJSON = new FileWriter( System.getProperty("user.dir")+"/resources/spaceeval/xml/XML_To_JSON/Example.json");
        FileWriter fileJSON = new FileWriter(outputfolder+ "/ACL_Example.json");
        //Initialize JSON Objects and initial values of the JSON file
        JSONObject json = new JSONObject();
        JSONArray objects = new JSONArray();
        json = initializeJson(json);
        int count = 1; // Count of JSONObject ID

        // 2.Parsen des XML Documents.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/ACL21_Example.xml"));
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        System.out.println(root.getNodeName());

        NodeList nList = root.getElementsByTagName("isospace:SpatialEntity");
        idMap_SUNCG = RommJsonImporter.IdSUNCG();
        idMap_ShapeNet = RommJsonImporter.IdShapeNet();
        dimsMap_ShapeNet = RommJsonImporter.DimsShapeNet();
        dimsMap_SUNCG = RommJsonImporter.DimsSUNCG();
        mmMap_SUNCG = RommJsonImporter.MMSUNCG();
        mmMap_ShapeNet = RommJsonImporter.MMShapeNet();
        roomsShapeNet = RommJsonImporter.Rooms();
        json.put("front", Arrays.asList(0, 0, 1 ));
        json.put("scaleToMeters", 1);
        json.put("scaleToMeters", 1);
        json.put("up", Arrays.asList(0, 1, 0));


        //different Scenes. We take 7836 in Example.xml
        String room = "7894";//"7894" or "7887"
        double[] roomGeom ={0.0, 0.0, 0.0};

        // Extract IDs for object, position, rotations und scale
        /*
        //if we work with Example.xml we have abstractareas and no Rooms
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE ) {
                Element eElement = (Element) nNode;
                String ID = eElement.getAttribute("object_id");


                if (ID.equals("abstractarea") & eElement.getAttribute("sofa").equals(room)) {
                    String scale = eElement.getAttribute("scale");
                    double[] scales = getScale(scale, root);
                    String position = eElement.getAttribute("position");
                    //room should be bigger that 2 (in each dim) and less than 10 (that is the whole apartment)
                    if (scales[0] > 2.0 & scales[0]  < 7.0 & scales[1] > 2.0 & scales[1]  < 7.0 &scales[2] > 2.0 & scales[2]  < 7.0 ) {
                        double[] positions = getPosition(position, root);
                        System.out.println("Room scale: " + scales[0] );
                        roomGeom[0]= positions[0] - scales[0];
                        roomGeom[1]= positions[1];
                        roomGeom[2]= positions[2] - scales[2];
                    }
                }
            }
        }
         */
        //in ACL21_Example.xml we do have rooms
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE ) {
                Element eElement = (Element) nNode;
                String ID = eElement.getAttribute("object_id");
                if (ID.contains("room") & eElement.getAttribute("sofa").equals(room)) {
                    String scale = eElement.getAttribute("scale");
                    double[] scales = getScale(scale, root);
                    double[] dimensions = roomsShapeNet.get(ID);
                    String position = eElement.getAttribute("position");
                    //room should be bigger that 2 (in each dim) and less than 10 (that is the whole apartment)
                    double[] positions = getPosition(position, root);

                    roomGeom[0]= dimensions[0]*scales[0]/100.0;
                    roomGeom[1]= dimensions[1]*scales[1]/100.0;
                    roomGeom[2]= dimensions[2]*scales[2]/100.0;
                }
            }
        }
        System.out.println("Room size: x = " + roomGeom[0] + ", z = " + roomGeom[2] );
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE ) {
                Element eElement = (Element) nNode;
                String ID = eElement.getAttribute("object_id");
                if (idMap_ShapeNet.containsKey(ID) & eElement.getAttribute("sofa").equals(room)) {
                    System.out.println("\n________________________");
                    System.out.println("Current object ID = " + ID);
                    String scale = eElement.getAttribute("scale");
                    String position = eElement.getAttribute("position");
                    String rotation = eElement.getAttribute("rotation");
                    //String axisrotation
                    // Extract x, y, z und w values for position, rotations and scale values from XML
                    double[] scales = getScale(scale, root);
                    double[] rotations = getRotation(rotation, root);
                    double[] positions = getPosition(position, root);
                    double x = positions[0] - roomGeom[0] + mins[0];
                    double z = positions[2] - roomGeom[2] + mins[2];
                    System.out.println("\n Quaternion: x = " + rotations[0] + " y = " + rotations[1] + " z = " + rotations[2] + " w = " + rotations[3]);



                    double[][] upfr = mmMap_ShapeNet.get(ID);
                    System.out.println("up and front vectors of the object");
                    for (int i = 0; i < upfr.length; i++) {
                        for (int j = 0; j < upfr[i].length; j++) {
                            System.out.print(upfr[i][j] + " ");
                        }
                        System.out.println("");
                    }

                    // we want to consider only the objects which can be rotated for DeepSynth
                    //actually almost all other objects are not objects DeepSynth is trained on e.g. Cellphone
                    String objID = ShapeNetIDtoSUNCGID(ID, scales);

                    System.out.println("\nObject rotation in euler angles ");
                    //in Unity - rotations of id_center object (parent object)
                    double[] euler = QuaternionToEuler(rotations);
                    for (int j = 0; j < euler.length; j++) {
                        System.out.print(euler[j] + " ");
                    }
                    double[][] axisRot= new double[][] {{1.0,0.0,0.0}, {0.0,1.0,0.0}, {0.0,0.0,1.0}};
                    System.out.println("\n");
                    if (upfr != null) {
                        //compute the euler angles to obtain correct orientation of the object according to its own up and front vectors
                        //in Unity - rotation of a child-object
                        double[] axis = axisRotation(upfr[0], upfr[1]);
                        if (axis != null) {
                            System.out.println("\nUp and Front euler angles: ");
                            for (int j = 0; j < axis.length; j++) {
                                System.out.print(axis[j] + " ");
                            }
                            System.out.println("\nUp and Front matrix: ");

                            axisRot = EulerToMatrix(axis);
                            for (int i = 0; i < axisRot.length; i++) {
                                for (int j = 0; j < axisRot[i].length; j++) {
                                    System.out.print(axisRot[i][j] + " ");
                                }
                                System.out.println("");
                            }
                        }
                    }




                    if (objID != null) {
                        double[] currsize = dimsMap_SUNCG.get(objID);
                        System.out.println("Take the SUNCG object with ID: " + objID + " \n and size " + currsize[0]/100.0 + " " + currsize[1]/100.0 + " " + currsize[2]/100.0 + "\n");
                        double[][] rotationsMatrix = QuaternionToMatrix(rotations, objID);
                        for (int i = 0; i < rotationsMatrix.length; i++) {
                            for (int j = 0; j < rotationsMatrix[i].length; j++) {
                                System.out.print(rotationsMatrix[i][j] + " ");
                            }
                            System.out.println("");
                        }


                        double[][] transformationMatrix = getTransformationMatrix(rotationsMatrix, x, z, upfr);

                        JSONObject internalObject = new JSONObject();
                        JSONObject object;

                            if (transformationMatrix != null) {

                                System.out.println("\nRotation matrix");
                                for (int i = 0; i < transformationMatrix.length; i++) {
                                    for (int j = 0; j < transformationMatrix[i].length; j++) {
                                        System.out.print(transformationMatrix[i][j] + " ");
                                    }
                                    System.out.println("");
                                }


                                System.out.println("Create a JSON node ...");
                                object = createJSONArray(internalObject, transformationMatrix, objID, count);
                                count++;
                                objects.put(object); // these are stored in item
                            }
                        }
                    }
                }
                else{
                    //System.out.println("Not a ShapeNet object");
                }

        }

        //create a room
        JSONObject roomObject = new JSONObject();
        roomObject.put("id", "0_0");
        roomObject.put("type", "Room");
        roomObject.put("valid", 1);
        JSONArray nodeInd = new JSONArray();
        //describe that we too a certain room
        for(int i=1; i<count; i++){
            nodeInd.put(String.valueOf(i));
        }
        roomObject.put("nodeIndices", nodeInd);
        roomObject.put("roomTypes", "Living_Room");
        roomObject.put("modelId", "fr_0rm_2");

        JSONArray min = new JSONArray();
        JSONArray max = new JSONArray();

        min.put(mins[0]).put(mins[1]).put(mins[2]);
        max.put(maxs[0]).put(maxs[1]).put(maxs[2]);
        JSONObject bbox = new JSONObject();
        bbox.put("min", min);
        bbox.put("max", max);
        roomObject.put("bbox", bbox);
        JSONObject currObjects = new JSONObject();
        currObjects.accumulate("nodes", roomObject);
        for(Object x: objects){
            currObjects.accumulate("nodes", x);
        }

        //System.out.println(objects.toString());
        // Insert the existing objects in the json Object
        JSONArray nodes = new JSONArray();
        nodes.put(currObjects);
        // Insert the existing objects in the json Object
        json.put("levels", nodes);

        // Write json Object the JSON file
        fileJSON.write(String.valueOf(json));
        fileJSON.flush();
    }

    //Dayana
    public static String ShapeNetIDtoSUNCGID(String shapeNetId, double[] scale) {
        double[] size = dimsMap_ShapeNet.get(shapeNetId);
        double[] dims = {size[0]*scale[0],size[1]*scale[1], size[2]*scale[2]};
        System.out.println("looking up names for suncgID " + shapeNetId + " and with sizes approx. " + dims[0]+" "+ dims[2]);

        List<List<String>> descriptionRanks = idMap_ShapeNet.get(shapeNetId);
        // { {}, {}, {}, {} }
        System.out.println("got description " + descriptionRanks.get(2));
        List<Set> matches = new ArrayList<>();
        for (int i=0; i<3; i++) {
            Set<String> match = new HashSet<>();
            matches.add(match);
        }
        //look for a very good fit
        for (int a=0; a<2; a++) {
            List<String> shapeNetRankWords = descriptionRanks.get(a);
            for (String shapeNetWord: shapeNetRankWords){
                if(shapeNetWord.equals("")) {
                    continue;
                }
                // now loop throught all SUNCG Data
                for (String suncgKey : idMap_SUNCG.keySet()) {
                    List<List<String>> suncgRanks = idMap_SUNCG.get(suncgKey);
                    //only now have we selected a shapenet object
                    for (int b=0; b<2; b++) {
                        //select words of a specific rank
                        List<String> suncgRankWords = suncgRanks.get(b);
                        if(suncgRankWords.contains(shapeNetWord)){
                            if (a==0 && b==0) {
                                matches.get(0).add(suncgKey);
                            } else {
                                matches.get(1).add(suncgKey);
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
                    double[] dim = dimsMap_SUNCG.get(m.toString());
                    if(Math.abs(dims[0]-dim[0])<r&Math.abs(dims[2]-dim[2])<r){
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
                    double[] dim = dimsMap_SUNCG.get(m.toString());
                    if(Math.abs(dims[0]-dim[0])<r&Math.abs(dims[0]-dim[0])<r){
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
            List<String> shapeNetRankWords = descriptionRanks.get(a);
            for (String shapeNetWord: shapeNetRankWords){
                if(shapeNetWord.equals("")) {
                    continue;
                }
                // now loop throught all SUNCG Data
                for (String suncgKey : idMap_SUNCG.keySet()) {
                    List<List<String>> suncgRanks = idMap_SUNCG.get(suncgKey);
                    //only now have we selected a shapenet object
                    for (int b=0; b<4; b++) {
                        //select words of a specific rank
                        List<String> suncgRankWords = suncgRanks.get(b);
                        if(suncgRankWords.contains(shapeNetWord)){
                            matches.get(2).add(suncgKey);
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
                    double[] dim = dimsMap_SUNCG.get(m.toString());
                    if(Math.abs(dims[0]-dim[0])<r&Math.abs(dims[2]-dim[2])<r){
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
        System.out.println("NOT FOUND");
        return null;
    }

    public static double[] axisRotation(double[] up, double front[]) {
        if (Arrays.equals(up, new double[]{0.0, 0.0, 1.0})) {
            if (Arrays.equals(front, new double[]{0.0, 0.0, 1.0})) {
                return new double[]{0.0, 0.0, 0.0};
            }
            if (Arrays.equals(front, new double[]{1.0, 0.0, 0.0})) {
                return new double[]{-90.0, 0.0, -90.0};
            }
            if (Arrays.equals(front, new double[]{-1.0, 0.0, 0.0})) {
                return new double[]{-90.0, 0.0, 90.0};
            }
            if (Arrays.equals(front, new double[]{0.0, 1.0, 0.0})) {
                return new double[]{90.0, 0.0, 0.0};
            }
            if (Arrays.equals(front, new double[]{0.0, -1.0, 0.0})) {
                return new double[]{-90.0, 0.0, 0.0};
            }
            if (Arrays.equals(front, new double[]{0.0, 0.0, -1.0})) {
                return new double[]{0.0, 0.0, 0.0};
            }
        }
        if (Arrays.equals(up, new double[]{0.0, 1.0, 0.0})) {
            if (Arrays.equals(front, new double[]{0.0, 0.0, 1.0})) {
                return new double[]{-180.0, 0.0, 0.0};
            }
            if (Arrays.equals(front, new double[]{0.0, 0.0, -1.0})) {
                return new double[]{0.0, 0.0, 0.0};
            }
            if (Arrays.equals(front, new double[]{0.0, 1.0, 0.0})) {
                return new double[]{-90.0, 0.0, 0.0};
            }
        }

        if (Arrays.equals(up, new double[]{0.0, 0.0, -1.0})) {
            if (Arrays.equals(front, new double[]{0.0, -1.0, 0.0})) {
                return new double[]{-90.0, 0.0, 0.0};
            }
            if (Arrays.equals(front, new double[]{1.0, 0.0,0.0})) {
                return new double[]{90.0, 0.0, 90.0};
            }
            if (Arrays.equals(front, new double[]{0.0, 1.0, 0.0})) {
                return new double[]{90.0, 0.0, 0.0};
            }
        }
        return null;

    }

    private static double[][] QuaternionToMatrix(double[] rotations, String id) {
        if(rotations.length == 4) {
            double x = rotations[0];
            double y = rotations[1];
            double z = rotations[2];
            double w = rotations[3];
            double xx = x * x;
            double xy = x * y;
            double xz = x * z;
            double xw = x * w;

            double yy = y * y;
            double yz = y * z;
            double yw = y * w;

            double zz = z * z;
            double zw = z * w;

            double rotMatrix[][] = new double[3][3];

            rotMatrix[0][0] = 1 - 2 * (yy + zz);
            rotMatrix[0][1] = 2 * (xy - zw);
            rotMatrix[0][2] = 2 * (xz + yw);

            rotMatrix[1][0] = 2 * (xy + zw);
            rotMatrix[1][1] = 1 - 2 * (xx + zz);
            rotMatrix[1][2] = 2 * (yz - xw);

            rotMatrix[2][0] = 2 * (xz - yw);
            rotMatrix[2][1] = 2 * (yz + xw);
            rotMatrix[2][2] = 1 - 2 * (xx + yy);
            return rotMatrix;
        }
        return null;
    }
    private static double[] QuaternionToEuler(double[] rotations) {
        if (rotations.length == 4) {
            /*
            double x = rotations[0];
            double y = rotations[1];
            double z = rotations[2];
            double w = rotations[3];
            double r11 = 2 * (y * z - w * x);
            double r12 = 2 * (x * z + w * y);
            double r21 = w * w - x * x - y * y + z * z;
            double r31 = 2 * (z * y + w * x);
            double r32 = -2*(x * z - w * y);
            double[] curr = {Math.toDegrees(Math.atan2(r31, r32)), Math.toDegrees(Math.asin(r21)), Math.toDegrees(Math.atan2(r11, r12))};
            return curr;
            */
            double x = rotations[0];
            double y = rotations[1];
            double z = rotations[2];
            double w = rotations[3];
            double r11 = 2 * (x * z + w * y);
            double r12 = w * w - x * x - y * y + z * z;
            double r21 = -2 * (y * z - w * x);
            double r31 = 2 * (x * y + w * z);
            double r32 = w * w - x * x + y * y - z * z;
            double[] curr = {Math.toDegrees(Math.atan2(r31, r32)), Math.toDegrees(Math.asin(r21)), Math.toDegrees(Math.atan2(r11, r12))};
            return curr;

        }
        return null;
    }


    //Dayana
    //DeepSynth knows only 16 angles : 0, 22,5, 45, ....
    public static double[] angles=roundangles();
    public static double[] roundangles (){
        double[] angles=new double[16];
        for(int i=0; i<16; i++){
            angles[i]=360.0/16.0*i;
        }
        System.out.println(angles[2]);
        return angles;
    }
    //Round the angle around the y-axis and set others to 0
    public static double[] roundEuler(double[] euler){
        double curr= euler[2]% 360.0;
        double[] res= {0.0, 0.0,0.0};
        for(int i=0; i<16; i++){
            if(curr>=angles[i] & curr<angles[i+1]){
                if (curr-angles[i]> angles[i+1]-curr){
                    res[2]=angles[i+1];
                }
                else{
                    res[2]=angles[i];
                }
                return res;
            }
        }
        return null;

    }

    //Alex
    // Function which extracts the position of an object from the XML and returns it
    private static double[] getPosition(String position, Element root) {
        NodeList nListPos = root.getElementsByTagName("IsoSpatial:Vec3");

        for (int i = 0; i < nListPos.getLength(); i++) {
            Node nNodePos = nListPos.item(i);

            if (nNodePos.getNodeType() == Node.ELEMENT_NODE) {
                Element eElementPos = (Element) nNodePos;
                String posID = eElementPos.getAttribute("xmi:id");
                int intPosID = Integer.parseInt(posID);
                int intPosition = 0;

                if (position != null && !position.isEmpty()) {
                    intPosition = Integer.parseInt(position);
                }

                if (intPosID == intPosition) {
                    String x = eElementPos.getAttribute("x");
                    String y = eElementPos.getAttribute("y");
                    String z = eElementPos.getAttribute("z");
                    /*
                    System.out.println("Position:");
                    System.out.println("X = " + x);
                    System.out.println("Y = " + y);
                    System.out.println("Z = " + z);
                    */


                    double[] positions = {Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z)};

                    return positions;
                }
            }
        }
        return new double[0];
    }
    // Function which extracts the rotation of an object from the XML and returns it
    private static double[] getRotation(String rotation, Element root) {
        NodeList nListRot = root.getElementsByTagName("IsoSpatial:Vec4");

        for (int i = 0; i < nListRot.getLength(); i++) {
            Node nNodeRot = nListRot.item(i);

            if (nNodeRot.getNodeType() == Node.ELEMENT_NODE) {
                Element eElementRot = (Element) nNodeRot;
                String rotID = eElementRot.getAttribute("xmi:id");
                int intRotID = Integer.parseInt(rotID);
                int intRotation = 0;

                if (rotation != null && !rotation.isEmpty()) {
                    intRotation = Integer.parseInt(rotation);
                }

                if (intRotID == intRotation) {
                    String x = eElementRot.getAttribute("x");
                    String y = eElementRot.getAttribute("y");
                    String z = eElementRot.getAttribute("z");
                    String w = eElementRot.getAttribute("w");
                    /*
                    System.out.println("Rotation:");
                    System.out.println("X = " + x);
                    System.out.println("Y = " + y);
                    System.out.println("Z = " + z);
                    System.out.println("W = " + w);
                     */

                    double[] rotations = {Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z), Double.parseDouble(w)};

                    return rotations;
                }
            }
        }
        return new double[0];
    }
    // Function which extracts the scale of an object from the XML and returns it
    private static double[] getScale(String scale, Element root) {
        NodeList nListScale = root.getElementsByTagName("IsoSpatial:Vec3");
        for (int i = 0; i < nListScale.getLength(); i++) {
            Node nNodeScale = nListScale.item(i);

            if (nNodeScale.getNodeType() == Node.ELEMENT_NODE) {
                Element eElementScale = (Element) nNodeScale;
                String scaleID = eElementScale.getAttribute("xmi:id");
                int intScaleID = Integer.parseInt(scaleID);
                int intScale = 0;

                if (scale != null && !scale.isEmpty()) {
                    intScale = Integer.parseInt(scale);
                }

                if (intScaleID == intScale) {
                    String x = eElementScale.getAttribute("x");
                    String y = eElementScale.getAttribute("y");
                    String z = eElementScale.getAttribute("z");
                    /*
                    System.out.println("Scale:");
                    System.out.println("X = " + x);
                    System.out.println("Y = " + y);
                    System.out.println("Z = " + z);
                    */

                    double[] scales = {Double.parseDouble(x), Double.parseDouble(y), Double.parseDouble(z)};
                    return scales;
                }
            }
        }
        return new double[0];
    }

    //Dayana, Alex
    public static double[][] padding(double[][] matrix){
        if(matrix.length==3){
            double[][] result = new double[4][4];
            for(int i=0;i<matrix.length; i++){
                for(int j=0;j<matrix[i].length; j++){
                    result[i][j]=matrix[i][j];
                }
            }
            return result;
        }
        return null;
    }
    private static double[][] getTransformationMatrix(double[][] rotationsMatrix, double x,double z, double[][] Matrix) {
        if (rotationsMatrix.length == 3) {
            double xscale = 1.0/100.0;
            double yscale = 1.0/100.0;
            double zscale = 1.0/100.0;

            rotationsMatrix = padding(rotationsMatrix);

            double[][] t_scale = {{xscale, 0,0,0}, {0,yscale,0,0}, {0,0, zscale, 0}, {0,0,0,1}};

            double[][] t_shift = {{1.0, 0,0,0}, {0,1.0,0,0}, {0,0, 1.0, 0}, {x,0.0,z,1}};

            double[][] dot1;
            double[][] dot2;
            dot1 = multiplyMatrices(rotationsMatrix, t_scale);
            dot2 = multiplyMatrices(dot1, t_shift);
            return dot2;
        } else {
            return null;
        }
    }


    //Dayana
    //matrix and vector operations
    //most are needed only if we compute the rotations for ecery possible combination of up and front of an object
    public static double[][] transpose(double[][] matrix){
        return new double[][] {{matrix[0][0],matrix[1][0],matrix[2][0]}, {matrix[0][1],matrix[1][1],matrix[2][1]}, {matrix[0][2],matrix[1][2],matrix[2][2]}};
    }
    public static double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix) {
        if(firstMatrix==null){
            return secondMatrix;
        }
        else if(secondMatrix==null){
            return firstMatrix;
        }
        double[][] result = new double[firstMatrix.length][secondMatrix[0].length];

        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[row].length; col++) {
                result[row][col] = multiplyMatricesCell(firstMatrix, secondMatrix, row, col);
            }
        }

        return result;
    }
    public static double multiplyMatricesCell(double[][] firstMatrix, double[][] secondMatrix, int row, int col) {
        double cell = 0;
        for (int i = 0; i < secondMatrix.length; i++) {
            cell += firstMatrix[row][i] * secondMatrix[i][col];
        }
        return cell;
    }
    public static double[] multiplyVecMatrix(double[] vec, double[][] Matrix) {
        double[] result = new double[Matrix.length];

        for (int row = 0; row < result.length; row++) {
            double cell = 0;
            for (int i = 0; i < result.length; i++) {
                cell += vec[i] * Matrix[i][row];
            }
            result[row]=cell;
        }
        return result;
    }
    private static double[] sum(double[] a, double[] b) {
        double result[] = new double[a.length];
        Arrays.setAll(result, i -> a[i] + b[i]);
        return result;
    }
    private static double[] sub(double[] a, double[] b) {
        double result[] = new double[a.length];
        Arrays.setAll(result, i -> a[i] - b[i]);
        return result;
    }
    public static double[] cross(double[] vector1, double[] vector2){
        return new double[] {vector1[1]*vector2[2]-vector1[2]*vector2[1], vector1[2]*vector2[0]-vector1[0]*vector2[2], vector1[0]*vector2[1]-vector1[1]*vector2[0]};
    }
    public static double[][] rot_fromto(double[] vector1, double[] vector2, boolean b){
        if(vector1[0]==vector2[0] & vector1[1]==vector2[1] & vector1[2]==vector2[2]){
            System.out.println(1);
            double[][] id={{1.0,0.0,0.0}, {0.0,1.0,0.0}, {0.0,0.0,1.0}};
            return id;
        }
        else if(vector1[0]==0.0 & vector1[1]==0.0 & vector1[2]==-vector2[2] & b & vector1[2]!=0.0) {
            System.out.println(2);
            double[][] id = {{1.0, 0.0, 0.0}, {0.0, -1.0, 0.0}, {0.0, 0.0, -1.0}};
            return id;
        }
        else if(vector1[0]==0.0 & vector1[1]==0.0 & vector1[2]==-vector2[2] & !b& vector1[2]!=0.0) {
            System.out.println(3);
            double[][] id = {{-1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, -1.0}};
            return id;
        }
        else if(vector1[1]==0.0 & vector1[2]==0.0 & vector1[0]==-vector2[0]&b& vector1[0]!=0.0) {
            double[][] id = {{-1.0, 0.0, 0.0}, {0.0, -1.0, 0.0}, {0.0, 0.0, 1.0}};
            return id;
        }

        else if(vector1[1]==0.0 & vector1[2]==0.0 & vector1[0]==-vector2[0]& !b& vector1[0]!=0.0) {
            double[][] id = {{-1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, -1.0}};
            return id;
        }
        else if(vector1[0]==0.0 & vector1[2]==0.0 & vector1[1]==-vector2[1] &b& vector1[1]!=0.0) {
            double[][] id = {{-1.0, 0.0, 0.0}, {0.0, -1.0, 0.0}, {0.0, 0.0, 1.0}};
            return id;
        }
        else if (vector1[0]==0.0 & vector1[2]==0.0 & vector1[1]==-vector2[1] &!b& vector1[1]!=0.0) {
            double[][] id = {{1.0, 0.0, 0.0}, {0.0, -1.0, 0.0}, {0.0, 0.0, -1.0}};
            return id;
        }

        else{
            System.out.println("else");
            double[] cross1=cross(vector1, vector2);
            double[] crossA=cross(cross1, vector1);
            double[] crossB=cross(cross1, vector2);
            double[][] A={vector1, cross1, crossA};
            double[][] B={{vector2[0], cross1[0], crossB[0]}, {vector2[1], cross1[1], crossB[1]}, {vector2[2], cross1[2], crossB[2]}};
            return multiplyMatrices(B, A);
        }
    }
    private static double[] MatrixToEuler(double[][] matrix) {
        double[] res;
        if(matrix[2][1]<1.0)
        {
            if(matrix[2][1]>-1.0)
            {
                res = new double[] {Math.toDegrees(Math.asin(matrix[2][1])), Math.toDegrees(Math.atan2(-matrix[0][1], matrix[1][1])), Math.toDegrees(Math.atan2(-matrix[2][0], matrix[2][2]))};

            }
            else{
                res = new double[] {-180.0/2.0, -Math.toDegrees(Math.atan2(-matrix[0][2], matrix[0][0])), 0.0};

            }
        }
        else {
            res = new double[] {180.0/2.0, Math.toDegrees(Math.atan2(matrix[0][2], matrix[0][0])), 0.0};
        }
        return res;
    }
    public static  double[][] EulerToMatrix(double[] euler){
        double[][] x = new double[3][3];
        double[][] y = new double[3][3];
        double[][] z = new double[3][3];
        x[0][0]=1.0;
        x[0][1]=0.0;
        x[0][2]=0.0;
        x[1][0]=0.0;
        x[1][1]=Math.cos(Math.toRadians(euler[1]));
        x[1][2]=-Math.sin(Math.toRadians(euler[1]));
        x[2][0]=0.0;
        x[2][1]=Math.sin(Math.toRadians(euler[1]));
        x[2][2]=Math.cos(Math.toRadians(euler[1]));

        y[0][0]=Math.cos(Math.toRadians(euler[2]));
        y[0][1]=0.0;
        y[0][2]=Math.sin(Math.toRadians(euler[2]));
        y[1][0]=0.0;
        y[1][1]=1.0;
        y[1][2]=0.0;
        y[2][0]=-Math.sin(Math.toRadians(euler[2]));
        y[2][1]=0.0;
        y[2][2]=Math.cos(Math.toRadians(euler[2]));

        z[0][0]=Math.cos(Math.toRadians(euler[0]));
        z[0][1]=-Math.sin(Math.toRadians(euler[0]));
        z[0][2]=0.0;
        z[1][0]=Math.sin(Math.toRadians(euler[0]));
        z[1][1]=Math.cos(Math.toRadians(euler[0]));
        z[1][2]=0.0;
        z[2][0]=0.0;
        z[2][1]=0.0;
        z[2][2]=1.0;
        return multiplyMatrices(y, multiplyMatrices(x, z));
    }

    //Alex
    // Function which returns an Array with the extracted object from the XML
    private static JSONObject createJSONArray(JSONObject internalObject, double[][] transformationMatrix, String modelID, int count) throws IOException {
        internalObject.put("id", "0_" + count);
        internalObject.put("type", "Object");
        internalObject.put("valid", 1);
        internalObject.put("transform", new double[]{transformationMatrix[0][0], transformationMatrix[1][0], transformationMatrix[2][0], transformationMatrix[3][0], transformationMatrix[0][1], transformationMatrix[1][1], transformationMatrix[2][1], transformationMatrix[3][1], transformationMatrix[0][2], transformationMatrix[1][2], transformationMatrix[2][2], transformationMatrix[3][2], transformationMatrix[0][3], transformationMatrix[1][3], transformationMatrix[2][3], transformationMatrix[3][3]});
        internalObject.put("modelId", modelID);
        return internalObject;
}
    private static JSONObject initializeJson(JSONObject json) {
        json.put("version", "suncg@1.0.0");
        json.put("id", "?"); // Room description should be here
        json.put("up", new int[]{0, 1, 0});
        json.put("front", new int[]{0, 0, 1});
        json.put("scaleToMeters", 1);
        return json;
    }


}