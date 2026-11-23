package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.dto.Dto.*;
import com.lostfound.repo.ClaimEvidenceRepository;
import com.lostfound.repo.ItemMatchRepository;
import com.lostfound.repo.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/** Entity -> DTO conversion. Sensitive fields are only included when the viewer is staff or the owner. */
@Component
@RequiredArgsConstructor
public class Mapper {
    private final ClaimEvidenceRepository evidenceRepo;
    private final ItemMatchRepository matchRepo;
    private final StatusHistoryRepository historyRepo;

    public static List<String> images(BaseItem i) {
        if (i.getImagePaths() == null || i.getImagePaths().isBlank()) return List.of();
        return Arrays.stream(i.getImagePaths().split(",")).filter(s -> !s.isBlank()).map(s -> "/uploads/" + s).toList();
    }

    public static boolean canSeeSensitive(User viewer, BaseItem i) {
        return viewer != null && (viewer.staff() || viewer.getId().equals(i.getReporter().getId()));
    }

    public ItemResponse item(BaseItem i, User viewer, boolean detail) {
        boolean lost = i instanceof LostItem;
        boolean sensitive = canSeeSensitive(viewer, i);
        String type = lost ? "LOST" : "FOUND";
        ItemResponse.ItemResponseBuilder b = ItemResponse.builder()
                .id(i.getId()).type(type).name(i.getName())
                .categoryId(i.getCategory().getId()).categoryName(i.getCategory().getName())
                .description(i.getDescription()).date(i.getEventDate()).location(i.getLocation())
                .color(i.getColor()).brand(i.getBrand()).verified(i.isVerified())
                .images(images(i)).createdAt(i.getCreatedAt());
        if (lost) {
            LostItem l = (LostItem) i;
            b.status(l.getStatus().name());
            if (sensitive) b.contactInfo(l.getContactInfo());
        } else {
            FoundItem f = (FoundItem) i;
            b.status(f.getStatus().name()).handoverLocation(f.getHandoverLocation());
        }
        if (sensitive) {
            b.identifyingCharacteristics(i.getIdentifyingCharacteristics())
             .reporterId(i.getReporter().getId()).reporterName(i.getReporter().getFullName());
        }
        if (detail && sensitive) {
            b.history(historyRepo.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(type, i.getId()).stream().map(this::history).toList());
            boolean showMatches = lost || (viewer != null && viewer.staff());
            if (showMatches) {
                List<ItemMatch> ms = lost ? matchRepo.findByLostItemOrderByScoreDesc((LostItem) i) : matchRepo.findByFoundItemOrderByScoreDesc((FoundItem) i);
                b.matches(ms.stream().map(this::match).toList());
            }
        }
        return b.build();
    }

    public StatusHistoryResponse history(StatusHistory h) {
        return new StatusHistoryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedBy() == null ? "System" : h.getChangedBy().getFullName(), h.getNote(), h.getCreatedAt());
    }

    /** Matches shown without sensitive details; the score is an indication, not proof of ownership. */
    public MatchResponse match(ItemMatch m) {
        return new MatchResponse(m.getId(), m.getScore(), m.getStatus().name(), item(m.getLostItem(), null, false), item(m.getFoundItem(), null, false));
    }

    public ClaimResponse claim(Claim c, boolean staff) {
        ClaimResponse.ClaimResponseBuilder b = ClaimResponse.builder()
                .id(c.getId()).foundItemId(c.getFoundItem().getId()).foundItemName(c.getFoundItem().getName())
                .foundItemStatus(c.getFoundItem().getStatus().name())
                .claimantId(c.getClaimant().getId()).claimantName(c.getClaimant().getFullName())
                .ownershipExplanation(c.getOwnershipExplanation())
                .distinguishingCharacteristics(c.getDistinguishingCharacteristics())
                .additionalDetails(c.getAdditionalDetails())
                .status(c.getStatus().name()).ownershipVerified(c.isOwnershipVerified())
                .decisionReason(c.getDecisionReason()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt());
        if (c.getId() != null) b.evidence(evidenceRepo.findByClaimOrderByCreatedAtAsc(c).stream().map(ClaimEvidence::getDescription).toList());
        if (staff) {
            b.claimantEmail(c.getClaimant().getEmail()).foundItemIdentifyingCharacteristics(c.getFoundItem().getIdentifyingCharacteristics());
        }
        return b.build();
    }

    public RecoveryResponse recovery(Recovery r) {
        return new RecoveryResponse(r.getId(), r.getClaim().getId(), r.getClaim().getFoundItem().getName(), r.getClaim().getClaimant().getFullName(),
                r.getStatus().name(), r.getScheduledAt(), r.getHandoverDate(), r.getLocation(),
                r.getHandledBy() == null ? null : r.getHandledBy().getFullName(), r.getConfirmationCode());
    }

    public NotificationResponse notification(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getMessage(), n.isReadStatus(), n.getRelatedType(), n.getRelatedId(), n.getCreatedAt());
    }

    public ActivityLogResponse log(ActivityLog l) {
        return new ActivityLogResponse(l.getId(), l.getUser() == null ? null : l.getUser().getFullName(), l.getAction(), l.getEntityType(), l.getEntityId(), l.getDetails(), l.isSuspicious(), l.getCreatedAt());
    }
}
