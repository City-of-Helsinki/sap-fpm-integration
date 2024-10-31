package fi.hel.integration.sapfpm.model.ID022_FI_TOSITE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// ignoring unknown allows reading only the values that will be actually processed further
@JsonIgnoreProperties(ignoreUnknown = true)
public final class E1FIKPF {
    public String BUKRS;
    //public String BELNR;
    public String GJAHR;
    //public String BLART;
    public String BLDAT;
    public String BUDAT;
    public String MONAT;
}
