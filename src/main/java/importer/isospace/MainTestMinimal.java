package importer.isospace;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

public class MainTestMinimal {
	
	public static void main(String[] args) throws UIMAException, CASRuntimeException, IOException {
		File inputfolder = getFileFromURL("spaceeval/Training/RFC");
		
        CollectionReaderDescription reader = createReaderDescription(
                Minimal.class,
                IsoSpaceImporter.PARAM_SOURCE_LOCATION, inputfolder,
                IsoSpaceImporter.PARAM_PATTERNS,"[+]**/*.xml");

		Iterator<JCas> iter = new JCasIterable(reader).iterator();
		while(iter.hasNext()) {
			JCas jcas = iter.next();
			jcas.setDocumentLanguage("en");

			System.out.println(XmlFormatter.getPrettyString(jcas.getCas()));

		}

        System.out.println("Import Finished!");
	}

	private static File getFileFromURL(String path) {
		URL url = MainTestMinimal.class.getClassLoader().getResource(path);
		File file = null;
		try {
			file = new File(url.toURI());
		} catch (URISyntaxException e) {
			file = new File(url.getPath());
		} finally {
			return file;
		}
	}

}
