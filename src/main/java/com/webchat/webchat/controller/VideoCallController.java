package com.webchat.webchat.controller;

import com.webchat.webchat.dto.VideoCallDTO;
import com.webchat.webchat.dto.VideoCallSignalDTO;
import com.webchat.webchat.entity.VideoCall;
import com.webchat.webchat.service.videocall.VideoCallService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/video-call")
@RequiredArgsConstructor
public class VideoCallController {
    
    private final VideoCallService videoCallService;

    @PostMapping("/initiate")
    public VideoCallDTO initiateCall(@RequestBody InitiateCallRequest request) {
        try {
            System.out.println("📞 VideoCallController.initiateCall called with: " + request.getCallerId() + " -> " + request.getCalleeId());
            VideoCall call = videoCallService.initiateCall(request.getCallerId(), request.getCalleeId());
            System.out.println("📞 VideoCall created: " + call.getId());
            VideoCallDTO dto = convertToDTO(call);
            System.out.println("📞 DTO created: " + dto);
            return dto;
        } catch (Exception e) {
            System.err.println("❌ Error in initiateCall: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @PostMapping("/{callId}/end")
    public void endCall(@PathVariable UUID callId) {
        videoCallService.endCallById(callId);
    }
    
    @GetMapping("/history")
    public List<VideoCallDTO> getCallHistory(@RequestParam UUID userId) {
        return videoCallService.getCallHistory(userId);
    }
    
    @GetMapping("/active")
    public Optional<VideoCallDTO> getActiveCall(@RequestParam UUID userId) {
        return videoCallService.getActiveCall(userId);
    }
    
    @MessageMapping("/video-call.signal")
    public void handleVideoCallSignal(@Payload VideoCallSignalDTO signal) {
        videoCallService.handleCallSignal(signal);
    }
    
    @MessageMapping("/video-call/end")
    public void handleEndCall(@Payload EndCallRequest request) {
        videoCallService.endCallById(request.getCallId());
    }
    
    @MessageMapping("/video-call/accept")
    public void handleAcceptCall(@Payload AcceptCallRequest request) {
        videoCallService.acceptCall(request.getCallId());
    }
    
    @MessageMapping("/video-call/reject")
    public void handleRejectCall(@Payload RejectCallRequest request) {
        videoCallService.rejectCall(request.getCallId());
    }
    
    @Data
    public static class InitiateCallRequest {
        private UUID callerId;
        private UUID calleeId;
    }
    
    @Data
    public static class EndCallRequest {
        private UUID callId;
    }
    
    @Data
    public static class AcceptCallRequest {
        private UUID callId;
    }
    
    @Data
    public static class RejectCallRequest {
        private UUID callId;
    }
    
    private VideoCallDTO convertToDTO(VideoCall videoCall) {
        return new VideoCallDTO(
                videoCall.getId(),
                videoCall.getCaller().getIdUser(),
                videoCall.getCaller().getFirstName() + " " + videoCall.getCaller().getLastName(),
                videoCall.getCaller().getAvatar(),
                videoCall.getCallee().getIdUser(),
                videoCall.getCallee().getFirstName() + " " + videoCall.getCallee().getLastName(),
                videoCall.getCallee().getAvatar(),
                videoCall.getStatus().name(),
                videoCall.getCreatedAt(),
                videoCall.getStartedAt(),
                videoCall.getEndedAt(),
                videoCall.getDurationSeconds()
        );
    }
}