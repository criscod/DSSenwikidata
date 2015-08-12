import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;


/**
 * @author csarasua
 */
public class ItemLooker {


    String itemWikidataID;

    public ItemLooker(String itemWikidataID)
    {
        this.itemWikidataID = itemWikidataID;
    }

    public String getItemWikidataID() {
        return itemWikidataID;
    }

    public void setItemWikidataID(String itemWikidataID) {
        this.itemWikidataID = itemWikidataID;
    }

    public boolean isItemConnectedToDSS()
    {

        boolean resultMethod=false;
        //Look the item via Linked Data dereferencable
        HttpClient client = new DefaultHttpClient();


        //HttpGet getFeedRecentChanges = new HttpGet("https://www.wikidata.org/entity/"+itemWikidataID);
        HttpGet getItemRDF = new HttpGet("https://www.wikidata.org/wiki/Special:EntityData/"+itemWikidataID+".rdf?flavor=full");

        getItemRDF.setHeader("Accept", "application/rdf+xml");

        HttpResponse response = null;

            try {
                response = client.execute(getItemRDF);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                Header header = response.getEntity().getContentType();

                InputStream rdfItemData = response.getEntity().getContent();
                System.out.println(response.getEntity().getContent().toString());
                Model responseModel = ModelFactory.createDefaultModel();
                responseModel.read(rdfItemData, "RDF/XML");

                String queryString = "SELECT ?item WHERE {?item ?p <https://www.wikidata.org/wiki/Q10313>}";

                QueryExecution qe = QueryExecutionFactory.create(queryString, responseModel);
                ResultSet results = qe.execSelect();
                QuerySolution qs = null;

               if(results.hasNext()){
                    resultMethod=true;

                }


            }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (Throwable t)
            {t.printStackTrace();}
        finally {
                return resultMethod;
            }



    }

    public boolean hasP31Statement()
    {

        boolean resultMethod = false;
        //Look the item via Linked Data dereferencable
        HttpClient client = new DefaultHttpClient();

        HttpGet getFeedRecentChanges = new HttpGet("https://www.wikidata.org/entity/"+itemWikidataID);

        getFeedRecentChanges.setHeader("Accept", "application/rdf+xml");

        HttpResponse response = null;

        try {
            response = client.execute(getFeedRecentChanges);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                Header header = response.getEntity().getContentType();
                System.out.println(header.getValue());
                InputStream rdfItemData = response.getEntity().getContent();
                Model responseModel = ModelFactory.createDefaultModel();
                responseModel.read(rdfItemData, "RDF/XML");

                String queryString = "SELECT ?item WHERE {?item ?p <https://www.wikidata.org/entity/Q10313>}";

                QueryExecution qe = QueryExecutionFactory.create(queryString, responseModel);
                ResultSet results = qe.execSelect();
                QuerySolution qs = null;

                if(results.hasNext()){
                    resultMethod=true;

                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            return resultMethod;
        }


    }



}
