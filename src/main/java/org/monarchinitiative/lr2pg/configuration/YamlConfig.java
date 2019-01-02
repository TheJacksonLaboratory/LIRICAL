package org.monarchinitiative.lr2pg.configuration;

//import org.openrdf.rio.LanguageHandlerRegistry;

import java.util.Map;

public class YamlConfig {

    private Map<String,String> analysis;
    private Map<String,String> output;
    private String[] hpoIds;

    public void setAnalysis(Map<String,String> an) {
        this.analysis=an;
    }
    public Map<String,String> getAnalysis(){
        return analysis;
    }
    public void setOutput(Map<String,String> out) {
        this.output=out;
    }
    public Map<String,String> getOutput() {
        return output;
    }
    public void setHpoIds(String[] ids) {
        this.hpoIds=ids;
    }
    public String[] getHpoIds() {
        return hpoIds;
    }

    //LanguageHandlerRegistry a;
}
