package lk.ijse.etechbackend.dto.chat;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryItemDTO {
    @JsonAlias({"role", "sender"})
    private String sender; // "user" or "assistant" / "bot" / "model"

    @JsonAlias({"content", "text", "message"})
    private String text;
}
