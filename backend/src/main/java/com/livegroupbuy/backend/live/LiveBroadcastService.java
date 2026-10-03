package com.livegroupbuy.backend.live;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LiveBroadcastService {

    private final LiveBroadcastRepository repository;

    public LiveBroadcastService(LiveBroadcastRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LiveBroadcastResponse> findAll() {
        return repository.findAllByOrderByIdDesc().stream().map(LiveBroadcastResponse::from).toList();
    }

    @Transactional
    public LiveBroadcastResponse create(LiveBroadcastRequest request) {
        LiveBroadcast broadcast = toEntity(request, LiveStatus.PENDING, null);
        return LiveBroadcastResponse.from(repository.save(broadcast));
    }

    @Transactional
    public List<LiveBroadcastResponse> importLegacy(List<LiveBroadcastRequest> requests) {
        if (repository.count() > 0) return findAll();
        requests.stream().limit(100).map(request -> toEntity(request, parseStatus(request.status()), request.streamKey()))
                .forEach(repository::save);
        return findAll();
    }

    @Transactional
    public LiveBroadcastResponse updateStatus(Long id, LiveStatusRequest request) {
        LiveBroadcast broadcast = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));
        LiveStatus status = parseStatus(request.status());
        if (status == LiveStatus.REJECTED && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new IllegalArgumentException("반려 사유를 입력해 주세요.");
        }
        broadcast.updateStatus(status, request.rejectionReason() == null ? null : request.rejectionReason().trim());
        return LiveBroadcastResponse.from(broadcast);
    }

    private LiveBroadcast toEntity(LiveBroadcastRequest request, LiveStatus status, String requestedStreamKey) {
        String streamKey = requestedStreamKey == null || requestedStreamKey.isBlank()
                ? "live-" + UUID.randomUUID().toString().substring(0, 12)
                : requestedStreamKey.trim();
        return new LiveBroadcast(
                request.title().trim(), request.seller().trim(), request.productId(), request.productTitle().trim(),
                request.emoji().trim(), blankToNull(request.imageUrl()), status, request.scheduledAt().trim(),
                request.viewers() == null ? 0 : Math.max(0, request.viewers()), request.description().trim(),
                blankToNull(request.rejectionReason()), streamKey, request.startedAt()
        );
    }

    private LiveStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return LiveStatus.PENDING;
        try {
            return LiveStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("올바르지 않은 방송 상태입니다.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
