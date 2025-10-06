package com.webchat.webchat.repository;

import com.webchat.webchat.entity.VideoCall;
import com.webchat.webchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoCallRepository extends JpaRepository<VideoCall, UUID> {
    
    @Query("SELECT vc FROM VideoCall vc WHERE (vc.caller = :user OR vc.callee = :user) AND vc.status IN ('INITIATED', 'RINGING', 'ACCEPTED')")
    Optional<VideoCall> findActiveCallByUser(@Param("user") User user);
    
    @Query("SELECT vc FROM VideoCall vc WHERE vc.callee = :callee AND vc.status = 'RINGING'")
    List<VideoCall> findIncomingCallsByCallee(@Param("callee") User callee);
    
    @Query("SELECT vc FROM VideoCall vc WHERE (vc.caller = :user OR vc.callee = :user) ORDER BY vc.createdAt DESC")
    List<VideoCall> findCallHistoryByUser(@Param("user") User user);
}