package uk.ac.ebi.reactome.core.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.List;

/**
 * Custom Tag to duplicate name in request with parameters List of Strings
 *
 * @author Florian Korninger (fkorn@ebi.ac.uk)
 * @version 1.0
 */
public class Param extends SimpleTagSupport {

    private String name;
    private List<String> value;

    @Override
    public void doTag() throws JspException {
        JspWriter out = getJspContext().getOut();
        String url = "";
        if (value != null) {
            boolean first = true;
            for (String val : value) {
                if (first) {
                    url = "&amp;" + name + "=" + val.replaceAll(" ", "+");
                    first = false;
                }
                else {
                    url = url + "&amp;" + name + "=" + val.replaceAll(" ", "+");
                }
            }
        }

        try {
            out.write(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }
}
