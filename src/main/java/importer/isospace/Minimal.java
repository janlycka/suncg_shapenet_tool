package importer.isospace;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
//import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.resources.CompressionUtils;
import org.texttechnologylab.annotation.semaf.isobase.Entity;
import org.texttechnologylab.annotation.semaf.isospace.*;
import org.texttechnologylab.annotation.semaf.meta.MetaLink;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;



public class Minimal extends JCasResourceCollectionReader_ImplBase {
	
    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);
    }
	
    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        setDocumentID(aJCas, res.getLocation());
        addEntity(aJCas);
    }

    private void addEntity(JCas aJCas){
        SpatialEntity entity = new SpatialEntity(aJCas);
        entity.addToIndexes();
    }

	private void setDocumentID(JCas aJCas, String path) {
		String[] pathSplit = path.split("/");
		String docname = pathSplit[pathSplit.length-1];
		DocumentMetaData m = DocumentMetaData.get(aJCas);
		m.setDocumentId(docname);
		m.setDocumentTitle(docname);
	}

}
