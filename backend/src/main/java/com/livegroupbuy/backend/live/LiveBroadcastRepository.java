package com.livegroupbuy.backend.live;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveBroadcastRepository extends JpaRepository<LiveBroadcast, Long> {
    List<LiveBroadcast> findAllByOrderByIdDesc();
    List<LiveBroadcast> findByStatus(LiveStatus status);
}
