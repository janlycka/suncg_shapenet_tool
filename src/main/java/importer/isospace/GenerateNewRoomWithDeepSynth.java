package importer.isospace;

// the following imports only concern the default file

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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

		try (Interpreter interp = new SharedInterpreter()) {
			interp.exec("from java.lang import System");
			interp.exec("s = 'Hello World'");
			interp.exec("System.out.println(s)");
			interp.exec("print(s)");
			//
			interp.exec("print(s[1:-1])");

		} catch (JepException ex) {
			//throw new ResourceInitializationException(ex);
		}


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

		String myFolder = "final_results_synth_bedroom";
		File inputfolder = getFileFromURL("spaceeval/json/"+myFolder);
		//File inputfolder = getFileFromURL("spaceeval/Training/CP");
		//File inputfolder = getFileFromURL("spaceeval/Training/RFC");
		//File inputfolder = getFileFromURL("spaceeval/spaceeval_trial_data");
//		boolean write = true;
//		String outputfolder = "C:/Users/Jan/text2scene/javastuff/anderes/resources/spaceeval/json/xmlResults/"+myFolder+'/';
//
//		File directory = new File(String.valueOf(outputfolder));
//		if(!directory.exists()){
//			directory.mkdir();
//		}
//
//        CollectionReaderDescription reader = createReaderDescription(
//				RommJsonImporter.class,
//				RommJsonImporter.PARAM_SOURCE_LOCATION, inputfolder,
//				RommJsonImporter.PARAM_PATTERNS,"[+]**/*.json");
//
//        Iterator<JCas> iter = new JCasIterable(reader).iterator();
//        while(iter.hasNext()) {
//        	JCas jcas = iter.next();
//        	jcas.setDocumentLanguage("en");
//
//        	if(write) {
//        		String path = DocumentMetaData.get(jcas).getDocumentId();
//				path = path.substring(path.lastIndexOf('/') + 1);
//				path = path.substring(0,path.lastIndexOf('.'));
//				System.out.println(path);
//            	writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path + ".xml");
//            	//writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path + ".xml");
//            	//writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + DocumentMetaData.get(jcas).getDocumentId() + ".xml");
//        	}
////break;
//        }
//        System.out.println("Import Finished!");
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
		System.out.println("file");
		System.out.println(output);
	    java.io.FileWriter fw = new java.io.FileWriter(output);
	    fw.write(file);
	    fw.close();
	}
}
