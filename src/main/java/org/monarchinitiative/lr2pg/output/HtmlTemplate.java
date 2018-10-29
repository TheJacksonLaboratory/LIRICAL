package org.monarchinitiative.lr2pg.output;


import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;


/**
 * Freemark template to output the results of the analysis as an HTML file.
 */
public class HtmlTemplate {

    public HtmlTemplate(){

    }

    public void testme() throws IOException {
        Configuration cfg = new Configuration(new Version("2.3.23"));
        cfg.setDefaultEncoding("UTF-8");
        ClassLoader classLoader = HtmlTemplate.class.getClassLoader();
        cfg.setClassLoaderForTemplateLoading(classLoader,"");



        Template template = cfg.getTemplate("lrhtml.ftl");

        Map<String, Object> templateData = new HashMap<>();
        templateData.put("msg", "Today is a beautiful day");

        try (StringWriter out = new StringWriter()) {

            template.process(templateData, out);
            System.out.println(out.getBuffer().toString());

            out.flush();
        } catch (TemplateException te) {
            te.printStackTrace();
        }



    }

}
