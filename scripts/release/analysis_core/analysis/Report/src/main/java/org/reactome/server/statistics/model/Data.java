package org.reactome.server.statistics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

/**
 * Part of JSON-Object for chart
 */
public class Data {
    @JsonProperty("xFormat")
    private final String dateFormat = "%Y-%m-%d %H:%M:%S";
    @JsonProperty("json")
    private List<Object> jsonObjectList = new ArrayList<Object>();
    @JsonProperty("names")
    private Map<String, String> names;
    @JsonProperty("keys")
    private Keys keys;


    public String getDateFormat() {
        return dateFormat;
    }

    public List<Object> getJson() {
        return jsonObjectList;
    }

    public void setJsonObjectList(List<Object> jsonObjectList) {
        this.jsonObjectList = jsonObjectList;
    }

    public void addObject(Object object) {
        this.jsonObjectList.add(object);
    }

    public Map<String, String> getNames() {
        return names;
    }

    public void setNames(Map<String, String> names) {
        this.names = names;
    }

    public Keys getKeys() {
        return keys;
    }

    public void setKeys(Keys keys) {
        this.keys = keys;
    }

    @Override
    public String toString() {
        return "Data{" +
                "dateFormat='" + dateFormat + '\'' +
                ", jsonObjectList=" + jsonObjectList +
                ", names=" + names +
                ", keys=" + keys +
                '}';
    }
}