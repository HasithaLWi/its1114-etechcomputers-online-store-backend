package lk.ijse.etechbackend.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicySectionDTO {
    private String id;
    private String sectionTitle;
    private String heading;
    private String sectionContent;
    private String content;
    private String bulletPoints;
}
