package fi.hel.integration.sapfpm.model.ID022_FI_TOSITE;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.ArrayList;
import java.util.List;

public class IDOC {
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<E1FIKPF> E1FIKPF = new ArrayList<>();
}
