package lk.ijse.etechbackend.dto.analytics;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportRequestDTO {
    private String format; // PDF, XLSX, CSV
    private String from;   // YYYY-MM-DD
    private String to;     // YYYY-MM-DD
    private String branchId; // ALL or specific branch ID
}
