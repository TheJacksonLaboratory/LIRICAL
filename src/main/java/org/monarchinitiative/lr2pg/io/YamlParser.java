package org.monarchinitiative.lr2pg.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.monarchinitiative.lr2pg.configuration.YamlConfig;

import java.io.File;


public class YamlParser {

    private YamlConfig yconfig;


    public YamlParser(String path) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            yconfig = mapper.readValue(new File(path), YamlConfig.class);
            System.out.println(ReflectionToStringBuilder.toString(yconfig, ToStringStyle.MULTI_LINE_STYLE));
        } catch (Exception e) {
            yconfig=null;
            e.printStackTrace();
        }
    }

    public String getMvStorePath() {
        if (yconfig.getAnalysis().containsKey("mvstore")) {
            return yconfig.getAnalysis().get("mvstore");
        } else {
            return "?"; // todo
        }
    }


}
