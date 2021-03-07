package importer.isospace;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.collection.CollectionReaderDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
//import org.apache.uima.json.JsonCasSerializer;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

public class Schritt12 {
	
	public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException {

		String myFolder = "final_results_synth_bedroom";
		File inputfolder = getFileFromURL("spaceeval/json/"+myFolder);
		//File inputfolder = getFileFromURL("spaceeval/Training/CP");
		//File inputfolder = getFileFromURL("spaceeval/Training/RFC");
		//File inputfolder = getFileFromURL("spaceeval/spaceeval_trial_data");
		boolean write = true;
		String outputfolder = "C:/Users/Jan/text2scene/javastuff/anderes/resources/spaceeval/json/xmlResults/"+myFolder+'/';

		File directory = new File(String.valueOf(outputfolder));
		if(!directory.exists()){
			directory.mkdir();
		}

        CollectionReaderDescription reader = createReaderDescription(
				RommJsonImporter.class,
				RommJsonImporter.PARAM_SOURCE_LOCATION, inputfolder,
				RommJsonImporter.PARAM_PATTERNS,"[+]**/*.json");

        Iterator<JCas> iter = new JCasIterable(reader).iterator();
        while(iter.hasNext()) {
        	JCas jcas = iter.next();
        	jcas.setDocumentLanguage("en");

        	if(write) {
        		String path = DocumentMetaData.get(jcas).getDocumentId();
				path = path.substring(path.lastIndexOf('/') + 1);
				path = path.substring(0,path.lastIndexOf('.'));
				System.out.println(path);
            	writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path + ".xml");
            	//writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + path + ".xml");
            	//writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + DocumentMetaData.get(jcas).getDocumentId() + ".xml");
        	}
//break;
        }
        System.out.println("Import Finished!");
	}

	private static File getFileFromURL(String path) {
		URL url = Schritt12.class.getClassLoader().getResource(path);
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
