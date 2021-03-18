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

// python interpreter
import jep.Interpreter;
import jep.JepException;
import jep.SharedInterpreter;

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
		 * Useful for debugging
		 * with this flag, forbid any directory deletion
		 * example usage: GenerateNewRoomWithDeepSynth bedroom nodel
		 */
		boolean allowFolderDeletion = true;

		if(args.length > 1){
			if (args[1] == "nodel") {
				allowFolderDeletion = false;
			}
		}

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

		System.out.println("room type (arg0): " + args[0]);

		// args input check has passed, so now we simply run python scripts running deepSynth

		//deep-synth creates a tmp directory to contain the new deepSynth model

		// parent directory (useful for debugging)
		// String pref = "./deep-synth/";
		String pref = "./";

		// we pass this value to deepsynth, to store data in it
		String result_deepSynth = "aufgabe_2_tmp_data";

		// if we use pref, add it now
		String path = pref + result_deepSynth;

		//delete the deepSynth output directory and all its contents, if it exists
		File dir = new File(path);
		if (Files.exists(dir.toPath()) && allowFolderDeletion) {
			FileUtils.deleteDirectory(dir);

			//dir.mkdir();
			//now we have an empty folder for deepSynth to save data into

			System.out.println("deleting directory : " + path + " (deep-synth will create it again)");
		}

		// now deepsynth has somewhere to put the generated json

		//prepare deep-synth command
		// we want to save in result_deepSynth
		// we want to pick a room from deepsynth/args[0]
		// we only want a single room, hence start 0, end 1
		String command = "batch_synth.py --save-dir " + result_deepSynth + " --data-dir " + args[0] + " --model-dir res_1_" + args[0] + " --start 0 --end 1";

		System.out.println("we will try to execute this in python: " + command);
		// jep executes the python script and waits until it's complete, which means we just wait till it's done

		try (Interpreter interp = new SharedInterpreter()) {
			/*
			//test data to see if the plugin even works
			interp.exec("from java.lang import System");
			interp.exec("s = 'Hello World'");
			for (int i=0; i<100; i++) {
				interp.exec("System.out.println(s)");
			}*/

			//interp.runScript("C:\\Users\\Jan\\Documents\\projects\\nemcina_lycka\\languages.py");
			interp.runScript(command);

		} catch (JepException ex) {
			throw new ResourceInitializationException(ex);
		}

		// now deep synth has finished, read in the newest generated JSON (the one with a filename containing the word 'final')
		boolean found = false;
		String resFilePath = "";
		File resFile = null;
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File child : directoryListing) {

				// If we find a JSON file containing the word "final", then we've found the latest output
				if(child.getName().contains("final")){
					resFilePath = child.getName();
					resFile = child;
					found = true;
					break;
				}
			}
		} else {
			// Handle the case where dir is not really a directory.
			// Checking dir.isDirectory() above would not be sufficient
			// to avoid race conditions with another process that deletes
			// directories.
		}

		// we will copy this one file into yet another directory "deepsynth_results", because the RoomImporter class
		// expects an entire directory which it's a better idea to just keep, insofar as code readability is to be upheld
		String toolInput = "deepsynth_results";
		File toolInputDirFile = new File(String.valueOf(pref + toolInput));

		if (!found) {
			System.out.println("FILE NOT FOUND.. deep synth hasn't provided our tool with a 'final' JSON file...");
		} else {
			System.out.println("resFilePath: " + resFilePath);

			//delete all contents of deepsynth_results, we need this dir to be empty
			if(toolInputDirFile.exists() && allowFolderDeletion){
				FileUtils.deleteDirectory(toolInputDirFile);
				System.out.println("deleted directory " + pref + toolInputDirFile);
			}
			if(!toolInputDirFile.exists()){
				toolInputDirFile.mkdir();
				System.out.println("created directory " + pref + toolInputDirFile);
			}

			// if result found, copy it to a new dir
			Path src = resFile.toPath();
			Path dest = Paths.get(pref + toolInput + "/" + src.getFileName());
			Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);

			// For completeness sake
			System.out.println("copying file: ");
			System.out.println(src);
			System.out.println(dest);
		}

		// let us suppose we did manage to get some result out of deep-synth..
		// then we have a valid path to the final JSON file - resFilePath

		// feed it to RoomJsonImporter to convert it to UIMA scene

		// this is basically a copy of Schritt12.java, originally known as MainTest3.java, that is, a main fct to demo the RoomImporter

		// RoomImporter output directory
		String myOutputFolder = "uima_xml_results";

		// add pref if pref used
		String outputfolder = pref+myOutputFolder+'/';

		// RoomImporter input dir
		File inputfolder = toolInputDirFile;

		//resFile;//getFileFromURL("spaceeval/json/"+myFolder);
		//File inputfolder = resFile;//getFileFromURL("spaceeval/json/"+myFolder);

		// write XML if successfull
		boolean write = true;

		File directory = new File(String.valueOf(outputfolder));

		// re-create UIMA output directory, we need this to be empty
		if(toolInputDirFile.exists() && allowFolderDeletion){
			FileUtils.deleteDirectory(directory);
			System.out.println("deleted directory " + outputfolder);
		}
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

			//UIMA XML Result file path
			//resFilePath

			if(write) {
				String path2 = DocumentMetaData.get(jcas).getDocumentId();
				path2 = path2.substring(path2.lastIndexOf('/') + 1);
				path2 = path2.substring(0,path2.lastIndexOf('.'));
				System.out.println(path2);

				writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path2 + ".xml");
			}
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

	private static void writeXml2File(String file, String output) throws IOException {

		System.out.println("SAVING XML FILE: ");
		//System.out.println("file");
		System.out.println(output);
	    java.io.FileWriter fw = new java.io.FileWriter(output);
	    fw.write(file);
	    fw.close();
	}
}
