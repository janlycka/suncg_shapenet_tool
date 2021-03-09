
package importer.isospace;
import java.io.*;

import java.util.*;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
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
		String inputfolder = "/resources/spaceeval/xml/Example.xml";
		String outputfolder = System.getProperty("user.dir")+"/resources/spaceeval/xml/XML_To_JSON";
//		File[] listOfFiles = inputfolder.listFiles();


		File directory = new File(String.valueOf(outputfolder));
		if (!directory.exists()) {
			directory.mkdir();
		}
		// 1.Initialisierung des JSON files und JSONObject

		//Initialize JSON to write to
//		FileWriter fileJSON = new FileWriter( System.getProperty("user.dir")+"/resources/spaceeval/xml/Example.json");
		// 1.Initialisierung des JSON files und JSONObject

		//Initialize JSON to write to
		FileWriter fileJSON = new FileWriter( System.getProperty("user.dir")+"/resources/spaceeval/xml/XML_To_JSON/Example.json");

		//Initialize JSON Objects and initial values of the JSON file
		JSONObject json = new JSONObject();
		JSONObject objects = new JSONObject();
		json = initializeJson(json);
		int count = 1; // Count of JSONObject ID


		// 2.Parsen des XML Documents.
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(new File(System.getProperty("user.dir") + "/resources/spaceeval/XML/Example.xml"));
		document.getDocumentElement().normalize();
		Element root = document.getDocumentElement();
		System.out.println(root.getNodeName());
		NodeList nList = root.getElementsByTagName("isospace:SpatialEntity");

		// Extract IDs for object, position, rotations und scale
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

				// Extract x, y, z und w values for position, rotations and scale values from XML
				String[] positions = getPosition(position, root);
				String[] rotations = getRotation(rotation, root);

				double[] rotationsEuler = QuaternionToEuler(rotations); // quaternion -> Euler (roll, pitch, yaw) -> return as ZYX Euler Angle
				double[][] rotationMatrix = QuaternionToMatrix(rotations); // quaternion -> Rotation Matrix
				String[] scales = getScale(scale, root);

				//Dayana
				Map map = IdShapeNet();
				Map map1 = IdSUNCG();

				//get similar Objects from SUNCG dataset
				String ids;
				ids = getSUNCGIds(ID, root, map, map1);

				double[][] transformationMatrix = getTransformationMatrix(positions, rotationsEuler, scales); // Deep-Synth Variante
				double[][] transformationMatrix2 = getTransformationMatrix2(positions, rotationMatrix, scales); // https://math.stackexchange.com/questions/718897/how-can-i-combine-affine-transformations-into-one-matrix Variante

				// Accumulate all nodes -> all Objects in which exist in the XML && which IDs are to be found in the SUNCG dataset
				// Create the Nodes for objects
				JSONObject internalObject = new JSONObject();
				JSONArray object;
				if (transformationMatrix != null && ids != null) {
					object = createJSONArray(internalObject, transformationMatrix2, ids, count);
					count++;
					objects.accumulate("nodes", object); // these are stored in item
				}

			}
		}

		// Insert the existing objects in the json Object
		json.put("levels", objects);

		// Write json Object the JSON file
		fileJSON.write(String.valueOf(json));
		fileJSON.flush();
	}

	private static double[][] getTransformationMatrix2(String[] positions, double[][] rotationMatrix, String[] scales) {
		if(positions.length == 3 && scales.length == 3) {
			double[][] positionMatrix = new double[4][4];
			positionMatrix[0][0] = 1;
			positionMatrix[0][1] = 0;
			positionMatrix[0][2] = 0;
			positionMatrix[0][3] = Double.parseDouble(positions[0]);
			positionMatrix[1][0] = 0;
			positionMatrix[1][1] = 1;
			positionMatrix[1][2] = 0;
			positionMatrix[1][3] = Double.parseDouble(positions[1]);
			positionMatrix[2][0] = 0;
			positionMatrix[2][1] = 0;
			positionMatrix[2][2] = 0;
			positionMatrix[2][3] = Double.parseDouble(positions[2]);
			positionMatrix[3][0] = 0;
			positionMatrix[3][1] = 0;
			positionMatrix[3][2] = 0;
			positionMatrix[3][3] = 1;

			double[][] scaleMatrix = new double[4][4];
			scaleMatrix[0][0] = Double.parseDouble(scales[0]);
			scaleMatrix[0][1] = 0;
			scaleMatrix[0][2] = 0;
			scaleMatrix[0][3] = 0;
			scaleMatrix[1][0] = 0;
			scaleMatrix[1][1] = Double.parseDouble(scales[1]);;
			scaleMatrix[1][2] = 0;
			scaleMatrix[1][3] = 0;
			scaleMatrix[2][0] = 0;
			scaleMatrix[2][1] = 0;
			scaleMatrix[2][2] = Double.parseDouble(scales[2]);
			scaleMatrix[2][3] = 0;
			scaleMatrix[3][0] = 0;
			scaleMatrix[3][1] = 0;
			scaleMatrix[3][2] = 0;
			scaleMatrix[3][3] = 1;

			double[][] product1 = new double[4][4];
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					for (int k = 0; k < 4; k++) {
						product1[i][j] += positionMatrix[i][k] * scaleMatrix[k][j];
					}
				}
			}

			double[][] transformationMatrix = new double[4][4];
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					for (int k = 0; k < 4; k++) {
						transformationMatrix[i][j] += rotationMatrix[i][k] * product1[k][j];
					}
				}
			}
			return transformationMatrix;
		} else {
			return null;
		}
	}

	private static double[][] QuaternionToMatrix(String[] rotations) {
		if(rotations.length == 4) {
			double x = Double.parseDouble(rotations[0]);
			double y = Double.parseDouble(rotations[1]);
			double z = Double.parseDouble(rotations[2]);
			double w = Double.parseDouble(rotations[3]);
			double xx = x * x;
			double xy = x * y;
			double xz = x * z;
			double xw = x * w;

			double yy = y * y;
			double yz = y * z;
			double yw = y * w;

			double zz = z * z;
			double zw = z * w;

			double rotMatrix[][] = new double[4][4];
			rotMatrix[0][0] = 1 - 2 * (yy + zz);
			rotMatrix[0][1] = 2 * (xy - zw);
			rotMatrix[0][2] = 2 * (xz + yw);

			rotMatrix[1][0] = 2 * (xy + zw);
			rotMatrix[1][1] = 1 - 2 * (xx + zz);
			rotMatrix[1][2] = 2 * (yz - xw);

			rotMatrix[2][0] = 2 * (xz - yw);
			rotMatrix[2][1] = 2 * (yz + xw);
			rotMatrix[2][2] = 1 - 2 * (xx + yy);

			rotMatrix[0][3] = 0;
			rotMatrix[1][3] = 0;
			rotMatrix[2][3] = 0;
			rotMatrix[3][0] = 0;
			rotMatrix[3][1] = 0;
			rotMatrix[3][2] = 0;
			rotMatrix[3][3] = 1;

			return rotMatrix;
		}
		return null;
	}

	private static double[] QuaternionToEuler(String[] rotations) {
		if (rotations.length == 4) {
			double x = Double.parseDouble(rotations[0]);
			double y = Double.parseDouble(rotations[1]);
			double z = Double.parseDouble(rotations[2]);
			double w = Double.parseDouble(rotations[3]);

			double t0 = 2.0 * (w * x + y * z);
			double t1 = 1.0 - 2.0 * (x * x + y * y);
			double roll_X = Math.atan2(t0, t1);

			double t2 = 2.0 * (w * y - z * x);

			if (t2 > 1.0) {
				t2 = 1;
			}

			if (t2 < -1.0) {
				t2 = -1.0;
			}

			double pitch_Y = Math.asin(t2);

			double t3 = 2.0 * (w * z + x * y);
			double t4 = 1.0 - 2.0 * (y * y + z * z);
			double yaw_Z = Math.atan2(t3, t4);

			double[] euler = new double[3];
			euler[0] = Math.toDegrees(roll_X);
			;
			euler[1] = Math.toDegrees(pitch_Y);
			euler[2] = Math.toDegrees(yaw_Z);

			return euler; // Euler Angles ZYX Order
		}
		return null;
	}

	private static JSONObject initializeJson(JSONObject json) {
		json.put("version", "suncg@1.0.0");
		json.put("id", "?"); // Room description should be here
		json.put("up", new int[]{0, 1, 0});
		json.put("front", new int[]{0, 0, 1});
		json.put("scaleToMeters", 1);
		return json;
	}

	// Methoden

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

	// Function which creates the Transformation Matrix for an object extracted
	// from the XML regarding it's position, rotation
	private static double[][] getTransformationMatrix(String[] positions, double[] rotationsEuler, String[] scales) {
		if (positions.length == 3 && rotationsEuler.length == 3 && scales.length == 3) {
			double x = Double.parseDouble(positions[0]);
			double y = Double.parseDouble(positions[1]);
			double r = rotationsEuler[1];
			double xscale = 1;
			double yscale = 1;
			double zscale = 1;
			double zpad = 0.5;

			double sin = Math.sin(r);
			double cos = Math.cos(r);

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

			for (int i = 0; i < t[0].length; i++) {
				for (int j = 0; j < t_scale.length; j++) {
					dot1[i][j] = t[i][j] * t_scale[j][i];
				}
			}

			double[][] dot2 = new double[4][4];

			for (int i = 0; i < dot1[0].length; i++) {
				for (int j = 0; j < t_shift.length; j++) {
					dot2[i][j] = dot1[i][j] * t_shift[j][i];
				}
			}
			return dot2;
		} else {
			return null;
		}
	}

	// Function which returns an Array with the extracted object from the XML
	private static JSONArray createJSONArray(JSONObject internalObject, double[][] transformationMatrix, String modelID, int count) throws IOException {
		internalObject.put("id", "0_" + count);
		internalObject.put("type", "object");
		internalObject.put("valid", 1);
		internalObject.put("transform", new double[]{transformationMatrix[0][0], transformationMatrix[0][1], transformationMatrix[0][2], transformationMatrix[0][3], transformationMatrix[1][0], transformationMatrix[1][1], transformationMatrix[1][2], transformationMatrix[1][3], transformationMatrix[2][0], transformationMatrix[2][1], transformationMatrix[2][2], transformationMatrix[2][3], transformationMatrix[3][0], transformationMatrix[3][1], transformationMatrix[3][2], transformationMatrix[3][3]});
		internalObject.put("modelID", modelID);
		JSONArray objectArray = new JSONArray();
		objectArray.put(internalObject);

		return objectArray;

	}
}
