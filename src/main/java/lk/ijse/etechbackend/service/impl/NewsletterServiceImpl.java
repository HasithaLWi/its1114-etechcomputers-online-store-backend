package lk.ijse.etechbackend.service.impl;

import lk.ijse.etechbackend.dto.newsletter.*;
import lk.ijse.etechbackend.entity.NewsletterCampaign;
import lk.ijse.etechbackend.entity.NewsletterSubscriber;
import lk.ijse.etechbackend.enumiration.SubscriberSource;
import lk.ijse.etechbackend.enumiration.SubscriberStatus;
import lk.ijse.etechbackend.exception.ResourceNotFoundException;
import lk.ijse.etechbackend.repository.NewsletterCampaignRepository;
import lk.ijse.etechbackend.repository.NewsletterSubscriberRepository;
import lk.ijse.etechbackend.service.EmailService;
import lk.ijse.etechbackend.service.NewsletterService;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NewsletterServiceImpl implements NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final NewsletterCampaignRepository campaignRepository;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSubscribers(String search, SubscriberStatus status, int page, int size) {
        log.info("Fetching subscribers list - search: {}, status: {}, page: {}, size: {}",
                search, status, page, size);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "subscribedAt"));
        Page<NewsletterSubscriber> subscriberPage = subscriberRepository.filterSubscribers(
                search != null && !search.isBlank() ? search.trim() : null,
                status,
                pageRequest
        );

        List<SubscriberDTO> dataList = subscriberPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());


        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", subscriberPage.getTotalElements());
        result.put("totalPages", subscriberPage.getTotalPages());
        result.put("currentPage", subscriberPage.getNumber());
        result.put("data", dataList);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriberDTO getSubscriberById(Long id) {
        log.info("Fetching subscriber by ID: {}", id);
        NewsletterSubscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));
        return toDTO(subscriber);
    }

    @Override
    public SubscriberDTO subscribe(SubscriberRequestDTO request, String ipAddress) {
        log.info("Processing newsletter subscription for email: {}", request.getEmail());
        String email = request.getEmail().trim().toLowerCase();

        Optional<NewsletterSubscriber> existingOpt = subscriberRepository.findByEmailIgnoreCase(email);
        NewsletterSubscriber subscriber;

        if (existingOpt.isPresent()) {
            subscriber = existingOpt.get();
            subscriber.setStatus(SubscriberStatus.SUBSCRIBED);
            subscriber.setUnsubscribedAt(null);
            if (request.getName() != null && !request.getName().isBlank()) {
                subscriber.setName(request.getName().trim());
            }
            subscriber.setIpAddress(ipAddress != null ? ipAddress : "127.0.0.1");
        } else {
            subscriber = NewsletterSubscriber.builder()
                    .email(email)
                    .name(request.getName() != null ? request.getName().trim() : null)
                    .status(SubscriberStatus.SUBSCRIBED)
                    .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                    .build();
        }

        NewsletterSubscriber saved = subscriberRepository.save(subscriber);
        log.info("Newsletter subscription confirmed for: {}", email);

        try {
            emailService.sendNewsletterWelcomeEmail(saved.getEmail(), saved.getName());
        } catch (Exception e) {
            log.warn("Could not dispatch welcome email to {}: {}", saved.getEmail(), e.getMessage());
        }

        return toDTO(saved);
    }

    @Override
    public void unsubscribe(String email) {
        log.info("Processing unsubscribe request for: {}", email);
        if (email == null || email.isBlank()) return;

        Optional<NewsletterSubscriber> opt = subscriberRepository.findByEmailIgnoreCase(email.trim());
        if (opt.isPresent()) {
            NewsletterSubscriber s = opt.get();
            s.setStatus(SubscriberStatus.UNSUBSCRIBED);
            s.setUnsubscribedAt(LocalDateTime.now());
            subscriberRepository.save(s);
            log.info("Subscriber marked as UNSUBSCRIBED: {}", email);
        }
    }

    @Override
    public SubscriberDTO updateStatus(Long id, String status) {
        log.info("Updating subscriber ID {} status to: {}", id, status);
        NewsletterSubscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));

        try{
            SubscriberStatus newStatus = SubscriberStatus.valueOf(status.toUpperCase());
            subscriber.setStatus(newStatus);
            if (newStatus == SubscriberStatus.UNSUBSCRIBED) {
                subscriber.setUnsubscribedAt(LocalDateTime.now());
            } else {
                subscriber.setUnsubscribedAt(null);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", status);
            throw new IllegalArgumentException("Invalid status value: " + status);
        }

        NewsletterSubscriber saved = subscriberRepository.save(subscriber);
        return toDTO(saved);
    }

    @Override
    public SubscriberDTO updateSubscriber(Long id, SubscriberRequestDTO request) {
        log.info("Updating subscriber details ID: {}", id);
        NewsletterSubscriber subscriber = subscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found with ID: " + id));

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            subscriber.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getName() != null) {
            subscriber.setName(request.getName().trim());
        }
        if (request.getStatus() != null) {
            subscriber.setStatus(request.getStatus());
        }

        NewsletterSubscriber saved = subscriberRepository.save(subscriber);
        return toDTO(saved);
    }

    @Override
    public void deleteSubscriber(Long id) {
        log.info("Deleting subscriber ID: {}", id);
        if (!subscriberRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subscriber not found with ID: " + id);
        }
        subscriberRepository.deleteById(id);
    }

    @Override
    public int bulkUpdateStatus(List<Long> ids, String status) {
        log.info("Bulk updating status to {} for IDs: {}", status, ids);
        List<NewsletterSubscriber> subscribers = subscriberRepository.findAllById(ids);
        SubscriberStatus newStatus;
        try{
            newStatus = SubscriberStatus.valueOf(status.toUpperCase());
            log.info("New status: {}", newStatus);

        }catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }

        if(newStatus == null){
            throw new IllegalArgumentException("Invalid status value: " + status);
        }

        for (NewsletterSubscriber s : subscribers) {
            s.setStatus(newStatus);
            if (newStatus == SubscriberStatus.UNSUBSCRIBED) {
                s.setUnsubscribedAt(LocalDateTime.now());
            } else {
                s.setUnsubscribedAt(null);
            }
        }
        subscriberRepository.saveAll(subscribers);
        return subscribers.size();
    }

    @Override
    public int bulkDelete(List<Long> ids) {
        log.info("Bulk deleting subscribers for IDs: {}", ids);
        List<NewsletterSubscriber> subscribers = subscriberRepository.findAllById(ids);
        subscriberRepository.deleteAll(subscribers);
        return subscribers.size();
    }

    @Override
    public CampaignDTO sendCampaign(CampaignSendRequestDTO request) {
        log.info("Dispatching email broadcast campaign: {}", request.getSubject());

        List<NewsletterSubscriber> activeSubscribers = subscriberRepository.findByStatus(SubscriberStatus.SUBSCRIBED);
        int recipientCount = activeSubscribers.size();

        String campaignId = "camp_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        NewsletterCampaign campaign = NewsletterCampaign.builder()
                .id(campaignId)
                .subject(request.getSubject().trim())
                .preheader(request.getPreheader())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL_NEWS")
                .targetSegment(request.getTargetSegment() != null ? request.getTargetSegment() : "ALL_ACTIVE")
                .contentHtml(request.getContentHtml())
                .recipientsCount(recipientCount)
                .status("DELIVERED")
                .authorName(request.getAuthorName() != null ? request.getAuthorName() : "Admin Team")
                .build();

        NewsletterCampaign saved = campaignRepository.save(campaign);

        LocalDateTime now = LocalDateTime.now();
        for (NewsletterSubscriber s : activeSubscribers) {
            s.setLastCampaignSentAt(now);
        }
        subscriberRepository.saveAll(activeSubscribers);

        // Asynchronously broadcast to all active subscribers via Gmail SMTP
        CompletableFuture.runAsync(() -> {
            log.info("Starting background broadcast of campaign {} to {} subscribers", campaignId, activeSubscribers.size());
            for (NewsletterSubscriber sub : activeSubscribers) {
                try {
                    emailService.sendCampaignEmail(
                        sub.getEmail(),
                        sub.getName(),
                        request.getSubject(),
                        request.getPreheader(),
                        request.getContentHtml()
                    );
                } catch (Exception e) {
                    log.error("Failed to deliver campaign email to {}: {}", sub.getEmail(), e.getMessage());
                }
            }
            log.info("Finished background broadcast for campaign {}", campaignId);
        });

        log.info("Campaign {} broadcast dispatched to {} active subscribers", campaignId, recipientCount);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignDTO> getAllCampaigns() {
        log.info("Fetching all marketing campaigns");
        return campaignRepository.findAllByOrderBySentAtDesc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }



    private SubscriberDTO toDTO(NewsletterSubscriber s) {
        return SubscriberDTO.builder()
                .id(s.getId())
                .email(s.getEmail())
                .name(s.getName())
                .status(s.getStatus())
                .subscribedAt(s.getSubscribedAt())
                .unsubscribedAt(s.getUnsubscribedAt())
                .lastCampaignSentAt(s.getLastCampaignSentAt())
                .build();
    }

    private CampaignDTO toDTO(NewsletterCampaign c) {
        return CampaignDTO.builder()
                .id(c.getId())
                .subject(c.getSubject())
                .preheader(c.getPreheader())
                .category(c.getCategory())
                .targetSegment(c.getTargetSegment())
                .contentHtml(c.getContentHtml())
                .sentAt(c.getSentAt())
                .recipientsCount(c.getRecipientsCount())
                .status(c.getStatus())
                .authorName(c.getAuthorName())
                .build();
    }
}
