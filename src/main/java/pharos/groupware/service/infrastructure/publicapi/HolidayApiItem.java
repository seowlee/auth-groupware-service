package pharos.groupware.service.infrastructure.publicapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HolidayApiItem {
    @JacksonXmlProperty(localName = "dateKind")
    private String dateKind;

    @JacksonXmlProperty(localName = "dateName")
    private String dateName;

    @JacksonXmlProperty(localName = "isHoliday")
    private String isHoliday; // "Y" / "N"

    @JacksonXmlProperty(localName = "locdate")
    private String locdate;   // e.g. 20251003 (yyyymmdd)

    @JacksonXmlProperty(localName = "seq")
    private String seq;
}
