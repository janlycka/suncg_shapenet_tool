package importer.isospace;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;

import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.corenlp.CoreNlpSegmenter;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

public class MainTest2 {
	
	public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException {
		
		JCas jcas = JCasFactory.createText("The old book is on the chair next to the desk.","en");
        
        AggregateBuilder builder = new AggregateBuilder();
        builder.add(createEngineDescription(CoreNlpSegmenter.class));
        

        	SimplePipeline.runPipeline(jcas,builder.createAggregate());
            	System.out.println(XmlFormatter.getPrettyString(jcas.getCas()));
            	
            	writeXml2File(XmlFormatter.getPrettyString(jcas.getCas()), "D:/tea"+ ".xml");
            	//writeJson2File(jcas, outputfolder + DocumentMetaData.get(jcas).getDocumentId() + ".json");
        

        
        
	}

	private static void writeXml2File(String file, String output) throws IOException {
	    java.io.FileWriter fw = new java.io.FileWriter(output);
	    fw.write(file);
	    fw.close();
	}
}
