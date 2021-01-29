package importer.isospace;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.collection.CollectionReaderDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
//import org.apache.uima.json.JsonCasSerializer;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class MainTest {
	
	public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException {
		
		File inputfolder = getFileFromURL("spaceeval/Training/ANC");
		//File inputfolder = getFileFromURL("spaceeval/Training/CP");
		//File inputfolder = getFileFromURL("spaceeval/Training/RFC");
		//File inputfolder = getFileFromURL("spaceeval/spaceeval_trial_data");
		boolean write = true;
		String outputfolder = "C:/Users/Henlein/Downloads/SpaceEvalUIMATest/Training/ANC/";
		
		File directory = new File(String.valueOf(outputfolder));
		if(!directory.exists()){
			directory.mkdir();
		}
		
        CollectionReaderDescription reader = createReaderDescription(
                IsoSpaceImporter.class, 
                IsoSpaceImporter.PARAM_SOURCE_LOCATION, inputfolder,
                IsoSpaceImporter.PARAM_PATTERNS,"[+]**/*.xml");
        
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createEngineDescription(BreakIteratorSegmenter.class));
        builder.add(createEngineDescription(QuickTreeNodeTagger.class));
		builder.add(createEngineDescription(MoveTokenAndNodesToView.class));

        Iterator<JCas> iter = new JCasIterable(reader).iterator();
        while(iter.hasNext()) {
        	JCas jcas = iter.next();
        	jcas.setDocumentLanguage("en");
        	SimplePipeline.runPipeline(jcas,builder.createAggregate());

        	if(write) {
            	writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), outputfolder + DocumentMetaData.get(jcas).getDocumentId() + ".xml");
        	}

        }
        System.out.println("Import Finished!");
	}

	private static File getFileFromURL(String path) {
		URL url = MainTest.class.getClassLoader().getResource(path);
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
	    java.io.FileWriter fw = new java.io.FileWriter(output);
	    fw.write(file);
	    fw.close();
	}
}
