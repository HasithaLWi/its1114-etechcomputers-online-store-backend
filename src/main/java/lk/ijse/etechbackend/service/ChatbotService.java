package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.chat.ChatMessageRequestDTO;
import lk.ijse.etechbackend.dto.chat.ChatMessageResponseDTO;
import lk.ijse.etechbackend.dto.chat.ChatStatusResponseDTO;
import org.springframework.security.core.Authentication;

public interface ChatbotService {
    ChatMessageResponseDTO processMessage(ChatMessageRequestDTO request, Authentication authentication);
    ChatStatusResponseDTO getChatbotStatus();
}
