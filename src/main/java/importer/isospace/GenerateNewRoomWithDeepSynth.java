package importer.isospace;

// the following imports only concern the default file

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

// the following imports are required to run python
// copied from https://github.com/texttechnologylab/textimager-uima/blob/master/textimager-uima-allennlp/src/main/java/org/hucompute/textimager/uima/allennlp/AllenNLPBase.java

import org.apache.uima.resource.ResourceInitializationException;


import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;

// python interpreter
// src https://www.jython.org/download
// quickstart https://www.edureka.co/community/7489/what-the-correct-way-add-external-jars-intellij-idea-project
//import org.python.util.PythonInterpreter;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

public class GenerateNewRoomWithDeepSynth {

	/**
	 * generates a new UIMA scene by the use of pre-trained Deep-synth models
	 * example usage: GenerateNewRoomWithDeepSynth bedroom
	 * @param args
	 * @throws UIMAException
	 * @throws CASRuntimeException
	 * @throws IOException
	 */
	public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException {

		// print current dir
		String current = new java.io.File( "." ).getCanonicalPath();
		System.out.println("Current dir: "+current);

		/**
		 * parse input arguments
		 * bedroom, office, living
		 * intelliJ - set start args for debugging https://stackoverflow.com/questions/2066307/how-do-you-input-command-line-arguments-in-intellij-idea
		 */
		switch (args[0]) {
			case "bedroom":
			case "living":
			case "office":
				break;
			default:
				System.out.println("arg0 should be one one of [bedroom, office, living]");
				System.out.println("example usage: GenerateNewRoomWithDeepSynth bedroom");
				// quit app if invalid arg0
				return;
		}

		System.out.println("arg0: " + args[0]);

		// args input check has passed, so now we simply run python scripts running deepSynth

		//idea src https://stackoverflow.com/questions/8898765/calling-python-in-java
/*		PythonInterpreter interpreter = new PythonInterpreter();
		//interpreter.exec("jython languages.py");
		interpreter.exec("print(\"test\");");
		interpreter.exec("import os");
		interpreter.exec("print os.getcwd()");
		interpreter.exec("import sys");
		interpreter.exec("print(sys.version)");
		interpreter.exec("languages.py");*/


		//create a tmp directory to contain the new deepSynth model

		String pref = "./deep-synth/";

		String result_deepSynth = "aufgabe_2_tmp_data";
		String path = pref + result_deepSynth;
		//String path = result_deepSynth;
		File dir = new File(path);

		//delete and recreate if exists
		if (Files.exists(dir.toPath())) {
			//xxmj//FileUtils.deleteDirectory(dir);
			// dir.mkdir();

			//xxmj//System.out.println("deleting directory : " + path + " (deep-synth will create it again)");
		}

		//now we have an empty folder for deepSynth to save data into

		//prepare deep-synth command
		// we want to save in result_deepSynth
		// we want to pick a room from deepsynth/args[0]
		// we only want a single room, hence start 0, end 1
		String command = "batch_synth.py --save-dir " + result_deepSynth + " --data-dir " + args[0] + " --model-dir res_1_" + args[0] + " --start 0 --end 1";

		try (Interpreter interp = new SharedInterpreter()) {
			interp.exec("from java.lang import System");
			/*for (int i=0; i<100; i++) {
				interp.exec("s = 'Hello World'");
				interp.exec("System.out.println(s)");
			}*/
			/*interp.exec("System.out.println(s)");
			interp.exec("print(s)");
			//
			interp.exec("print(s[1:-1])");*/
			//interp.exec();
			//interp.runScript("C:\\Users\\Jan\\Documents\\projects\\nemcina_lycka\\languages.py");



			///interp.runScript(command);

		} catch (JepException ex) {
			throw new ResourceInitializationException(ex);
		}

		// now deep synth has finished, read in the newest generated JSON (the one with label 'final')
		boolean found = false;
		String resFilePath = "";
		File resFile = null;
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {
				// Do something with child
				if(child.getName().contains("final")){
					resFilePath = child.getName();
					resFile = child;
					found = true;
				}
			}
		} else {
			// Handle the case where dir is not really a directory.
			// Checking dir.isDirectory() above would not be sufficient
			// to avoid race conditions with another process that deletes
			// directories.
		}


		// maybe use this instead to loop over dir .. ?
//		CollectionReaderDescription reader = createReaderDescription(
//				RommJsonImporter.class,
//				RommJsonImporter.PARAM_SOURCE_LOCATION, inputfolder,
//				RommJsonImporter.PARAM_PATTERNS,"[+]**/*.json");
//
//		Iterator<JCas> iter = new JCasIterable(reader).iterator();
//		while(iter.hasNext()) {
//		//break;
//		}






		//xxmj//String toolInput = "deepsynth_results";
		String toolInput = "deepsynth_results";
		File toolInputDirFile = new File(String.valueOf(pref + toolInput));

		if (!found) {
			System.out.println("FILE NOT FOUND.. deep synth hasn't provided our tool with a 'final' JSON file...");
		} else {
			System.out.println("resFilePath: " + resFilePath);

			if(!toolInputDirFile.exists()){
				toolInputDirFile.mkdir();
				System.out.println("created directory " + pref + toolInputDirFile);
			}

			Path src = resFile.toPath();
			Path dest = Paths.get(pref + toolInput + "/" + src.getFileName());
			Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);

			System.out.println("copying file: ");
			System.out.println(src);
			System.out.println(dest);
		}

		// let us suppose we did manage to get some result out of deep-synth..
		// then we have a valid path to the final JSON file - resFilePath

		// feed it to RoomJsonImporter to convert it to UIMA scene






		//System.out.println("deleting directory : " + path + " (deep-synth will create it again)");

		// now we wait until deepsynth completes


		//Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);


/*		if (envDepsPip == null || envDepsPip.isEmpty()) {
			envDepsPip = "allennlp==1.2.1 textblob==0.15.3 textblob-de==0.4.3";
		}
		if (envDepsConda == null || envDepsConda.isEmpty()) {
			envDepsConda = "";
		}
		if (envPythonVersion == null || envPythonVersion.isEmpty()) {
			envPythonVersion = "3.7";
		}
		if (envName == null || envName.isEmpty()) {
			envName = "textimager_allennlp121_py37_v1";
		}
		if (condaVersion == null || condaVersion.isEmpty()) {
			condaVersion = "py37_4.8.3";
		}

		System.out.println("initializing spacy base class: conda");

		initConda();

		System.out.println("initializing spacy base class: interprter extras...");

		try {
			interpreter.exec("import sys");
			interpreter.exec("sys.argv=['']");
			interpreter.exec("from allennlp.predictors.predictor import Predictor");
			interpreter.exec("import allennlp_models.structured_prediction");
			interpreter.exec("import allennlp_models.tagging");
		} catch (JepException ex) {
			throw new ResourceInitializationException(ex);
		}*/


		/*interpreter.exec("import sys\nsys.path.append('pathToModules if they are not there by default')\nimport yourModule");
		interpreter.exec("verbs = predicted.get('verbs')");
*/

		// this is basically a copy of Schritt12.java, originally known as MainTest3.java, that is, a main fct to demo the RoomImporter

		//output
		String myFolder = "uima_xml_results";
		//File inputfile = getFileFromURL("spaceeval/json/"+myFolder);

		//input
		File inputfolder = toolInputDirFile;//resFile;//getFileFromURL("spaceeval/json/"+myFolder);
		//File inputfolder = resFile;//getFileFromURL("spaceeval/json/"+myFolder);

		boolean write = true;
		String outputfolder = pref+myFolder+'/';

		File directory = new File(String.valueOf(outputfolder));


		if(!directory.exists()){
			directory.mkdir();
			System.out.println("created directory " + outputfolder);
		}


		System.out.println("inputfolder: " + inputfolder);
		System.out.println("outputfolder: " + outputfolder);


		CollectionReaderDescription reader = createReaderDescription(
				RommJsonImporter.class,
				RommJsonImporter.PARAM_SOURCE_LOCATION, inputfolder,
				RommJsonImporter.PARAM_PATTERNS,"[+]**/*.json");

		Iterator<JCas> iter = new JCasIterable(reader).iterator();


		while(iter.hasNext()) {

			JCas jcas = iter.next();
			jcas.setDocumentLanguage("en");

			//resFilePath

			if(write) {
				String path2 = DocumentMetaData.get(jcas).getDocumentId();
				path2 = path2.substring(path2.lastIndexOf('/') + 1);
				path2 = path2.substring(0,path2.lastIndexOf('.'));
				System.out.println(path2);

				//writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path + ".xml");
				writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path2 + ".xml");


				//writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path + ".xml");
				//writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + DocumentMetaData.get(jcas).getDocumentId() + ".xml");
			}
			//break;
		}


        System.out.println("Import Finished!");
	}

	private static File getFileFromURL(String path) {
		URL url = GenerateNewRoomWithDeepSynth.class.getClassLoader().getResource(path);
		File file = null;
		try {
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			file = new File(url.getPath());
		} finally {
			return file;
		}
	}
/*
	private static void writeJson2File(JCas jcas, String output) throws IOException {
		JsonCasSerializer jcs = new JsonCasSerializer();
		jcs.setPrettyPrint(true); // do some configuratio
		
		StringWriter sw = new StringWriter();
		jcs.serialize(jcas.getCas(), sw); // serialize into sw
		
	    java.io.FileWriter fw = new java.io.FileWriter(output);
	    fw.write(sw.toString());
	    fw.close();
	}*/

	private static void writeXml2File(String file, String output) throws IOException {

		System.out.println("SAVING XML FILE: ");
		//System.out.println("file");
		System.out.println(output);
	    java.io.FileWriter fw = new java.io.FileWriter(output);
	    fw.write(file);
	    fw.close();
	}
}
