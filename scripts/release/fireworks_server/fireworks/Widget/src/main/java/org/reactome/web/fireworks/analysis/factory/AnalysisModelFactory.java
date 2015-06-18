package org.reactome.web.fireworks.analysis.factory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import org.reactome.web.fireworks.analysis.EntityStatistics;
import org.reactome.web.fireworks.analysis.PathwayBase;
import org.reactome.web.fireworks.analysis.SpeciesFilteredResult;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */

public abstract class AnalysisModelFactory {

    /**
     *
     */
    public interface AnalysisModelFactoryHandler {
        void onPathwaysBaseListRetrieved(SpeciesFilteredResult result);
        void onPathwaysBaseListError(Throwable e);
    }

    public static void retrievePathwayBaseList(String token,
                                               Long species,
                                               String resource,
                                               final AnalysisModelFactoryHandler handler){
        String url = "/AnalysisService/token/" + token + "/filter/species/" + species + "?resource=" + resource;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        requestBuilder.setHeader("Accept", "application/json");
        try {
            requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    try{
                        String json = response.getText();
                        handler.onPathwaysBaseListRetrieved(getSpeciesFilteredResult(json));
                    }catch (Exception ex){
                        handler.onPathwaysBaseListError(ex);
                    }
                }
                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onPathwaysBaseListError(exception);
                }
            });
        }catch (RequestException ex) {
            handler.onPathwaysBaseListError(ex);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    protected interface AnalysisAutoBeanFactory extends AutoBeanFactory {
        AutoBean<PathwayBase> pathwaySummary();
        AutoBean<EntityStatistics> entityStatistics();
        AutoBean<SpeciesFilteredResult> speciesFilteredResult();
    }

    private static <T> T getModelObject(Class<T> cls, String json) throws AnalysisModelException {
        try{
            AutoBeanFactory factory = GWT.create(AnalysisAutoBeanFactory.class);
            AutoBean<T> bean = AutoBeanCodex.decode(factory, cls, json);
            return bean.as();
        }catch (Throwable e){
            throw new AnalysisModelException("Error mapping json string for [" + cls + "]: " + json, e);
        }
    }

    private static SpeciesFilteredResult getSpeciesFilteredResult(String json) throws AnalysisModelException {
        return getModelObject(SpeciesFilteredResult.class, json);
    }

//    @Deprecated
//    private static List<PathwayBase> getPathwayBaseList(String json)
//            throws AnalysisModelException, NullPointerException, IllegalArgumentException, JSONException {
//        List<PathwayBase> rtn = new LinkedList<PathwayBase>();
//        JSONArray array = JSONParser.parseStrict(json).isArray();
//        for (int i = 0; i < array.size(); i++) {
//            JSONObject jsonObject = array.get(i).isObject();
//            rtn.add(getModelObject(PathwayBase.class, jsonObject.toString()));
//        }
//        return rtn;
//    }

}
