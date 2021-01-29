package importer.isospace;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;
import java.io.File;
import java.io.FileWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.json.Json;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

public class EvalTest {

	public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException, SAXException, ParserConfigurationException {

//		File inputfolder = getFileFromURL("C:/Users/Alex/Desktop/Text2Scene/resources/spaceeval/XML");
		File file = new File("C:/Users/Jan/text2scene/javastuff/anderes/resources/spaceeval/XML/Example.xml");
//		File[] listOfFiles = inputfolder.listFiles();

		String outputfolder = "C:/Users/Jan/text2scene/javastuff/anderes/resources/spaceeval/XML/XML_To_JSON";

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

		String xml = XmlFormatter.getPrettyString(jcas.getCas());

		Document document = builder.parse(new File("C:/Users/Jan/text2scene/javastuff/anderes/resources/spaceeval/XML/Example.xml"));
		document.getDocumentElement().normalize();
		Element root = document.getDocumentElement();
		System.out.println(root.getNodeName());
		NodeList nList = root.getElementsByTagName("isospace:SpatialEntity");
		JSONObject json = new JSONObject();

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

				positions = getPosition(position, root);
				rotations = getRotation(rotation, root);
				scales = getScale(scale, root);

				JSONObject jsonPOS = new JSONObject();
				if (positions.length == 3) {
					jsonPOS.put("x", Float.parseFloat(positions[0]));
					jsonPOS.put("y", Float.parseFloat(positions[1]));
					jsonPOS.put("z", Float.parseFloat(positions[2]));
				}

				JSONObject jsonROT = new JSONObject();
				if (rotations.length == 4) {
					System.out.println(rotations[0]);
					jsonROT.put("x", Float.parseFloat(rotations[0]));
					jsonROT.put("y", Float.parseFloat(rotations[1]));
					jsonROT.put("z", Float.parseFloat(rotations[2]));
					jsonROT.put("w", Float.parseFloat(rotations[3]));
				}

				JSONObject jsonSCL = new JSONObject();
				if (scales.length == 3) {
					jsonSCL.put("x", Float.parseFloat(scales[0]));
					jsonSCL.put("y", Float.parseFloat(scales[1]));
					jsonSCL.put("z", Float.parseFloat(scales[2]));
				}

				json.put("Position", jsonPOS);
				json.put("Rotation", jsonROT);
				json.put("Scale", jsonSCL);

				System.out.println(json);

//				JSONObject json = new JSONObject();
//				json.put("modelID", ID);
//				json.put("type", "Object");
//				json.put("valid", 1);

//				JSONObject jsonPOS = new JSONObject();
//				jsonPOS.put("x", positions[0]);
//				jsonPOS.put("y", positions[1]);
//				jsonPOS.put("y", positions[2]);
////
//				JSONObject jsonROT = new JSONObject();
//				jsonPOS.put("x", rotations[0]);
//				jsonPOS.put("y", rotations[1]);
//				jsonPOS.put("y", rotations[2]);
//				jsonPOS.put("w", rotations[3]);
//
//				JSONObject jsonSCL = new JSONObject();
//				jsonPOS.put("x", scales[0]);
//				jsonPOS.put("y", scales[1]);
//				jsonPOS.put("y", scales[2]);
//
//				json.put("Position", jsonPOS);
//				json.put("Rotation", jsonROT);
//				json.put("Scale", jsonSCL);
//
//				System.out.println(json);

			}
		}

//		JSONObject json = new JSONObject();
//		json.put("version", "suncg@1.0.0");
//		json.put("id", "aa0f9a9092667bf0aa91f52d8733968f");
//		json.put("up", new int[]{0, 1, 0});
//		json.put("front", new int[]{0, 0, 1});
//		json.put("scaleToMeters", 1);
//		JSONObject item = new JSONObject();
//		item.put("id", "0_0");
//		JSONObject item2 = new JSONObject();
//		item2.put("id", "0_0");
//		item2.put("type", "Room");
//		item2.put("valid", 1);
//		item2.put("modelId", "fr_0rm_0");
//		item2.put("nodeIndices", new char[]{1, 2, 3});
//		item.put("nodes", item2);
//		json.put("levels", item);
//
//		FileWriter fileJSON = new FileWriter("C:/Users/Alex/Desktop/Example.json");
//		fileJSON.write(String.valueOf(json));
//		fileJSON.flush();
//		fileJSON.close();
//
//		System.out.println(json);


	}

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


}

//		private static File getFileFromURL (String path){
//			URL url = EvalTest.class.getClassLoader().getResource(path);
//			File file = null;
//			try {
//				file = new File(url.toURI());
//			} catch (URISyntaxException e) {
//				file = new File(url.getPath());
//			} finally {
//				return file;
//			}
//		}


