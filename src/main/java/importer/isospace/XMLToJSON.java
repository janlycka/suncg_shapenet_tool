
package importer.isospace;
import java.io.*;

import java.util.*;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
//import org.ejml.ops.ReadCsv;
//import org.graalvm.compiler.nodes.calc.ObjectEqualsNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Scanner;

public class XMLToJSON {

	public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException, SAXException, ParserConfigurationException {
		//_____________________________
		String outputfolder = "/resources/spaceeval/xml/XML_To_JSON";
		String inputfolder = "/resources/spaceeval/xml/Example.xml";
		File file = new File(System.getProperty("user.dir") + inputfolder);
		//___________________________

		File directory = new File(String.valueOf(outputfolder));
		if (!directory.exists()) {
			directory.mkdir();
		}

		//AggregateBuilder builder = new AggregateBuilder();
		//builder.add(createEngineDescription(BreakIteratorSegmenter.class));

//		for (File file : listOfFiles) {
//			if (file.isFile()) {
		System.out.println(file.getName());

		InputStream inputStream = new FileInputStream(file);
		JCas jcas = JCasFactory.createJCas();
		CasIOUtils.load(inputStream, jcas.getCas());

		//SimplePipeline.runPipeline(jcas,builder.createAggregate());

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

//		String xml = XmlFormatter.getPrettyString(jcas.getCas());

		Document document = builder.parse(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/Example.xml"));
		document.getDocumentElement().normalize();
		Element root = document.getDocumentElement();
		System.out.println(root.getNodeName());
		NodeList nList = root.getElementsByTagName("isospace:SpatialEntity");

		//Initialize JSON to write to
		FileWriter fileJSON = new FileWriter("C:/Users/Alex/Desktop/Example.json");

		//Initialize JSON Objects and initial values of the JSON file
		JSONObject json = new JSONObject();
		JSONObject item = new JSONObject();
		// Count of JSONObject ID
		int count = 1;
		json.put("version", "suncg@1.0.0");
		json.put("id", "?"); // Room description should be here
		json.put("up", new int[]{0, 1, 0});
		json.put("front", new int[]{0, 0, 1});
		json.put("scaleToMeters", 1);

		//Room description
//		item2.put("id", "0_0");
//		item2.put("type", "Room");
//		item2.put("valid", 1);
//		item2.put("modelId", "fr_0rm_0");
//		item2.put("nodeIndices", new String[]{"1", "2", "3"});
//		item2.put("roomTypes", new String[]{"Bedroom"});

		//Dayana
		Map map = IdShapeNet();
		Map map1 = IdSUNCG();


		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			System.out.println("\nCurrent Element :" + nNode.getNodeName());

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				System.out.println("Object ID = "
						+ eElement.getAttribute("object_id"));
				String ID = eElement.getAttribute("object_id");
				System.out.println("Position = "
						+ eElement.getAttribute("position"));
				String position = eElement.getAttribute("position");
				System.out.println("Rotation = "
						+ eElement.getAttribute("rotation"));
				String rotation = eElement.getAttribute("rotation");
				System.out.println("Scale = "
						+ eElement.getAttribute("scale"));
				String scale = eElement.getAttribute("scale");

				String[] positions = new String[3];
				String[] rotations = new String[3];
				String[] scales = new String[3];
				String ids;

				positions = getPosition(position, root);
				rotations = getRotation(rotation, root);
				scales = getScale(scale, root);

				//get similar Objects from SUNCG dataset
				ids = getSUNCGIds(ID, root, map, map1);

				double[][] transformationMatrix = getTransformationMatrix(positions, rotations, scales);

				if (transformationMatrix != null) {
					for (int i = 0; i < 4; i++) {
						for (int j = 0; j < 4; j++) {
							System.out.println(transformationMatrix[i][j]);
						}
					}
				}

				JSONObject item2 = new JSONObject();
				JSONArray object;

				// Accumulate all nodes -> all Objects in which exist in the XML && which IDs are to be found
				// in the SUNCG dataset
				if (transformationMatrix != null && ids != null) {
					object = createJSONArray(item2, transformationMatrix, ids, count);
					count++;
					item.accumulate("nodes", object); // these are stored in item
				}

			}
		}

		// Insert the existing objects in the json Object
		json.put("levels", item);

		// Write json Object the JSON file
		fileJSON.write(String.valueOf(json));
		fileJSON.flush();
	}

	// Function which returns an Array with the extracted object from the XML
	private static JSONArray createJSONArray(JSONObject item2, double[][] transformationMatrix, String modelID, int count) throws IOException {
		item2.put("id", "0_" + count);
		item2.put("type", "object");
		item2.put("valid", 1);
		item2.put("transform", new double[]{transformationMatrix[0][0], transformationMatrix[0][1], transformationMatrix[0][2], transformationMatrix[0][3], transformationMatrix[1][0], transformationMatrix[1][1], transformationMatrix[1][2], transformationMatrix[1][3], transformationMatrix[2][0], transformationMatrix[2][1], transformationMatrix[2][2], transformationMatrix[2][3], transformationMatrix[3][0], transformationMatrix[3][1], transformationMatrix[3][2], transformationMatrix[3][3]});
		item2.put("modelID", modelID);
		JSONArray item3 = new JSONArray();
		item3.put(item2);

		return item3;

	}

	//Dayana
	private static Map IdShapeNet() throws FileNotFoundException {
		// Creating a dictionary: key=Id, value=wnlemmas
		Map<String, String> map = new HashMap<String, String>();

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
			if (elem[3].replace("\"", "").length() == 0) {
				map.put(elem[0], elem[1].replace("\"", "").replace("[", "").replace("]", "").toLowerCase());
			} else {
				map.put(elem[0], elem[3].replace("\"", "").replace("[", "").replace("]", "").toLowerCase());
			}

			//System.out.println(elem[0] + "\n " + map.get(elem[0]));
		}
		//Do not forget to close the scanner
		scanner.close();

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
		}
		return map;
	}

	//Dayana
	private static Map IdSUNCG() throws FileNotFoundException {
		// Creating a dictionary: key=Id, value=wnlemmas
		Map<String, String> map = new HashMap<String, String>();
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
			map.put(elem[1], elem[2].replaceAll("_", " "));
		}
		//Do not forget to close the scanner
		scanner.close();
		return map;
	}

	//Dayana
	private static String getSUNCGIds(String idShapeNet, Element root, Map map, Map map1) throws FileNotFoundException {
		if (idShapeNet.matches("([A-Za-z]+[0-9]|[0-9]+[A-Za-z])[A-Za-z0-9]*") & idShapeNet.length() > 10) {
			Object wnlemma = map.get(idShapeNet);
			List<String> where = new ArrayList<String>();
			if (wnlemma != null) {
				System.out.println("wnlemmas: " + wnlemma);
				String[] elem = wnlemma.toString().replace("[", "").replace("]", "").split(",|\"");
				for (String v : elem) {
					for (Object key : map1.keySet()) {
						for (String u : map1.get(key).toString().split(",")) {
							if (v.equals(u)) {
								where.add(key.toString());
							}
						}
					}
				}
				System.out.println(where.toString());
				if (where.size() != 0) {
					Random rand = new Random();
					int randomIndex = rand.nextInt(where.size());

					Object curId = where.get(randomIndex);
					if (curId != null) {
						System.out.println("new Id: " + curId);
						return curId.toString();
					} else {
						System.out.println("Object not found in SUNCG");
						return null;
					}
				} else {
					System.out.println("Object not found in SUNCG");
					return null;
				}
			} else {
				System.out.println("No wnlemmas were provided");
				return null;
			}
		} else {
			System.out.println("Not a ShapeNet object");
			return null;
		}
	}

	//DAYANA
	private static String getShapeNetIds(String idSUNCG, Element root, Map map, Map map1) throws FileNotFoundException {
		Object wnlemma = map1.get(idSUNCG);
		List<String> where = new ArrayList<String>();
		if (wnlemma != null) {
			System.out.println("wnlemmas: " + wnlemma);
			String[] elem = wnlemma.toString().replace("[", "").replace("]", "").split(",|\"");
			for (String v : elem) {
				for (Object key : map.keySet()) {
					for (String u : map.get(key).toString().split(",")) {
						if (v.equals(u)) {
							where.add(key.toString());
						}
					}
				}
			}
			System.out.println(where.toString());
			if (where.size() != 0) {
				Random rand = new Random();
				int randomIndex = rand.nextInt(where.size());
				Object curId = where.get(randomIndex);
				if (curId != null) {
					System.out.println("new Id: " + curId);
					return curId.toString();
				} else {
					System.out.println("Object not found in ShapeNet");
					return null;
				}
			} else {
				System.out.println("Object not found in ShapeNet");
				return null;
			}
		} else {
			System.out.println("No wnlemmas were provided");
			return null;
		}
	}

	// Function which extracts the position of an object from the XML and returns it
	private static String[] getPosition(String position, Element root) {
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
					System.out.println("Position:");
					System.out.println("X = " + x);
					System.out.println("Y = " + y);
					System.out.println("Z = " + z);
					String[] positions = {x, y, z};
					return positions;
				}
			}
		}
		return new String[0];
	}

	// Function which extracts the rotation of an object from the XML and returns it
	private static String[] getRotation(String rotation, Element root) {
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
					System.out.println("Rotation:");
					System.out.println("X = " + x);
					System.out.println("Y = " + y);
					System.out.println("Z = " + z);
					System.out.println("W = " + w);
					String[] rotations = {x, y, z, w};
					return rotations;
				}
			}
		}
		return new String[0];
	}

	// Function which extracts the scale of an object from the XML and returns it
	private static String[] getScale(String scale, Element root) {
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
					System.out.println("Scale:");
					System.out.println("X = " + x);
					System.out.println("Y = " + y);
					System.out.println("Z = " + z);
					String[] scales = {x, y, z};
					return scales;
				}
			}
		}
		return new String[0];
	}

	// Function which creates the Transformation Matrix for an object extracted
	// from the XML regarding it's position, rotation
	private static double[][] getTransformationMatrix(String[] positions, String[] rotations, String[] scales) {
		if (positions.length == 3 && rotations.length == 4 && scales.length == 3) {
			double x = Double.parseDouble(positions[0]);
			double y = Double.parseDouble(positions[1]);
			double r = Double.parseDouble(rotations[2]);
			double xscale = Double.parseDouble(scales[0]);
			double yscale = Double.parseDouble(scales[1]);
			double zscale = Double.parseDouble(scales[2]);
			double zpad = 0.5;

			double sin = Math.sin(r);
			double cos = Math.cos(r);
			System.out.println(sin);
			System.out.println(cos);

			double[][] t = new double[4][4];
			t[0][0] = cos;
			t[0][1] = 0;
			t[0][2] = -sin;
			t[0][3] = 0;
			t[1][0] = 0;
			t[1][1] = 1;
			t[1][2] = 0;
			t[1][3] = 0;
			t[2][0] = sin;
			t[2][1] = 0;
			t[2][2] = cos;
			t[2][3] = 0;
			t[3][0] = 0;
			t[3][1] = zpad;
			t[3][2] = 0;
			t[3][3] = 1;

			double[][] t_scale = new double[4][4];
			t_scale[0][0] = xscale;
			t_scale[0][1] = 0;
			t_scale[0][2] = 0;
			t_scale[0][3] = 0;
			t_scale[1][0] = 0;
			t_scale[1][1] = zscale;
			t_scale[1][2] = 0;
			t_scale[1][3] = 0;
			t_scale[2][0] = 0;
			t_scale[2][1] = 0;
			t_scale[2][2] = xscale;
			t_scale[2][3] = 0;
			t_scale[3][0] = 0;
			t_scale[3][1] = 0;
			t_scale[3][2] = 0;
			t_scale[3][3] = 1;

			double[][] t_shift = new double[4][4];
			t_shift[0][0] = 1;
			t_shift[0][1] = 0;
			t_shift[0][2] = 0;
			t_shift[0][3] = 0;
			t_shift[1][0] = 0;
			t_shift[1][1] = 1;
			t_shift[1][2] = 0;
			t_shift[1][3] = 0;
			t_shift[2][0] = 0;
			t_shift[2][1] = 0;
			t_shift[2][2] = 1;
			t_shift[2][3] = 0;
			t_shift[3][0] = x;
			t_shift[3][1] = 0;
			t_shift[3][2] = y;
			t_shift[3][3] = 1;

			double[][] dot1 = new double[4][4];

			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					dot1[i][j] = t[i][j] * t_scale[j][i];
				}
			}

			double[][] dot2 = new double[4][4];

			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					dot2[i][j] = t[i][j] * dot1[j][i];
				}
			}
			return dot2;
		} else {
			return null;
		}
	}
}
